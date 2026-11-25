package com.myday.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.app.domain.model.CategoryItem
import com.myday.app.domain.model.HabitItem
import com.myday.app.domain.model.HabitScheduleType
import com.myday.app.domain.model.HabitWithStats
import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.ReminderScheduleType
import com.myday.app.domain.model.ReminderTargetType
import com.myday.app.domain.repository.CategoryRepository
import com.myday.app.domain.repository.HabitRepository
import com.myday.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

enum class HabitFilterTab(val label: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    MISSED("Missed"),
    ARCHIVED("Archived")
}

data class HabitsUiState(
    val isLoading: Boolean = true,
    val selectedTab: HabitFilterTab = HabitFilterTab.ACTIVE,
    val selectedEpochDay: Long = LocalDate.now().toEpochDay(),
    val habits: List<HabitWithStats> = emptyList(),
    val categories: List<CategoryItem> = emptyList(),
    val selectedHistoryHabit: HabitWithStats? = null
)

class HabitsViewModel(
    private val habitRepository: HabitRepository,
    categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val selectedTab = MutableStateFlow(HabitFilterTab.ACTIVE)
    private val selectedEpochDay = MutableStateFlow(LocalDate.now().toEpochDay())
    private val historyHabitId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HabitsUiState> = combine(
        habitRepository.observeHabitsWithStats(includeArchived = true),
        categoryRepository.observeAll(),
        selectedTab,
        selectedEpochDay,
        historyHabitId
    ) { allHabits, categories, tab, epochDay, histId ->
        val filtered = when (tab) {
            HabitFilterTab.ACTIVE -> allHabits.filter { !it.habit.isArchived && !it.habit.isPaused }
            HabitFilterTab.PAUSED -> allHabits.filter { !it.habit.isArchived && it.habit.isPaused }
            HabitFilterTab.MISSED -> allHabits.filter { !it.habit.isArchived && it.stats.missedRecentDays.isNotEmpty() }
            HabitFilterTab.ARCHIVED -> allHabits.filter { it.habit.isArchived }
        }
        HabitsUiState(
            isLoading = false,
            selectedTab = tab,
            selectedEpochDay = epochDay,
            habits = filtered,
            categories = categories,
            selectedHistoryHabit = histId?.let { id -> allHabits.firstOrNull { it.habit.id == id } }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HabitsUiState()
    )

    fun selectTab(tab: HabitFilterTab) {
        selectedTab.value = tab
    }

    fun selectDate(epochDay: Long) {
        selectedEpochDay.value = epochDay
    }

    fun openHistory(habitId: String?) {
        historyHabitId.value = habitId
    }

    fun toggleCompletionForSelectedDate(habitId: String) {
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habitId, selectedEpochDay.value)
        }
    }

    fun toggleCompletionForDay(habitId: String, epochDay: Long) {
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habitId, epochDay)
        }
    }

    fun saveHabit(
        existing: HabitItem?,
        title: String,
        description: String,
        categoryId: String?,
        colorHex: Long,
        scheduleType: HabitScheduleType,
        selectedWeekdays: Set<Int>,
        weeklyTarget: Int,
        reminderTimeMinutes: Int?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val habitId = existing?.id ?: UUID.randomUUID().toString()
            val habit = HabitItem(
                id = habitId,
                title = title.trim(),
                description = description.trim(),
                categoryId = categoryId,
                colorHex = colorHex,
                scheduleType = scheduleType,
                selectedWeekdays = if (scheduleType == HabitScheduleType.WEEKDAYS) {
                    selectedWeekdays.ifEmpty { setOf(1, 2, 3, 4, 5) }
                } else {
                    emptySet()
                },
                weeklyTarget = weeklyTarget.coerceIn(1, 7),
                reminderTimeMinutes = reminderTimeMinutes,
                isPaused = existing?.isPaused ?: false,
                pausedAtEpochDay = existing?.pausedAtEpochDay,
                isArchived = existing?.isArchived ?: false,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
            habitRepository.upsertHabit(habit)

            val linkedReminderId = "habit_rem_$habitId"
            if (reminderTimeMinutes != null && !habit.isArchived && !habit.isPaused) {
                val reminder = ReminderItem(
                    id = linkedReminderId,
                    title = "Habit: ${habit.title}",
                    notes = habit.description.ifBlank { "Time for your scheduled habit!" },
                    scheduleType = when (scheduleType) {
                        HabitScheduleType.WEEKDAYS -> ReminderScheduleType.CUSTOM_WEEKDAYS
                        else -> ReminderScheduleType.DAILY
                    },
                    hour = reminderTimeMinutes / 60,
                    minute = reminderTimeMinutes % 60,
                    oneTimeEpochDay = null,
                    weekdays = habit.selectedWeekdays,
                    dayOfMonth = null,
                    intervalHours = null,
                    nextTriggerMillis = now,
                    isEnabled = true,
                    isCompleted = false,
                    snoozedUntilMillis = null,
                    playSound = true,
                    vibrate = true,
                    targetType = ReminderTargetType.HABIT,
                    targetId = habitId,
                    lastTriggeredMillis = null,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
                reminderRepository.upsertAndSchedule(reminder)
            } else {
                reminderRepository.deleteReminder(linkedReminderId)
            }
        }
    }

    fun setPaused(habitId: String, paused: Boolean) {
        viewModelScope.launch {
            habitRepository.setPaused(
                habitId = habitId,
                paused = paused,
                pausedAtEpochDay = if (paused) LocalDate.now().toEpochDay() else null
            )
            val linkedReminderId = "habit_rem_$habitId"
            if (paused) {
                reminderRepository.toggleEnabled(linkedReminderId, false)
            } else {
                val existingReminder = reminderRepository.getReminderById(linkedReminderId)
                if (existingReminder != null) {
                    reminderRepository.toggleEnabled(linkedReminderId, true)
                } else {
                    val habit = habitRepository.getHabitById(habitId)
                    val remMin = habit?.reminderTimeMinutes
                    if (habit != null && remMin != null && !habit.isArchived) {
                        val now = System.currentTimeMillis()
                        reminderRepository.upsertAndSchedule(
                            ReminderItem(
                                id = linkedReminderId,
                                title = "Habit: ${habit.title}",
                                notes = habit.description.ifBlank { "Time for your scheduled habit!" },
                                scheduleType = when (habit.scheduleType) {
                                    HabitScheduleType.WEEKDAYS -> ReminderScheduleType.CUSTOM_WEEKDAYS
                                    else -> ReminderScheduleType.DAILY
                                },
                                hour = remMin / 60,
                                minute = remMin % 60,
                                oneTimeEpochDay = null,
                                weekdays = habit.selectedWeekdays,
                                dayOfMonth = null,
                                intervalHours = null,
                                nextTriggerMillis = now,
                                isEnabled = true,
                                isCompleted = false,
                                snoozedUntilMillis = null,
                                playSound = true,
                                vibrate = true,
                                targetType = ReminderTargetType.HABIT,
                                targetId = habitId,
                                lastTriggeredMillis = null,
                                createdAtMillis = now,
                                updatedAtMillis = now
                            )
                        )
                    }
                }
            }
        }
    }

    fun setArchived(habitId: String, archived: Boolean) {
        viewModelScope.launch {
            habitRepository.setArchived(habitId, archived)
            if (archived) {
                reminderRepository.toggleEnabled("habit_rem_$habitId", false)
            }
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            reminderRepository.deleteReminder("habit_rem_$habitId")
            habitRepository.deleteHabit(habitId)
        }
    }
}
