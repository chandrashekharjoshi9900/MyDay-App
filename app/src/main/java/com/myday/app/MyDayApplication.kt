package com.myday.app

import android.app.Application
import com.myday.app.data.local.db.MyDayDatabase
import com.myday.app.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyDayApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.ensureChannels()
        appScope.launch {
            container.database.openHelper.writableDatabase
            container.reminderRepository.rescheduleAllActiveReminders()
        }
    }
}
