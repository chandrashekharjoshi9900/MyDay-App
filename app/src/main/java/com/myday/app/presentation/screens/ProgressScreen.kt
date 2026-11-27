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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myday.app.domain.model.AnalyticsPeriod
import com.myday.app.presentation.components.CalendarHeatmapGrid
import com.myday.app.presentation.components.LoadingStateView
import com.myday.app.presentation.components.MyDayCard
import com.myday.app.presentation.components.SectionHeader
import com.myday.app.presentation.components.WeeklyProgressBarChart
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
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "Progress",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${report.startDate.format(dateFmt)} – ${report.endDate.format(dateFmt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MyDayThemeExtras.colors.mutedText
                )
                Spacer(modifier = Modifier.height(12.dp))
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

        // Numbers-only compact stat cards (no decorative icons inside stat cards)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsMetricCard(
                    title = "Habit rate",
                    value = report.habitCompletionRate?.let { "${(it * 100).roundToInt()}%" } ?: "Rest",
                    subtitle = if (report.scheduledHabitSlots > 0) {
                        "${report.completedHabitSlots}/${report.scheduledHabitSlots} slots"
                    } else {
                        "No scheduled habits"
                    },
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricCard(
                    title = "Tasks done",
                    value = report.tasksCompleted.toString(),
                    subtitle = report.taskCompletionRate?.let { "${(it * 100).roundToInt()}% rate • ${report.tasksOverdue} overdue" }
                        ?: "${report.tasksPending} pending",
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
                    title = "Active streak",
                    value = "${report.activeCurrentStreak}d",
                    subtitle = "Best: ${report.bestLongestStreak} days",
                    modifier = Modifier.weight(1f)
                )
                AnalyticsMetricCard(
                    title = "Focus time",
                    value = "${report.totalFocusMinutes}m",
                    subtitle = "${report.totalFocusSessions} sessions • ${report.journalEntriesCount} notes",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Primary Productivity Trend Chart
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Productivity trend",
                subtitle = if (report.restDaysCount > 0) {
                    "${report.restDaysCount} rest days excluded from penalty"
                } else {
                    "Habit completion rate"
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
                        text = "No activity data recorded in this period",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    WeeklyProgressBarChart(points = pointsToShow)
                }
            }
        }

        // 35-Day Heatmap
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "35-day activity",
                subtitle = "Habits, tasks, focus, and journal entries"
            )
        }

        item {
            MyDayCard {
                CalendarHeatmapGrid(days = report.heatmapDays)
            }
        }

        // Transparent Calculation Summary
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(
                title = "Breakdown",
                subtitle = "Exact counts from local SQLite records"
            )
        }

        item {
            MyDayCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryLine("Scheduled habit check-ins", "${report.scheduledHabitSlots}")
                    HorizontalDivider(thickness = 1.dp, color = MyDayThemeExtras.colors.cardBorder)
                    SummaryLine("Completed habit check-ins", "${report.completedHabitSlots}")
                    HorizontalDivider(thickness = 1.dp, color = MyDayThemeExtras.colors.cardBorder)
                    SummaryLine("Tasks completed", "${report.tasksCompleted}")
                    HorizontalDivider(thickness = 1.dp, color = MyDayThemeExtras.colors.cardBorder)
                    SummaryLine("Tasks pending", "${report.tasksPending} (${report.tasksOverdue} overdue)")
                    HorizontalDivider(thickness = 1.dp, color = MyDayThemeExtras.colors.cardBorder)
                    SummaryLine("Focus sessions", "${report.totalFocusMinutes} min (${report.totalFocusSessions})")
                    HorizontalDivider(thickness = 1.dp, color = MyDayThemeExtras.colors.cardBorder)
                    SummaryLine("Journal entries", "${report.journalEntriesCount}")
                    HorizontalDivider(thickness = 1.dp, color = MyDayThemeExtras.colors.cardBorder)
                    SummaryLine("Unscheduled rest days", "${report.restDaysCount}")
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    MyDayCard(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MyDayThemeExtras.colors.mutedText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
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
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
