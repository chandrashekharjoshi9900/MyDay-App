package com.myday.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.app.data.reminder.NotificationDiagnostics
import com.myday.app.data.reminder.NotificationDiagnosticsStatus
import com.myday.app.data.reminder.NotificationHelper
import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.ReminderScheduleType
import com.myday.app.domain.model.ReminderTargetType
import com.myday.app.domain.repository.ReminderRepository
import com.myday.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class RemindersUiState(
    val isLoading: Boolean = true,
    val reminders: List<ReminderItem> = emptyList(),
    val diagnostics: NotificationDiagnosticsStatus? = null,
    val defaultSnoozeMinutes: Int = 10,
    val statusMessage: String? = null
)

class RemindersViewModel(
    private val reminderRepository: ReminderRepository,
    val notificationDiagnostics: NotificationDiagnostics,
    private val notificationHelper: NotificationHelper,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val diagnosticsRefreshTick = MutableStateFlow(0)
    private val statusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<RemindersUiState> = combine(
        reminderRepository.observeAllReminders(),
        settingsRepository.observeSettings(),
        diagnosticsRefreshTick,
        statusMessage
    ) { reminders, settings, _, msg ->
        val active = reminders.filter { it.isEnabled && !it.isCompleted }
        val nextAlarm = active.minOfOrNull { it.snoozedUntilMillis ?: it.nextTriggerMillis }
        val diag = notificationDiagnostics.inspect(
            activeScheduledCount = active.size,
            nextAlarmMillis = nextAlarm
        )
        RemindersUiState(
            isLoading = false,
            reminders = reminders,
            diagnostics = diag,
            defaultSnoozeMinutes = settings.defaultSnoozeMinutes,
            statusMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RemindersUiState()
    )

    fun refreshDiagnostics() {
        diagnosticsRefreshTick.value++
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            val tone = settingsRepository.getSettings().reminderTone
            val posted = notificationHelper.showTestNotification(tone)
            statusMessage.value = if (posted) {
                "Test notification sent with ${tone.label} sound."
            } else {
                "Notification permission is not granted yet."
            }
            refreshDiagnostics()
        }
    }

    fun previewTone() {
        viewModelScope.launch {
            val tone = settingsRepository.getSettings().reminderTone
            val played = notificationHelper.playPreviewTone(tone)
            statusMessage.value = if (played) {
                "Playing ${tone.label} preview..."
            } else {
                "Could not play preview sound."
            }
        }
    }

    fun rescheduleAll() {
        viewModelScope.launch {
            reminderRepository.rescheduleAllActiveReminders()
            refreshDiagnostics()
            statusMessage.value = "All active reminders verified and rescheduled."
        }
    }

    fun saveReminder(
        existing: ReminderItem?,
        title: String,
        notes: String,
        scheduleType: ReminderScheduleType,
        hour: Int,
        minute: Int,
        oneTimeEpochDay: Long?,
        weekdays: Set<Int>,
        dayOfMonth: Int?,
        intervalHours: Int?,
        playSound: Boolean,
        vibrate: Boolean
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val reminder = ReminderItem(
                id = existing?.id ?: UUID.randomUUID().toString(),
                title = title.trim(),
                notes = notes.trim(),
                scheduleType = scheduleType,
                hour = hour.coerceIn(0, 23),
                minute = minute.coerceIn(0, 59),
                oneTimeEpochDay = oneTimeEpochDay ?: LocalDate.now().toEpochDay(),
                weekdays = weekdays,
                dayOfMonth = dayOfMonth ?: LocalDate.now().dayOfMonth,
                intervalHours = intervalHours ?: 6,
                nextTriggerMillis = now,
                isEnabled = true,
                isCompleted = false,
                snoozedUntilMillis = null,
                playSound = playSound,
                vibrate = vibrate,
                targetType = existing?.targetType ?: ReminderTargetType.STANDALONE,
                targetId = existing?.targetId,
                lastTriggeredMillis = existing?.lastTriggeredMillis,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
            val saved = reminderRepository.upsertAndSchedule(reminder)
            if (saved.nextTriggerMillis <= now && scheduleType == ReminderScheduleType.ONE_TIME) {
                statusMessage.value = "Selected date/time is in the past; pick a future time."
            } else {
                statusMessage.value = "Reminder scheduled."
            }
            refreshDiagnostics()
        }
    }

    fun toggleEnabled(reminderId: String, enabled: Boolean) {
        viewModelScope.launch {
            reminderRepository.toggleEnabled(reminderId, enabled)
            refreshDiagnostics()
        }
    }

    fun snoozeReminder(reminderId: String, minutes: Int) {
        viewModelScope.launch {
            reminderRepository.snoozeReminder(reminderId, minutes)
            statusMessage.value = "Snoozed for $minutes minutes."
            refreshDiagnostics()
        }
    }

    fun markCompleted(reminderId: String) {
        viewModelScope.launch {
            reminderRepository.markCompleted(reminderId)
            refreshDiagnostics()
        }
    }

    fun deleteReminder(reminderId: String) {
        viewModelScope.launch {
            reminderRepository.deleteReminder(reminderId)
            refreshDiagnostics()
        }
    }
}
