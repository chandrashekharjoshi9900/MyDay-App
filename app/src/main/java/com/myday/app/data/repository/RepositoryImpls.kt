package com.myday.app.data.repository

import androidx.room.withTransaction
import com.myday.app.data.local.datastore.UserPreferencesDataStore
import com.myday.app.data.local.db.AppSettingsDao
import com.myday.app.data.local.db.CategoryDao
import com.myday.app.data.local.db.FocusSessionDao
import com.myday.app.data.local.db.GoalDao
import com.myday.app.data.local.db.HabitDao
import com.myday.app.data.local.db.HabitLogDao
import com.myday.app.data.local.db.JournalEntryDao
import com.myday.app.data.local.db.MilestoneDao
import com.myday.app.data.local.db.MyDayDatabase
import com.myday.app.data.local.db.ReminderDao
import com.myday.app.data.local.db.TaskDao
import com.myday.app.data.local.db.toDomain
import com.myday.app.data.local.db.toEntity
import com.myday.app.data.reminder.ReminderAlarmScheduler
import com.myday.app.domain.calculator.GoalProgressCalculator
import com.myday.app.domain.calculator.ReminderScheduleCalculator
import com.myday.app.domain.calculator.StreakCalculator
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
import com.myday.app.domain.model.ReminderScheduleType
import com.myday.app.domain.model.ReminderTargetType
import com.myday.app.domain.model.TaskItem
import com.myday.app.domain.model.TaskRecurrence
import com.myday.app.domain.repository.CategoryRepository
import com.myday.app.domain.repository.FocusRepository
import com.myday.app.domain.repository.GoalRepository
import com.myday.app.domain.repository.HabitRepository
import com.myday.app.domain.repository.JournalRepository
import com.myday.app.domain.repository.ReminderRepository
import com.myday.app.domain.repository.SettingsRepository
import com.myday.app.domain.repository.TaskRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

