package com.myday.app.data.reminder

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

data class NotificationDiagnosticsStatus(
    val postNotificationsGranted: Boolean,
    val notificationsEnabledInSystem: Boolean,
    val exactAlarmsAllowed: Boolean,
    val reminderChannelEnabled: Boolean,
    val ignoringBatteryOptimizations: Boolean,
    val activeScheduledRemindersCount: Int,
    val nextAlarmMillis: Long?
) {
    val isFullyHealthy: Boolean
        get() = postNotificationsGranted &&
            notificationsEnabledInSystem &&
            exactAlarmsAllowed &&
            reminderChannelEnabled
}

class NotificationDiagnostics(
    private val context: Context,
    private val notificationHelper: NotificationHelper,
    private val alarmScheduler: ReminderAlarmScheduler
) {

    fun inspect(activeScheduledCount: Int, nextAlarmMillis: Long?): NotificationDiagnosticsStatus {
        notificationHelper.ensureChannels()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = notificationManager.getNotificationChannel(NotificationChannels.REMINDERS_SOUND)
        val channelEnabled = channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val ignoringBattery = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        return NotificationDiagnosticsStatus(
            postNotificationsGranted = notificationHelper.hasNotificationPermission(),
            notificationsEnabledInSystem = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            exactAlarmsAllowed = alarmScheduler.canScheduleExactAlarms(),
            reminderChannelEnabled = channelEnabled,
            ignoringBatteryOptimizations = ignoringBattery,
            activeScheduledRemindersCount = activeScheduledCount,
            nextAlarmMillis = nextAlarmMillis
        )
    }

    fun buildAppNotificationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildExactAlarmSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            null
        }
    }

    fun buildBatteryOptimizationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
