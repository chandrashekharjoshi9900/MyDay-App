package com.myday.app.presentation.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import com.myday.app.presentation.components.MyDayStatusDot
import com.myday.app.presentation.components.SectionHeader
import com.myday.app.presentation.components.WeeklyProgressBarChart
import com.myday.app.presentation.components.formatEpochDay
import com.myday.app.presentation.components.formatTimeMinutes
import com.myday.app.presentation.theme.MyDayMotion
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
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Grounded Context Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.dateLabel,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MYDAY • ${state.userName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MyDayThemeExtras.colors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MyDayStatusDot(color = MyDayThemeExtras.colors.success)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Offline",
                        style = MaterialTheme.typography.labelMedium,
                        color = MyDayThemeExtras.colors.mutedText
                    )
                }
            }
        }

        // Asymmetric Daily Overview Card
        item {
            MyDayCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressRing(
                        progress = state.dailyOverviewPercent / 100f,
                        size = 80.dp,
                        strokeWidth = 6.dp,
                        accentColor = if (state.dailyOverviewPercent >= 80) {
                            MyDayThemeExtras.colors.success
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    ) {
                        Text(
                            text = "${state.dailyOverviewPercent}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily completion",
                            style = MaterialTheme.typography.labelMedium,
                            color = MyDayThemeExtras.colors.mutedText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val totalDone = state.habitsDoneToday + state.completedTasksToday.size
                        val totalScheduled = state.habitsScheduledToday + state.completedTasksToday.size + state.pendingTasks.size
                        Text(
                            text = "$totalDone of $totalScheduled items",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.pendingTasks.size} pending tasks",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = MyDayThemeExtras.colors.cardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricColumn(
                        label = "Habits",
                        value = "${state.habitsDoneToday}/${state.habitsScheduledToday}",
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp),
                        thickness = 1.dp,
                        color = MyDayThemeExtras.colors.cardBorder
                    )
                    MetricColumn(
                        label = "Streak",
                        value = "${state.currentStreak}d",
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp),
                        thickness = 1.dp,
                        color = MyDayThemeExtras.colors.cardBorder
                    )
                    MetricColumn(
                        label = "Focus",
                        value = "${state.focusMinutesToday}m",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quiet Quick-Add Actions
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionChip(
                    icon = Icons.Rounded.Repeat,
                    label = "Habit",
                    onClick = onNavigateToHabits
                )
                QuickActionChip(
                    icon = Icons.Rounded.AddTask,
                    label = "Task",
                    onClick = onNavigateToTasks
                )
                QuickActionChip(
                    icon = Icons.Rounded.NotificationsActive,
                    label = "Reminder",
                    onClick = onNavigateToReminders
                )
                QuickActionChip(
                    icon = Icons.Rounded.Timer,
                    label = "Focus",
                    onClick = onNavigateToFocus
                )
                QuickActionChip(
                    icon = Icons.Rounded.AutoStories,
                    label = "Journal",
                    onClick = onNavigateToJournal
                )
            }
        }

        // Today's Habits
        item {
            Spacer(modifier = Modifier.height(4.dp))
            SectionHeader(
                title = "Today's habits",
                subtitle = if (state.todayHabits.isEmpty()) {
                    "No habits scheduled today"
                } else {
                    "${state.habitsDoneToday} of ${state.habitsScheduledToday} completed"
                },
                actionLabel = "View all",
                onActionClick = onNavigateToHabits
            )
        }

        if (state.todayHabits.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Repeat,
                    title = "No habits scheduled",
                    message = "Add a daily or weekday habit to begin tracking streaks",
                    actionLabel = "Add habit",
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

        // Tasks
        item {
            Spacer(modifier = Modifier.height(8.dp))
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
                    title = "No pending tasks",
                    message = "Create tasks with due dates, recurrence, and reminders",
                    actionLabel = "Add task",
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
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Upcoming reminders",
                subtitle = "${state.upcomingReminders.size} active",
                actionLabel = "Manage",
                onActionClick = onNavigateToReminders
            )
        }

        if (state.upcomingReminders.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Rounded.Alarm,
                    title = "No upcoming reminders",
                    message = "Schedule offline alarms that persist across reboots",
                    actionLabel = "Set reminder",
                    onAction = onNavigateToReminders
                )
            }
        } else {
            item {
                MyDayCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        state.upcomingReminders.forEachIndexed { index, reminder ->
                            if (index > 0) {
                                HorizontalDivider(thickness = 1.dp, color = MyDayThemeExtras.colors.cardBorder)
                            }
                            DashboardReminderRow(
                                reminder = reminder,
                                onClick = onNavigateToReminders
                            )
                        }
                    }
                }
            }
        }

        // Weekly Rhythm Chart
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Weekly rhythm",
                subtitle = "Last 7 days habit completion",
                actionLabel = "Analytics",
                onActionClick = onNavigateToProgress
            )
        }

        item {
            MyDayCard {
                if (state.weeklyTrend.isEmpty()) {
                    Text(
                        text = "Complete habits or tasks to populate your 7-day trend",
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
private fun MetricColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MyDayThemeExtras.colors.mutedText
        )
    }
}

@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MyDayThemeExtras.colors.cardBorder),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
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
        targetValue = if (done) MyDayThemeExtras.colors.success else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(MyDayMotion.FAST_MS, easing = MyDayMotion.EaseOut),
        label = "habit_check"
    )

    MyDayCard(onClick = onToggle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(circleColor),
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    MyDayStatusDot(color = accent, size = 8.dp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habitWithStats.habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (done) MyDayThemeExtras.colors.mutedText else MaterialTheme.colorScheme.onSurface,
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
                    color = MyDayThemeExtras.colors.mutedText
                )
            }
            Text(
                text = if (done) "Done" else "Log",
                style = MaterialTheme.typography.labelMedium,
                color = if (done) MyDayThemeExtras.colors.success else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        TaskPriority.LOW -> MyDayThemeExtras.colors.mutedText
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
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) MyDayThemeExtras.colors.mutedText else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildString {
                    append(task.priority.label)
                    task.dueEpochDay?.let { append(" • ${formatEpochDay(it)}") }
                    task.dueTimeMinutes?.let { append(" • ${formatTimeMinutes(it)}") }
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MyDayThemeExtras.colors.mutedText
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
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MyDayStatusDot(
            color = if (reminder.snoozedUntilMillis != null) MyDayPalette.AmberWarning else MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
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
                color = MyDayThemeExtras.colors.mutedText
            )
        }
    }
}
