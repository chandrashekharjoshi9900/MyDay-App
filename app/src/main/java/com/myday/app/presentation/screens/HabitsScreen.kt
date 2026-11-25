package com.myday.app.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myday.app.domain.calculator.StreakCalculator
import com.myday.app.domain.model.CategoryItem
import com.myday.app.domain.model.HabitItem
import com.myday.app.domain.model.HabitScheduleType
import com.myday.app.domain.model.HabitWithStats
import com.myday.app.domain.model.sortedWeekdaysLabel
import com.myday.app.presentation.components.CategorySelectorRow
import com.myday.app.presentation.components.ColorPickerRow
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
import com.myday.app.presentation.viewmodel.HabitFilterTab
import com.myday.app.presentation.viewmodel.HabitsUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HabitsScreen(
    state: HabitsUiState,
    onSelectTab: (HabitFilterTab) -> Unit,
    onSelectDate: (Long) -> Unit,
    onToggleCompletion: (String) -> Unit,
    onToggleDayCompletion: (String, Long) -> Unit,
    onSaveHabit: (
        existing: HabitItem?,
        title: String,
        description: String,
        categoryId: String?,
        colorHex: Long,
        scheduleType: HabitScheduleType,
        selectedWeekdays: Set<Int>,
        weeklyTarget: Int,
        reminderTimeMinutes: Int?
    ) -> Unit,
    onSetPaused: (String, Boolean) -> Unit,
    onSetArchived: (String, Boolean) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onOpenHistory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberMyDayHaptic()
    var showEditor by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitItem?>(null) }
    var deletingHabit by remember { mutableStateOf<HabitItem?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingHabit = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Habit")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header & Date Switcher
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Habit Tracker",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Tracking for ${formatEpochDay(state.selectedEpochDay)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = "Choose Date",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(formatEpochDay(state.selectedEpochDay))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HabitFilterTab.entries.forEach { tab ->
                        FilterChip(
                            selected = state.selectedTab == tab,
                            onClick = { onSelectTab(tab) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }

            if (state.isLoading) {
                LoadingStateView()
            } else if (state.habits.isEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Rounded.Repeat,
                        title = "No ${state.selectedTab.label.lowercase()} habits",
                        message = if (state.selectedTab == HabitFilterTab.ACTIVE) {
                            "Tap + to build a habit with daily, weekday, or weekly targets."
                        } else {
                            "Habits in this category will appear here."
                        },
                        actionLabel = if (state.selectedTab == HabitFilterTab.ACTIVE) "Add Habit" else null,
                        onAction = if (state.selectedTab == HabitFilterTab.ACTIVE) {
                            { editingHabit = null; showEditor = true }
                        } else null
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.habits, key = { it.habit.id }) { item ->
                        HabitTrackerCard(
                            item = item,
                            selectedEpochDay = state.selectedEpochDay,
                            onToggle = {
                                haptic()
                                onToggleCompletion(item.habit.id)
                            },
                            onEdit = {
                                editingHabit = item.habit
                                showEditor = true
                            },
                            onHistory = { onOpenHistory(item.habit.id) },
                            onTogglePause = { onSetPaused(item.habit.id, !item.habit.isPaused) },
                            onToggleArchive = { onSetArchived(item.habit.id, !item.habit.isArchived) },
                            onDelete = { deletingHabit = item.habit }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        MyDayDatePickerDialog(
            initialEpochDay = state.selectedEpochDay,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                onSelectDate(it)
                showDatePicker = false
            }
        )
    }

    if (showEditor) {
        HabitEditorDialog(
            existing = editingHabit,
            categories = state.categories,
            onDismiss = { showEditor = false },
            onConfirm = { title, desc, catId, colorHex, schedType, weekdays, target, remMin ->
                onSaveHabit(editingHabit, title, desc, catId, colorHex, schedType, weekdays, target, remMin)
                showEditor = false
            }
        )
    }

    state.selectedHistoryHabit?.let { historyHabit ->
        HabitHistoryDialog(
            item = historyHabit,
            onToggleDay = { epoch ->
                haptic()
                onToggleDayCompletion(historyHabit.habit.id, epoch)
            },
            onDismiss = { onOpenHistory(null) }
        )
    }

    deletingHabit?.let { habit ->
        AlertDialog(
            onDismissRequest = { deletingHabit = null },
            title = { Text("Delete Habit?") },
            text = {
                Text("Permanently deleting \"${habit.title}\" will also remove its completion history. Archive instead if you want to keep historical records.")
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteHabit(habit.id)
                    deletingHabit = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingHabit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HabitTrackerCard(
    item: HabitWithStats,
    selectedEpochDay: Long,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
    onTogglePause: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    val habit = item.habit
    val accent = Color(habit.colorHex)
    val completedForSelectedDay = item.logs.any { it.epochDay == selectedEpochDay && it.completed }
    var menuExpanded by remember { mutableStateOf(false) }

    MyDayCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (completedForSelectedDay) MyDayThemeExtras.colors.success else accent.copy(alpha = 0.18f)
                    )
                    .clickable(enabled = !habit.isArchived, onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                if (completedForSelectedDay) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Completed",
                        tint = Color.White
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (habit.isPaused) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusPill(
                            text = "Paused",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (habit.isArchived) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusPill(
                            text = "Archived",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val scheduleText = when (habit.scheduleType) {
                    HabitScheduleType.DAILY -> "Daily"
                    HabitScheduleType.WEEKDAYS -> habit.selectedWeekdays.sortedWeekdaysLabel()
                    HabitScheduleType.WEEKLY_TARGET -> "${habit.weeklyTarget}x per week"
                }
                Text(
                    text = buildString {
                        append(scheduleText)
                        item.category?.let { append(" • ${it.name}") }
                        habit.reminderTimeMinutes?.let { append(" • ${formatTimeMinutes(it)}") }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Habit Options")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Completion History") },
                        leadingIcon = { Icon(Icons.Rounded.History, null) },
                        onClick = { menuExpanded = false; onHistory() }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = { menuExpanded = false; onEdit() }
                    )
                    if (!habit.isArchived) {
                        DropdownMenuItem(
                            text = { Text(if (habit.isPaused) "Resume Habit" else "Pause Habit") },
                            leadingIcon = {
                                Icon(if (habit.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, null)
                            },
                            onClick = { menuExpanded = false; onTogglePause() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (habit.isArchived) "Unarchive" else "Archive (Keep Logs)") },
                        leadingIcon = {
                            Icon(if (habit.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive, null)
                        },
                        onClick = { menuExpanded = false; onToggleArchive() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Permanently", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Weekly Target Progress Bar & Streak Row
        val weekRatio = (item.stats.completedThisWeek.toFloat() / item.stats.targetThisWeek.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { weekRatio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.LocalFireDepartment,
                    contentDescription = null,
                    tint = MyDayPalette.AmberWarning,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${item.stats.currentStreak} current • ${item.stats.longestStreak} best",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (item.stats.missedRecentDays.isNotEmpty()) {
                    "${item.stats.completedThisWeek}/${item.stats.targetThisWeek} wk • ${item.stats.missedRecentDays.size} missed"
                } else {
                    "${item.stats.completedThisWeek}/${item.stats.targetThisWeek} this week"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (item.stats.missedRecentDays.isNotEmpty()) {
                    MyDayPalette.AmberWarning
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun HabitEditorDialog(
    existing: HabitItem?,
    categories: List<CategoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        categoryId: String?,
        colorHex: Long,
        scheduleType: HabitScheduleType,
        selectedWeekdays: Set<Int>,
        weeklyTarget: Int,
        reminderTimeMinutes: Int?
    ) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var colorHex by remember { mutableLongStateOf(existing?.colorHex ?: MyDayPalette.AccentSwatches.first()) }
    var scheduleType by remember { mutableStateOf(existing?.scheduleType ?: HabitScheduleType.DAILY) }
    var weekdays by remember { mutableStateOf(existing?.selectedWeekdays ?: setOf(1, 2, 3, 4, 5)) }
    var weeklyTarget by remember { mutableIntStateOf(existing?.weeklyTarget ?: 3) }
    var hasReminder by remember { mutableStateOf(existing?.reminderTimeMinutes != null) }
    var reminderMinutes by remember { mutableIntStateOf(existing?.reminderTimeMinutes ?: (9 * 60)) }
    var showTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Habit" else "Edit Habit") },
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
                    label = { Text("Habit Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelLarge)
                CategorySelectorRow(
                    categories = categories,
                    selectedCategoryId = categoryId,
                    onSelectCategory = { categoryId = it }
                )

                Text("Color", style = MaterialTheme.typography.labelLarge)
                ColorPickerRow(
                    selectedColorHex = colorHex,
                    onSelectColor = { colorHex = it }
                )

                Text("Schedule", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        HabitScheduleType.DAILY to "Daily",
                        HabitScheduleType.WEEKDAYS to "Weekdays",
                        HabitScheduleType.WEEKLY_TARGET to "Weekly Goal"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = scheduleType == type,
                            onClick = { scheduleType = type },
                            label = { Text(label) }
                        )
                    }
                }

                if (scheduleType == HabitScheduleType.WEEKDAYS) {
                    WeekdaySelectorRow(
                        selectedWeekdays = weekdays,
                        onToggleWeekday = { d ->
                            weekdays = if (weekdays.contains(d)) weekdays - d else weekdays + d
                        }
                    )
                }

                if (scheduleType == HabitScheduleType.WEEKLY_TARGET) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Days per week: $weeklyTarget", style = MaterialTheme.typography.bodyLarge)
                        Row {
                            OutlinedButton(
                                onClick = { if (weeklyTarget > 1) weeklyTarget-- },
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) { Text("-") }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { if (weeklyTarget < 7) weeklyTarget++ },
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) { Text("+") }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Daily Reminder", style = MaterialTheme.typography.labelLarge)
                        if (hasReminder) {
                            Text(
                                text = formatTimeMinutes(reminderMinutes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { showTimePicker = true }
                            )
                        }
                    }
                    Switch(
                        checked = hasReminder,
                        onCheckedChange = {
                            hasReminder = it
                            if (it) showTimePicker = true
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
                        categoryId,
                        colorHex,
                        scheduleType,
                        weekdays,
                        weeklyTarget,
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

    if (showTimePicker) {
        MyDayTimePickerDialog(
            initialHour = reminderMinutes / 60,
            initialMinute = reminderMinutes % 60,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                reminderMinutes = h * 60 + m
                showTimePicker = false
            }
        )
    }
}

@Composable
private fun HabitHistoryDialog(
    item: HabitWithStats,
    onToggleDay: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    val last14Days = (13 downTo 0).map { today.minusDays(it.toLong()) }
    val completedEpochs = item.logs.filter { it.completed }.map { it.epochDay }.toSet()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${item.habit.title} • History") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total completions: ${item.stats.totalCompletions}", fontWeight = FontWeight.SemiBold)
                    Text("Best: ${item.stats.longestStreak}d", color = MaterialTheme.colorScheme.primary)
                }

                Text("Last 14 days (tap to toggle)", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    last14Days.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            week.forEach { date ->
                                val epoch = date.toEpochDay()
                                val done = completedEpochs.contains(epoch)
                                val scheduled = StreakCalculator.isScheduledOn(item.habit, date)
                                val bg = when {
                                    done -> MyDayThemeExtras.colors.success
                                    scheduled -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.background
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .clickable { onToggleDay(epoch) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (done) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (item.stats.missedRecentDays.isNotEmpty()) {
                    Text(
                        text = "Missed scheduled days: " + item.stats.missedRecentDays
                            .take(5)
                            .joinToString { it.format(DateTimeFormatter.ofPattern("MMM d")) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MyDayPalette.AmberWarning
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
