package com.myday.app.presentation.viewmodel

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.app.domain.model.AppSettingsModel
import com.myday.app.domain.model.ThemeMode
import com.myday.app.domain.repository.BackupPreview
import com.myday.app.domain.repository.BackupRepository
import com.myday.app.domain.repository.BackupValidationResult
import com.myday.app.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val settings: AppSettingsModel = AppSettingsModel(),
    val isBusy: Boolean = false,
    val pendingRestorePreview: BackupPreview? = null,
    val pendingRestoreRawJson: String? = null,
    val message: String? = null,
    val isError: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val transientState = MutableStateFlow(TransientBackupState())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.observeSettings(),
        transientState
    ) { settings, transient ->
        SettingsUiState(
            settings = settings,
            isBusy = transient.isBusy,
            pendingRestorePreview = transient.pendingPreview,
            pendingRestoreRawJson = transient.pendingJson,
            message = transient.message,
            isError = transient.isError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(themeMode = themeMode) }
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(userName = name.trim().ifBlank { "Friend" }) }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(notificationsEnabled = enabled) }
        }
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(notificationSoundEnabled = enabled) }
        }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(vibrationEnabled = enabled) }
        }
    }

    fun setDefaultSnoozeMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(defaultSnoozeMinutes = minutes.coerceIn(1, 60)) }
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(hapticFeedbackEnabled = enabled) }
        }
    }

    fun setDefaultFocusDurations(pomodoro: Int, shortBreak: Int, longBreak: Int) {
        viewModelScope.launch {
            settingsRepository.updateSettings {
                it.copy(
                    defaultPomodoroMinutes = pomodoro.coerceIn(5, 120),
                    shortBreakMinutes = shortBreak.coerceIn(1, 30),
                    longBreakMinutes = longBreak.coerceIn(5, 60)
                )
            }
        }
    }

    fun exportBackupToUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            transientState.value = TransientBackupState(isBusy = true)
            runCatching {
                val json = backupRepository.exportJson()
                withContext(Dispatchers.IO) {
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: error("Unable to open selected file for writing.")
                }
            }.onSuccess {
                transientState.value = TransientBackupState(
                    message = "Backup exported successfully.",
                    isError = false
                )
            }.onFailure { err ->
                transientState.value = TransientBackupState(
                    message = "Export failed: ${err.message}",
                    isError = true
                )
            }
        }
    }

    fun inspectBackupUri(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch {
            transientState.value = TransientBackupState(isBusy = true)
            val rawJson = runCatching {
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    } ?: error("Unable to read selected file.")
                }
            }.getOrElse { err ->
                transientState.value = TransientBackupState(
                    message = "Could not open file: ${err.message}",
                    isError = true
                )
                return@launch
            }

            when (val validation = backupRepository.validateBackupJson(rawJson)) {
                is BackupValidationResult.Valid -> {
                    transientState.value = TransientBackupState(
                        pendingPreview = validation.preview,
                        pendingJson = validation.rawJson
                    )
                }
                is BackupValidationResult.Invalid -> {
                    transientState.value = TransientBackupState(
                        message = validation.reason,
                        isError = true
                    )
                }
            }
        }
    }

    fun confirmRestore() {
        val rawJson = transientState.value.pendingJson ?: return
        viewModelScope.launch {
            transientState.value = transientState.value.copy(isBusy = true)
            backupRepository.restoreFromJson(rawJson).fold(
                onSuccess = { preview ->
                    transientState.value = TransientBackupState(
                        message = "Restored ${preview.habitsCount} habits, ${preview.tasksCount} tasks, ${preview.goalsCount} goals, and ${preview.journalEntriesCount} journal entries.",
                        isError = false
                    )
                },
                onFailure = { err ->
                    transientState.value = TransientBackupState(
                        message = "Restore failed: ${err.message}",
                        isError = true
                    )
                }
            )
        }
    }

    fun cancelRestorePreview() {
        transientState.value = TransientBackupState()
    }

    fun clearMessage() {
        transientState.value = transientState.value.copy(message = null, isError = false)
    }

    private data class TransientBackupState(
        val isBusy: Boolean = false,
        val pendingPreview: BackupPreview? = null,
        val pendingJson: String? = null,
        val message: String? = null,
        val isError: Boolean = false
    )
}
