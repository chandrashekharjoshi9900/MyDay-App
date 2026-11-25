package com.myday.app.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY isDefault DESC, name ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: String): HabitEntity?

    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Upsert
    suspend fun upsertAll(habits: List<HabitEntity>)

    @Query("UPDATE habits SET isArchived = :archived, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun setArchived(id: String, archived: Boolean, updatedAtMillis: Long)

    @Query("UPDATE habits SET isPaused = :paused, pausedAtEpochDay = :pausedAtEpochDay, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun setPaused(id: String, paused: Boolean, pausedAtEpochDay: Long?, updatedAtMillis: Long)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM habits")
    suspend fun clearAll()
}

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs ORDER BY epochDay DESC")
    fun observeAll(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs ORDER BY epochDay DESC")
    suspend fun getAll(): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay LIMIT 1")
    suspend fun getByHabitAndDay(habitId: String, epochDay: Long): HabitLogEntity?

    @Upsert
    suspend fun upsert(log: HabitLogEntity)

    @Upsert
    suspend fun upsertAll(logs: List<HabitLogEntity>)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteByHabitAndDay(habitId: String, epochDay: Long)

    @Query("DELETE FROM habit_logs")
    suspend fun clearAll()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueEpochDay ASC, createdAtMillis DESC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY isEnabled DESC, nextTriggerMillis ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY nextTriggerMillis ASC")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: String): ReminderEntity?

    @Upsert
    suspend fun upsert(reminder: ReminderEntity)

    @Upsert
    suspend fun upsertAll(reminders: List<ReminderEntity>)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM reminders")
    suspend fun clearAll()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY createdAtMillis DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY createdAtMillis DESC")
    suspend fun getAll(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: String): GoalEntity?

    @Upsert
    suspend fun upsert(goal: GoalEntity)

    @Upsert
    suspend fun upsertAll(goals: List<GoalEntity>)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM goals")
    suspend fun clearAll()
}

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestones ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM milestones ORDER BY sortOrder ASC")
    suspend fun getAll(): List<MilestoneEntity>

    @Query("SELECT * FROM milestones WHERE id = :id")
    suspend fun getById(id: String): MilestoneEntity?

    @Upsert
    suspend fun upsert(milestone: MilestoneEntity)

    @Upsert
    suspend fun upsertAll(milestones: List<MilestoneEntity>)

    @Query("DELETE FROM milestones WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM milestones")
    suspend fun clearAll()
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAtMillis DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAtMillis DESC")
    suspend fun getAll(): List<FocusSessionEntity>

    @Upsert
    suspend fun upsert(session: FocusSessionEntity)

    @Upsert
    suspend fun upsertAll(sessions: List<FocusSessionEntity>)

    @Query("DELETE FROM focus_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM focus_sessions")
    suspend fun clearAll()
}

@Dao
interface JournalEntryDao {
    @Query("SELECT * FROM journal_entries ORDER BY epochDay DESC, updatedAtMillis DESC")
    fun observeAll(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries ORDER BY epochDay DESC, updatedAtMillis DESC")
    suspend fun getAll(): List<JournalEntryEntity>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getById(id: String): JournalEntryEntity?

    @Upsert
    suspend fun upsert(entry: JournalEntryEntity)

    @Upsert
    suspend fun upsertAll(entries: List<JournalEntryEntity>)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM journal_entries")
    suspend fun clearAll()
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 'default' LIMIT 1")
    fun observe(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 'default' LIMIT 1")
    suspend fun get(): AppSettingsEntity?

    @Upsert
    suspend fun upsert(settings: AppSettingsEntity)

    @Query("DELETE FROM app_settings")
    suspend fun clearAll()
}
