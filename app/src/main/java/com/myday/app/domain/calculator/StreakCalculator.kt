package com.myday.app.domain.calculator

import com.myday.app.domain.model.HabitItem
import com.myday.app.domain.model.HabitLogItem
import com.myday.app.domain.model.HabitScheduleType
import com.myday.app.domain.model.HabitStreakSummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Pure, deterministic habit streak and schedule calculator.
 *
 * Rules:
 * - Never counts days before the habit was created as missed.
 * - Preserves historical completion records regardless of edits or archiving.
 * - For DAILY and WEEKDAYS schedules, streaks advance on consecutive scheduled days completed.
 *   Unscheduled days (e.g. weekends on a Mon-Fri habit) neither increment nor break the streak.
 * - For paused habits, days from [HabitItem.pausedAtEpochDay] onward are treated as paused:
 *   they are not marked missed and do not break the streak earned before pausing.
 * - For WEEKLY_TARGET habits, streaks count consecutive scheduled weeks (Mon..Sun) meeting
 *   [HabitItem.weeklyTarget], while the current in-progress week is counted once met (or preserved
 *   if prior week was met).
 */
object StreakCalculator {

    fun isScheduledOn(habit: HabitItem, date: LocalDate): Boolean {
        return when (habit.scheduleType) {
            HabitScheduleType.DAILY -> true
            HabitScheduleType.WEEKDAYS -> habit.selectedWeekdays.contains(date.dayOfWeek.value)
            HabitScheduleType.WEEKLY_TARGET -> true
        }
    }

    fun calculate(
        habit: HabitItem,
        logs: List<HabitLogItem>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): HabitStreakSummary {
        val completedDays: Set<Long> = logs
            .asSequence()
            .filter { it.completed }
            .map { it.epochDay }
            .toSet()

        val createdDate = Instant.ofEpochMilli(habit.createdAtMillis)
            .atZone(zoneId)
            .toLocalDate()
        val firstRelevantDate = minOf(
            createdDate,
            completedDays.minOrNull()?.let { LocalDate.ofEpochDay(it) } ?: createdDate
        )

        val todayEpoch = today.toEpochDay()
        val completedToday = completedDays.contains(todayEpoch)
        val scheduledToday = !habit.isPaused && isScheduledOn(habit, today)

        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusDays(6)
        val completedThisWeek = completedDays.count { epochDay ->
            epochDay in weekStart.toEpochDay()..weekEnd.toEpochDay()
        }
        val targetThisWeek = when (habit.scheduleType) {
            HabitScheduleType.DAILY -> 7
            HabitScheduleType.WEEKDAYS -> habit.selectedWeekdays.size.coerceAtLeast(1)
            HabitScheduleType.WEEKLY_TARGET -> habit.weeklyTarget.coerceIn(1, 7)
        }

        val (currentStreak, longestStreak) = when (habit.scheduleType) {
            HabitScheduleType.WEEKLY_TARGET -> calculateWeeklyTargetStreaks(
                habit = habit,
                completedDays = completedDays,
                firstDate = firstRelevantDate,
                today = today
            )
            HabitScheduleType.DAILY, HabitScheduleType.WEEKDAYS -> calculateDailyOrWeekdayStreaks(
                habit = habit,
                completedDays = completedDays,
                firstDate = firstRelevantDate,
                today = today
            )
        }

        // Missed days over the last 14 days (strictly before today, after creation, not paused)
        val missedStart = maxOf(firstRelevantDate, today.minusDays(14))
        val missedRecent = mutableListOf<LocalDate>()
        var cursor = today.minusDays(1)
        while (!cursor.isBefore(missedStart)) {
            val epoch = cursor.toEpochDay()
            val isPausedOnDay = habit.isPaused && habit.pausedAtEpochDay != null && epoch >= habit.pausedAtEpochDay
            if (!isPausedOnDay &&
                habit.scheduleType != HabitScheduleType.WEEKLY_TARGET &&
                isScheduledOn(habit, cursor) &&
                !completedDays.contains(epoch)
            ) {
                missedRecent.add(cursor)
            }
            cursor = cursor.minusDays(1)
        }

        return HabitStreakSummary(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            completedToday = completedToday,
            scheduledToday = scheduledToday,
            completedThisWeek = completedThisWeek,
            targetThisWeek = targetThisWeek,
            missedRecentDays = missedRecent,
            totalCompletions = completedDays.size
        )
    }

