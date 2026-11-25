package com.myday.app.presentation.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddTask
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myday.app.domain.model.HabitWithStats
import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.TaskItem
import com.myday.app.domain.model.TaskPriority
import com.myday.app.presentation.components.CircularProgressRing
import com.myday.app.presentation.components.EmptyStateCard
import com.myday.app.presentation.components.LoadingStateView
import com.myday.app.presentation.components.MyDayCard
import com.myday.app.presentation.components.SectionHeader
import com.myday.app.presentation.components.StatusPill
import com.myday.app.presentation.components.WeeklyProgressBarChart
import com.myday.app.presentation.components.formatEpochDay
import com.myday.app.presentation.components.formatTimeMinutes
import com.myday.app.presentation.theme.MyDayPalette
import com.myday.app.presentation.theme.MyDayThemeExtras
import com.myday.app.presentation.theme.rememberMyDayHaptic
import com.myday.app.presentation.viewmodel.DashboardUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onToggleHabit: (String) -> Unit,
    onToggleTask: (String) -> Unit,
    onNavigateToHabits: () -> Unit,
    onNavigateToTasks: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToReminders: () -> Unit,
    onNavigateToFocus: () -> Unit,
    onNavigateToJournal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberMyDayHaptic()

    if (state.isLoading) {
        LoadingStateView(modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Greeting & Date
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${state.greeting}, ${state.userName}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = "MYDAY",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Daily Productivity Overview Card
        item {
            MyDayCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressRing(
                        progress = state.dailyOverviewPercent / 100f,
                        size = 86.dp,
                        strokeWidth = 8.dp,
                        accentColor = if (state.dailyOverviewPercent >= 80) {
                            MyDayThemeExtras.colors.success
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.dailyOverviewPercent}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Daily Overview",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniStatTile(
                                icon = Icons.Rounded.Repeat,
                                label = "Habits",
                                value = "${state.habitsDoneToday}/${state.habitsScheduledToday}",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatTile(
                                icon = Icons.Rounded.CheckCircle,
                                label = "Done",
                                value = "${state.completedTasksToday.size}",
                                tint = MyDayThemeExtras.colors.success,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MiniStatTile(
                                icon = Icons.Rounded.LocalFireDepartment,
                                label = "Streak",
                                value = "${state.currentStreak}d",
                                tint = MyDayPalette.AmberWarning,
                                modifier = Modifier.weight(1f)
                            )
                            MiniStatTile(
                                icon = Icons.Rounded.Timer,
                                label = "Focus",
                                value = "${state.focusMinutesToday}m",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Quick-Add Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionChip(
                    icon = Icons.Rounded.Repeat,
                    label = "Add Habit",
                    tint = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToHabits
                )
                QuickActionChip(
                    icon = Icons.Rounded.AddTask,
                    label = "Add Task",
                    tint = MaterialTheme.colorScheme.secondary,
                    onClick = onNavigateToTasks
                )
                QuickActionChip(
                    icon = Icons.Rounded.NotificationsActive,
                    label = "Reminder",
                    tint = MyDayPalette.AmberWarning,
                    onClick = onNavigateToReminders
                )
                QuickActionChip(
                    icon = Icons.Rounded.Timer,
                    label = "Focus",
                    tint = MyDayThemeExtras.colors.success,
                    onClick = onNavigateToFocus
                )
                QuickActionChip(
                    icon = Icons.Rounded.AutoStories,
                    label = "Journal",
                    tint = Color(0xFFE879F9),
                    onClick = onNavigateToJournal
                )
            }
        }

        // Today's Habits
        item {
            SectionHeader(
                title = "Today's Habits",
                subtitle = if (state.todayHabits.isEmpty()) {
                    "No habits scheduled today"
                } else {
                    "${state.habitsDoneToday} of ${state.habitsScheduledToday} completed"
                },
                actionLabel = "All Habits",
                onActionClick = onNavigateToHabits
            )
        }

        if (state.todayHabits.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Repeat,
                    title = "Build your daily routine",
                    message = "Create habits with daily or custom weekday schedules to track streaks.",
                    actionLabel = "Create Habit",
                    onAction = onNavigateToHabits
                )
            }
        } else {
            items(state.todayHabits.take(6), key = { "dash_habit_${it.habit.id}" }) { item ->
                DashboardHabitRow(
                    habitWithStats = item,
                    onToggle = {
                        haptic()
                        onToggleHabit(item.habit.id)
                    }
                )
            }
        }

        // Tasks (Pending & Completed Today)
        item {
            SectionHeader(
                title = "Tasks",
                subtitle = "${state.pendingTasks.size} pending • ${state.completedTasksToday.size} done today",
                actionLabel = "Manage",
                onActionClick = onNavigateToTasks
            )
        }

        if (state.pendingTasks.isEmpty() && state.completedTasksToday.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.CheckCircle,
                    title = "All clear for now",
                    message = "Add tasks with priorities, due dates, and optional background reminders.",
                    actionLabel = "New Task",
                    onAction = onNavigateToTasks
                )
            }
        } else {
            val previewTasks = (state.pendingTasks.take(4) + state.completedTasksToday.take(2))
            items(previewTasks, key = { "dash_task_${it.id}" }) { task ->
                DashboardTaskRow(
                    task = task,
                    onToggle = {
                        haptic()
                        onToggleTask(task.id)
                    }
                )
            }
        }

        // Upcoming Reminders
        item {
            SectionHeader(
                title = "Upcoming Reminders",
                subtitle = "${state.upcomingReminders.size} active scheduled",
                actionLabel = "Reminders",
                onActionClick = onNavigateToReminders
            )
        }

        if (state.upcomingReminders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Alarm,
                    title = "No upcoming reminders",
                    message = "Schedule offline alarms that survive reboots and timezone changes.",
                    actionLabel = "Set Reminder",
                    onAction = onNavigateToReminders
                )
            }
        } else {
            item {
                MyDayCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.upcomingReminders.forEach { reminder ->
                            DashboardReminderRow(
                                reminder = reminder,
                                onClick = onNavigateToReminders
                            )
                        }
                    }
                }
            }
        }

        // Weekly Progress Chart
        item {
            SectionHeader(
                title = "Weekly Progress",
                subtitle = "Last 7 days habit completion",
                actionLabel = "Analytics",
                onActionClick = onNavigateToProgress
            )
        }

        item {
            MyDayCard {
                if (state.weeklyTrend.isEmpty()) {
                    Text(
                        text = "Complete habits or tasks to build your weekly progress trend.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    WeeklyProgressBarChart(points = state.weeklyTrend)
                }
            }
        }
    }
}