class CategoryRepositoryImpl(
    private val dao: CategoryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CategoryRepository {
    override fun observeAll(): Flow<List<CategoryItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getAll(): List<CategoryItem> = withContext(ioDispatcher) {
        dao.getAll().map { it.toDomain() }
    }

    override suspend fun upsert(category: CategoryItem) = withContext(ioDispatcher) {
        dao.upsert(category.toEntity())
    }

    override suspend fun delete(id: String) = withContext(ioDispatcher) {
        dao.deleteById(id)
    }
}

class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val categoryDao: CategoryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : HabitRepository {

    override fun observeAllHabits(): Flow<List<HabitItem>> =
        habitDao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observeAllLogs(): Flow<List<HabitLogItem>> =
        habitLogDao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override fun observeHabitsWithStats(includeArchived: Boolean): Flow<List<HabitWithStats>> =
        combine(
            habitDao.observeAll(),
            habitLogDao.observeAll(),
            categoryDao.observeAll()
        ) { habits, logs, categories ->
            val categoryMap = categories.associate { it.id to it.toDomain() }
            val logsByHabit = logs.map { it.toDomain() }.groupBy { it.habitId }
            val today = LocalDate.now()
            habits
                .asSequence()
                .map { it.toDomain() }
                .filter { includeArchived || !it.isArchived }
                .map { habit ->
                    val habitLogs = logsByHabit[habit.id].orEmpty()
                    HabitWithStats(
                        habit = habit,
                        category = habit.categoryId?.let { categoryMap[it] },
                        logs = habitLogs,
                        stats = StreakCalculator.calculate(habit, habitLogs, today)
                    )
                }
                .toList()
        }.flowOn(ioDispatcher)

    override suspend fun getHabitById(id: String): HabitItem? = withContext(ioDispatcher) {
        habitDao.getById(id)?.toDomain()
    }

    override suspend fun upsertHabit(habit: HabitItem) = withContext(ioDispatcher) {
        // Uses Room @Upsert (SQL UPDATE when existing) so historical HabitLog rows are never erased.
        habitDao.upsert(habit.toEntity())
    }

    override suspend fun setArchived(habitId: String, archived: Boolean) = withContext(ioDispatcher) {
        habitDao.setArchived(habitId, archived, System.currentTimeMillis())
    }

    override suspend fun setPaused(habitId: String, paused: Boolean, pausedAtEpochDay: Long?) =
        withContext(ioDispatcher) {
            habitDao.setPaused(habitId, paused, pausedAtEpochDay, System.currentTimeMillis())
        }

    override suspend fun deleteHabit(habitId: String) = withContext(ioDispatcher) {
        habitDao.deleteById(habitId)
    }

    override suspend fun toggleHabitCompletion(habitId: String, epochDay: Long, note: String) =
        withContext(ioDispatcher) {
            val existing = habitLogDao.getByHabitAndDay(habitId, epochDay)
            val newCompleted = !(existing?.completed ?: false)
            setHabitCompletion(habitId, epochDay, newCompleted, note)
        }

    override suspend fun setHabitCompletion(
        habitId: String,
        epochDay: Long,
        completed: Boolean,
        note: String
    ) = withContext(ioDispatcher) {
        val existing = habitLogDao.getByHabitAndDay(habitId, epochDay)
        val log = HabitLogItem(
            id = existing?.id ?: UUID.randomUUID().toString(),
            habitId = habitId,
            epochDay = epochDay,
            completed = completed,
            completedAtMillis = if (completed) System.currentTimeMillis() else null,
            note = note.ifBlank { existing?.note.orEmpty() }
        )
        habitLogDao.upsert(log.toEntity())
    }
}

class TaskRepositoryImpl(
    private val db: MyDayDatabase,
    private val taskDao: TaskDao,
    private val reminderRepository: ReminderRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : TaskRepository {

    override fun observeAllTasks(): Flow<List<TaskItem>> =
        taskDao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getTaskById(id: String): TaskItem? = withContext(ioDispatcher) {
        taskDao.getById(id)?.toDomain()
    }

    override suspend fun upsertTask(task: TaskItem) = withContext(ioDispatcher) {
        taskDao.upsert(task.toEntity())
    }

    override suspend fun toggleTaskCompletion(taskId: String) = withContext(ioDispatcher) {
        val task = taskDao.getById(taskId)?.toDomain() ?: return@withContext
        val now = System.currentTimeMillis()

        if (!task.isCompleted && task.recurrence != TaskRecurrence.NONE) {
            val currentDue = task.dueEpochDay?.let { LocalDate.ofEpochDay(it) } ?: LocalDate.now()
            val nextDue = computeNextDueDate(currentDue, task.recurrence, task.recurrenceWeekdays)
            val historySnapshot = task.copy(
                id = UUID.randomUUID().toString(),
                recurrence = TaskRecurrence.NONE,
                recurrenceWeekdays = emptySet(),
                reminderId = null,
                isCompleted = true,
                completedAtMillis = now,
                completionCount = 1,
                updatedAtMillis = now
            )
            val advancedTask = task.copy(
                dueEpochDay = nextDue.toEpochDay(),
                isCompleted = false,
                completedAtMillis = null,
                completionCount = task.completionCount + 1,
                updatedAtMillis = now
            )

            db.withTransaction {
                taskDao.upsert(historySnapshot.toEntity())
                taskDao.upsert(advancedTask.toEntity())
            }

            if (task.reminderId != null) {
                val reminder = reminderRepository.getReminderById(task.reminderId)
                if (reminder != null) {
                    val updatedReminder = reminder.copy(
                        oneTimeEpochDay = nextDue.toEpochDay(),
                        isCompleted = false,
                        isEnabled = true,
                        snoozedUntilMillis = null,
                        updatedAtMillis = now
                    )
                    reminderRepository.upsertAndSchedule(updatedReminder)
                }
            }
        } else {
            val nowCompleted = !task.isCompleted
            val updated = task.copy(
                isCompleted = nowCompleted,
                completedAtMillis = if (nowCompleted) now else null,
                completionCount = if (nowCompleted) task.completionCount + 1 else task.completionCount,
                updatedAtMillis = now
            )
            taskDao.upsert(updated.toEntity())
            if (task.reminderId != null) {
                if (nowCompleted) {
                    reminderRepository.markCompleted(task.reminderId)
                } else {
                    reminderRepository.toggleEnabled(task.reminderId, true)
                }
            }
        }
    }

    override suspend fun deleteTask(taskId: String) = withContext(ioDispatcher) {
        val existing = taskDao.getById(taskId)?.toDomain()
        taskDao.deleteById(taskId)
        if (existing?.reminderId != null) {
            reminderRepository.deleteReminder(existing.reminderId)
        }
    }

    private fun computeNextDueDate(
        current: LocalDate,
        recurrence: TaskRecurrence,
        weekdays: Set<Int>
    ): LocalDate {
        return when (recurrence) {
            TaskRecurrence.NONE -> current
            TaskRecurrence.DAILY -> current.plusDays(1)
            TaskRecurrence.WEEKLY -> current.plusWeeks(1)
            TaskRecurrence.MONTHLY -> current.plusMonths(1)
            TaskRecurrence.CUSTOM_WEEKDAYS -> {
                if (weekdays.isEmpty()) return current.plusDays(1)
                var candidate = current.plusDays(1)
                repeat(7) {
                    if (weekdays.contains(candidate.dayOfWeek.value)) return candidate
                    candidate = candidate.plusDays(1)
                }
                current.plusWeeks(1)
            }
        }
    }
}

class ReminderRepositoryImpl(
    private val reminderDao: ReminderDao,
    private val taskDao: TaskDao,
    private val habitLogDao: HabitLogDao,
    private val alarmScheduler: ReminderAlarmScheduler,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ReminderRepository {

    override fun observeAllReminders(): Flow<List<ReminderItem>> =
        reminderDao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getAllReminders(): List<ReminderItem> = withContext(ioDispatcher) {
        reminderDao.getAll().map { it.toDomain() }
    }

    override suspend fun getReminderById(id: String): ReminderItem? = withContext(ioDispatcher) {
        reminderDao.getById(id)?.toDomain()
    }

    override suspend fun upsertAndSchedule(reminder: ReminderItem): ReminderItem = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val computedNext = ReminderScheduleCalculator.computeNextTriggerMillis(
            reminder = reminder,
            afterMillis = now
        )
        val normalized = reminder.copy(
            nextTriggerMillis = computedNext ?: reminder.nextTriggerMillis,
            updatedAtMillis = now
        )
        reminderDao.upsert(normalized.toEntity())
        if (normalized.isEnabled && !normalized.isCompleted && computedNext != null) {
            alarmScheduler.schedule(normalized)
        } else {
            alarmScheduler.cancel(normalized.id)
        }
        normalized
    }

    override suspend fun toggleEnabled(reminderId: String, enabled: Boolean) = withContext(ioDispatcher) {
        val existing = reminderDao.getById(reminderId)?.toDomain() ?: return@withContext
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            isEnabled = enabled,
            isCompleted = if (enabled) false else existing.isCompleted,
            snoozedUntilMillis = null,
            updatedAtMillis = now
        )
        upsertAndSchedule(updated)
    }

    override suspend fun markCompleted(reminderId: String) = withContext(ioDispatcher) {
        val existing = reminderDao.getById(reminderId)?.toDomain() ?: return@withContext
        val now = System.currentTimeMillis()
        val updated = if (existing.scheduleType == ReminderScheduleType.ONE_TIME) {
            existing.copy(
                isCompleted = true,
                isEnabled = false,
                snoozedUntilMillis = null,
                updatedAtMillis = now
            )
        } else {
            val next = ReminderScheduleCalculator.computeNextTriggerMillis(
                reminder = existing.copy(snoozedUntilMillis = null, isCompleted = false),
                afterMillis = now + 1000L
            )
            existing.copy(
                snoozedUntilMillis = null,
                nextTriggerMillis = next ?: existing.nextTriggerMillis,
                updatedAtMillis = now
            )
        }

        reminderDao.upsert(updated.toEntity())
        if (updated.isEnabled && !updated.isCompleted) {
            alarmScheduler.schedule(updated)
        } else {
            alarmScheduler.cancel(updated.id)
        }

        // Synchronize linked Task or Habit when user clicks "Complete" in notification
        when (existing.targetType) {
            ReminderTargetType.TASK -> {
                val targetId = existing.targetId
                if (targetId != null) {
                    val task = taskDao.getById(targetId)?.toDomain()
                    if (task != null && !task.isCompleted) {
                        taskDao.upsert(
                            task.copy(
                                isCompleted = true,
                                completedAtMillis = now,
                                completionCount = task.completionCount + 1,
                                updatedAtMillis = now
                            ).toEntity()
                        )
                    }
                }
            }
            ReminderTargetType.HABIT -> {
                val targetId = existing.targetId
                if (targetId != null) {
                    val todayEpoch = LocalDate.now().toEpochDay()
                    val existingLog = habitLogDao.getByHabitAndDay(targetId, todayEpoch)
                    habitLogDao.upsert(
                        HabitLogItem(
                            id = existingLog?.id ?: UUID.randomUUID().toString(),
                            habitId = targetId,
                            epochDay = todayEpoch,
                            completed = true,
                            completedAtMillis = now,
                            note = existingLog?.note.orEmpty()
                        ).toEntity()
                    )
                }
            }
            ReminderTargetType.STANDALONE -> Unit
        }
    }

    override suspend fun snoozeReminder(reminderId: String, snoozeMinutes: Int) = withContext(ioDispatcher) {
        val existing = reminderDao.getById(reminderId)?.toDomain() ?: return@withContext
        val now = System.currentTimeMillis()
        val snoozeUntil = now + snoozeMinutes.coerceAtLeast(1) * 60_000L
        val updated = existing.copy(
            isEnabled = true,
            isCompleted = false,
            snoozedUntilMillis = snoozeUntil,
            nextTriggerMillis = snoozeUntil,
            updatedAtMillis = now
        )
        reminderDao.upsert(updated.toEntity())
        alarmScheduler.schedule(updated)
    }

    override suspend fun dismissCurrentOccurrence(reminderId: String) = withContext(ioDispatcher) {
        val existing = reminderDao.getById(reminderId)?.toDomain() ?: return@withContext
        val now = System.currentTimeMillis()
        if (existing.scheduleType == ReminderScheduleType.ONE_TIME) {
            val updated = existing.copy(
                isEnabled = false,
                snoozedUntilMillis = null,
                updatedAtMillis = now
            )
            reminderDao.upsert(updated.toEntity())
            alarmScheduler.cancel(reminderId)
        } else {
            val cleared = existing.copy(snoozedUntilMillis = null, updatedAtMillis = now)
            upsertAndSchedule(cleared)
        }
    }

    override suspend fun deleteReminder(reminderId: String) = withContext(ioDispatcher) {
        alarmScheduler.cancel(reminderId)
        reminderDao.deleteById(reminderId)
    }

    override suspend fun rescheduleAllActiveReminders() = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val all = reminderDao.getAll().map { it.toDomain() }
        for (reminder in all) {
            if (!reminder.isEnabled || reminder.isCompleted) {
                alarmScheduler.cancel(reminder.id)
                continue
            }
            val next = ReminderScheduleCalculator.computeNextTriggerMillis(reminder, afterMillis = now)
            if (next != null) {
                val updated = reminder.copy(nextTriggerMillis = next, updatedAtMillis = now)
                reminderDao.upsert(updated.toEntity())
                alarmScheduler.schedule(updated)
            } else {
                val expired = reminder.copy(isEnabled = false, snoozedUntilMillis = null, updatedAtMillis = now)
                reminderDao.upsert(expired.toEntity())
                alarmScheduler.cancel(reminder.id)
            }
        }
    }
}

