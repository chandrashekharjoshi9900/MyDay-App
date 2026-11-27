package com.myday.app.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.myday.app.MyDayApplication
import com.myday.app.data.local.db.toEntity
import com.myday.app.domain.calculator.ReminderScheduleCalculator
import com.myday.app.domain.model.ReminderScheduleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID) ?: return
        val pendingResult = goAsync()
        val app = context.applicationContext as MyDayApplication
        val container = app.container

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = container.reminderRepository.getReminderById(reminderId) ?: return@launch
                if (!reminder.isEnabled || reminder.isCompleted) return@launch

                val settings = container.settingsRepository.getSettings()
                if (settings.notificationsEnabled && container.notificationHelper.hasNotificationPermission()) {
                    val shouldLoopSound = settings.notificationSoundEnabled && reminder.playSound
                    if (shouldLoopSound) {
                        val startedService = runCatching {
                            ReminderSoundService.start(
                                context = context,
                                reminder = reminder,
                                tone = settings.reminderTone,
                                vibrate = settings.vibrationEnabled,
                                snoozeMinutes = settings.defaultSnoozeMinutes
                            )
                        }.isSuccess
                        if (!startedService) {
                            container.notificationHelper.showReminderNotification(
                                reminder = reminder,
                                soundEnabled = true,
                                vibrateEnabled = settings.vibrationEnabled,
                                snoozeMinutes = settings.defaultSnoozeMinutes,
                                tone = settings.reminderTone
                            )
                        }
                    } else {
                        container.notificationHelper.showReminderNotification(
                            reminder = reminder,
                            soundEnabled = false,
                            vibrateEnabled = settings.vibrationEnabled,
                            snoozeMinutes = settings.defaultSnoozeMinutes,
                            tone = settings.reminderTone
                        )
                    }
                }

                val now = System.currentTimeMillis()
                val clearedSnooze = reminder.copy(
                    snoozedUntilMillis = null,
                    lastTriggeredMillis = now,
                    updatedAtMillis = now
                )

                if (reminder.scheduleType == ReminderScheduleType.ONE_TIME) {
                    val updated = clearedSnooze.copy(isEnabled = false)
                    container.database.reminderDao().upsert(updated.toEntity())
                } else {
                    val nextTrigger = ReminderScheduleCalculator.computeNextTriggerMillis(
                        reminder = clearedSnooze,
                        afterMillis = now + 1000L
                    )
                    if (nextTrigger != null) {
                        val updated = clearedSnooze.copy(nextTriggerMillis = nextTrigger)
                        container.reminderRepository.upsertAndSchedule(updated)
                    } else {
                        val updated = clearedSnooze.copy(isEnabled = false)
                        container.reminderRepository.upsertAndSchedule(updated)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