@Composable
private fun MiniStatTile(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MyDayThemeExtras.colors.elevatedSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MyDayThemeExtras.colors.cardBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DashboardHabitRow(
    habitWithStats: HabitWithStats,
    onToggle: () -> Unit
) {
    val done = habitWithStats.stats.completedToday
    val accent = Color(habitWithStats.habit.colorHex)
    val circleColor by animateColorAsState(
        targetValue = if (done) MyDayThemeExtras.colors.success else accent.copy(alpha = 0.2f),
        label = "habit_check"
    )

    MyDayCard(onClick = onToggle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(circleColor),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habitWithStats.habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = buildString {
                    append("${habitWithStats.stats.currentStreak}d streak")
                    if (habitWithStats.category != null) {
                        append(" • ${habitWithStats.category.name}")
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (done) {
                StatusPill(
                    text = "Done",
                    containerColor = MyDayThemeExtras.colors.successContainer,
                    contentColor = MyDayThemeExtras.colors.success
                )
            }
        }
    }
}

@Composable
private fun DashboardTaskRow(
    task: TaskItem,
    onToggle: () -> Unit
) {
    val priorityColor = when (task.priority) {
        TaskPriority.HIGH -> MyDayPalette.CoralError
        TaskPriority.MEDIUM -> MyDayPalette.AmberWarning
        TaskPriority.LOW -> MaterialTheme.colorScheme.secondary
    }

    MyDayCard(onClick = onToggle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (task.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (task.isCompleted) "Completed" else "Pending",
                tint = if (task.isCompleted) MyDayThemeExtras.colors.success else priorityColor,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildString {
                    append(task.priority.label)
                    task.dueEpochDay?.let { append(" • ${formatEpochDay(it)}") }
                    task.dueTimeMinutes?.let { append(" at ${formatTimeMinutes(it)}") }
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DashboardReminderRow(
    reminder: ReminderItem,
    onClick: () -> Unit
) {
    val triggerMillis = reminder.snoozedUntilMillis ?: reminder.nextTriggerMillis
    val formatted = Instant.ofEpochMilli(triggerMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (reminder.snoozedUntilMillis != null) "Snoozed until $formatted" else "$formatted • ${reminder.scheduleType.label}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
