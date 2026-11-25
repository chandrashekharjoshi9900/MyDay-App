package com.myday.app.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.myday.app.domain.model.ReminderItem

data class AlarmScheduleResult(
    val scheduled: Boolean,
    val exact: Boolean,
    val triggerAtMillis: Long?
)

class ReminderAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(reminder: ReminderItem): AlarmScheduleResult {
        cancel(reminder.id)
        if (!reminder.isEnabled || reminder.isCompleted) {
            return AlarmScheduleResult(scheduled = false, exact = false, triggerAtMillis = null)
        }

        val triggerAt = reminder.snoozedUntilMillis ?: reminder.nextTriggerMillis
        if (triggerAt <= System.currentTimeMillis()) {
            return AlarmScheduleResult(scheduled = false, exact = false, triggerAtMillis = null)
        }

        val pendingIntent = buildAlarmPendingIntent(reminder.id)
        val canExact = canScheduleExactAlarms()

        return try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
                AlarmScheduleResult(scheduled = true, exact = true, triggerAtMillis = triggerAt)
            } else {
                // Graceful fallback when exact alarm permission is not granted on Android 12+
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
                AlarmScheduleResult(scheduled = true, exact = false, triggerAtMillis = triggerAt)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
            AlarmScheduleResult(scheduled = true, exact = false, triggerAtMillis = triggerAt)
        }
    }

    fun cancel(reminderId: String) {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
        }
        val existing = PendingIntent.getBroadcast(
            context,
            requestCodeFor(reminderId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
        }
    }

    internal fun buildAlarmPendingIntent(reminderId: String): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            putExtra(NotificationHelper.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(reminderId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.myday.app.ACTION_TRIGGER_REMINDER"

        fun requestCodeFor(reminderId: String): Int {
            return (reminderId.hashCode() and 0x7FFFFFFF).coerceAtLeast(1)
        }
    }
}
