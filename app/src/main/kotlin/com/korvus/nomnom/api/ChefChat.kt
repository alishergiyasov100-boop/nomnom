package com.korvus.nomnom.api

import com.korvus.nomnom.data.FoodEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
private val json = Json { ignoreUnknownKeys = true }

class ChefChat(
    private val baseUrl: String,
    private val model: String,
    private val apiKey: String = "",
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun ask(diary: String, history: List<Pair<String, String>>): String =
        withContext(Dispatchers.IO) {
            val systemPrompt = """
                Ты — Шеф Пикстраль, AI-нутрициолог приложения NomNom. Дерзкий, остроумный,
                с тёплой иронией. Юзер на русском — отвечай по-русски. Кратко, по делу,
                максимум 4-5 предложений. Эмодзи 0-1 шт, только когда уместно.
                Можешь подколоть за переедание и похвалить за норм. Не морализируй.

                ВОТ ДНЕВНИК ЮЗЕРА НА СЕГОДНЯ:
                $diary

                Опирайся на эти цифры в ответах. Если спрашивают совет — конкретный.
            """.trimIndent()

            val payload = buildJsonObject {
                put("model", model)
                put("temperature", 0.7)
                put("max_tokens", 500)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    // первое assistant-приветствие пропускаем (оно UI-локальное)
                    history.drop(1).forEach { (role, text) ->
                        add(buildJsonObject {
                            put("role", role)
                            put("content", text)
                        })
                    }
                })
            }

            val url = baseUrl.trimEnd('/') + "/chat/completions"
            val reqBuilder = Request.Builder().url(url)
                .post(payload.toString().toRequestBody(JSON_MEDIA))
                .header("Content-Type", "application/json")
            if (apiKey.isNotBlank()) reqBuilder.header("Authorization", "Bearer $apiKey")

            val resp = client.newCall(reqBuilder.build()).execute()
            val body = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}: ${body.take(300)}")

            json.parseToJsonElement(body).jsonObject
                .get("choices")?.jsonArray?.get(0)?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.content
                ?.trim()
                ?: throw IOException("unexpected response: ${body.take(300)}")
        }

    companion object {
        private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun buildDiaryContext(today: List<FoodEntry>, target: Int): String {
            if (today.isEmpty()) {
                return "Сегодня юзер ещё ничего не ел. Дневная норма: $target ккал."
            }
            val kcal = today.sumOf { it.kcal }
            val p = today.sumOf { it.proteinG }
            val f = today.sumOf { it.fatG }
            val c = today.sumOf { it.carbsG }
            val remaining = target - kcal
            val lines = today.sortedBy { it.timestamp }.joinToString("\n") { e ->
                val meal = e.meal ?: "—"
                "• ${timeFmt.format(Date(e.timestamp))} [$meal] ${e.dish}: ${e.kcal} ккал (Б ${e.proteinG} · Ж ${e.fatG} · У ${e.carbsG})"
            }
            val deltaLine = if (remaining >= 0) "Осталось до нормы: $remaining ккал."
                            else "Перебор: ${-remaining} ккал сверху нормы."
            return """
                Дневная норма: $target ккал.
                Съедено: $kcal ккал · Б $p · Ж $f · У $c.
                $deltaLine

                Блюда:
                $lines
            """.trimIndent()
        }
    }
}
