package com.korvus.nomnom

import android.app.Application
import com.korvus.nomnom.data.DayLog
import com.korvus.nomnom.data.ReminderStore
import com.korvus.nomnom.data.Settings
import com.korvus.nomnom.notif.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NomNomApp : Application() {
    lateinit var settings: Settings
        private set
    lateinit var dayLog: DayLog
        private set
    lateinit var reminderStore: ReminderStore
        private set

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = Settings(this)
        dayLog = DayLog(this)
        reminderStore = ReminderStore(this)
        appScope.launch {
            dayLog.load()
            reminderStore.load()
            ReminderScheduler.ensureChannel(this@NomNomApp)
            ReminderScheduler.rescheduleAll(this@NomNomApp, reminderStore.items.value)
        }
    }

    companion object {
        lateinit var instance: NomNomApp
            private set
    }
}
