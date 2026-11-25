package com.myday.app.data

import com.myday.app.data.backup.BackupDataDto
import com.myday.app.data.backup.BackupGoalDto
import com.myday.app.data.backup.BackupHabitDto
import com.myday.app.data.backup.BackupHabitLogDto
import com.myday.app.data.backup.BackupMilestoneDto
import com.myday.app.data.backup.BackupValidator
import com.myday.app.data.backup.MyDayBackupEnvelope
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupValidatorTest {

    @Test
    fun `valid backup envelope passes checksum and structural validation`() {
        val data = BackupDataDto(
            habits = listOf(
                BackupHabitDto(
                    id = "h1",
                    title = "Stretch",
                    description = "Morning",
                    colorHex = 0xFF8E7CFFL,
                    scheduleType = "DAILY",
                    createdAtMillis = 1000L,
                    updatedAtMillis = 1000L
                )
            ),
            habitLogs = listOf(
                BackupHabitLogDto("hl1", "h1", 20000L, true, 1500L)
            ),
            goals = listOf(
                BackupGoalDto("g1", "Run 5K", "", null, 0xFF2FD180L, null, "ACTIVE", 0, 1000L, 1000L, null)
            ),
            milestones = listOf(
                BackupMilestoneDto("m1", "g1", "Week 1", true, 0, 1200L)
            )
        )
        val envelope = MyDayBackupEnvelope(
            appId = "MYDAY",
            schemaVersion = 1,
            appVersion = "1.0.0",
            exportedAtMillis = 2000L,
            checksumSha256 = BackupValidator.computeChecksum(data),
            data = data
        )

        val json = BackupValidator.json.encodeToString(envelope)
        val result = BackupValidator.validate(json)
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().data.habits.size)
        assertEquals(1, result.getOrThrow().data.habitLogs.size)
    }

    @Test
    fun `tampered backup fails checksum verification`() {
        val data = BackupDataDto()
        val envelope = MyDayBackupEnvelope(
            exportedAtMillis = 1000L,
            checksumSha256 = "0000deadbeef",
            data = data
        )
        val json = BackupValidator.json.encodeToString(envelope)
        val result = BackupValidator.validate(json)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("checksum", ignoreCase = true) == true)
    }

    @Test
    fun `backup with orphan habit log fails structural validation`() {
        val data = BackupDataDto(
            habits = emptyList(),
            habitLogs = listOf(
                BackupHabitLogDto("hl1", "missing_habit", 20000L, true, 1500L)
            )
        )
        val envelope = MyDayBackupEnvelope(
            exportedAtMillis = 1000L,
            checksumSha256 = BackupValidator.computeChecksum(data),
            data = data
        )
        val json = BackupValidator.json.encodeToString(envelope)
        val result = BackupValidator.validate(json)
        assertTrue(result.isFailure)
    }
}
