package com.myday.app.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

enum class HabitScheduleType {
    DAILY,
    WEEKDAYS,
    WEEKLY_TARGET
}

enum class TaskPriority(val weight: Int, val label: String) {
    HIGH(3, "High"),
    MEDIUM(2, "Medium"),
    LOW(1, "Low")
}

enum class TaskRecurrence(val label: String) {
    NONE("One-time"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM_WEEKDAYS("Custom days")
}

enum class ReminderScheduleType(val label: String) {
    ONE_TIME("One-time"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    CUSTOM_WEEKDAYS("Custom weekdays"),
    MONTHLY("Monthly"),
    CUSTOM_INTERVAL("Custom interval")
}

enum class ReminderTargetType {
    STANDALONE,
    TASK,
    HABIT
}

enum class GoalStatus(val label: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed")
}

enum class FocusSessionType(val label: String, val defaultMinutes: Int) {
    POMODORO("Pomodoro", 25),
    SHORT_BREAK("Short Break", 5),
    LONG_BREAK("Long Break", 15),
    CUSTOM("Custom", 30)
}

enum class ThemeMode(val label: String) {
    DARK("Dark"),
    LIGHT("Light")
}

enum class AnalyticsPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

data class CategoryItem(
    val id: String,
    val name: String,
    val colorHex: Long,
    val iconName: String,
    val isDefault: Boolean = false
)

data class HabitItem(
    val id: String,
    val title: String,
    val description: String,
    val categoryId: String?,
    val colorHex: Long,
    val scheduleType: HabitScheduleType,
    val selectedWeekdays: Set<Int>, // ISO-8601: 1 (Mon) .. 7 (Sun)
    val weeklyTarget: Int, // used when scheduleType == WEEKLY_TARGET
    val reminderTimeMinutes: Int?, // minutes from midnight, optional
    val isPaused: Boolean,
    val pausedAtEpochDay: Long?,
    val isArchived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class HabitLogItem(
    val id: String,
    val habitId: String,
    val epochDay: Long,
    val completed: Boolean,
    val completedAtMillis: Long?,
    val note: String = ""
)

data class HabitStreakSummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val completedToday: Boolean,
    val scheduledToday: Boolean,
    val completedThisWeek: Int,
    val targetThisWeek: Int,
    val missedRecentDays: List<LocalDate>,
    val totalCompletions: Int
)

data class HabitWithStats(
    val habit: HabitItem,
    val category: CategoryItem?,
    val logs: List<HabitLogItem>,
    val stats: HabitStreakSummary
)

data class TaskItem(
    val id: String,
    val title: String,
    val description: String,
    val priority: TaskPriority,
    val categoryId: String?,
    val dueEpochDay: Long?,
    val dueTimeMinutes: Int?, // minutes from midnight
    val recurrence: TaskRecurrence,
    val recurrenceWeekdays: Set<Int>,
    val reminderId: String?,
    val isCompleted: Boolean,
    val completedAtMillis: Long?,
    val completionCount: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class ReminderItem(
    val id: String,
    val title: String,
    val notes: String,
    val scheduleType: ReminderScheduleType,
    val hour: Int,
    val minute: Int,
    val oneTimeEpochDay: Long?,
    val weekdays: Set<Int>,
    val dayOfMonth: Int?,
    val intervalHours: Int?,
    val nextTriggerMillis: Long,
    val isEnabled: Boolean,
    val isCompleted: Boolean,
    val snoozedUntilMillis: Long?,
    val playSound: Boolean,
    val vibrate: Boolean,
    val targetType: ReminderTargetType,
    val targetId: String?,
    val lastTriggeredMillis: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class MilestoneItem(
    val id: String,
    val goalId: String,
    val title: String,
    val isCompleted: Boolean,
    val sortOrder: Int,
    val completedAtMillis: Long?
)

data class GoalItem(
    val id: String,
    val title: String,
    val description: String,
    val categoryId: String?,
    val colorHex: Long,
    val targetEpochDay: Long?,
    val status: GoalStatus,
    val manualProgressPercent: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val completedAtMillis: Long?
)

data class GoalWithMilestones(
    val goal: GoalItem,
    val category: CategoryItem?,
    val milestones: List<MilestoneItem>,
    val progressPercent: Int
)

data class FocusSessionItem(
    val id: String,
    val type: FocusSessionType,
    val plannedDurationSeconds: Int,
    val actualDurationSeconds: Int,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val epochDay: Long,
    val completed: Boolean,
    val linkedTaskId: String?,
    val note: String = ""
)

data class JournalEntryItem(
    val id: String,
    val epochDay: Long,
    val title: String,
    val content: String,
    val achievements: String,
    val reflection: String,
    val moodLevel: Int, // 1..5
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class AppSettingsModel(
    val userName: String = "Friend",
    val themeMode: ThemeMode = ThemeMode.DARK,
    val notificationsEnabled: Boolean = true,
    val notificationSoundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val defaultSnoozeMinutes: Int = 10,
    val hapticFeedbackEnabled: Boolean = true,
    val defaultPomodoroMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val lastBackupMillis: Long? = null
)

data class DayActivitySummary(
    val date: LocalDate,
    val habitsScheduled: Int,
    val habitsCompleted: Int,
    val tasksDueOrCompleted: Int,
    val tasksCompleted: Int,
    val focusMinutes: Int,
    val hasJournalEntry: Boolean,
    val completionRatio: Float?, // null if no scheduled activities & no completions
    val intensity: Float // 0f..1f for calendar heatmap
)

data class TrendPoint(
    val label: String,
    val date: LocalDate,
    val habitRate: Float, // 0f..1f
    val tasksCompleted: Int,
    val focusMinutes: Int
)

data class ProductivityReport(
    val period: AnalyticsPeriod,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val scheduledHabitSlots: Int,
    val completedHabitSlots: Int,
    val habitCompletionRate: Float?, // null when 0 scheduled slots
    val tasksCompleted: Int,
    val tasksPending: Int,
    val tasksOverdue: Int,
    val taskCompletionRate: Float?,
    val totalFocusMinutes: Int,
    val totalFocusSessions: Int,
    val journalEntriesCount: Int,
    val activeCurrentStreak: Int,
    val bestLongestStreak: Int,
    val restDaysCount: Int,
    val heatmapDays: List<DayActivitySummary>,
    val trendPoints: List<TrendPoint>
)

fun Set<Int>.sortedWeekdaysLabel(): String {
    if (isEmpty()) return "None"
    if (size == 7) return "Every day"
    val names = mapOf(
        DayOfWeek.MONDAY.value to "Mon",
        DayOfWeek.TUESDAY.value to "Tue",
        DayOfWeek.WEDNESDAY.value to "Wed",
        DayOfWeek.THURSDAY.value to "Thu",
        DayOfWeek.FRIDAY.value to "Fri",
        DayOfWeek.SATURDAY.value to "Sat",
        DayOfWeek.SUNDAY.value to "Sun"
    )
    return sorted().mapNotNull { names[it] }.joinToString(", ")
}
