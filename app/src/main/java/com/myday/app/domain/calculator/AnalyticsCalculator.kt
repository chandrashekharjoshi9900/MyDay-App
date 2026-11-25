package com.myday.app.domain.calculator

import com.myday.app.domain.model.AnalyticsPeriod
import com.myday.app.domain.model.DayActivitySummary
import com.myday.app.domain.model.FocusSessionItem
import com.myday.app.domain.model.HabitItem
import com.myday.app.domain.model.HabitLogItem
import com.myday.app.domain.model.HabitScheduleType
import com.myday.app.domain.model.JournalEntryItem
import com.myday.app.domain.model.ProductivityReport
import com.myday.app.domain.model.TaskItem
import com.myday.app.domain.model.TrendPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Transparent productivity analytics engine.
 * Days with zero scheduled habits or tasks are treated as rest days (ratio = null) rather than 0%.
 */
object AnalyticsCalculator {

    private val shortDayFormatter = DateTimeFormatter.ofPattern("EEE")
    private val monthDayFormatter = DateTimeFormatter.ofPattern("MMM d")

    fun buildReport(
        period: AnalyticsPeriod,
        habits: List<HabitItem>,
        habitLogs: List<HabitLogItem>,
        tasks: List<TaskItem>,
        focusSessions: List<FocusSessionItem>,
        journalEntries: List<JournalEntryItem>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ProductivityReport {
        val startDate = when (period) {
            AnalyticsPeriod.DAILY -> today
            AnalyticsPeriod.WEEKLY -> today.minusDays(6)
            AnalyticsPeriod.MONTHLY -> today.minusDays(29)
        }
        val endDate = today

        val logsByHabit = habitLogs.groupBy { it.habitId }
        val completedLogKeys = habitLogs
            .asSequence()
            .filter { it.completed }
            .map { it.habitId to it.epochDay }
            .toSet()

        val activeOrRelevantHabits = habits.filter { !it.isArchived }
        val focusByDay = focusSessions
            .filter { it.completed }
            .groupBy { it.epochDay }
        val journalByDay = journalEntries.associateBy { it.epochDay }

        val daysInRange = generateSequence(startDate) { prev ->
            val next = prev.plusDays(1)
            if (next.isAfter(endDate)) null else next
        }.toList()

        var totalScheduledHabitSlots = 0
        var totalCompletedHabitSlots = 0
        var restDaysCount = 0

        val daySummaries = daysInRange.map { date ->
            val summary = summarizeDay(
                date = date,
                habits = activeOrRelevantHabits,
                completedLogKeys = completedLogKeys,
                tasks = tasks,
                focusMinutes = (focusByDay[date.toEpochDay()] ?: emptyList()).sumOf { it.actualDurationSeconds / 60 },
                hasJournal = journalByDay.containsKey(date.toEpochDay()),
                zoneId = zoneId
            )
            totalScheduledHabitSlots += summary.habitsScheduled
            totalCompletedHabitSlots += summary.habitsCompleted
            if (summary.habitsScheduled == 0 && summary.tasksDueOrCompleted == 0) {
                restDaysCount++
            }
            summary
        }

        // Heatmap always shows last 35 days (5 full weeks) for rich visual continuity
        val heatmapStart = today.minusDays(34)
        val heatmapDays = generateSequence(heatmapStart) { prev ->
            val next = prev.plusDays(1)
            if (next.isAfter(today)) null else next
        }.map { date ->
            summarizeDay(
                date = date,
                habits = activeOrRelevantHabits,
                completedLogKeys = completedLogKeys,
                tasks = tasks,
                focusMinutes = (focusByDay[date.toEpochDay()] ?: emptyList()).sumOf { it.actualDurationSeconds / 60 },
                hasJournal = journalByDay.containsKey(date.toEpochDay()),
                zoneId = zoneId
            )
        }.toList()

        val startEpoch = startDate.toEpochDay()
        val endEpoch = endDate.toEpochDay()
        val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val tasksCompletedInPeriod = tasks.count { task ->
            task.isCompleted && task.completedAtMillis != null &&
                task.completedAtMillis in startMillis until endMillis
        }
        val tasksPending = tasks.count { !it.isCompleted }
        val tasksOverdue = tasks.count { !it.isCompleted && it.dueEpochDay != null && it.dueEpochDay < today.toEpochDay() }
        val totalRelevantTasks = tasksCompletedInPeriod + tasks.count {
            !it.isCompleted && (it.dueEpochDay == null || it.dueEpochDay in startEpoch..endEpoch)
        }

        val periodFocusSessions = focusSessions.filter {
            it.completed && it.epochDay in startEpoch..endEpoch
        }
        val totalFocusMinutes = periodFocusSessions.sumOf { it.actualDurationSeconds } / 60
        val journalCount = journalEntries.count { it.epochDay in startEpoch..endEpoch }

        val streakSummaries = activeOrRelevantHabits.map { habit ->
            StreakCalculator.calculate(
                habit = habit,
                logs = logsByHabit[habit.id].orEmpty(),
                today = today,
                zoneId = zoneId
            )
        }
        val activeCurrentStreak = streakSummaries.maxOfOrNull { it.currentStreak } ?: 0
        val bestLongestStreak = habits.maxOfOrNull { habit ->
            StreakCalculator.calculate(
                habit = habit,
                logs = logsByHabit[habit.id].orEmpty(),
                today = today,
                zoneId = zoneId
            ).longestStreak
        } ?: 0

        val trendPoints = daySummaries.map { day ->
            val label = if (period == AnalyticsPeriod.WEEKLY || period == AnalyticsPeriod.DAILY) {
                day.date.format(shortDayFormatter)
            } else {
                day.date.format(monthDayFormatter)
            }
            TrendPoint(
                label = label,
                date = day.date,
                habitRate = if (day.habitsScheduled > 0) {
                    (day.habitsCompleted.toFloat() / day.habitsScheduled.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                },
                tasksCompleted = day.tasksCompleted,
                focusMinutes = day.focusMinutes
            )
        }

        return ProductivityReport(
            period = period,
            startDate = startDate,
            endDate = endDate,
            scheduledHabitSlots = totalScheduledHabitSlots,
            completedHabitSlots = totalCompletedHabitSlots,
            habitCompletionRate = if (totalScheduledHabitSlots > 0) {
                (totalCompletedHabitSlots.toFloat() / totalScheduledHabitSlots.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            },
            tasksCompleted = tasksCompletedInPeriod,
            tasksPending = tasksPending,
            tasksOverdue = tasksOverdue,
            taskCompletionRate = if (totalRelevantTasks > 0) {
                (tasksCompletedInPeriod.toFloat() / totalRelevantTasks.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            },
            totalFocusMinutes = totalFocusMinutes,
            totalFocusSessions = periodFocusSessions.size,
            journalEntriesCount = journalCount,
            activeCurrentStreak = activeCurrentStreak,
            bestLongestStreak = bestLongestStreak,
            restDaysCount = restDaysCount,
            heatmapDays = heatmapDays,
            trendPoints = trendPoints
        )
    }

    fun summarizeDay(
        date: LocalDate,
        habits: List<HabitItem>,
        completedLogKeys: Set<Pair<String, Long>>,
        tasks: List<TaskItem>,
        focusMinutes: Int,
        hasJournal: Boolean,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DayActivitySummary {
        val epoch = date.toEpochDay()
        val dayStartMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val dayEndMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        var scheduledHabits = 0
        var completedHabits = 0

        for (habit in habits) {
            val createdDate = Instant.ofEpochMilli(habit.createdAtMillis).atZone(zoneId).toLocalDate()
            val pausedOnDay = habit.isPaused && habit.pausedAtEpochDay != null && epoch >= habit.pausedAtEpochDay
            val isCompleted = completedLogKeys.contains(habit.id to epoch)

            if (!createdDate.isAfter(date) && !pausedOnDay) {
                val scheduled = when (habit.scheduleType) {
                    HabitScheduleType.DAILY -> true
                    HabitScheduleType.WEEKDAYS -> habit.selectedWeekdays.contains(date.dayOfWeek.value)
                    HabitScheduleType.WEEKLY_TARGET -> isCompleted // Counted when logged, no false penalty on unlogged days
                }
                if (scheduled) scheduledHabits++
            }
            if (isCompleted) completedHabits++
        }

        val tasksCompletedOnDay = tasks.count {
            it.isCompleted && it.completedAtMillis != null && it.completedAtMillis in dayStartMillis until dayEndMillis
        }
        val tasksDueOnDay = tasks.count { it.dueEpochDay == epoch }
        val tasksDenominator = maxOf(tasksDueOnDay, tasksCompletedOnDay)

        val totalPlanned = scheduledHabits + tasksDenominator
        val totalDone = completedHabits + tasksCompletedOnDay

        val ratio = if (totalPlanned > 0) {
            (totalDone.toFloat() / totalPlanned.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }

        val bonusScore = (if (focusMinutes > 0) 0.2f else 0f) + (if (hasJournal) 0.1f else 0f)
        val intensity = ((ratio ?: 0f) * 0.7f + bonusScore).coerceIn(0f, 1f)

        return DayActivitySummary(
            date = date,
            habitsScheduled = scheduledHabits,
            habitsCompleted = completedHabits,
            tasksDueOrCompleted = tasksDenominator,
            tasksCompleted = tasksCompletedOnDay,
            focusMinutes = focusMinutes,
            hasJournalEntry = hasJournal,
            completionRatio = ratio,
            intensity = intensity
        )
    }
}
