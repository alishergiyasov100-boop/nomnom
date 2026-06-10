package com.korvus.nomnom.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Calendar

private const val FILE_NAME = "nomnom_log.json"
private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

class DayLog(ctx: Context) {

    private val file = File(ctx.filesDir, FILE_NAME)
    private val mutex = Mutex()
    private val _entries = MutableStateFlow<List<FoodEntry>>(emptyList())
    val entries: StateFlow<List<FoodEntry>> = _entries.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            _entries.value = try {
                if (file.exists()) {
                    json.decodeFromString(ListSerializer(FoodEntry.serializer()), file.readText())
                } else emptyList()
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    suspend fun add(entry: FoodEntry) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = listOf(entry) + _entries.value
            _entries.value = updated
            file.writeText(json.encodeToString(ListSerializer(FoodEntry.serializer()), updated))
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val updated = _entries.value.filterNot { it.id == id }
            _entries.value = updated
            file.writeText(json.encodeToString(ListSerializer(FoodEntry.serializer()), updated))
        }
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            _entries.value = emptyList()
            if (file.exists()) file.delete()
        }
    }
}

fun startOfDayMillis(now: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
