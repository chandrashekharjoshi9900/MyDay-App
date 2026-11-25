package com.myday.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.app.data.reminder.NotificationHelper
import com.myday.app.domain.model.FocusSessionItem
import com.myday.app.domain.model.FocusSessionType
import com.myday.app.domain.model.TaskItem
import com.myday.app.domain.repository.FocusRepository
import com.myday.app.domain.repository.SettingsRepository
import com.myday.app.domain.repository.TaskRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class FocusTimerState(
    val selectedType: FocusSessionType = FocusSessionType.POMODORO,
    val customMinutes: Int = 30,
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val isRunning: Boolean = false,
    val sessionStartedAtMillis: Long? = null,
    val linkedTaskId: String? = null,
    val statusBanner: String? = null
)

data class FocusUiState(
    val timer: FocusTimerState = FocusTimerState(),
    val todayFocusMinutes: Int = 0,
    val todaySessionCount: Int = 0,
    val totalFocusMinutes: Int = 0,
    val recentSessions: List<FocusSessionItem> = emptyList(),
    val pendingTasks: List<TaskItem> = emptyList()
)

class FocusViewModel(
    private val focusRepository: FocusRepository,
    taskRepository: TaskRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    private val timerState = MutableStateFlow(FocusTimerState())
    private var tickerJob: Job? = null

    init {
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            val secs = settings.defaultPomodoroMinutes * 60
            if (!timerState.value.isRunning && timerState.value.sessionStartedAtMillis == null) {
                timerState.value = timerState.value.copy(
                    totalSeconds = secs,
                    remainingSeconds = secs
                )
            }
        }
    }

    val uiState: StateFlow<FocusUiState> = combine(
        timerState,
        focusRepository.observeAllSessions(),
        taskRepository.observeAllTasks(),
        settingsRepository.observeSettings()
    ) { timer, sessions, tasks, _ ->
        val todayEpoch = LocalDate.now().toEpochDay()
        val completedSessions = sessions.filter { it.completed }
        val todaySessions = completedSessions.filter { it.epochDay == todayEpoch }

        FocusUiState(
            timer = timer,
            todayFocusMinutes = todaySessions.sumOf { it.actualDurationSeconds } / 60,
            todaySessionCount = todaySessions.size,
            totalFocusMinutes = completedSessions.sumOf { it.actualDurationSeconds } / 60,
            recentSessions = sessions.take(25),
            pendingTasks = tasks.filter { !it.isCompleted }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FocusUiState()
    )

    fun selectMode(type: FocusSessionType) {
        if (timerState.value.isRunning) return
        viewModelScope.launch {
            val settings = settingsRepository.getSettings()
            val minutes = when (type) {
                FocusSessionType.POMODORO -> settings.defaultPomodoroMinutes
                FocusSessionType.SHORT_BREAK -> settings.shortBreakMinutes
                FocusSessionType.LONG_BREAK -> settings.longBreakMinutes
                FocusSessionType.CUSTOM -> timerState.value.customMinutes
            }.coerceIn(1, 180)
            timerState.value = timerState.value.copy(
                selectedType = type,
                totalSeconds = minutes * 60,
                remainingSeconds = minutes * 60,
                sessionStartedAtMillis = null,
                statusBanner = null
            )
        }
    }

    fun setCustomMinutes(minutes: Int) {
        if (timerState.value.isRunning) return
        val clamped = minutes.coerceIn(1, 180)
        timerState.value = timerState.value.copy(
            selectedType = FocusSessionType.CUSTOM,
            customMinutes = clamped,
            totalSeconds = clamped * 60,
            remainingSeconds = clamped * 60,
            sessionStartedAtMillis = null
        )
    }

    fun linkTask(taskId: String?) {
        timerState.value = timerState.value.copy(linkedTaskId = taskId)
    }

    fun clearBanner() {
        timerState.value = timerState.value.copy(statusBanner = null)
    }

    fun startOrResume() {
        if (timerState.value.isRunning) return
        val current = timerState.value
        val startedAt = current.sessionStartedAtMillis ?: System.currentTimeMillis()
        timerState.value = current.copy(
            isRunning = true,
            sessionStartedAtMillis = startedAt,
            statusBanner = null
        )
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (timerState.value.isRunning && timerState.value.remainingSeconds > 0) {
                delay(1000L)
                if (!timerState.value.isRunning) break
                val nextRemaining = (timerState.value.remainingSeconds - 1).coerceAtLeast(0)
                timerState.value = timerState.value.copy(remainingSeconds = nextRemaining)
                if (nextRemaining == 0) {
                    completeSession(early = false)
                    break
                }
            }
        }
    }

    fun pause() {
        tickerJob?.cancel()
        timerState.value = timerState.value.copy(isRunning = false)
    }

    fun reset() {
        tickerJob?.cancel()
        val current = timerState.value
        timerState.value = current.copy(
            isRunning = false,
            remainingSeconds = current.totalSeconds,
            sessionStartedAtMillis = null,
            statusBanner = null
        )
    }

    fun finishEarly() {
        completeSession(early = true)
    }

    private fun completeSession(early: Boolean) {
        tickerJob?.cancel()
        val current = timerState.value
        val elapsedSeconds = (current.totalSeconds - current.remainingSeconds).coerceAtLeast(0)
        val now = System.currentTimeMillis()
        val startedAt = current.sessionStartedAtMillis ?: (now - elapsedSeconds * 1000L)

        if (!early || elapsedSeconds >= 60) {
            val recordedSeconds = if (early) elapsedSeconds else current.totalSeconds
            val session = FocusSessionItem(
                id = UUID.randomUUID().toString(),
                type = current.selectedType,
                plannedDurationSeconds = current.totalSeconds,
                actualDurationSeconds = recordedSeconds,
                startedAtMillis = startedAt,
                endedAtMillis = now,
                epochDay = LocalDate.now().toEpochDay(),
                completed = true,
                linkedTaskId = current.linkedTaskId,
                note = if (early) "Ended early" else "Completed"
            )
            viewModelScope.launch {
                focusRepository.recordSession(session)
                val settings = settingsRepository.getSettings()
                if (!early && settings.notificationsEnabled) {
                    notificationHelper.showFocusCompletedNotification(
                        sessionLabel = current.selectedType.label,
                        minutes = recordedSeconds / 60
                    )
                }
            }
        }

        val message = if (early && elapsedSeconds < 60) {
            "Session cancelled (under 1 minute not logged)."
        } else {
            "${current.selectedType.label} logged (${(if (early) elapsedSeconds else current.totalSeconds) / 60} min)."
        }

        timerState.value = current.copy(
            isRunning = false,
            remainingSeconds = current.totalSeconds,
            sessionStartedAtMillis = null,
            statusBanner = message
        )
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            focusRepository.deleteSession(sessionId)
        }
    }

    override fun onCleared() {
        tickerJob?.cancel()
        super.onCleared()
    }
}
