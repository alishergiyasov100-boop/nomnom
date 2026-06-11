package com.korvus.nomnom.notif

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.korvus.nomnom.NomNomApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val app = ctx.applicationContext as? NomNomApp ?: return@launch
                app.reminderStore.load()
                ReminderScheduler.rescheduleAll(ctx, app.reminderStore.items.value)
            } finally {
                pending.finish()
            }
        }
    }
}
