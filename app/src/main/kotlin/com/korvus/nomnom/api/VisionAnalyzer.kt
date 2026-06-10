package com.korvus.nomnom.api

import android.util.Base64
import com.korvus.nomnom.data.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

private const val SYSTEM_PROMPT = """Ты — еда-эксперт NomNom. По фото оцениваешь блюдо.

Верни СТРОГО валидный JSON, без markdown, без ```. Поля:
{
  "is_food": true/false,
  "dish": "Название блюда на русском",
  "kcal": <целое число калорий в порции>,
  "protein_g": <грамм белка>,
  "fat_g": <грамм жира>,
  "carbs_g": <грамм углеводов>,
  "confidence": "high"/"medium"/"low",
  "comment": "Аппетитный комментарий 2-3 предложения. Опиши вкус и текстуру так, чтобы захотелось съесть. Эмодзи 1-2 шт. Если калорийность зашкаливает — можешь дерзко подколоть. Если блюдо полезное — отметь это с теплом."
}

Если на фото не еда — is_food=false, kcal=0, dish="не еда", comment объясни что видишь.
Будь точен с калориями — оценивай порцию на тарелке, не теоретически."""

class VisionAnalyzer(
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String = "",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyze(imageBytes: ByteArray, mimeType: String = "image/jpeg"): AnalysisResult =
        withContext(Dispatchers.IO) {
            val b64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val dataUri = "data:$mimeType;base64,$b64"

            val payload = buildJsonObject {
                put("model", model)
                put("temperature", 0.4)
                put("max_tokens", 600)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", SYSTEM_PROMPT)
                    })
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", buildJsonArray {
                            add(buildJsonObject {
                                put("type", "image_url")
                                put("image_url", buildJsonObject { put("url", dataUri) })
                            })
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", "Что это за блюдо? Оцени калории.")
                            })
                        })
                    })
                })
            }

            val url = baseUrl.trimEnd('/') + "/chat/completions"
            val reqBuilder = Request.Builder().url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .header("Content-Type", "application/json")
            if (apiKey.isNotBlank()) reqBuilder.header("Authorization", "Bearer $apiKey")

            val resp = client.newCall(reqBuilder.build()).execute()
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}: ${body.take(300)}")
            }

            val raw = json.parseToJsonElement(body).jsonObject
                .get("choices")?.jsonArray?.get(0)?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.content
                ?: throw IOException("unexpected response: ${body.take(300)}")

            parseResult(raw)
        }

    private fun parseResult(raw: String): AnalysisResult {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val obj = json.parseToJsonElement(cleaned).jsonObject
        return AnalysisResult(
            dish = obj["dish"]?.jsonPrimitive?.content ?: "блюдо",
            kcal = (obj["kcal"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
            proteinG = (obj["protein_g"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
            fatG = (obj["fat_g"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
            carbsG = (obj["carbs_g"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
            comment = obj["comment"]?.jsonPrimitive?.content ?: "",
            confidence = obj["confidence"]?.jsonPrimitive?.content ?: "medium",
            isFood = obj["is_food"]?.jsonPrimitive?.content?.toBoolean() ?: true,
        )
    }

    private fun JsonObject.get(key: String) = this[key]
}