class GoalRepositoryImpl(
    private val goalDao: GoalDao,
    private val milestoneDao: MilestoneDao,
    private val categoryDao: CategoryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : GoalRepository {

    override fun observeGoalsWithMilestones(): Flow<List<GoalWithMilestones>> =
        combine(
            goalDao.observeAll(),
            milestoneDao.observeAll(),
            categoryDao.observeAll()
        ) { goals, milestones, categories ->
            val categoryMap = categories.associate { it.id to it.toDomain() }
            val milestonesByGoal = milestones.map { it.toDomain() }.groupBy { it.goalId }
            goals.map { entity ->
                val goal = entity.toDomain()
                val goalMilestones = milestonesByGoal[goal.id].orEmpty().sortedBy { it.sortOrder }
                GoalWithMilestones(
                    goal = goal,
                    category = goal.categoryId?.let { categoryMap[it] },
                    milestones = goalMilestones,
                    progressPercent = GoalProgressCalculator.calculateProgressPercent(goal, goalMilestones)
                )
            }
        }.flowOn(ioDispatcher)

    override suspend fun upsertGoal(goal: GoalItem) = withContext(ioDispatcher) {
        goalDao.upsert(goal.toEntity())
    }

    override suspend fun updateGoalStatus(goalId: String, status: GoalStatus) = withContext(ioDispatcher) {
        val existing = goalDao.getById(goalId)?.toDomain() ?: return@withContext
        val now = System.currentTimeMillis()
        val updated = existing.copy(
            status = status,
            completedAtMillis = if (status == GoalStatus.COMPLETED) now else null,
            updatedAtMillis = now
        )
        goalDao.upsert(updated.toEntity())
    }

    override suspend fun deleteGoal(goalId: String) = withContext(ioDispatcher) {
        goalDao.deleteById(goalId)
    }

    override suspend fun upsertMilestone(milestone: MilestoneItem) = withContext(ioDispatcher) {
        milestoneDao.upsert(milestone.toEntity())
    }

    override suspend fun toggleMilestone(milestoneId: String) = withContext(ioDispatcher) {
        val existing = milestoneDao.getById(milestoneId)?.toDomain() ?: return@withContext
        val completed = !existing.isCompleted
        milestoneDao.upsert(
            existing.copy(
                isCompleted = completed,
                completedAtMillis = if (completed) System.currentTimeMillis() else null
            ).toEntity()
        )
    }

    override suspend fun deleteMilestone(milestoneId: String) = withContext(ioDispatcher) {
        milestoneDao.deleteById(milestoneId)
    }
}

class FocusRepositoryImpl(
    private val dao: FocusSessionDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FocusRepository {
    override fun observeAllSessions(): Flow<List<FocusSessionItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun recordSession(session: FocusSessionItem) = withContext(ioDispatcher) {
        dao.upsert(session.toEntity())
    }

    override suspend fun deleteSession(sessionId: String) = withContext(ioDispatcher) {
        dao.deleteById(sessionId)
    }
}

class JournalRepositoryImpl(
    private val dao: JournalEntryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : JournalRepository {
    override fun observeAllEntries(): Flow<List<JournalEntryItem>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }.flowOn(ioDispatcher)

    override suspend fun getEntryById(id: String): JournalEntryItem? = withContext(ioDispatcher) {
        dao.getById(id)?.toDomain()
    }

    override suspend fun upsertEntry(entry: JournalEntryItem) = withContext(ioDispatcher) {
        dao.upsert(entry.toEntity())
    }

    override suspend fun deleteEntry(entryId: String) = withContext(ioDispatcher) {
        dao.deleteById(entryId)
    }
}

class SettingsRepositoryImpl(
    private val dataStore: UserPreferencesDataStore,
    private val settingsDao: AppSettingsDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettingsModel> =
        dataStore.settingsFlow.flowOn(ioDispatcher)

    override suspend fun getSettings(): AppSettingsModel = withContext(ioDispatcher) {
        dataStore.getSettings()
    }

    override suspend fun updateSettings(transform: (AppSettingsModel) -> AppSettingsModel) =
        withContext(ioDispatcher) {
            val current = dataStore.getSettings()
            val updated = transform(current)
            dataStore.saveSettings(updated)
            settingsDao.upsert(updated.toEntity())
        }
}
