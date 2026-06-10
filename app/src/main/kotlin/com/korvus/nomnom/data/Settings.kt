package com.korvus.nomnom.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.dataStore by preferencesDataStore("nomnom_settings")

class Settings(private val ctx: Context) {

    private val keyBaseUrl = stringPreferencesKey("base_url")
    private val keyModel = stringPreferencesKey("model")
    private val keyApiKey = stringPreferencesKey("api_key")
    private val keyDailyTarget = stringPreferencesKey("daily_target")

    val baseUrl: Flow<String> = ctx.dataStore.data
        .map { it[keyBaseUrl] ?: DEFAULT_BASE_URL }
        .flowOn(Dispatchers.IO)

    val model: Flow<String> = ctx.dataStore.data
        .map { it[keyModel] ?: DEFAULT_MODEL }
        .flowOn(Dispatchers.IO)

    val apiKey: Flow<String> = ctx.dataStore.data
        .map { it[keyApiKey] ?: "" }
        .flowOn(Dispatchers.IO)

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

    companion object {
        const val DEFAULT_BASE_URL = "http://127.0.0.1:8765/v1"
        const val DEFAULT_MODEL = "qwen3-vl-plus"
    }
}
