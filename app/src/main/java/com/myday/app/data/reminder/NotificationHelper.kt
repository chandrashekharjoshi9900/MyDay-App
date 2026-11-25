package com.myday.app.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.myday.app.MainActivity
import com.myday.app.R
import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.ReminderTone

object NotificationChannels {
    const val REMINDERS_CHIME = "myday_reminders_chime_v2"
    const val REMINDERS_SYSTEM_NOTIF = "myday_reminders_sys_notif_v2"
    const val REMINDERS_ALARM = "myday_reminders_alarm_v2"
    const val REMINDERS_SILENT = "myday_reminders_silent_v2"
    const val FOCUS_TIMER = "myday_focus_timer_v2"

    fun channelIdFor(soundEnabled: Boolean, tone: ReminderTone): String {
        if (!soundEnabled) return REMINDERS_SILENT
        return when (tone) {
            ReminderTone.CHIME -> REMINDERS_CHIME
            ReminderTone.SYSTEM_NOTIFICATION -> REMINDERS_SYSTEM_NOTIF
            ReminderTone.ALARM_RINGTONE -> REMINDERS_ALARM
        }
    }
}

class NotificationHelper(private val context: Context) {

    @Volatile
    private var activePreviewRingtone: Ringtone? = null

    fun soundUriFor(tone: ReminderTone): Uri {
        val rawChimeUri = Uri.parse("android.resource://${context.packageName}/${R.raw.myday_reminder_tone}")
        return when (tone) {
            ReminderTone.CHIME -> rawChimeUri
            ReminderTone.SYSTEM_NOTIFICATION ->
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) ?: rawChimeUri
            ReminderTone.ALARM_RINGTONE ->
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: rawChimeUri
        }
    }

    fun ensureChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val vibPattern = longArrayOf(0L, 250L, 150L, 250L)

        val chimeChannel = NotificationChannel(
            NotificationChannels.REMINDERS_CHIME,
            "Reminders (MYDAY Chime)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled reminders with built-in MYDAY chime"
            enableVibration(true)
            vibrationPattern = vibPattern
            setSound(
                soundUriFor(ReminderTone.CHIME),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val sysNotifChannel = NotificationChannel(
            NotificationChannels.REMINDERS_SYSTEM_NOTIF,
            "Reminders (System Sound)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled reminders with default notification sound"
            enableVibration(true)
            vibrationPattern = vibPattern
            setSound(
                soundUriFor(ReminderTone.SYSTEM_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val alarmChannel = NotificationChannel(
            NotificationChannels.REMINDERS_ALARM,
            "Reminders (Alarm Ringtone)",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Scheduled reminders with alarm ringtone"
            enableVibration(true)
            vibrationPattern = vibPattern
            setSound(
                soundUriFor(ReminderTone.ALARM_RINGTONE),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
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
            vibrationPattern = vibPattern
            setSound(
                soundUriFor(ReminderTone.CHIME),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        manager.createNotificationChannels(
            listOf(chimeChannel, sysNotifChannel, alarmChannel, silentChannel, focusChannel)
        )
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

    fun playPreviewTone(tone: ReminderTone): Boolean {
        return runCatching {
            activePreviewRingtone?.stop()
            val uri = soundUriFor(tone)
            val ringtone = RingtoneManager.getRingtone(context, uri)
            if (ringtone != null) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(
                        if (tone == ReminderTone.ALARM_RINGTONE) {
                            AudioAttributes.USAGE_ALARM
                        } else {
                            AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                        }
                    )
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                ringtone.play()
                activePreviewRingtone = ringtone
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    fun showReminderNotification(
        reminder: ReminderItem,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean,
        snoozeMinutes: Int,
        tone: ReminderTone = ReminderTone.CHIME
    ) {
        if (!hasNotificationPermission()) return
        ensureChannels()

        val notificationId = notificationIdFor(reminder.id)
        val useSound = soundEnabled && reminder.playSound
        val channelId = NotificationChannels.channelIdFor(useSound, tone)

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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Complete", completePendingIntent)
            .addAction(0, "Snooze ${snoozeMinutes}m", snoozePendingIntent)
            .addAction(0, "Dismiss", dismissPendingIntent)

        if (useSound) {
            builder.setSound(soundUriFor(tone))
        }

        if (!vibrateEnabled || !reminder.vibrate) {
            builder.setVibrate(longArrayOf(0L))
        } else {
            builder.setVibrate(longArrayOf(0L, 250L, 150L, 250L))
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
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUriFor(ReminderTone.CHIME))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(FOCUS_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
        }
    }

    fun showTestNotification(tone: ReminderTone = ReminderTone.CHIME): Boolean {
        if (!hasNotificationPermission()) return false
        ensureChannels()

        val channelId = NotificationChannels.channelIdFor(soundEnabled = true, tone = tone)
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            TEST_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("MYDAY Notification Diagnostics")
            .setContentText("Sound (${tone.label}) & notification channel active.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUriFor(tone))
            .setVibrate(longArrayOf(0L, 250L, 150L, 250L))
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
