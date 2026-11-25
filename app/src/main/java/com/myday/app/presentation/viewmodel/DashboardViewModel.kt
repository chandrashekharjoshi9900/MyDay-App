package com.myday.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.app.domain.calculator.AnalyticsCalculator
import com.myday.app.domain.model.AnalyticsPeriod
import com.myday.app.domain.model.CategoryItem
import com.myday.app.domain.model.HabitWithStats
import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.TaskItem
import com.myday.app.domain.model.TrendPoint
import com.myday.app.domain.repository.CategoryRepository
import com.myday.app.domain.repository.FocusRepository
import com.myday.app.domain.repository.HabitRepository
import com.myday.app.domain.repository.JournalRepository
import com.myday.app.domain.repository.ReminderRepository
import com.myday.app.domain.repository.SettingsRepository
import com.myday.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class DashboardUiState(
    val isLoading: Boolean = true,
    val greeting: String = "Good day",
    val userName: String = "Friend",
    val dateLabel: String = "",
    val dailyOverviewPercent: Int = 0,
    val habitsDoneToday: Int = 0,
    val habitsScheduledToday: Int = 0,
    val todayHabits: List<HabitWithStats> = emptyList(),
    val pendingTasks: List<TaskItem> = emptyList(),
    val completedTasksToday: List<TaskItem> = emptyList(),
    val upcomingReminders: List<ReminderItem> = emptyList(),
    val currentStreak: Int = 0,
    val focusMinutesToday: Int = 0,
    val weeklyTrend: List<TrendPoint> = emptyList(),
    val categories: List<CategoryItem> = emptyList()
)

class DashboardViewModel(
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    focusRepository: FocusRepository,
    journalRepository: JournalRepository,
    settingsRepository: SettingsRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d")

    private val coreStreams = combine(
        habitRepository.observeHabitsWithStats(includeArchived = false),
        habitRepository.observeAllLogs(),
        taskRepository.observeAllTasks(),
        reminderRepository.observeAllReminders()
    ) { habits, logs, tasks, reminders ->
        Quad(habits, logs, tasks, reminders)
    }

    private val secondaryStreams = combine(
        focusRepository.observeAllSessions(),
        journalRepository.observeAllEntries(),
        settingsRepository.observeSettings(),
        categoryRepository.observeAll()
    ) { focus, journal, settings, categories ->
        Quad(focus, journal, settings, categories)
    }

    val uiState: StateFlow<DashboardUiState> = combine(coreStreams, secondaryStreams) { core, sec ->
        val habitsWithStats = core.first
        val allLogs = core.second
        val allTasks = core.third
        val allReminders = core.fourth

        val focusSessions = sec.first
        val journalEntries = sec.second
        val settings = sec.third
        val categories = sec.fourth

        val today = LocalDate.now()
        val todayEpoch = today.toEpochDay()
        val nowMillis = System.currentTimeMillis()

        val todayHabits = habitsWithStats.filter { !it.habit.isPaused && it.stats.scheduledToday }
        val habitsDoneToday = todayHabits.count { it.stats.completedToday }

        val pendingTasks = allTasks.filter { !it.isCompleted }
            .sortedWith(
                compareBy<TaskItem> { it.dueEpochDay ?: Long.MAX_VALUE }
                    .thenByDescending { it.priority.weight }
            )

        val todayStartMillis = today.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val completedTodayTasks = allTasks.filter {
            it.isCompleted && (it.completedAtMillis ?: 0L) >= todayStartMillis
        }

        val upcomingReminders = allReminders
            .filter { it.isEnabled && !it.isCompleted && (it.snoozedUntilMillis ?: it.nextTriggerMillis) > nowMillis - 60_000L }
            .sortedBy { it.snoozedUntilMillis ?: it.nextTriggerMillis }
            .take(4)

        val focusMinutesToday = focusSessions
            .filter { it.completed && it.epochDay == todayEpoch }
            .sumOf { it.actualDurationSeconds } / 60

        val weeklyReport = AnalyticsCalculator.buildReport(
            period = AnalyticsPeriod.WEEKLY,
            habits = habitsWithStats.map { it.habit },
            habitLogs = allLogs,
            tasks = allTasks,
            focusSessions = focusSessions,
            journalEntries = journalEntries,
            today = today
        )

        val dueTodayPendingCount = pendingTasks.count { it.dueEpochDay == null || it.dueEpochDay <= todayEpoch }
        val totalTodayItems = todayHabits.size + completedTodayTasks.size + dueTodayPendingCount
        val doneTodayItems = habitsDoneToday + completedTodayTasks.size
        val overviewPercent = if (totalTodayItems > 0) {
            ((doneTodayItems.toFloat() / totalTodayItems.toFloat()) * 100f).toInt().coerceIn(0, 100)
        } else {
            0
        }

        DashboardUiState(
            isLoading = false,
            greeting = greetingForHour(LocalTime.now().hour),
            userName = settings.userName.ifBlank { "Friend" },
            dateLabel = today.format(dateFormatter),
            dailyOverviewPercent = overviewPercent,
            habitsDoneToday = habitsDoneToday,
            habitsScheduledToday = todayHabits.size,
            todayHabits = todayHabits,
            pendingTasks = pendingTasks,
            completedTasksToday = completedTodayTasks,
            upcomingReminders = upcomingReminders,
            currentStreak = habitsWithStats.maxOfOrNull { it.stats.currentStreak } ?: 0,
            focusMinutesToday = focusMinutesToday,
            weeklyTrend = weeklyReport.trendPoints,
            categories = categories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun toggleHabitToday(habitId: String) {
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habitId, LocalDate.now().toEpochDay())
        }
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(taskId)
        }
    }

    private fun greetingForHour(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }

    private data class Quad<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
