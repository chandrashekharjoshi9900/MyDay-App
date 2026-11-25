package com.myday.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.app.domain.model.CategoryItem
import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.ReminderScheduleType
import com.myday.app.domain.model.ReminderTargetType
import com.myday.app.domain.model.TaskItem
import com.myday.app.domain.model.TaskPriority
import com.myday.app.domain.model.TaskRecurrence
import com.myday.app.domain.repository.CategoryRepository
import com.myday.app.domain.repository.ReminderRepository
import com.myday.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

enum class TaskFilterTab(val label: String) {
    PENDING("Pending"),
    COMPLETED("Completed"),
    HISTORY("History")
}

data class TasksUiState(
    val isLoading: Boolean = true,
    val selectedTab: TaskFilterTab = TaskFilterTab.PENDING,
    val priorityFilter: TaskPriority? = null,
    val tasks: List<TaskItem> = emptyList(),
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val categories: List<CategoryItem> = emptyList()
)

class TasksViewModel(
    private val taskRepository: TaskRepository,
    categoryRepository: CategoryRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    private val selectedTab = MutableStateFlow(TaskFilterTab.PENDING)
    private val priorityFilter = MutableStateFlow<TaskPriority?>(null)

    val uiState: StateFlow<TasksUiState> = combine(
        taskRepository.observeAllTasks(),
        categoryRepository.observeAll(),
        selectedTab,
        priorityFilter
    ) { allTasks, categories, tab, pFilter ->
        val byPriority = if (pFilter == null) allTasks else allTasks.filter { it.priority == pFilter }
        val filtered = when (tab) {
            TaskFilterTab.PENDING -> byPriority
                .filter { !it.isCompleted }
                .sortedWith(
                    compareBy<TaskItem> { it.dueEpochDay ?: Long.MAX_VALUE }
                        .thenByDescending { it.priority.weight }
                )
            TaskFilterTab.COMPLETED, TaskFilterTab.HISTORY -> byPriority
                .filter { it.isCompleted }
                .sortedByDescending { it.completedAtMillis ?: it.updatedAtMillis }
        }
        TasksUiState(
            isLoading = false,
            selectedTab = tab,
            priorityFilter = pFilter,
            tasks = filtered,
            pendingCount = allTasks.count { !it.isCompleted },
            completedCount = allTasks.count { it.isCompleted },
            categories = categories
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TasksUiState()
    )

    fun selectTab(tab: TaskFilterTab) {
        selectedTab.value = tab
    }

    fun selectPriorityFilter(priority: TaskPriority?) {
        priorityFilter.value = priority
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(taskId)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            taskRepository.deleteTask(taskId)
        }
    }

    fun saveTask(
        existing: TaskItem?,
        title: String,
        description: String,
        priority: TaskPriority,
        categoryId: String?,
        dueEpochDay: Long?,
        dueTimeMinutes: Int?,
        recurrence: TaskRecurrence,
        recurrenceWeekdays: Set<Int>,
        hasReminder: Boolean,
        reminderMinutes: Int?
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val taskId = existing?.id ?: UUID.randomUUID().toString()
            val reminderId = if (hasReminder && reminderMinutes != null) {
                existing?.reminderId ?: "task_rem_$taskId"
            } else {
                null
            }

            if (existing?.reminderId != null && reminderId == null) {
                reminderRepository.deleteReminder(existing.reminderId)
            }

            val task = TaskItem(
                id = taskId,
                title = title.trim(),
                description = description.trim(),
                priority = priority,
                categoryId = categoryId,
                dueEpochDay = dueEpochDay,
                dueTimeMinutes = dueTimeMinutes,
                recurrence = recurrence,
                recurrenceWeekdays = if (recurrence == TaskRecurrence.CUSTOM_WEEKDAYS) {
                    recurrenceWeekdays.ifEmpty { setOf(LocalDate.now().dayOfWeek.value) }
                } else {
                    emptySet()
                },
                reminderId = reminderId,
                isCompleted = existing?.isCompleted ?: false,
                completedAtMillis = existing?.completedAtMillis,
                completionCount = existing?.completionCount ?: 0,
                createdAtMillis = existing?.createdAtMillis ?: now,
                updatedAtMillis = now
            )
            taskRepository.upsertTask(task)

            if (reminderId != null && reminderMinutes != null) {
                val targetDay = dueEpochDay ?: LocalDate.now().toEpochDay()
                val scheduleType = when (recurrence) {
                    TaskRecurrence.NONE -> ReminderScheduleType.ONE_TIME
                    TaskRecurrence.DAILY -> ReminderScheduleType.DAILY
                    TaskRecurrence.WEEKLY -> ReminderScheduleType.WEEKLY
                    TaskRecurrence.MONTHLY -> ReminderScheduleType.MONTHLY
                    TaskRecurrence.CUSTOM_WEEKDAYS -> ReminderScheduleType.CUSTOM_WEEKDAYS
                }
                val reminder = ReminderItem(
                    id = reminderId,
                    title = "Task: ${task.title}",
                    notes = task.description.ifBlank { "Scheduled task reminder" },
                    scheduleType = scheduleType,
                    hour = reminderMinutes / 60,
                    minute = reminderMinutes % 60,
                    oneTimeEpochDay = targetDay,
                    weekdays = if (recurrence == TaskRecurrence.WEEKLY) {
                        setOf(LocalDate.ofEpochDay(targetDay).dayOfWeek.value)
                    } else {
                        task.recurrenceWeekdays
                    },
                    dayOfMonth = LocalDate.ofEpochDay(targetDay).dayOfMonth,
                    intervalHours = null,
                    nextTriggerMillis = now,
                    isEnabled = !task.isCompleted,
                    isCompleted = task.isCompleted,
                    snoozedUntilMillis = null,
                    playSound = true,
                    vibrate = true,
                    targetType = ReminderTargetType.TASK,
                    targetId = taskId,
                    lastTriggeredMillis = null,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
                reminderRepository.upsertAndSchedule(reminder)
            }
        }
    }
}
