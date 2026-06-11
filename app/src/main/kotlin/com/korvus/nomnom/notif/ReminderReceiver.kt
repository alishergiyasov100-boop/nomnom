package com.korvus.nomnom.notif

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.korvus.nomnom.NomNomApp
import com.korvus.nomnom.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val text = intent.getStringExtra(EXTRA_REMINDER_TEXT) ?: ""
        val daily = intent.getBooleanExtra(EXTRA_REMINDER_DAILY, false)

        showNotification(ctx, id, text)

        if (daily) {
            // ре-планируем на следующий день: читаем актуальное состояние из store
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val app = ctx.applicationContext as? NomNomApp
                    val reminder = app?.reminderStore?.items?.value?.firstOrNull { it.id == id }
                    if (reminder != null && reminder.enabled && reminder.daily) {
                        ReminderScheduler.schedule(ctx, reminder)
                    }
                } finally {
                    pending.finish()
                }
            }
        } else {
            // одноразовое — выключаем enabled
            val pending = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    val app = ctx.applicationContext as? NomNomApp
                    val reminder = app?.reminderStore?.items?.value?.firstOrNull { it.id == id }
                    if (reminder != null) {
                        app.reminderStore.upsert(reminder.copy(enabled = false))
                    }
                } finally {
                    pending.finish()
                }
            }
        }
    }

    private fun showNotification(ctx: Context, id: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("NomNom")
            .setContentText(text.ifBlank { "Пора покушать 🍽" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(id.hashCode(), notif)
    }
}
