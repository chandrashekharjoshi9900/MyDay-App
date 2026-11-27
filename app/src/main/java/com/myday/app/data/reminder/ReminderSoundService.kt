package com.myday.app.data.reminder

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.ReminderScheduleType
import com.myday.app.domain.model.ReminderTargetType
import com.myday.app.domain.model.ReminderTone

/**
 * Foreground audio service that loops the selected reminder ringtone until the user
 * completes, snoozes, dismisses, swipes the notification, opens the app, or hits the
 * safety timeout.
 */
class ReminderSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        stopRinging(removeNotification = false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_RINGING -> {
                stopRinging(removeNotification = false)
                return START_NOT_STICKY
            }
            ACTION_START_RINGING -> {
                val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return START_NOT_STICKY
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "MYDAY Reminder"
                val notes = intent.getStringExtra(EXTRA_NOTES).orEmpty()
                val tone = runCatching {
                    ReminderTone.valueOf(intent.getStringExtra(EXTRA_TONE) ?: ReminderTone.CHIME.name)
                }.getOrDefault(ReminderTone.CHIME)
                val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
                val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
                val timeoutMs = intent.getLongExtra(EXTRA_TIMEOUT_MS, DEFAULT_MAX_LOOP_MS)

                val notificationHelper = NotificationHelper(this)
                val stubReminder = ReminderItem(
                    id = reminderId,
                    title = title,
                    notes = notes,
                    scheduleType = ReminderScheduleType.ONE_TIME,
                    hour = 0,
                    minute = 0,
                    oneTimeEpochDay = null,
                    weekdays = emptySet(),
                    dayOfMonth = null,
                    intervalHours = null,
                    nextTriggerMillis = System.currentTimeMillis(),
                    isEnabled = true,
                    isCompleted = false,
                    snoozedUntilMillis = null,
                    playSound = true,
                    vibrate = vibrate,
                    targetType = ReminderTargetType.STANDALONE,
                    targetId = null,
                    lastTriggeredMillis = null,
                    createdAtMillis = 0L,
                    updatedAtMillis = 0L
                )

                val notificationId = NotificationHelper.notificationIdFor(reminderId)
                val notification = notificationHelper.buildReminderNotification(
                    reminder = stubReminder,
                    soundEnabled = true,
                    vibrateEnabled = vibrate,
                    snoozeMinutes = snoozeMinutes,
                    tone = tone,
                    loopingHandledByService = true
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        notificationId,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(notificationId, notification)
                }

                startLoopingAudio(notificationHelper, tone)
                if (vibrate) {
                    startLoopingVibration()
                }
                mainHandler.removeCallbacks(timeoutRunnable)
                mainHandler.postDelayed(timeoutRunnable, timeoutMs)
            }
        }
        return START_NOT_STICKY
    }

    private fun startLoopingAudio(notificationHelper: NotificationHelper, tone: ReminderTone) {
        releaseAudio()
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val attrs = AudioAttributes.Builder()
            .setUsage(
                if (tone == ReminderTone.ALARM_RINGTONE) {
                    AudioAttributes.USAGE_ALARM
                } else {
                    AudioAttributes.USAGE_NOTIFICATION_RINGTONE
                }
            )
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(attrs)
            .setAcceptsDelayedFocusGain(false)
            .build()
        audioFocusRequest = focusReq
        audioManager.requestAudioFocus(focusReq)

        val uri = notificationHelper.soundUriFor(tone)
        val fallbackUri = notificationHelper.soundUriFor(ReminderTone.CHIME)

        val player = MediaPlayer()
        val prepared = runCatching {
            player.setAudioAttributes(attrs)
            player.setDataSource(this, uri)
            player.isLooping = true
            player.prepare()
            true
        }.getOrElse {
            runCatching {
                player.reset()
                player.setAudioAttributes(attrs)
                player.setDataSource(this, fallbackUri)
                player.isLooping = true
                player.prepare()
                true
            }.getOrDefault(false)
        }

        if (prepared) {
            player.start()
            mediaPlayer = player
        } else {
            player.release()
        }
    }

    private fun startLoopingVibration() {
        stopVibration()
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        vibrator = vib
        if (vib.hasVibrator()) {
            vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 400L, 650L), 0))
        }
    }

    private fun stopRinging(removeNotification: Boolean) {
        mainHandler.removeCallbacks(timeoutRunnable)
        releaseAudio()
        stopVibration()
        if (removeNotification) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(STOP_FOREGROUND_DETACH)
        }
        stopSelf()
    }

    private fun releaseAudio() {
        runCatching {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        }
        mediaPlayer = null

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioFocusRequest?.let { req ->
            runCatching { audioManager.abandonAudioFocusRequest(req) }
        }
        audioFocusRequest = null
    }

    private fun stopVibration() {
        runCatching { vibrator?.cancel() }
        vibrator = null
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(timeoutRunnable)
        releaseAudio()
        stopVibration()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_RINGING = "com.myday.app.ACTION_START_RINGING"
        const val ACTION_STOP_RINGING = "com.myday.app.ACTION_STOP_RINGING"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_NOTES = "extra_notes"
        const val EXTRA_TONE = "extra_tone"
        const val EXTRA_VIBRATE = "extra_vibrate"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_TIMEOUT_MS = "extra_timeout_ms"
        private const val DEFAULT_MAX_LOOP_MS = 60_000L

        fun start(
            context: Context,
            reminder: ReminderItem,
            tone: ReminderTone,
            vibrate: Boolean,
            snoozeMinutes: Int,
            timeoutMs: Long = DEFAULT_MAX_LOOP_MS
        ) {
            val intent = Intent(context, ReminderSoundService::class.java).apply {
                action = ACTION_START_RINGING
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_TITLE, reminder.title)
                putExtra(EXTRA_NOTES, reminder.notes)
                putExtra(EXTRA_TONE, tone.name)
                putExtra(EXTRA_VIBRATE, vibrate && reminder.vibrate)
                putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
                putExtra(EXTRA_TIMEOUT_MS, timeoutMs)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, ReminderSoundService::class.java)
            context.stopService(intent)
        }
    }
}
