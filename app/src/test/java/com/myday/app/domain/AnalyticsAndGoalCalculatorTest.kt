package com.myday.app.domain

import com.myday.app.domain.calculator.AnalyticsCalculator
import com.myday.app.domain.calculator.GoalProgressCalculator
import com.myday.app.domain.calculator.ReminderScheduleCalculator
import com.myday.app.domain.model.AnalyticsPeriod
import com.myday.app.domain.model.FocusSessionItem
import com.myday.app.domain.model.FocusSessionType
import com.myday.app.domain.model.GoalItem
import com.myday.app.domain.model.GoalStatus
import com.myday.app.domain.model.HabitItem
import com.myday.app.domain.model.HabitLogItem
import com.myday.app.domain.model.HabitScheduleType
import com.myday.app.domain.model.JournalEntryItem
import com.myday.app.domain.model.MilestoneItem
import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.ReminderScheduleType
import com.myday.app.domain.model.ReminderTargetType
import com.myday.app.domain.model.TaskItem
import com.myday.app.domain.model.TaskPriority
import com.myday.app.domain.model.TaskRecurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class AnalyticsAndGoalCalculatorTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun `analytics handles rest days with zero scheduled habits without divide by zero`() {
        val today = LocalDate.of(2026, 11, 25)
        val report = AnalyticsCalculator.buildReport(
            period = AnalyticsPeriod.WEEKLY,
            habits = emptyList(),
            habitLogs = emptyList(),
            tasks = emptyList(),
            focusSessions = emptyList(),
            journalEntries = emptyList(),
            today = today,
            zoneId = zone
        )

        assertEquals(0, report.scheduledHabitSlots)
        assertNull(report.habitCompletionRate)
        assertNull(report.taskCompletionRate)
        assertEquals(7, report.restDaysCount)
        assertEquals(35, report.heatmapDays.size)
    }

    @Test
    fun `analytics computes habit task focus and journal metrics accurately`() {
        val today = LocalDate.of(2026, 11, 25) // Wednesday
        val createdMillis = today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        val habit = HabitItem(
            id = "h1",
            title = "Workout",
            description = "",
            categoryId = null,
            colorHex = 0xFF8E7CFFL,
            scheduleType = HabitScheduleType.DAILY,
            selectedWeekdays = emptySet(),
            weeklyTarget = 7,
            reminderTimeMinutes = null,
            isPaused = false,
            pausedAtEpochDay = null,
            isArchived = false,
            createdAtMillis = createdMillis,
            updatedAtMillis = createdMillis
        )
        val logs = (0L..3L).map { offset ->
            HabitLogItem(
                id = "l_$offset",
                habitId = "h1",
                epochDay = today.minusDays(offset).toEpochDay(),
                completed = true,
                completedAtMillis = today.minusDays(offset).atStartOfDay(zone).toInstant().toEpochMilli()
            )
        }
        val completedTask = TaskItem(
            id = "t1",
            title = "Ship feature",
            description = "",
            priority = TaskPriority.HIGH,
            categoryId = null,
            dueEpochDay = today.toEpochDay(),
            dueTimeMinutes = null,
            recurrence = TaskRecurrence.NONE,
            recurrenceWeekdays = emptySet(),
            reminderId = null,
            isCompleted = true,
            completedAtMillis = today.atStartOfDay(zone).toInstant().toEpochMilli() + 3600_000L,
            completionCount = 1,
            createdAtMillis = createdMillis,
            updatedAtMillis = createdMillis
        )
        val focus = FocusSessionItem(
            id = "f1",
            type = FocusSessionType.POMODORO,
            plannedDurationSeconds = 1500,
            actualDurationSeconds = 1500,
            startedAtMillis = createdMillis,
            endedAtMillis = createdMillis + 1500_000L,
            epochDay = today.toEpochDay(),
            completed = true,
            linkedTaskId = "t1"
        )
        val journal = JournalEntryItem(
            id = "j1",
            epochDay = today.toEpochDay(),
            title = "Win",
            content = "Focused well",
            achievements = "Finished module",
            reflection = "Sleep earlier",
            moodLevel = 5,
            createdAtMillis = createdMillis,
            updatedAtMillis = createdMillis
        )

        val report = AnalyticsCalculator.buildReport(
            period = AnalyticsPeriod.WEEKLY,
            habits = listOf(habit),
            habitLogs = logs,
            tasks = listOf(completedTask),
            focusSessions = listOf(focus),
            journalEntries = listOf(journal),
            today = today,
            zoneId = zone
        )

        assertEquals(7, report.scheduledHabitSlots)
        assertEquals(4, report.completedHabitSlots)
        assertEquals(4f / 7f, report.habitCompletionRate!!, 0.001f)
        assertEquals(1, report.tasksCompleted)
        assertEquals(25, report.totalFocusMinutes)
        assertEquals(1, report.journalEntriesCount)
        assertEquals(4, report.activeCurrentStreak)
    }

    @Test
    fun `goal progress uses milestone ratio when milestones exist and manual otherwise`() {
        val goal = GoalItem(
            id = "g1",
            title = "Learn Kotlin",
            description = "",
            categoryId = null,
            colorHex = 0xFF8E7CFFL,
            targetEpochDay = null,
            status = GoalStatus.ACTIVE,
            manualProgressPercent = 40,
            createdAtMillis = 0L,
            updatedAtMillis = 0L,
            completedAtMillis = null
        )
        assertEquals(40, GoalProgressCalculator.calculateProgressPercent(goal, emptyList()))

        val milestones = listOf(
            MilestoneItem("m1", "g1", "Step 1", true, 0, 100L),
            MilestoneItem("m2", "g1", "Step 2", true, 1, 100L),
            MilestoneItem("m3", "g1", "Step 3", false, 2, null)
        )
        assertEquals(67, GoalProgressCalculator.calculateProgressPercent(goal, milestones))
        assertEquals(100, GoalProgressCalculator.calculateProgressPercent(goal.copy(status = GoalStatus.COMPLETED), milestones))
    }

    @Test
    fun `monthly reminder clamps 31st safely to February 28`() {
        val now = ZonedDateTime.of(2026, 2, 10, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        val reminder = ReminderItem(
            id = "r1",
            title = "Pay rent",
            notes = "",
            scheduleType = ReminderScheduleType.MONTHLY,
            hour = 10,
            minute = 0,
            oneTimeEpochDay = null,
            weekdays = emptySet(),
            dayOfMonth = 31,
            intervalHours = null,
            nextTriggerMillis = 0L,
            isEnabled = true,
            isCompleted = false,
            snoozedUntilMillis = null,
            playSound = true,
            vibrate = true,
            targetType = ReminderTargetType.STANDALONE,
            targetId = null,
            lastTriggeredMillis = null,
            createdAtMillis = now,
            updatedAtMillis = now
        )

        val next = ReminderScheduleCalculator.computeNextTriggerMillis(reminder, afterMillis = now, zoneId = zone)
        assertNotNull(next)
        val zdt = Instant.ofEpochMilli(next!!).atZone(zone)
        assertEquals(2026, zdt.year)
        assertEquals(2, zdt.monthValue)
        assertEquals(28, zdt.dayOfMonth)
        assertEquals(10, zdt.hour)
    }

    @Test
    fun `snoozed reminder prioritizes snooze timestamp while in future`() {
        val now = 1_700_000_000_000L
        val snoozeTarget = now + 600_000L
        val reminder = ReminderItem(
            id = "r2",
            title = "Hydrate",
            notes = "",
            scheduleType = ReminderScheduleType.DAILY,
            hour = 8,
            minute = 0,
            oneTimeEpochDay = null,
            weekdays = emptySet(),
            dayOfMonth = null,
            intervalHours = null,
            nextTriggerMillis = now,
            isEnabled = true,
            isCompleted = false,
            snoozedUntilMillis = snoozeTarget,
            playSound = true,
            vibrate = true,
            targetType = ReminderTargetType.STANDALONE,
            targetId = null,
            lastTriggeredMillis = null,
            createdAtMillis = now,
            updatedAtMillis = now
        )

        val next = ReminderScheduleCalculator.computeNextTriggerMillis(reminder, afterMillis = now, zoneId = zone)
        assertTrue(next == snoozeTarget)
    }
}
