package com.myday.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.app.domain.calculator.AnalyticsCalculator
import com.myday.app.domain.model.AnalyticsPeriod
import com.myday.app.domain.model.ProductivityReport
import com.myday.app.domain.repository.FocusRepository
import com.myday.app.domain.repository.HabitRepository
import com.myday.app.domain.repository.JournalRepository
import com.myday.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class ProgressUiState(
    val isLoading: Boolean = true,
    val selectedPeriod: AnalyticsPeriod = AnalyticsPeriod.WEEKLY,
    val report: ProductivityReport? = null
)

class ProgressViewModel(
    habitRepository: HabitRepository,
    taskRepository: TaskRepository,
    focusRepository: FocusRepository,
    journalRepository: JournalRepository
) : ViewModel() {

    private val selectedPeriod = MutableStateFlow(AnalyticsPeriod.WEEKLY)

    private val dataStreams = combine(
        habitRepository.observeAllHabits(),
        habitRepository.observeAllLogs(),
        taskRepository.observeAllTasks(),
        focusRepository.observeAllSessions(),
        journalRepository.observeAllEntries()
    ) { habits, logs, tasks, focus, journal ->
        AnalyticsInputs(habits, logs, tasks, focus, journal)
    }

    val uiState: StateFlow<ProgressUiState> = combine(
        selectedPeriod,
        dataStreams
    ) { period, inputs ->
        val report = AnalyticsCalculator.buildReport(
            period = period,
            habits = inputs.habits,
            habitLogs = inputs.logs,
            tasks = inputs.tasks,
            focusSessions = inputs.focus,
            journalEntries = inputs.journal,
            today = LocalDate.now()
        )
        ProgressUiState(
            isLoading = false,
            selectedPeriod = period,
            report = report
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProgressUiState()
    )

    fun selectPeriod(period: AnalyticsPeriod) {
        selectedPeriod.value = period
    }

    private data class AnalyticsInputs(
        val habits: List<com.myday.app.domain.model.HabitItem>,
        val logs: List<com.myday.app.domain.model.HabitLogItem>,
        val tasks: List<com.myday.app.domain.model.TaskItem>,
        val focus: List<com.myday.app.domain.model.FocusSessionItem>,
        val journal: List<com.myday.app.domain.model.JournalEntryItem>
    )
}