    private fun calculateDailyOrWeekdayStreaks(
        habit: HabitItem,
        completedDays: Set<Long>,
        firstDate: LocalDate,
        today: LocalDate
    ): Pair<Int, Int> {
        val lastLogDate = completedDays.maxOrNull()?.let { LocalDate.ofEpochDay(it) }
        val endDate = if (lastLogDate != null && lastLogDate.isAfter(today)) lastLogDate else today

        // Longest streak scan forward from firstDate to endDate
        var running = 0
        var longest = 0
        var d = firstDate
        while (!d.isAfter(endDate)) {
            val epoch = d.toEpochDay()
            val pausedOnDay = habit.isPaused && habit.pausedAtEpochDay != null && epoch >= habit.pausedAtEpochDay
            if (!pausedOnDay && isScheduledOn(habit, d)) {
                if (completedDays.contains(epoch)) {
                    running++
                    if (running > longest) longest = running
                } else if (d.isBefore(today)) {
                    running = 0
                }
            }
            d = d.plusDays(1)
        }

        // Current streak scan backward from today
        val effectiveAnchor = if (habit.isPaused && habit.pausedAtEpochDay != null) {
            minOf(today, LocalDate.ofEpochDay(habit.pausedAtEpochDay - 1))
        } else {
            today
        }

        var current = 0
        var back = effectiveAnchor
        while (!back.isBefore(firstDate)) {
            val epoch = back.toEpochDay()
            if (isScheduledOn(habit, back)) {
                if (completedDays.contains(epoch)) {
                    current++
                } else {
                    // Today doesn't break yesterday's active streak if today hasn't ended yet
                    if (back != today) {
                        break
                    }
                }
            }
            back = back.minusDays(1)
        }

        return current to maxOf(longest, current)
    }

    private fun calculateWeeklyTargetStreaks(
        habit: HabitItem,
        completedDays: Set<Long>,
        firstDate: LocalDate,
        today: LocalDate
    ): Pair<Int, Int> {
        val target = habit.weeklyTarget.coerceIn(1, 7)
        val firstWeekStart = firstDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        // Forward scan for longest streak of consecutive weeks meeting target
        var running = 0
        var longest = 0
        var week = firstWeekStart
        while (!week.isAfter(currentWeekStart)) {
            val startEpoch = week.toEpochDay()
            val endEpoch = week.plusDays(6).toEpochDay()
            val count = completedDays.count { it in startEpoch..endEpoch }
            val isPausedWeek = habit.isPaused && habit.pausedAtEpochDay != null && startEpoch >= habit.pausedAtEpochDay
            if (!isPausedWeek) {
                if (count >= target) {
                    running++
                    if (running > longest) longest = running
                } else if (week.isBefore(currentWeekStart)) {
                    running = 0
                }
            }
            week = week.plusWeeks(1)
        }

        // Backward scan for current streak
        var current = 0
        var backWeek = currentWeekStart
        while (!backWeek.isBefore(firstWeekStart)) {
            val startEpoch = backWeek.toEpochDay()
            val endEpoch = backWeek.plusDays(6).toEpochDay()
            val isPausedWeek = habit.isPaused && habit.pausedAtEpochDay != null && startEpoch >= habit.pausedAtEpochDay
            if (!isPausedWeek) {
                val count = completedDays.count { it in startEpoch..endEpoch }
                if (count >= target) {
                    current++
                } else if (backWeek != currentWeekStart) {
                    break
                }
            }
            backWeek = backWeek.minusWeeks(1)
        }

        return current to maxOf(longest, current)
    }
}
