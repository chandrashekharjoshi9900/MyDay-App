package com.myday.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.myday.app.domain.model.AppSettingsModel
import com.myday.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.myDayDataStore: DataStore<Preferences> by preferencesDataStore(name = "myday_preferences")

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val POMODORO_MINUTES = intPreferencesKey("pomodoro_minutes")
        val SHORT_BREAK_MINUTES = intPreferencesKey("short_break_minutes")
        val LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        val LAST_BACKUP_MILLIS = longPreferencesKey("last_backup_millis")
    }

    val settingsFlow: Flow<AppSettingsModel> = context.myDayDataStore.data.map { prefs ->
        AppSettingsModel(
            userName = prefs[Keys.USER_NAME] ?: "Friend",
            themeMode = runCatching {
                ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name)
            }.getOrDefault(ThemeMode.DARK),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            notificationSoundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            defaultSnoozeMinutes = prefs[Keys.DEFAULT_SNOOZE_MINUTES] ?: 10,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            defaultPomodoroMinutes = prefs[Keys.POMODORO_MINUTES] ?: 25,
            shortBreakMinutes = prefs[Keys.SHORT_BREAK_MINUTES] ?: 5,
            longBreakMinutes = prefs[Keys.LONG_BREAK_MINUTES] ?: 15,
            lastBackupMillis = prefs[Keys.LAST_BACKUP_MILLIS]
        )
    }

    suspend fun getSettings(): AppSettingsModel = settingsFlow.first()

    suspend fun saveSettings(settings: AppSettingsModel) {
        context.myDayDataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = settings.userName
            prefs[Keys.THEME_MODE] = settings.themeMode.name
            prefs[Keys.NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            prefs[Keys.SOUND_ENABLED] = settings.notificationSoundEnabled
            prefs[Keys.VIBRATION_ENABLED] = settings.vibrationEnabled
            prefs[Keys.DEFAULT_SNOOZE_MINUTES] = settings.defaultSnoozeMinutes
            prefs[Keys.HAPTIC_FEEDBACK] = settings.hapticFeedbackEnabled
            prefs[Keys.POMODORO_MINUTES] = settings.defaultPomodoroMinutes
            prefs[Keys.SHORT_BREAK_MINUTES] = settings.shortBreakMinutes
            prefs[Keys.LONG_BREAK_MINUTES] = settings.longBreakMinutes
            if (settings.lastBackupMillis != null) {
                prefs[Keys.LAST_BACKUP_MILLIS] = settings.lastBackupMillis
            } else {
                prefs.remove(Keys.LAST_BACKUP_MILLIS)
            }
        }
    }
}
