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
private val json = Json {
    ignoreUnknownKeys = true
    prettyPrint = false
    isLenient = true            // single-quoted keys, unquoted numbers/bools
    coerceInputValues = true     // null → default
    allowSpecialFloatingPointValues = true
    allowTrailingComma = true
}

private const val SYSTEM_PROMPT = """Ты — еда-эксперт NomNom. По фото оцениваешь блюдо.
Если фото несколько — это разные ракурсы ОДНОГО блюда (помогает оценить объём).

ОЧЕНЬ ВАЖНО — сначала ОПИСАНИЕ, потом классификация:
1. Сначала внимательно опиши форму, цвет, текстуру каждого элемента (поле "description").
2. Только потом по ОПИСАНИЮ определи блюдо (поле "dish"). Не угадывай по первому впечатлению.
3. Если видишь хлеб/булку с мясом сверху — это бутерброд/сэндвич/хычин/чебурек, НЕ эклер. Эклер — это маленькое продолговатое заварное пирожное с кремом и шоколадной глазурью.
4. Если сомневаешься — ставь confidence='low' и выбирай более общее название ("выпечка с начинкой") вместо узкого ("эклер").

Разложи блюдо на основные компоненты (например: хлеб, мясо, сыр, соус, овощи).
Для каждого компонента оцени массу в граммах. Это критично — общие ккал суммируются из компонентов.
Если есть скрытое масло/сахар/майонез — отдельным компонентом.

Если в кадре есть предмет для масштаба (ложка ~14см, монета ~2см, рука) — используй его.

Если юзер дал ПОДСКАЗКУ что это за блюдо — доверяй ей, перепроверь свою классификацию.

Верни СТРОГО валидный JSON, без markdown, без ```. Поля:
{
  "is_food": true/false,
  "description": "Что я вижу: тёмная буханка хлеба, сверху — куски обжаренного мяса с зеленью. Размер ~15см.",
  "dish": "Хлеб с мясом",
  "components": [
    {"name": "хлеб ржаной", "grams": 120, "kcal": 250, "protein_g": 8, "fat_g": 2, "carbs_g": 48},
    {"name": "говядина жареная", "grams": 80, "kcal": 200, "protein_g": 20, "fat_g": 12, "carbs_g": 0}
  ],
  "kcal": <сумма по компонентам>,
  "protein_g": <сумма>,
  "fat_g": <сумма>,
  "carbs_g": <сумма>,
  "confidence": "high"/"medium"/"low",
  "comment": "Аппетитный комментарий 2-3 предложения. Эмодзи 1-2. Можно дерзко подколоть за калории."
}

Если не еда — is_food=false, kcal=0, components=[], dish="не еда", description опиши что видишь.
Точность важна. Считай ту порцию что НА фото."""

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

    suspend fun analyze(
        images: List<ByteArray>,
        mimeType: String = "image/jpeg",
        userHint: String = "",
    ): AnalysisResult =
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

            val baseText = if (images.size > 1)
                "${images.size} ракурса одного блюда. Что это? Разложи на компоненты, оцени калории."
            else
                "Что это за блюдо? Разложи на компоненты, оцени калории."
            val userText = if (userHint.isNotBlank())
                "$baseText\n\nПОДСКАЗКА ОТ ЮЗЕРА (доверяй ей!): $userHint"
            else baseText

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
        val cleaned = extractJson(raw)
        val (res, tag) = try {
            parseStrict(cleaned) to "strict"
        } catch (_: Throwable) {
            val patched = patchJson(cleaned)
            try { parseStrict(patched) to "patched" }
            catch (_: Throwable) { parseRegex(cleaned) to "regex" }
        }
        // Если по факту вытащили нули — впихнём raw в description чтобы юзер показал.
        return if (res.kcal == 0 && res.components.isEmpty() && res.dish == "блюдо") {
            val dump = raw.take(600).replace("\n", " ").replace("\"", "'")
            res.copy(
                dish = "(не распознано)",
                description = "[$tag] raw: $dump",
                comment = res.comment.ifBlank { "Модель не отдала ккал. Покажи скрин разработчику." },
            )
        } else res
    }

    private fun parseStrict(cleaned: String): AnalysisResult {
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
            description = obj["description"]?.jsonPrimitive?.content ?: "",
        )
    }

    private fun JsonObject.get(key: String) = this[key]

    /**
     * Заменить буквальные \n / \r внутри значений строк на пробел.
     * Грубо, но спасает когда модель в description вставляет реальный перевод строки.
     */
    private fun patchJson(s: String): String {
        val sb = StringBuilder(s.length)
        var inString = false
        var prev = '\u0000'
        for (c in s) {
            when {
                c == '"' && prev != '\\' -> { inString = !inString; sb.append(c) }
                inString && (c == '\n' || c == '\r') -> sb.append(' ')
                else -> sb.append(c)
            }
            prev = c
        }
        return sb.toString()
    }

    private fun parseRegex(raw: String): AnalysisResult {
        fun s(key: String): String? = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(raw)?.groupValues?.get(1)
        // поддержать int/float и string-обёрнутые числа
        fun n(key: String): Int {
            val rx = Regex("\"$key\"\\s*:\\s*\"?(-?\\d+(?:\\.\\d+)?)\"?")
            return rx.find(raw)?.groupValues?.get(1)?.toDoubleOrNull()?.toInt() ?: 0
        }
        // Если top-level kcal отсутствует — сумма всех "kcal" внутри components
        fun sumAll(key: String): Int {
            val rx = Regex("\"$key\"\\s*:\\s*\"?(-?\\d+(?:\\.\\d+)?)\"?")
            return rx.findAll(raw).sumOf { it.groupValues[1].toDoubleOrNull()?.toInt() ?: 0 }
        }
        val kcal = n("kcal").let { if (it > 0) it else sumAll("kcal") }
        val p = n("protein_g").let { if (it > 0) it else sumAll("protein_g") }
        val f = n("fat_g").let { if (it > 0) it else sumAll("fat_g") }
        val c = n("carbs_g").let { if (it > 0) it else sumAll("carbs_g") }
        return AnalysisResult(
            dish = s("dish") ?: "блюдо",
            kcal = kcal,
            proteinG = p,
            fatG = f,
            carbsG = c,
            comment = s("comment") ?: "",
            confidence = s("confidence") ?: "low",
            isFood = (Regex("\"is_food\"\\s*:\\s*(true|false)").find(raw)?.groupValues?.get(1) == "true"),
            components = emptyList(),
            description = s("description") ?: "",
        )
    }

    /**
     * Вытащить чистый JSON-объект из ответа модели. Gemini любит обернуть в
     * ```json ... ``` или добавить «Here is the analysis:» перед {.
     */
    private fun extractJson(raw: String): String {
        val trimmed = raw.trim()
            .removePrefix("```json").removePrefix("```JSON").removePrefix("```")
            .removeSuffix("```").trim()
        // Найти первую { и последнюю } — отбросить всё что вокруг.
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1)
        else trimmed
    }
}
