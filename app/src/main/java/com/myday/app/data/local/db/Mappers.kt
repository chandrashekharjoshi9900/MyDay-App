package com.myday.app.data.local.db

import com.myday.app.domain.model.AppSettingsModel
import com.myday.app.domain.model.CategoryItem
import com.myday.app.domain.model.FocusSessionItem
import com.myday.app.domain.model.FocusSessionType
import com.myday.app.domain.model.GoalItem
import com.myday.app.domain.model.GoalStatus
import com.myday.app.domain.model.HabitItem
import com.myday.app.domain.model.HabitLogItem
import com.myday.app.domain.model.HabitScheduleType
import com.myday.app.domain.model.JournalEntryItem
import com.myday.app.domain.model.MilestoneItem
import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.ReminderScheduleType
import com.myday.app.domain.model.ReminderTargetType
import com.myday.app.domain.model.TaskItem
import com.myday.app.domain.model.TaskPriority
import com.myday.app.domain.model.TaskRecurrence
import com.myday.app.domain.model.ThemeMode

private fun String.toWeekdaySet(): Set<Int> {
    if (isBlank()) return emptySet()
    return split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .filter { it in 1..7 }
        .toSet()
}

private fun Set<Int>.toCsv(): String = sorted().joinToString(",")

fun CategoryEntity.toDomain(): CategoryItem = CategoryItem(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    isDefault = isDefault
)

fun CategoryItem.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    iconName = iconName,
    isDefault = isDefault
)

