package com.myday.app.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: Long,
    val iconName: String,
    val isDefault: Boolean
)

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["isArchived"])
    ]
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val categoryId: String?,
    val colorHex: Long,
    val scheduleType: String,
    val selectedWeekdaysCsv: String,
    val weeklyTarget: Int,
    val reminderTimeMinutes: Int?,
    val isPaused: Boolean,
    val pausedAtEpochDay: Long?,
    val isArchived: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["habitId"]),
        Index(value = ["epochDay"]),
        Index(value = ["habitId", "epochDay"], unique = true)
    ]
)
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val epochDay: Long,
    val completed: Boolean,
    val completedAtMillis: Long?,
    val note: String
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["isCompleted"]),
        Index(value = ["dueEpochDay"]),
        Index(value = ["reminderId"])
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priority: String,
    val categoryId: String?,
    val dueEpochDay: Long?,
    val dueTimeMinutes: Int?,
    val recurrence: String,
    val recurrenceWeekdaysCsv: String,
    val reminderId: String?,
    val isCompleted: Boolean,
    val completedAtMillis: Long?,
    val completionCount: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["isEnabled"]),
        Index(value = ["nextTriggerMillis"]),
        Index(value = ["targetType", "targetId"])
    ]
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val notes: String,
    val scheduleType: String,
    val hour: Int,
    val minute: Int,
    val oneTimeEpochDay: Long?,
    val weekdaysCsv: String,
    val dayOfMonth: Int?,
    val intervalHours: Int?,
    val nextTriggerMillis: Long,
    val isEnabled: Boolean,
    val isCompleted: Boolean,
    val snoozedUntilMillis: Long?,
    val playSound: Boolean,
    val vibrate: Boolean,
    val targetType: String,
    val targetId: String?,
    val lastTriggeredMillis: Long?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["status"])
    ]
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val categoryId: String?,
    val colorHex: Long,
    val targetEpochDay: Long?,
    val status: String,
    val manualProgressPercent: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val completedAtMillis: Long?
)

@Entity(
    tableName = "milestones",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goalId"]),
        Index(value = ["sortOrder"])
    ]
)
data class MilestoneEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val title: String,
    val isCompleted: Boolean,
    val sortOrder: Int,
    val completedAtMillis: Long?
)

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["linkedTaskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["epochDay"]),
        Index(value = ["linkedTaskId"])
    ]
)
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val plannedDurationSeconds: Int,
    val actualDurationSeconds: Int,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val epochDay: Long,
    val completed: Boolean,
    val linkedTaskId: String?,
    val note: String
)

@Entity(
    tableName = "journal_entries",
    indices = [
        Index(value = ["epochDay"])
    ]
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val epochDay: Long,
    val title: String,
    val content: String,
    val achievements: String,
    val reflection: String,
    val moodLevel: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: String = "default",
    val userName: String,
    val themeMode: String,
    val notificationsEnabled: Boolean,
    val notificationSoundEnabled: Boolean,
    val reminderTone: String,
    val vibrationEnabled: Boolean,
    val defaultSnoozeMinutes: Int,
    val hapticFeedbackEnabled: Boolean,
    val defaultPomodoroMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val lastBackupMillis: Long?
)
