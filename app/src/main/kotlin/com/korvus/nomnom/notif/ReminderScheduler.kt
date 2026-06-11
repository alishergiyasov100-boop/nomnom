package com.korvus.nomnom.notif

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.korvus.nomnom.data.Reminder
import java.util.Calendar

const val CHANNEL_ID = "nomnom_reminders"
const val EXTRA_REMINDER_ID = "reminder_id"
const val EXTRA_REMINDER_TEXT = "reminder_text"
const val EXTRA_REMINDER_DAILY = "reminder_daily"

object ReminderScheduler {

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(
            CHANNEL_ID,
            "Напоминания",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Напоминания поесть/попить от NomNom"
            enableLights(true)
            enableVibration(true)
        }
        nm.createNotificationChannel(ch)
    }

    fun schedule(ctx: Context, r: Reminder) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(ctx, r)
        val triggerAt = nextTriggerMillis(r.hour, r.minute)
        // Exact alarm — на API 31+ требует SCHEDULE_EXACT_ALARM. Если не дано — упадёт.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                // fallback: setAndAllowWhileIdle (неточная по системе)
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    fun cancel(ctx: Context, r: Reminder) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(ctx, r))
    }

    fun rescheduleAll(ctx: Context, all: List<Reminder>) {
        ensureChannel(ctx)
        all.forEach { if (it.enabled) schedule(ctx, it) else cancel(ctx, it) }
    }

    private fun pendingIntent(ctx: Context, r: Reminder): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, r.id)
            putExtra(EXTRA_REMINDER_TEXT, r.text)
            putExtra(EXTRA_REMINDER_DAILY, r.daily)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(ctx, r.id.hashCode(), intent, flags)
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
