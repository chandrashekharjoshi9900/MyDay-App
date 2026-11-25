package com.myday.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.myday.app.di.AppContainer

class MyDayViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(
                habitRepository = container.habitRepository,
                taskRepository = container.taskRepository,
                reminderRepository = container.reminderRepository,
                focusRepository = container.focusRepository,
                journalRepository = container.journalRepository,
                settingsRepository = container.settingsRepository,
                categoryRepository = container.categoryRepository
            ) as T

            modelClass.isAssignableFrom(HabitsViewModel::class.java) -> HabitsViewModel(
                habitRepository = container.habitRepository,
                categoryRepository = container.categoryRepository,
                reminderRepository = container.reminderRepository
            ) as T

            modelClass.isAssignableFrom(TasksViewModel::class.java) -> TasksViewModel(
                taskRepository = container.taskRepository,
                categoryRepository = container.categoryRepository,
                reminderRepository = container.reminderRepository
            ) as T

            modelClass.isAssignableFrom(RemindersViewModel::class.java) -> RemindersViewModel(
                reminderRepository = container.reminderRepository,
                notificationDiagnostics = container.notificationDiagnostics,
                notificationHelper = container.notificationHelper,
                settingsRepository = container.settingsRepository
            ) as T

            modelClass.isAssignableFrom(GoalsViewModel::class.java) -> GoalsViewModel(
                goalRepository = container.goalRepository,
                categoryRepository = container.categoryRepository
            ) as T

            modelClass.isAssignableFrom(ProgressViewModel::class.java) -> ProgressViewModel(
                habitRepository = container.habitRepository,
                taskRepository = container.taskRepository,
                focusRepository = container.focusRepository,
                journalRepository = container.journalRepository
            ) as T

            modelClass.isAssignableFrom(FocusViewModel::class.java) -> FocusViewModel(
                focusRepository = container.focusRepository,
                taskRepository = container.taskRepository,
                settingsRepository = container.settingsRepository,
                notificationHelper = container.notificationHelper
            ) as T

            modelClass.isAssignableFrom(JournalViewModel::class.java) -> JournalViewModel(
                journalRepository = container.journalRepository
            ) as T

            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(
                settingsRepository = container.settingsRepository,
                backupRepository = container.backupRepository,
                notificationHelper = container.notificationHelper
            ) as T

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
