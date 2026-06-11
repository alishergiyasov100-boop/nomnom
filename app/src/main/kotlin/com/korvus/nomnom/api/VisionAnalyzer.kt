package com.korvus.nomnom.api

import android.util.Base64
import com.korvus.nomnom.data.AnalysisResult
import com.korvus.nomnom.data.Component
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
Если фото несколько — это разные ракурсы ОДНОГО блюда (помогает оценить объём).

Разложи блюдо на основные компоненты (например: рис, масло, мясо, соус, овощи).
Для каждого компонента оцени массу в граммах. Это критично — общие ккал суммируются из компонентов.
Если есть скрытое масло/сахар/майонез — отдельным компонентом.

Если в кадре есть предмет для масштаба (ложка ~14см, монета ~2см, рука) — используй его.

Верни СТРОГО валидный JSON, без markdown, без ```. Поля:
{
  "is_food": true/false,
  "dish": "Название блюда на русском",
  "components": [
    {"name": "рис варёный", "grams": 180, "kcal": 234, "protein_g": 5, "fat_g": 0, "carbs_g": 51},
    {"name": "масло сливочное", "grams": 15, "kcal": 112, "protein_g": 0, "fat_g": 12, "carbs_g": 0}
  ],
  "kcal": <сумма kcal по компонентам>,
  "protein_g": <сумма protein_g>,
  "fat_g": <сумма fat_g>,
  "carbs_g": <сумма carbs_g>,
  "confidence": "high"/"medium"/"low",
  "comment": "Аппетитный комментарий 2-3 предложения. Опиши вкус и текстуру так, чтобы захотелось съесть. Эмодзи 1-2 шт. Если калорийность зашкаливает — можешь дерзко подколоть."
}

Если не еда — is_food=false, kcal=0, components=[], dish="не еда".
Точность важна. Не теоретическая порция — а та что НА фото."""

class VisionAnalyzer(
    private val baseUrl: String,
    private val model: String,
    private val keys: List<String> = emptyList(),
    private val startIdx: Int = 0,
    private val onAdvance: suspend () -> Unit = {},
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyze(images: List<ByteArray>, mimeType: String = "image/jpeg"): AnalysisResult =
        withContext(Dispatchers.IO) {
            require(images.isNotEmpty()) { "no images" }
            val isMistral = baseUrl.contains("mistral", ignoreCase = true)

            val imageParts = images.map { bytes ->
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                val dataUri = "data:$mimeType;base64,$b64"
                buildJsonObject {
                    put("type", "image_url")
                    if (isMistral) put("image_url", dataUri)
                    else put("image_url", buildJsonObject { put("url", dataUri) })
                }
            }

            val userText = if (images.size > 1)
                "${images.size} ракурса одного блюда. Что это? Разложи на компоненты, оцени калории."
            else
                "Что это за блюдо? Разложи на компоненты, оцени калории."

            val payload = buildJsonObject {
                put("model", model)
                put("temperature", 0.4)
                put("max_tokens", 900)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", SYSTEM_PROMPT)
                    })
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", buildJsonArray {
                            imageParts.forEach { add(it) }
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", userText)
                            })
                        })
                    })
                })
            }

            val url = baseUrl.trimEnd('/') + "/chat/completions"
            val bodyBytes = payload.toString().toRequestBody(JSON_MEDIA)

            val raw = rotateKeys(keys, startIdx, onAdvance) { key ->
                val reqBuilder = Request.Builder().url(url)
                    .post(bodyBytes)
                    .header("Content-Type", "application/json")
                if (key.isNotBlank()) reqBuilder.header("Authorization", "Bearer $key")
                val resp = client.newCall(reqBuilder.build()).execute()
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw RotatableHttpException(resp.code, body.take(300))
                json.parseToJsonElement(body).jsonObject
                    .get("choices")?.jsonArray?.get(0)?.jsonObject
                    ?.get("message")?.jsonObject
                    ?.get("content")?.jsonPrimitive?.content
                    ?: throw IOException("unexpected response: ${body.take(300)}")
            }

            parseResult(raw)
        }

    private fun parseResult(raw: String): AnalysisResult {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val obj = json.parseToJsonElement(cleaned).jsonObject
        val components = obj["components"]?.jsonArray?.mapNotNull { el ->
            val o = el.jsonObject
            Component(
                name = o["name"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                grams = (o["grams"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
                kcal = (o["kcal"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
                proteinG = (o["protein_g"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
                fatG = (o["fat_g"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
                carbsG = (o["carbs_g"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
            )
        }.orEmpty()
        // Если модель не дала компонентов — синтезируем один из общих чисел.
        val totalKcal = (obj["kcal"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0
        val effectiveComponents = if (components.isEmpty() && totalKcal > 0) {
            listOf(Component(
                name = obj["dish"]?.jsonPrimitive?.content ?: "блюдо",
                grams = 0,
                kcal = totalKcal,
                proteinG = (obj["protein_g"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
                fatG = (obj["fat_g"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
                carbsG = (obj["carbs_g"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
            ))
        } else components
        return AnalysisResult(
            dish = obj["dish"]?.jsonPrimitive?.content ?: "блюдо",
            kcal = totalKcal,
            proteinG = (obj["protein_g"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
            fatG = (obj["fat_g"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
            carbsG = (obj["carbs_g"]?.jsonPrimitive?.content ?: "0").toIntOrNull() ?: 0,
            comment = obj["comment"]?.jsonPrimitive?.content ?: "",
            confidence = obj["confidence"]?.jsonPrimitive?.content ?: "medium",
            isFood = obj["is_food"]?.jsonPrimitive?.content?.toBoolean() ?: true,
            components = effectiveComponents,
        )
    }

    private fun JsonObject.get(key: String) = this[key]
}
