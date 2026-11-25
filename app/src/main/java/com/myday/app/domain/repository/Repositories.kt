package com.myday.app.domain.repository

import com.myday.app.domain.model.AppSettingsModel
import com.myday.app.domain.model.CategoryItem
import com.myday.app.domain.model.FocusSessionItem
import com.myday.app.domain.model.GoalItem
import com.myday.app.domain.model.GoalStatus
import com.myday.app.domain.model.GoalWithMilestones
import com.myday.app.domain.model.HabitItem
import com.myday.app.domain.model.HabitLogItem
import com.myday.app.domain.model.HabitWithStats
import com.myday.app.domain.model.JournalEntryItem
import com.myday.app.domain.model.MilestoneItem
import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.TaskItem
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeAll(): Flow<List<CategoryItem>>
    suspend fun getAll(): List<CategoryItem>
    suspend fun upsert(category: CategoryItem)
    suspend fun delete(id: String)
}

interface HabitRepository {
    fun observeAllHabits(): Flow<List<HabitItem>>
    fun observeAllLogs(): Flow<List<HabitLogItem>>
    fun observeHabitsWithStats(includeArchived: Boolean = false): Flow<List<HabitWithStats>>
    suspend fun getHabitById(id: String): HabitItem?
    suspend fun upsertHabit(habit: HabitItem)
    suspend fun setArchived(habitId: String, archived: Boolean)
    suspend fun setPaused(habitId: String, paused: Boolean, pausedAtEpochDay: Long?)
    suspend fun deleteHabit(habitId: String)
    suspend fun toggleHabitCompletion(habitId: String, epochDay: Long, note: String = "")
    suspend fun setHabitCompletion(habitId: String, epochDay: Long, completed: Boolean, note: String = "")
}

interface TaskRepository {
    fun observeAllTasks(): Flow<List<TaskItem>>
    suspend fun getTaskById(id: String): TaskItem?
    suspend fun upsertTask(task: TaskItem)
    suspend fun toggleTaskCompletion(taskId: String)
    suspend fun deleteTask(taskId: String)
}

interface ReminderRepository {
    fun observeAllReminders(): Flow<List<ReminderItem>>
    suspend fun getAllReminders(): List<ReminderItem>
    suspend fun getReminderById(id: String): ReminderItem?
    suspend fun upsertAndSchedule(reminder: ReminderItem): ReminderItem
    suspend fun toggleEnabled(reminderId: String, enabled: Boolean)
    suspend fun markCompleted(reminderId: String)
    suspend fun snoozeReminder(reminderId: String, snoozeMinutes: Int)
    suspend fun dismissCurrentOccurrence(reminderId: String)
    suspend fun deleteReminder(reminderId: String)
    suspend fun rescheduleAllActiveReminders()
}

interface GoalRepository {
    fun observeGoalsWithMilestones(): Flow<List<GoalWithMilestones>>
    suspend fun upsertGoal(goal: GoalItem)
    suspend fun updateGoalStatus(goalId: String, status: GoalStatus)
    suspend fun deleteGoal(goalId: String)
    suspend fun upsertMilestone(milestone: MilestoneItem)
    suspend fun toggleMilestone(milestoneId: String)
    suspend fun deleteMilestone(milestoneId: String)
}

interface FocusRepository {
    fun observeAllSessions(): Flow<List<FocusSessionItem>>
    suspend fun recordSession(session: FocusSessionItem)
    suspend fun deleteSession(sessionId: String)
}

interface JournalRepository {
    fun observeAllEntries(): Flow<List<JournalEntryItem>>
    suspend fun getEntryById(id: String): JournalEntryItem?
    suspend fun upsertEntry(entry: JournalEntryItem)
    suspend fun deleteEntry(entryId: String)
}

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettingsModel>
    suspend fun getSettings(): AppSettingsModel
    suspend fun updateSettings(transform: (AppSettingsModel) -> AppSettingsModel)
}

data class BackupPreview(
    val schemaVersion: Int,
    val exportedAtMillis: Long,
    val appVersion: String,
    val categoriesCount: Int,
    val habitsCount: Int,
    val habitLogsCount: Int,
    val tasksCount: Int,
    val remindersCount: Int,
    val goalsCount: Int,
    val milestonesCount: Int,
    val focusSessionsCount: Int,
    val journalEntriesCount: Int
)

sealed interface BackupValidationResult {
    data class Valid(val preview: BackupPreview, val rawJson: String) : BackupValidationResult
    data class Invalid(val reason: String) : BackupValidationResult
}

interface BackupRepository {
    suspend fun exportJson(): String
    suspend fun validateBackupJson(json: String): BackupValidationResult
    suspend fun restoreFromJson(json: String): Result<BackupPreview>
}
