package com.myday.app.domain

import com.myday.app.domain.calculator.StreakCalculator
import com.myday.app.domain.model.HabitItem
import com.myday.app.domain.model.HabitLogItem
import com.myday.app.domain.model.HabitScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class StreakCalculatorTest {

    private val zone = ZoneId.of("UTC")

    private fun habit(
        scheduleType: HabitScheduleType = HabitScheduleType.DAILY,
        weekdays: Set<Int> = emptySet(),
        weeklyTarget: Int = 3,
        createdDaysAgo: Long = 20,
        today: LocalDate = LocalDate.of(2026, 11, 25),
        isPaused: Boolean = false,
        pausedAtEpochDay: Long? = null
    ): HabitItem {
        val createdMillis = today.minusDays(createdDaysAgo).atStartOfDay(zone).toInstant().toEpochMilli()
        return HabitItem(
            id = "h1",
            title = "Read",
            description = "",
            categoryId = null,
            colorHex = 0xFF8E7CFFL,
            scheduleType = scheduleType,
            selectedWeekdays = weekdays,
            weeklyTarget = weeklyTarget,
            reminderTimeMinutes = null,
            isPaused = isPaused,
            pausedAtEpochDay = pausedAtEpochDay,
            isArchived = false,
            createdAtMillis = createdMillis,
            updatedAtMillis = createdMillis
        )
    }

    private fun log(day: LocalDate, completed: Boolean = true) = HabitLogItem(
        id = "log_${day.toEpochDay()}",
        habitId = "h1",
        epochDay = day.toEpochDay(),
        completed = completed,
        completedAtMillis = day.atStartOfDay(zone).toInstant().toEpochMilli()
    )

    @Test
    fun `daily habit tracks current and longest streak accurately`() {
        val today = LocalDate.of(2026, 11, 25)
        val h = habit(today = today, createdDaysAgo = 15)
        val logs = listOf(
            log(today.minusDays(12)),
            log(today.minusDays(11)),
            log(today.minusDays(10)),
            log(today.minusDays(9)),
            // gap at -8
            log(today.minusDays(2)),
            log(today.minusDays(1)),
            log(today)
        )

        val result = StreakCalculator.calculate(h, logs, today, zone)
        assertEquals(3, result.currentStreak)
        assertEquals(4, result.longestStreak)
        assertTrue(result.completedToday)
        assertEquals(7, result.totalCompletions)
    }

    @Test
    fun `daily habit does not break yesterday streak before today ends`() {
        val today = LocalDate.of(2026, 11, 25)
        val h = habit(today = today, createdDaysAgo = 10)
        val logs = listOf(
            log(today.minusDays(3)),
            log(today.minusDays(2)),
            log(today.minusDays(1))
        )

        val result = StreakCalculator.calculate(h, logs, today, zone)
        assertEquals(3, result.currentStreak)
        assertFalse(result.completedToday)
    }

    @Test
    fun `weekday habit skips unscheduled days without breaking streak`() {
        // 2026-11-25 is Wednesday (3)
        val today = LocalDate.of(2026, 11, 25)
        val monWedFri = setOf(1, 3, 5)
        val h = habit(
            scheduleType = HabitScheduleType.WEEKDAYS,
            weekdays = monWedFri,
            today = today,
            createdDaysAgo = 7
        )
        val logs = listOf(
            log(LocalDate.of(2026, 11, 18)), // Wed
            log(LocalDate.of(2026, 11, 20)), // Fri
            log(LocalDate.of(2026, 11, 23)), // Mon
            log(LocalDate.of(2026, 11, 25))  // Wed (today)
        )

        val result = StreakCalculator.calculate(h, logs, today, zone)
        assertEquals(4, result.currentStreak)
        assertEquals(4, result.longestStreak)
        assertTrue(result.missedRecentDays.isEmpty())
    }

    @Test
    fun `paused habit preserves earned streak and does not record missed days after pause`() {
        val today = LocalDate.of(2026, 11, 25)
        val pauseStart = today.minusDays(3)
        val h = habit(
            today = today,
            createdDaysAgo = 10,
            isPaused = true,
            pausedAtEpochDay = pauseStart.toEpochDay()
        )
        val logs = listOf(
            log(today.minusDays(6)),
            log(today.minusDays(5)),
            log(today.minusDays(4))
        )

        val result = StreakCalculator.calculate(h, logs, today, zone)
        assertEquals(3, result.currentStreak)
        assertFalse(result.scheduledToday)
        assertFalse(result.missedRecentDays.any { it >= pauseStart })
    }

    @Test
    fun `weekly target habit counts consecutive successful weeks`() {
        val today = LocalDate.of(2026, 11, 25) // Wed of week starting Nov 23
        val h = habit(
            scheduleType = HabitScheduleType.WEEKLY_TARGET,
            weeklyTarget = 2,
            today = today,
            createdDaysAgo = 20
        )
        val logs = listOf(
            // Week Nov 9..15 (2 completions)
            log(LocalDate.of(2026, 11, 10)),
            log(LocalDate.of(2026, 11, 13)),
            // Week Nov 16..22 (2 completions)
            log(LocalDate.of(2026, 11, 17)),
            log(LocalDate.of(2026, 11, 21)),
            // Current week Nov 23..29 (2 completions)
            log(LocalDate.of(2026, 11, 23)),
            log(LocalDate.of(2026, 11, 25))
        )

        val result = StreakCalculator.calculate(h, logs, today, zone)
        assertEquals(3, result.currentStreak)
        assertEquals(3, result.longestStreak)
        assertEquals(2, result.completedThisWeek)
    }
}
