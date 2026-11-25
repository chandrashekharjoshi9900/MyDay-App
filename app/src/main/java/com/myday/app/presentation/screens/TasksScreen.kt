package com.myday.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myday.app.domain.model.CategoryItem
import com.myday.app.domain.model.TaskItem
import com.myday.app.domain.model.TaskPriority
import com.myday.app.domain.model.TaskRecurrence
import com.myday.app.presentation.components.CategorySelectorRow
import com.myday.app.presentation.components.EmptyStateCard
import com.myday.app.presentation.components.LoadingStateView
import com.myday.app.presentation.components.MyDayCard
import com.myday.app.presentation.components.MyDayDatePickerDialog
import com.myday.app.presentation.components.MyDayTimePickerDialog
import com.myday.app.presentation.components.StatusPill
import com.myday.app.presentation.components.WeekdaySelectorRow
import com.myday.app.presentation.components.formatEpochDay
import com.myday.app.presentation.components.formatTimeMinutes
import com.myday.app.presentation.theme.MyDayPalette
import com.myday.app.presentation.theme.MyDayThemeExtras
import com.myday.app.presentation.theme.rememberMyDayHaptic
import com.myday.app.presentation.viewmodel.TaskFilterTab
import com.myday.app.presentation.viewmodel.TasksUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TasksScreen(
    state: TasksUiState,
    onSelectTab: (TaskFilterTab) -> Unit,
    onSelectPriority: (TaskPriority?) -> Unit,
    onToggleTask: (String) -> Unit,
    onSaveTask: (
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
    ) -> Unit,
    onDeleteTask: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberMyDayHaptic()
    var showEditor by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTask = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Task Manager",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${state.pendingCount} pending • ${state.completedCount} completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskFilterTab.entries.forEach { tab ->
                        FilterChip(
                            selected = state.selectedTab == tab,
                            onClick = { onSelectTab(tab) },
                            label = { Text(tab.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.priorityFilter == null,
                        onClick = { onSelectPriority(null) },
                        label = { Text("All Priorities") }
                    )
                    TaskPriority.entries.forEach { priority ->
                        FilterChip(
                            selected = state.priorityFilter == priority,
                            onClick = { onSelectPriority(priority) },
                            label = { Text(priority.label) }
                        )
                    }
                }
            }

            if (state.isLoading) {
                LoadingStateView()
            } else if (state.tasks.isEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Rounded.CheckCircle,
                        title = "No tasks here",
                        message = if (state.selectedTab == TaskFilterTab.PENDING) {
                            "Tap + to add tasks with priorities, due dates, recurrence, and reminders."
                        } else {
                            "Completed tasks and recurring completion history will appear here."
                        },
                        actionLabel = if (state.selectedTab == TaskFilterTab.PENDING) "Add Task" else null,
                        onAction = if (state.selectedTab == TaskFilterTab.PENDING) {
                            { editingTask = null; showEditor = true }
                        } else null
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.tasks, key = { it.id }) { task ->
                        val category = state.categories.firstOrNull { it.id == task.categoryId }
                        TaskCard(
                            task = task,
                            category = category,
                            showCompletedTimestamp = state.selectedTab == TaskFilterTab.HISTORY,
                            onToggle = {
                                haptic()
                                onToggleTask(task.id)
                            },
                            onEdit = {
                                editingTask = task
                                showEditor = true
                            },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        TaskEditorDialog(
            existing = editingTask,
            categories = state.categories,
            onDismiss = { showEditor = false },
            onConfirm = { title, desc, priority, catId, dueDay, dueMin, rec, recWeekdays, hasRem, remMin ->
                onSaveTask(editingTask, title, desc, priority, catId, dueDay, dueMin, rec, recWeekdays, hasRem, remMin)
                showEditor = false
            }
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskItem,
    category: CategoryItem?,
    showCompletedTimestamp: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> MyDayPalette.CoralError
        TaskPriority.MEDIUM -> MyDayPalette.AmberWarning
        TaskPriority.LOW -> MaterialTheme.colorScheme.secondary
    }
    val todayEpoch = LocalDate.now().toEpochDay()
    val isOverdue = !task.isCompleted && task.dueEpochDay != null && task.dueEpochDay < todayEpoch

    MyDayCard(onClick = onToggle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (task.isCompleted) "Completed" else "Pending",
                tint = if (task.isCompleted) MyDayThemeExtras.colors.success else priorityColor,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = task.priority.label,
                        containerColor = priorityColor.copy(alpha = 0.16f),
                        contentColor = priorityColor
                    )
                    if (task.dueEpochDay != null) {
                        StatusPill(
                            text = buildString {
                                append(formatEpochDay(task.dueEpochDay))
                                task.dueTimeMinutes?.let { append(" • ${formatTimeMinutes(it)}") }
                            },
                            containerColor = if (isOverdue) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (isOverdue) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    if (task.recurrence != TaskRecurrence.NONE) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Recurring",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (task.reminderId != null) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = "Has Reminder",
                            tint = MyDayPalette.AmberWarning,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (category != null || (showCompletedTimestamp && task.completedAtMillis != null)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    val info = buildString {
                        category?.let { append(it.name) }
                        if (showCompletedTimestamp && task.completedAtMillis != null) {
                            if (isNotEmpty()) append(" • ")
                            val fmt = Instant.ofEpochMilli(task.completedAtMillis)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
                            append("Completed $fmt")
                        }
                    }
                    Text(
                        text = info,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Task", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskEditorDialog(
    existing: TaskItem?,
    categories: List<CategoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (
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
    ) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var priority by remember { mutableStateOf(existing?.priority ?: TaskPriority.MEDIUM) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var dueEpochDay by remember { mutableStateOf(existing?.dueEpochDay ?: LocalDate.now().toEpochDay()) }
    var dueTimeMinutes by remember { mutableStateOf(existing?.dueTimeMinutes) }
    var recurrence by remember { mutableStateOf(existing?.recurrence ?: TaskRecurrence.NONE) }
    var weekdays by remember { mutableStateOf(existing?.recurrenceWeekdays ?: setOf(1, 3, 5)) }
    var hasReminder by remember { mutableStateOf(existing?.reminderId != null) }
    var reminderMinutes by remember { mutableIntStateOf(existing?.dueTimeMinutes ?: (10 * 60)) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showDueTimePicker by remember { mutableStateOf(false) }
    var showReminderTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Task" else "Edit Task") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) }
                        )
                    }
                }

                Text("Category", style = MaterialTheme.typography.labelLarge)
                CategorySelectorRow(
                    categories = categories,
                    selectedCategoryId = categoryId,
                    onSelectCategory = { categoryId = it }
                )

                Text("Due Date & Time", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(dueEpochDay?.let { formatEpochDay(it) } ?: "No Date")
                    }
                    OutlinedButton(onClick = { showDueTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(dueTimeMinutes?.let { formatTimeMinutes(it) } ?: "Set Time")
                    }
                }

                Text("Recurrence", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskRecurrence.entries.forEach { rec ->
                        FilterChip(
                            selected = recurrence == rec,
                            onClick = { recurrence = rec },
                            label = { Text(rec.label) }
                        )
                    }
                }

                if (recurrence == TaskRecurrence.CUSTOM_WEEKDAYS) {
                    WeekdaySelectorRow(
                        selectedWeekdays = weekdays,
                        onToggleWeekday = { d ->
                            weekdays = if (weekdays.contains(d)) weekdays - d else weekdays + d
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Schedule Reminder", style = MaterialTheme.typography.labelLarge)
                        if (hasReminder) {
                            Text(
                                text = formatTimeMinutes(reminderMinutes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showReminderTimePicker = true }
                            )
                        }
                    }
                    Switch(
                        checked = hasReminder,
                        onCheckedChange = {
                            hasReminder = it
                            if (it) {
                                reminderMinutes = dueTimeMinutes ?: reminderMinutes
                                showReminderTimePicker = true
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title,
                        description,
                        priority,
                        categoryId,
                        dueEpochDay,
                        dueTimeMinutes,
                        recurrence,
                        weekdays,
                        hasReminder,
                        if (hasReminder) reminderMinutes else null
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showDatePicker) {
        MyDayDatePickerDialog(
            initialEpochDay = dueEpochDay,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                dueEpochDay = it
                showDatePicker = false
            }
        )
    }

    if (showDueTimePicker) {
        val current = dueTimeMinutes ?: (9 * 60)
        MyDayTimePickerDialog(
            initialHour = current / 60,
            initialMinute = current % 60,
            onDismiss = { showDueTimePicker = false },
            onConfirm = { h, m ->
                dueTimeMinutes = h * 60 + m
                showDueTimePicker = false
            }
        )
    }

    if (showReminderTimePicker) {
        MyDayTimePickerDialog(
            initialHour = reminderMinutes / 60,
            initialMinute = reminderMinutes % 60,
            onDismiss = { showReminderTimePicker = false },
            onConfirm = { h, m ->
                reminderMinutes = h * 60 + m
                showReminderTimePicker = false
            }
        )
    }
}
