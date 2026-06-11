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

private const val FILE_NAME = "nomnom_reminders.json"
private val json = Json { ignoreUnknownKeys = true }

class ReminderStore(ctx: Context) {
    private val file = File(ctx.filesDir, FILE_NAME)
    private val mutex = Mutex()
    private val _items = MutableStateFlow<List<Reminder>>(emptyList())
    val items: StateFlow<List<Reminder>> = _items.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            _items.value = try {
                if (file.exists())
                    json.decodeFromString(ListSerializer(Reminder.serializer()), file.readText())
                else emptyList()
            } catch (_: Throwable) {
                emptyList()
            }
        }
    }

    suspend fun upsert(r: Reminder) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val list = _items.value.toMutableList()
            val idx = list.indexOfFirst { it.id == r.id }
            if (idx >= 0) list[idx] = r else list.add(r)
            list.sortBy { it.hour * 60 + it.minute }
            _items.value = list
            file.writeText(json.encodeToString(ListSerializer(Reminder.serializer()), list))
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val list = _items.value.filterNot { it.id == id }
            _items.value = list
            file.writeText(json.encodeToString(ListSerializer(Reminder.serializer()), list))
        }
    }
}
