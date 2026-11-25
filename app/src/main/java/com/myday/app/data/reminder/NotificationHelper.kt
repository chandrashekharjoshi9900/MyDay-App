package com.myday.app.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.myday.app.MainActivity
import com.myday.app.R
import com.myday.app.domain.model.ReminderItem

object NotificationChannels {
    const val REMINDERS_SOUND = "myday_reminders_sound"
    const val REMINDERS_SILENT = "myday_reminders_silent"
    const val FOCUS_TIMER = "myday_focus_timer"
}

class NotificationHelper(private val context: Context) {

    fun ensureChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val soundChannel = NotificationChannel(
            NotificationChannels.REMINDERS_SOUND,
            "Scheduled Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for tasks, habits, and personal alerts with sound"
            enableVibration(true)
            setSound(soundUri, audioAttributes)
        }

        val silentChannel = NotificationChannel(
            NotificationChannels.REMINDERS_SILENT,
            "Silent Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Quiet reminder notifications without sound"
            setSound(null, null)
            enableVibration(true)
        }

        val focusChannel = NotificationChannel(
            NotificationChannels.FOCUS_TIMER,
            "Focus Timer",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alerts when a Pomodoro or focus session finishes"
            enableVibration(true)
            setSound(soundUri, audioAttributes)
        }

        manager.createNotificationChannels(listOf(soundChannel, silentChannel, focusChannel))
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showReminderNotification(
        reminder: ReminderItem,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean,
        snoozeMinutes: Int
    ) {
        if (!hasNotificationPermission()) return
        ensureChannels()

        val notificationId = notificationIdFor(reminder.id)
        val useSound = soundEnabled && reminder.playSound
        val channelId = if (useSound) {
            NotificationChannels.REMINDERS_SOUND
        } else {
            NotificationChannels.REMINDERS_SILENT
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 10_000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_COMPLETE
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 20_000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_DISMISS
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 30_000,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminder.title)
            .setContentText(reminder.notes.ifBlank { "Scheduled reminder • MYDAY" })
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(reminder.notes.ifBlank { "Tap to open MYDAY or take quick action below." })
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Complete", completePendingIntent)
            .addAction(0, "Snooze ${snoozeMinutes}m", snoozePendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)

        if (!vibrateEnabled || !reminder.vibrate) {
            builder.setVibrate(longArrayOf(0L))
        }

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked between check and notify
        }
    }

    fun showFocusCompletedNotification(sessionLabel: String, minutes: Int) {
        if (!hasNotificationPermission()) return
        ensureChannels()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            FOCUS_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.FOCUS_TIMER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("$sessionLabel complete")
            .setContentText("Great work! You completed a $minutes-minute focus session.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(FOCUS_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun showTestNotification(): Boolean {
        if (!hasNotificationPermission()) return false
        ensureChannels()

        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            TEST_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDERS_SOUND)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("MYDAY Notification Diagnostics")
            .setContentText("Background notifications and channels are working properly.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancelNotification(reminderId: String) {
        NotificationManagerCompat.from(context).cancel(notificationIdFor(reminderId))
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        private const val FOCUS_NOTIFICATION_ID = 900_001
        private const val TEST_NOTIFICATION_ID = 900_002

        fun notificationIdFor(reminderId: String): Int {
            return (reminderId.hashCode() and 0x0FFFFFFF).coerceAtLeast(1000)
        }
    }
}
