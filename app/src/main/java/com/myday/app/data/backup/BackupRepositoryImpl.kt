package com.myday.app.data.backup

import com.myday.app.BuildConfig
import com.myday.app.data.local.datastore.UserPreferencesDataStore
import com.myday.app.data.local.db.AppSettingsEntity
import com.myday.app.data.local.db.CategoryEntity
import com.myday.app.data.local.db.FocusSessionEntity
import com.myday.app.data.local.db.GoalEntity
import com.myday.app.data.local.db.HabitEntity
import com.myday.app.data.local.db.HabitLogEntity
import com.myday.app.data.local.db.JournalEntryEntity
import com.myday.app.data.local.db.MilestoneEntity
import com.myday.app.data.local.db.MyDayDatabase
import com.myday.app.data.local.db.ReminderEntity
import com.myday.app.data.local.db.TaskEntity
import com.myday.app.data.local.db.toDomain
import com.myday.app.data.local.db.toEntity
import com.myday.app.domain.model.AppSettingsModel
import com.myday.app.domain.repository.BackupPreview
import com.myday.app.domain.repository.BackupRepository
import com.myday.app.domain.repository.BackupValidationResult
import com.myday.app.domain.repository.ReminderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

@Serializable
data class BackupCategoryDto(
    val id: String,
    val name: String,
    val colorHex: Long,
    val iconName: String,
    val isDefault: Boolean
)