fun HabitEntity.toDomain(): HabitItem = HabitItem(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    colorHex = colorHex,
    scheduleType = runCatching { HabitScheduleType.valueOf(scheduleType) }.getOrDefault(HabitScheduleType.DAILY),
    selectedWeekdays = selectedWeekdaysCsv.toWeekdaySet(),
    weeklyTarget = weeklyTarget,
    reminderTimeMinutes = reminderTimeMinutes,
    isPaused = isPaused,
    pausedAtEpochDay = pausedAtEpochDay,
    isArchived = isArchived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun HabitItem.toEntity(): HabitEntity = HabitEntity(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    colorHex = colorHex,
    scheduleType = scheduleType.name,
    selectedWeekdaysCsv = selectedWeekdays.toCsv(),
    weeklyTarget = weeklyTarget,
    reminderTimeMinutes = reminderTimeMinutes,
    isPaused = isPaused,
    pausedAtEpochDay = pausedAtEpochDay,
    isArchived = isArchived,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun HabitLogEntity.toDomain(): HabitLogItem = HabitLogItem(
    id = id,
    habitId = habitId,
    epochDay = epochDay,
    completed = completed,
    completedAtMillis = completedAtMillis,
    note = note
)

fun HabitLogItem.toEntity(): HabitLogEntity = HabitLogEntity(
    id = id,
    habitId = habitId,
    epochDay = epochDay,
    completed = completed,
    completedAtMillis = completedAtMillis,
    note = note
)

fun TaskEntity.toDomain(): TaskItem = TaskItem(
    id = id,
    title = title,
    description = description,
    priority = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.MEDIUM),
    categoryId = categoryId,
    dueEpochDay = dueEpochDay,
    dueTimeMinutes = dueTimeMinutes,
    recurrence = runCatching { TaskRecurrence.valueOf(recurrence) }.getOrDefault(TaskRecurrence.NONE),
    recurrenceWeekdays = recurrenceWeekdaysCsv.toWeekdaySet(),
    reminderId = reminderId,
    isCompleted = isCompleted,
    completedAtMillis = completedAtMillis,
    completionCount = completionCount,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun TaskItem.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    priority = priority.name,
    categoryId = categoryId,
    dueEpochDay = dueEpochDay,
    dueTimeMinutes = dueTimeMinutes,
    recurrence = recurrence.name,
    recurrenceWeekdaysCsv = recurrenceWeekdays.toCsv(),
    reminderId = reminderId,
    isCompleted = isCompleted,
    completedAtMillis = completedAtMillis,
    completionCount = completionCount,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun ReminderEntity.toDomain(): ReminderItem = ReminderItem(
    id = id,
    title = title,
    notes = notes,
    scheduleType = runCatching { ReminderScheduleType.valueOf(scheduleType) }
        .getOrDefault(ReminderScheduleType.ONE_TIME),
    hour = hour,
    minute = minute,
    oneTimeEpochDay = oneTimeEpochDay,
    weekdays = weekdaysCsv.toWeekdaySet(),
    dayOfMonth = dayOfMonth,
    intervalHours = intervalHours,
    nextTriggerMillis = nextTriggerMillis,
    isEnabled = isEnabled,
    isCompleted = isCompleted,
    snoozedUntilMillis = snoozedUntilMillis,
    playSound = playSound,
    vibrate = vibrate,
    targetType = runCatching { ReminderTargetType.valueOf(targetType) }
        .getOrDefault(ReminderTargetType.STANDALONE),
    targetId = targetId,
    lastTriggeredMillis = lastTriggeredMillis,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun ReminderItem.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    title = title,
    notes = notes,
    scheduleType = scheduleType.name,
    hour = hour,
    minute = minute,
    oneTimeEpochDay = oneTimeEpochDay,
    weekdaysCsv = weekdays.toCsv(),
    dayOfMonth = dayOfMonth,
    intervalHours = intervalHours,
    nextTriggerMillis = nextTriggerMillis,
    isEnabled = isEnabled,
    isCompleted = isCompleted,
    snoozedUntilMillis = snoozedUntilMillis,
    playSound = playSound,
    vibrate = vibrate,
    targetType = targetType.name,
    targetId = targetId,
    lastTriggeredMillis = lastTriggeredMillis,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun GoalEntity.toDomain(): GoalItem = GoalItem(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    colorHex = colorHex,
    targetEpochDay = targetEpochDay,
    status = runCatching { GoalStatus.valueOf(status) }.getOrDefault(GoalStatus.ACTIVE),
    manualProgressPercent = manualProgressPercent,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    completedAtMillis = completedAtMillis
)

fun GoalItem.toEntity(): GoalEntity = GoalEntity(
    id = id,
    title = title,
    description = description,
    categoryId = categoryId,
    colorHex = colorHex,
    targetEpochDay = targetEpochDay,
    status = status.name,
    manualProgressPercent = manualProgressPercent,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis,
    completedAtMillis = completedAtMillis
)

fun MilestoneEntity.toDomain(): MilestoneItem = MilestoneItem(
    id = id,
    goalId = goalId,
    title = title,
    isCompleted = isCompleted,
    sortOrder = sortOrder,
    completedAtMillis = completedAtMillis
)

fun MilestoneItem.toEntity(): MilestoneEntity = MilestoneEntity(
    id = id,
    goalId = goalId,
    title = title,
    isCompleted = isCompleted,
    sortOrder = sortOrder,
    completedAtMillis = completedAtMillis
)

fun FocusSessionEntity.toDomain(): FocusSessionItem = FocusSessionItem(
    id = id,
    type = runCatching { FocusSessionType.valueOf(type) }.getOrDefault(FocusSessionType.POMODORO),
    plannedDurationSeconds = plannedDurationSeconds,
    actualDurationSeconds = actualDurationSeconds,
    startedAtMillis = startedAtMillis,
    endedAtMillis = endedAtMillis,
    epochDay = epochDay,
    completed = completed,
    linkedTaskId = linkedTaskId,
    note = note
)

fun FocusSessionItem.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id = id,
    type = type.name,
    plannedDurationSeconds = plannedDurationSeconds,
    actualDurationSeconds = actualDurationSeconds,
    startedAtMillis = startedAtMillis,
    endedAtMillis = endedAtMillis,
    epochDay = epochDay,
    completed = completed,
    linkedTaskId = linkedTaskId,
    note = note
)

fun JournalEntryEntity.toDomain(): JournalEntryItem = JournalEntryItem(
    id = id,
    epochDay = epochDay,
    title = title,
    content = content,
    achievements = achievements,
    reflection = reflection,
    moodLevel = moodLevel,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun JournalEntryItem.toEntity(): JournalEntryEntity = JournalEntryEntity(
    id = id,
    epochDay = epochDay,
    title = title,
    content = content,
    achievements = achievements,
    reflection = reflection,
    moodLevel = moodLevel,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

fun AppSettingsEntity.toDomain(): AppSettingsModel = AppSettingsModel(
    userName = userName,
    themeMode = runCatching { ThemeMode.valueOf(themeMode) }.getOrDefault(ThemeMode.DARK),
    notificationsEnabled = notificationsEnabled,
    notificationSoundEnabled = notificationSoundEnabled,
    vibrationEnabled = vibrationEnabled,
    defaultSnoozeMinutes = defaultSnoozeMinutes,
    hapticFeedbackEnabled = hapticFeedbackEnabled,
    defaultPomodoroMinutes = defaultPomodoroMinutes,
    shortBreakMinutes = shortBreakMinutes,
    longBreakMinutes = longBreakMinutes,
    lastBackupMillis = lastBackupMillis
)

fun AppSettingsModel.toEntity(): AppSettingsEntity = AppSettingsEntity(
    id = "default",
    userName = userName,
    themeMode = themeMode.name,
    notificationsEnabled = notificationsEnabled,
    notificationSoundEnabled = notificationSoundEnabled,
    vibrationEnabled = vibrationEnabled,
    defaultSnoozeMinutes = defaultSnoozeMinutes,
    hapticFeedbackEnabled = hapticFeedbackEnabled,
    defaultPomodoroMinutes = defaultPomodoroMinutes,
    shortBreakMinutes = shortBreakMinutes,
    longBreakMinutes = longBreakMinutes,
    lastBackupMillis = lastBackupMillis
)
