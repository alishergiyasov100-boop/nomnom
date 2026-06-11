package com.korvus.nomnom.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore by preferencesDataStore("nomnom_settings")

class Settings(private val ctx: Context) {

    private val keyBaseUrl = stringPreferencesKey("base_url")
    private val keyModel = stringPreferencesKey("model")
    private val keyApiKey = stringPreferencesKey("api_key")
    private val keyDailyTarget = stringPreferencesKey("daily_target")
    private val keyRotationIdx = intPreferencesKey("rotation_idx")

    val baseUrl: Flow<String> = ctx.dataStore.data
        .map { it[keyBaseUrl] ?: DEFAULT_BASE_URL }
        .flowOn(Dispatchers.IO)

    val model: Flow<String> = ctx.dataStore.data
        .map { it[keyModel] ?: DEFAULT_MODEL }
        .flowOn(Dispatchers.IO)

    val apiKey: Flow<String> = ctx.dataStore.data
        .map { it[keyApiKey] ?: DEFAULT_API_KEY }
        .flowOn(Dispatchers.IO)

    val apiKeys: Flow<List<String>> = apiKey.map { parseKeys(it) }

    val dailyTarget: Flow<Int> = ctx.dataStore.data
        .map { (it[keyDailyTarget] ?: "2000").toIntOrNull() ?: 2000 }
        .flowOn(Dispatchers.IO)

    suspend fun setBaseUrl(v: String) = withContext(Dispatchers.IO) {
        ctx.dataStore.edit { it[keyBaseUrl] = v }
    }

    suspend fun setModel(v: String) = withContext(Dispatchers.IO) {
        ctx.dataStore.edit { it[keyModel] = v }
    }

    suspend fun setApiKey(v: String) = withContext(Dispatchers.IO) {
        ctx.dataStore.edit { it[keyApiKey] = v }
    }

    suspend fun setDailyTarget(v: Int) = withContext(Dispatchers.IO) {
        ctx.dataStore.edit { it[keyDailyTarget] = v.toString() }
    }

    /** Текущий индекс ротации, начиная с которого пробовать ключи. */
    suspend fun currentRotationIdx(): Int = withContext(Dispatchers.IO) {
        ctx.dataStore.data.first()[keyRotationIdx] ?: 0
    }

    /** Сдвинуть указатель ротации на следующий ключ. */
    suspend fun advanceRotation() = withContext(Dispatchers.IO) {
        val keys = parseKeys(apiKey.first())
        if (keys.size <= 1) return@withContext
        ctx.dataStore.edit { prefs ->
            val cur = prefs[keyRotationIdx] ?: 0
            prefs[keyRotationIdx] = (cur + 1) % keys.size
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.mistral.ai/v1"
        const val DEFAULT_MODEL = "pixtral-12b-2409"
        const val DEFAULT_API_KEY = "J52ankEgDpvYTlmDXLnDTkEPUuUNd9PC"

        data class Preset(val label: String, val baseUrl: String, val model: String)

        val PRESETS = listOf(
            Preset("Gemini 2.5 Pro",   "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.5-pro"),
            Preset("Gemini 2.5 Flash", "https://generativelanguage.googleapis.com/v1beta/openai", "gemini-2.5-flash"),
            Preset("Mistral Pixtral",  DEFAULT_BASE_URL, DEFAULT_MODEL),
        )

        fun parseKeys(raw: String): List<String> =
            raw.split('\n', ',').map { it.trim() }.filter { it.isNotEmpty() }
    }
}