@Serializable
data class BackupHabitDto(
    val id: String,
    val title: String,
    val description: String,
    val categoryId: String? = null,
    val colorHex: Long,
    val scheduleType: String,
    val selectedWeekdaysCsv: String = "",
    val weeklyTarget: Int = 3,
    val reminderTimeMinutes: Int? = null,
    val isPaused: Boolean = false,
    val pausedAtEpochDay: Long? = null,
    val isArchived: Boolean = false,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Serializable
data class BackupHabitLogDto(
    val id: String,
    val habitId: String,
    val epochDay: Long,
    val completed: Boolean,
    val completedAtMillis: Long? = null,
    val note: String = ""
)

@Serializable
data class BackupTaskDto(
    val id: String,
    val title: String,
    val description: String = "",
    val priority: String,
    val categoryId: String? = null,
    val dueEpochDay: Long? = null,
    val dueTimeMinutes: Int? = null,
    val recurrence: String = "NONE",
    val recurrenceWeekdaysCsv: String = "",
    val reminderId: String? = null,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val completionCount: Int = 0,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Serializable
data class BackupReminderDto(
    val id: String,
    val title: String,
    val notes: String = "",
    val scheduleType: String,
    val hour: Int,
    val minute: Int,
    val oneTimeEpochDay: Long? = null,
    val weekdaysCsv: String = "",
    val dayOfMonth: Int? = null,
    val intervalHours: Int? = null,
    val nextTriggerMillis: Long,
    val isEnabled: Boolean = true,
    val isCompleted: Boolean = false,
    val snoozedUntilMillis: Long? = null,
    val playSound: Boolean = true,
    val vibrate: Boolean = true,
    val targetType: String = "STANDALONE",
    val targetId: String? = null,
    val lastTriggeredMillis: Long? = null,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Serializable
data class BackupGoalDto(
    val id: String,
    val title: String,
    val description: String = "",
    val categoryId: String? = null,
    val colorHex: Long,
    val targetEpochDay: Long? = null,
    val status: String,
    val manualProgressPercent: Int = 0,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val completedAtMillis: Long? = null
)

@Serializable
data class BackupMilestoneDto(
    val id: String,
    val goalId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int = 0,
    val completedAtMillis: Long? = null
)

@Serializable
data class BackupFocusSessionDto(
    val id: String,
    val type: String,
    val plannedDurationSeconds: Int,
    val actualDurationSeconds: Int,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val epochDay: Long,
    val completed: Boolean = true,
    val linkedTaskId: String? = null,
    val note: String = ""
)

@Serializable
data class BackupJournalEntryDto(
    val id: String,
    val epochDay: Long,
    val title: String,
    val content: String,
    val achievements: String = "",
    val reflection: String = "",
    val moodLevel: Int = 3,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Serializable
data class BackupAppSettingsDto(
    val userName: String = "Friend",
    val themeMode: String = "DARK",
    val notificationsEnabled: Boolean = true,
    val notificationSoundEnabled: Boolean = true,
    val reminderTone: String = "CHIME",
    val vibrationEnabled: Boolean = true,
    val defaultSnoozeMinutes: Int = 10,
    val hapticFeedbackEnabled: Boolean = true,
    val defaultPomodoroMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val lastBackupMillis: Long? = null
)

@Serializable
data class BackupDataDto(
    val categories: List<BackupCategoryDto> = emptyList(),
    val habits: List<BackupHabitDto> = emptyList(),
    val habitLogs: List<BackupHabitLogDto> = emptyList(),
    val tasks: List<BackupTaskDto> = emptyList(),
    val reminders: List<BackupReminderDto> = emptyList(),
    val goals: List<BackupGoalDto> = emptyList(),
    val milestones: List<BackupMilestoneDto> = emptyList(),
    val focusSessions: List<BackupFocusSessionDto> = emptyList(),
    val journalEntries: List<BackupJournalEntryDto> = emptyList(),
    val settings: BackupAppSettingsDto = BackupAppSettingsDto()
)

@Serializable
data class MyDayBackupEnvelope(
    val appId: String = "MYDAY",
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appVersion: String = BuildConfig.VERSION_NAME,
    val exportedAtMillis: Long,
    val checksumSha256: String,
    val data: BackupDataDto
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

object BackupValidator {

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val compactJson = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun computeChecksum(data: BackupDataDto): String {
        val raw = compactJson.encodeToString(data)
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun validate(rawJson: String): Result<MyDayBackupEnvelope> {
        if (rawJson.isBlank()) {
            return Result.failure(IllegalArgumentException("Backup file is empty."))
        }
        val envelope = runCatching {
            json.decodeFromString<MyDayBackupEnvelope>(rawJson)
        }.getOrElse { err ->
            return Result.failure(IllegalArgumentException("Invalid JSON format: ${err.message}"))
        }

        if (envelope.appId != "MYDAY") {
            return Result.failure(IllegalArgumentException("This file is not a MYDAY backup."))
        }
        if (envelope.schemaVersion < 1 || envelope.schemaVersion > MyDayBackupEnvelope.CURRENT_SCHEMA_VERSION) {
            return Result.failure(
                IllegalArgumentException("Unsupported backup schema version: ${envelope.schemaVersion}.")
            )
        }
        val expectedChecksum = computeChecksum(envelope.data)
        if (!envelope.checksumSha256.equals(expectedChecksum, ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException("Backup checksum verification failed. File may be corrupted or modified.")
            )
        }

        val habitIds = envelope.data.habits.map { it.id }.toSet()
        val orphanLogs = envelope.data.habitLogs.any { it.habitId !in habitIds }
        if (orphanLogs) {
            return Result.failure(
                IllegalArgumentException("Backup contains habit logs referencing missing habits.")
            )
        }
        val goalIds = envelope.data.goals.map { it.id }.toSet()
        val orphanMilestones = envelope.data.milestones.any { it.goalId !in goalIds }
        if (orphanMilestones) {
            return Result.failure(
                IllegalArgumentException("Backup contains milestones referencing missing goals.")
            )
        }

        return Result.success(envelope)
    }

    fun toPreview(envelope: MyDayBackupEnvelope): BackupPreview = BackupPreview(
        schemaVersion = envelope.schemaVersion,
        exportedAtMillis = envelope.exportedAtMillis,
        appVersion = envelope.appVersion,
        categoriesCount = envelope.data.categories.size,
        habitsCount = envelope.data.habits.size,
        habitLogsCount = envelope.data.habitLogs.size,
        tasksCount = envelope.data.tasks.size,
        remindersCount = envelope.data.reminders.size,
        goalsCount = envelope.data.goals.size,
        milestonesCount = envelope.data.milestones.size,
        focusSessionsCount = envelope.data.focusSessions.size,
        journalEntriesCount = envelope.data.journalEntries.size
    )
}

class BackupRepositoryImpl(
    private val db: MyDayDatabase,
    private val preferencesDataStore: UserPreferencesDataStore,
    private val reminderRepository: ReminderRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : BackupRepository {

    override suspend fun exportJson(): String = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val currentSettings = preferencesDataStore.getSettings().copy(lastBackupMillis = now)
        preferencesDataStore.saveSettings(currentSettings)
        db.appSettingsDao().upsert(currentSettings.toEntity())

        val data = BackupDataDto(
            categories = db.categoryDao().getAll().map {
                BackupCategoryDto(it.id, it.name, it.colorHex, it.iconName, it.isDefault)
            },
            habits = db.habitDao().getAll().map {
                BackupHabitDto(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    categoryId = it.categoryId,
                    colorHex = it.colorHex,
                    scheduleType = it.scheduleType,
                    selectedWeekdaysCsv = it.selectedWeekdaysCsv,
                    weeklyTarget = it.weeklyTarget,
                    reminderTimeMinutes = it.reminderTimeMinutes,
                    isPaused = it.isPaused,
                    pausedAtEpochDay = it.pausedAtEpochDay,
                    isArchived = it.isArchived,
                    createdAtMillis = it.createdAtMillis,
                    updatedAtMillis = it.updatedAtMillis
                )
            },
            habitLogs = db.habitLogDao().getAll().map {
                BackupHabitLogDto(it.id, it.habitId, it.epochDay, it.completed, it.completedAtMillis, it.note)
            },
            tasks = db.taskDao().getAll().map {
                BackupTaskDto(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    priority = it.priority,
                    categoryId = it.categoryId,
                    dueEpochDay = it.dueEpochDay,
                    dueTimeMinutes = it.dueTimeMinutes,
                    recurrence = it.recurrence,
                    recurrenceWeekdaysCsv = it.recurrenceWeekdaysCsv,
                    reminderId = it.reminderId,
                    isCompleted = it.isCompleted,
                    completedAtMillis = it.completedAtMillis,
                    completionCount = it.completionCount,
                    createdAtMillis = it.createdAtMillis,
                    updatedAtMillis = it.updatedAtMillis
                )
            },
            reminders = db.reminderDao().getAll().map {
                BackupReminderDto(
                    id = it.id,
                    title = it.title,
                    notes = it.notes,
                    scheduleType = it.scheduleType,
                    hour = it.hour,
                    minute = it.minute,
                    oneTimeEpochDay = it.oneTimeEpochDay,
                    weekdaysCsv = it.weekdaysCsv,
                    dayOfMonth = it.dayOfMonth,
                    intervalHours = it.intervalHours,
                    nextTriggerMillis = it.nextTriggerMillis,
                    isEnabled = it.isEnabled,
                    isCompleted = it.isCompleted,
                    snoozedUntilMillis = it.snoozedUntilMillis,
                    playSound = it.playSound,
                    vibrate = it.vibrate,
                    targetType = it.targetType,
                    targetId = it.targetId,
                    lastTriggeredMillis = it.lastTriggeredMillis,
                    createdAtMillis = it.createdAtMillis,
                    updatedAtMillis = it.updatedAtMillis
                )
            },
            goals = db.goalDao().getAll().map {
                BackupGoalDto(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    categoryId = it.categoryId,
                    colorHex = it.colorHex,
                    targetEpochDay = it.targetEpochDay,
                    status = it.status,
                    manualProgressPercent = it.manualProgressPercent,
                    createdAtMillis = it.createdAtMillis,
                    updatedAtMillis = it.updatedAtMillis,
                    completedAtMillis = it.completedAtMillis
                )
            },
            milestones = db.milestoneDao().getAll().map {
                BackupMilestoneDto(it.id, it.goalId, it.title, it.isCompleted, it.sortOrder, it.completedAtMillis)
            },
            focusSessions = db.focusSessionDao().getAll().map {
                BackupFocusSessionDto(
                    id = it.id,
                    type = it.type,
                    plannedDurationSeconds = it.plannedDurationSeconds,
                    actualDurationSeconds = it.actualDurationSeconds,
                    startedAtMillis = it.startedAtMillis,
                    endedAtMillis = it.endedAtMillis,
                    epochDay = it.epochDay,
                    completed = it.completed,
                    linkedTaskId = it.linkedTaskId,
                    note = it.note
                )
            },
            journalEntries = db.journalEntryDao().getAll().map {
                BackupJournalEntryDto(
                    id = it.id,
                    epochDay = it.epochDay,
                    title = it.title,
                    content = it.content,
                    achievements = it.achievements,
                    reflection = it.reflection,
                    moodLevel = it.moodLevel,
                    createdAtMillis = it.createdAtMillis,
                    updatedAtMillis = it.updatedAtMillis
                )
            },
            settings = BackupAppSettingsDto(
                userName = currentSettings.userName,
                themeMode = currentSettings.themeMode.name,
                notificationsEnabled = currentSettings.notificationsEnabled,
                notificationSoundEnabled = currentSettings.notificationSoundEnabled,
                reminderTone = currentSettings.reminderTone.name,
                vibrationEnabled = currentSettings.vibrationEnabled,
                defaultSnoozeMinutes = currentSettings.defaultSnoozeMinutes,
                hapticFeedbackEnabled = currentSettings.hapticFeedbackEnabled,
                defaultPomodoroMinutes = currentSettings.defaultPomodoroMinutes,
                shortBreakMinutes = currentSettings.shortBreakMinutes,
                longBreakMinutes = currentSettings.longBreakMinutes,
                lastBackupMillis = currentSettings.lastBackupMillis
            )
        )

        val envelope = MyDayBackupEnvelope(
            exportedAtMillis = now,
            checksumSha256 = BackupValidator.computeChecksum(data),
            data = data
        )
        BackupValidator.json.encodeToString(envelope)
    }

    override suspend fun validateBackupJson(json: String): BackupValidationResult =
        withContext(ioDispatcher) {
            BackupValidator.validate(json).fold(
                onSuccess = { envelope ->
                    BackupValidationResult.Valid(
                        preview = BackupValidator.toPreview(envelope),
                        rawJson = json
                    )
                },
                onFailure = { error ->
                    BackupValidationResult.Invalid(
                        error.message ?: "Unable to parse backup file."
                    )
                }
            )
        }

    override suspend fun restoreFromJson(json: String): Result<BackupPreview> =
        withContext(ioDispatcher) {
            val envelope = BackupValidator.validate(json).getOrElse {
                return@withContext Result.failure(it)
            }

            val data = envelope.data
            val categories = data.categories.map {
                CategoryEntity(it.id, it.name, it.colorHex, it.iconName, it.isDefault)
            }.ifEmpty { MyDayDatabase.DEFAULT_CATEGORIES }
            val validCategoryIds = categories.map { it.id }.toSet()

            val habits = data.habits.map {
                HabitEntity(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    categoryId = it.categoryId?.takeIf { cid -> cid in validCategoryIds },
                    colorHex = it.colorHex,
                    scheduleType = it.scheduleType,
                    selectedWeekdaysCsv = it.selectedWeekdaysCsv,
                    weeklyTarget = it.weeklyTarget,
                    reminderTimeMinutes = it.reminderTimeMinutes,
                    isPaused = it.isPaused,
                    pausedAtEpochDay = it.pausedAtEpochDay,
                    isArchived = it.isArchived,
                    createdAtMillis = it.createdAtMillis,
                    updatedAtMillis = it.updatedAtMillis
                )
            }

            val habitLogs = data.habitLogs.map {
                HabitLogEntity(it.id, it.habitId, it.epochDay, it.completed, it.completedAtMillis, it.note)
            }

            val tasks = data.tasks.map {
                TaskEntity(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    priority = it.priority,
                    categoryId = it.categoryId?.takeIf { cid -> cid in validCategoryIds },
                    dueEpochDay = it.dueEpochDay,
                    dueTimeMinutes = it.dueTimeMinutes,
                    recurrence = it.recurrence,
                    recurrenceWeekdaysCsv = it.recurrenceWeekdaysCsv,
                    reminderId = it.reminderId,
                    isCompleted = it.isCompleted,
                    completedAtMillis = it.completedAtMillis,
                    completionCount = it.completionCount,
                    createdAtMillis = it.createdAtMillis,
                    updatedAtMillis = it.updatedAtMillis
                )
            }
            val validTaskIds = tasks.map { it.id }.toSet()

            val reminders = data.reminders.map {
                ReminderEntity(
                    id = it.id,
                    title = it.title,
                    notes = it.notes,
                    scheduleType = it.scheduleType,
                    hour = it.hour,
                    minute = it.minute,
                    oneTimeEpochDay = it.oneTimeEpochDay,
                    weekdaysCsv = it.weekdaysCsv,
                    dayOfMonth = it.dayOfMonth,
                    intervalHours = it.intervalHours,
                    nextTriggerMillis = it.nextTriggerMillis,
                    isEnabled = it.isEnabled,
                    isCompleted = it.isCompleted,
                    snoozedUntilMillis = it.snoozedUntilMillis,
                    playSound = it.playSound,
                    vibrate = it.vibrate,
                    targetType = it.targetType,
                    targetId = it.targetId,
                    lastTriggeredMillis = it.lastTriggeredMillis,
                    createdAtMillis = it.createdAtMillis,
                    updatedAtMillis = it.updatedAtMillis
                )
            }

            val goals = data.goals.map {
                GoalEntity(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    categoryId = it.categoryId?.takeIf { cid -> cid in validCategoryIds },
                    colorHex = it.colorHex,
                    targetEpochDay = it.targetEpochDay,
                    status = it.status,
                    manualProgressPercent = it.manualProgressPercent,
                    createdAtMillis = it.createdAtMillis,
                    updatedAtMillis = it.updatedAtMillis,
                    completedAtMillis = it.completedAtMillis
                )
            }

            val milestones = data.milestones.map {
                MilestoneEntity(it.id, it.goalId, it.title, it.isCompleted, it.sortOrder, it.completedAtMillis)
            }

            val focusSessions = data.focusSessions.map {
                FocusSessionEntity(
                    id = it.id,
                    type = it.type,
                    plannedDurationSeconds = it.plannedDurationSeconds,
                    actualDurationSeconds = it.actualDurationSeconds,
                    startedAtMillis = it.startedAtMillis,
                    endedAtMillis = it.endedAtMillis,
                    epochDay = it.epochDay,
                    completed = it.completed,
                    linkedTaskId = it.linkedTaskId?.takeIf { tid -> tid in validTaskIds },
                    note = it.note
                )
            }

            val journalEntries = data.journalEntries.map {
                JournalEntryEntity(
                    id = it.id,
                    epochDay = it.epochDay,
                    title = it.title,
                    content = it.content,
                    achievements = it.achievements,
                    reflection = it.reflection,
                    moodLevel = it.moodLevel,
                    createdAtMillis = it.createdAtMillis,
                    updatedAtMillis = it.updatedAtMillis
                )
            }

            val settingsEntity = AppSettingsEntity(
                id = "default",
                userName = data.settings.userName,
                themeMode = data.settings.themeMode,
                notificationsEnabled = data.settings.notificationsEnabled,
                notificationSoundEnabled = data.settings.notificationSoundEnabled,
                reminderTone = data.settings.reminderTone,
                vibrationEnabled = data.settings.vibrationEnabled,
                defaultSnoozeMinutes = data.settings.defaultSnoozeMinutes,
                hapticFeedbackEnabled = data.settings.hapticFeedbackEnabled,
                defaultPomodoroMinutes = data.settings.defaultPomodoroMinutes,
                shortBreakMinutes = data.settings.shortBreakMinutes,
                longBreakMinutes = data.settings.longBreakMinutes,
                lastBackupMillis = data.settings.lastBackupMillis
            )

            // Cancel existing alarms before wiping/restoring
            db.reminderDao().getAll().forEach {
                reminderRepository.deleteReminder(it.id)
            }

            db.replaceAllDataAtomically(
                categories = categories,
                habits = habits,
                habitLogs = habitLogs,
                tasks = tasks,
                reminders = reminders,
                goals = goals,
                milestones = milestones,
                focusSessions = focusSessions,
                journalEntries = journalEntries,
                appSettings = settingsEntity
            )

            val restoredModel: AppSettingsModel = settingsEntity.toDomain()
            preferencesDataStore.saveSettings(restoredModel)
            reminderRepository.rescheduleAllActiveReminders()

            Result.success(BackupValidator.toPreview(envelope))
        }
}
