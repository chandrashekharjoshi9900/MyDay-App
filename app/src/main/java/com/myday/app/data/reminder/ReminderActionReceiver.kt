package com.myday.app.data.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.myday.app.MyDayApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(NotificationHelper.EXTRA_REMINDER_ID) ?: return
        val action = intent.action ?: return

        // Always stop looping ringtone immediately when the user interacts with or swipes the notification
        ReminderSoundService.stop(context)

        if (action == ACTION_SILENCE) {
            return
        }

        val pendingResult = goAsync()
        val app = context.applicationContext as MyDayApplication
        val container = app.container

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_SNOOZE -> {
                        val snoozeMinutes = intent.getIntExtra(NotificationHelper.EXTRA_SNOOZE_MINUTES, 10)
                        if (reminderId != NotificationHelper.TEST_REMINDER_ID) {
                            container.reminderRepository.snoozeReminder(reminderId, snoozeMinutes)
                        }
                        container.notificationHelper.cancelNotification(reminderId)
                    }
                    ACTION_COMPLETE -> {
                        if (reminderId != NotificationHelper.TEST_REMINDER_ID) {
                            container.reminderRepository.markCompleted(reminderId)
                        }
                        container.notificationHelper.cancelNotification(reminderId)
                    }
                    ACTION_DISMISS -> {
                        if (reminderId != NotificationHelper.TEST_REMINDER_ID) {
                            container.reminderRepository.dismissCurrentOccurrence(reminderId)
                        }
                        container.notificationHelper.cancelNotification(reminderId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SNOOZE = "com.myday.app.REMINDER_ACTION_SNOOZE"
        const val ACTION_COMPLETE = "com.myday.app.REMINDER_ACTION_COMPLETE"
        const val ACTION_DISMISS = "com.myday.app.REMINDER_ACTION_DISMISS"
        const val ACTION_SILENCE = "com.myday.app.REMINDER_ACTION_SILENCE"
    }
}
