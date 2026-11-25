package com.myday.app.di

import android.content.Context
import com.myday.app.data.backup.BackupRepositoryImpl
import com.myday.app.data.local.datastore.UserPreferencesDataStore
import com.myday.app.data.local.db.MyDayDatabase
import com.myday.app.data.reminder.NotificationDiagnostics
import com.myday.app.data.reminder.NotificationHelper
import com.myday.app.data.reminder.ReminderAlarmScheduler
import com.myday.app.data.repository.CategoryRepositoryImpl
import com.myday.app.data.repository.FocusRepositoryImpl
import com.myday.app.data.repository.GoalRepositoryImpl
import com.myday.app.data.repository.HabitRepositoryImpl
import com.myday.app.data.repository.JournalRepositoryImpl
import com.myday.app.data.repository.ReminderRepositoryImpl
import com.myday.app.data.repository.SettingsRepositoryImpl
import com.myday.app.data.repository.TaskRepositoryImpl
import com.myday.app.domain.repository.BackupRepository
import com.myday.app.domain.repository.CategoryRepository
import com.myday.app.domain.repository.FocusRepository
import com.myday.app.domain.repository.GoalRepository
import com.myday.app.domain.repository.HabitRepository
import com.myday.app.domain.repository.JournalRepository
import com.myday.app.domain.repository.ReminderRepository
import com.myday.app.domain.repository.SettingsRepository
import com.myday.app.domain.repository.TaskRepository

class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: MyDayDatabase by lazy { MyDayDatabase.build(appContext) }
    val preferencesDataStore: UserPreferencesDataStore by lazy { UserPreferencesDataStore(appContext) }

    val notificationHelper: NotificationHelper by lazy { NotificationHelper(appContext) }
    val alarmScheduler: ReminderAlarmScheduler by lazy { ReminderAlarmScheduler(appContext) }
    val notificationDiagnostics: NotificationDiagnostics by lazy {
        NotificationDiagnostics(appContext, notificationHelper, alarmScheduler)
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepositoryImpl(database.categoryDao())
    }

    val habitRepository: HabitRepository by lazy {
        HabitRepositoryImpl(
            habitDao = database.habitDao(),
            habitLogDao = database.habitLogDao(),
            categoryDao = database.categoryDao()
        )
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepositoryImpl(
            reminderDao = database.reminderDao(),
            taskDao = database.taskDao(),
            habitLogDao = database.habitLogDao(),
            alarmScheduler = alarmScheduler
        )
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl(
            db = database,
            taskDao = database.taskDao(),
            reminderRepository = reminderRepository
        )
    }

    val goalRepository: GoalRepository by lazy {
        GoalRepositoryImpl(
            goalDao = database.goalDao(),
            milestoneDao = database.milestoneDao(),
            categoryDao = database.categoryDao()
        )
    }

    val focusRepository: FocusRepository by lazy {
        FocusRepositoryImpl(database.focusSessionDao())
    }

    val journalRepository: JournalRepository by lazy {
        JournalRepositoryImpl(database.journalEntryDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(
            dataStore = preferencesDataStore,
            settingsDao = database.appSettingsDao()
        )
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepositoryImpl(
            db = database,
            preferencesDataStore = preferencesDataStore,
            reminderRepository = reminderRepository
        )
    }
}
