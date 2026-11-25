package com.myday.app.domain.calculator

import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.ReminderScheduleType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Computes exact next trigger timestamps for reminders across timezones and calendar boundaries.
 */
object ReminderScheduleCalculator {

    fun computeNextTriggerMillis(
        reminder: ReminderItem,
        afterMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long? {
        if (!reminder.isEnabled || reminder.isCompleted) return null

        val snooze = reminder.snoozedUntilMillis
        if (snooze != null && snooze > afterMillis) {
            return snooze
        }

        val nowZdt = Instant.ofEpochMilli(afterMillis).atZone(zoneId)
        val targetTime = LocalTime.of(
            reminder.hour.coerceIn(0, 23),
            reminder.minute.coerceIn(0, 59)
        )

        return when (reminder.scheduleType) {
            ReminderScheduleType.ONE_TIME -> {
                val date = reminder.oneTimeEpochDay?.let { LocalDate.ofEpochDay(it) } ?: nowZdt.toLocalDate()
                val trigger = ZonedDateTime.of(date, targetTime, zoneId).toInstant().toEpochMilli()
                if (trigger > afterMillis) trigger else null
            }

            ReminderScheduleType.DAILY -> {
                var candidate = ZonedDateTime.of(nowZdt.toLocalDate(), targetTime, zoneId)
                if (!candidate.toInstant().toEpochMilli().let { it > afterMillis }) {
                    candidate = candidate.plusDays(1)
                }
                candidate.toInstant().toEpochMilli()
            }

            ReminderScheduleType.WEEKLY -> {
                val allowedDays = reminder.weekdays.ifEmpty {
                    setOf(
                        reminder.oneTimeEpochDay?.let { LocalDate.ofEpochDay(it).dayOfWeek.value }
                            ?: nowZdt.dayOfWeek.value
                    )
                }
                findNextMatchingWeekday(nowZdt, targetTime, allowedDays, afterMillis, zoneId)
            }

            ReminderScheduleType.CUSTOM_WEEKDAYS -> {
                val allowedDays = reminder.weekdays.ifEmpty { setOf(nowZdt.dayOfWeek.value) }
                findNextMatchingWeekday(nowZdt, targetTime, allowedDays, afterMillis, zoneId)
            }

            ReminderScheduleType.MONTHLY -> {
                val desiredDay = (reminder.dayOfMonth ?: nowZdt.dayOfMonth).coerceIn(1, 31)
                var yearMonth = nowZdt.toLocalDate().withDayOfMonth(1)
                repeat(24) {
                    val clampedDay = desiredDay.coerceAtMost(yearMonth.lengthOfMonth())
                    val candidate = ZonedDateTime.of(
                        yearMonth.withDayOfMonth(clampedDay),
                        targetTime,
                        zoneId
                    )
                    val millis = candidate.toInstant().toEpochMilli()
                    if (millis > afterMillis) return millis
                    yearMonth = yearMonth.plusMonths(1)
                }
                null
            }

            ReminderScheduleType.CUSTOM_INTERVAL -> {
                val intervalHours = (reminder.intervalHours ?: 24).coerceAtLeast(1)
                val intervalMillis = intervalHours * 3_600_000L
                val anchorDate = reminder.oneTimeEpochDay?.let { LocalDate.ofEpochDay(it) } ?: nowZdt.toLocalDate()
                var candidateMillis = ZonedDateTime.of(anchorDate, targetTime, zoneId).toInstant().toEpochMilli()
                if (candidateMillis <= afterMillis) {
                    val diff = afterMillis - candidateMillis
                    val steps = (diff / intervalMillis) + 1
                    candidateMillis += steps * intervalMillis
                }
                candidateMillis
            }
        }
    }

    private fun findNextMatchingWeekday(
        nowZdt: ZonedDateTime,
        targetTime: LocalTime,
        allowedWeekdays: Set<Int>,
        afterMillis: Long,
        zoneId: ZoneId
    ): Long {
        var date = nowZdt.toLocalDate()
        repeat(8) {
            if (allowedWeekdays.contains(date.dayOfWeek.value)) {
                val candidate = ZonedDateTime.of(date, targetTime, zoneId).toInstant().toEpochMilli()
                if (candidate > afterMillis) return candidate
            }
            date = date.plusDays(1)
        }
        return ZonedDateTime.of(nowZdt.toLocalDate().plusWeeks(1), targetTime, zoneId)
            .toInstant()
            .toEpochMilli()
    }
}
