package com.myday.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myday.app.domain.model.AnalyticsPeriod
import com.myday.app.presentation.components.CalendarHeatmapGrid
import com.myday.app.presentation.components.LoadingStateView
import com.myday.app.presentation.components.MyDayCard
import com.myday.app.presentation.components.SectionHeader
import com.myday.app.presentation.components.WeeklyProgressBarChart
import com.myday.app.presentation.theme.MyDayPalette
import com.myday.app.presentation.theme.MyDayThemeExtras
import com.myday.app.presentation.viewmodel.ProgressUiState
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onSelectPeriod: (AnalyticsPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading || state.report == null) {
        LoadingStateView(modifier = modifier)
        return
    }

    val report = state.report
    val dateFmt = DateTimeFormatter.ofPattern("MMM d")

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Progress Analytics",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${report.startDate.format(dateFmt)} – ${report.endDate.format(dateFmt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnalyticsPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = state.selectedPeriod == period,
                            onClick = { onSelectPeriod(period) },
                            label = { Text(period.label) }
                        )
                    }
                }
            }
        }

        // Key Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsMetricCard(
                    icon = Icons.Rounded.Repeat,
                    title = "Habit Rate",
                    value = report.habitCompletionRate?.let { "${(it * 100).roundToInt()}%" } ?: "Rest",
                    subtitle = if (report.scheduledHabitSlots > 0) {
                        "${report.completedHabitSlots}/${report.scheduledHabitSlots} slots"
                    } else {
                        "No scheduled habits"
                    },
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricCard(
                    icon = Icons.Rounded.CheckCircle,
                    title = "Tasks Done",
                    value = report.tasksCompleted.toString(),
                    subtitle = report.taskCompletionRate?.let { "${(it * 100).roundToInt()}% rate • ${report.tasksOverdue} overdue" }
                        ?: "${report.tasksPending} pending",
                    accent = MyDayThemeExtras.colors.success,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsMetricCard(
                    icon = Icons.Rounded.LocalFireDepartment,
                    title = "Streaks",
                    value = "${report.activeCurrentStreak}d",
                    subtitle = "Best streak: ${report.bestLongestStreak} days",
                    accent = MyDayPalette.AmberWarning,
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricCard(
                    icon = Icons.Rounded.Timer,
                    title = "Focus Time",
                    value = "${report.totalFocusMinutes}m",
                    subtitle = "${report.totalFocusSessions} sessions • ${report.journalEntriesCount} notes",
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Productivity Trend
        item {
            SectionHeader(
                title = "Productivity Trend",
                subtitle = if (report.restDaysCount > 0) {
                    "${report.restDaysCount} rest day(s) without scheduled tasks/habits excluded from penalties"
                } else {
                    "Habit completion rate over selected range"
                }
            )
        }

        item {
            MyDayCard {
                val pointsToShow = if (report.trendPoints.size > 10) {
                    report.trendPoints.takeLast(10)
                } else {
                    report.trendPoints
                }
                if (pointsToShow.isEmpty()) {
                    Text(
                        text = "No activity data recorded in this period yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    WeeklyProgressBarChart(points = pointsToShow)
                }
            }
        }

        // Calendar Heatmap
        item {
            SectionHeader(
                title = "35-Day Activity Heatmap",
                subtitle = "Combines habits, tasks, focus sessions, and journal entries"
            )
        }

        item {
            MyDayCard {
                CalendarHeatmapGrid(days = report.heatmapDays)
            }
        }

        // Transparent Breakdown Card
        item {
            SectionHeader(
                title = "Transparent Calculation Summary",
                subtitle = "Exact counts from local SQLite records"
            )
        }

        item {
            MyDayCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryLine("Scheduled habit check-ins", "${report.scheduledHabitSlots}")
                    SummaryLine("Completed habit check-ins", "${report.completedHabitSlots}")
                    SummaryLine("Tasks completed in period", "${report.tasksCompleted}")
                    SummaryLine("Tasks currently pending", "${report.tasksPending}")
                    SummaryLine("Overdue pending tasks", "${report.tasksOverdue}")
                    SummaryLine("Completed focus minutes", "${report.totalFocusMinutes} min (${report.totalFocusSessions} sessions)")
                    SummaryLine("Journal entries in period", "${report.journalEntriesCount}")
                    SummaryLine("Rest / unscheduled days", "${report.restDaysCount}")
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    MyDayCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = title, tint = accent)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
