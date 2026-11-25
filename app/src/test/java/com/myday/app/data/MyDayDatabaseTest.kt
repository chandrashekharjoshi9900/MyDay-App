package com.myday.app.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.myday.app.data.local.db.MyDayDatabase
import com.myday.app.data.reminder.ReminderAlarmScheduler
import com.myday.app.data.repository.HabitRepositoryImpl
import com.myday.app.data.repository.ReminderRepositoryImpl
import com.myday.app.data.repository.TaskRepositoryImpl
import com.myday.app.domain.model.HabitItem
import com.myday.app.domain.model.HabitScheduleType
import com.myday.app.domain.model.TaskItem
import com.myday.app.domain.model.TaskPriority
import com.myday.app.domain.model.TaskRecurrence
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class MyDayDatabaseTest {

    private lateinit var context: Context
    private lateinit var db: MyDayDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, MyDayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `editing and archiving habit preserves all historical completion logs`() = runTest {
        val repo = HabitRepositoryImpl(
            habitDao = db.habitDao(),
            habitLogDao = db.habitLogDao(),
            categoryDao = db.categoryDao()
        )
        val day1 = LocalDate.of(2026, 11, 20).toEpochDay()
        val day2 = LocalDate.of(2026, 11, 21).toEpochDay()
        val habit = HabitItem(
            id = "h_keep",
            title = "Meditate",
            description = "10 mins",
            categoryId = null,
            colorHex = 0xFF8E7CFFL,
            scheduleType = HabitScheduleType.DAILY,
            selectedWeekdays = emptySet(),
            weeklyTarget = 7,
            reminderTimeMinutes = null,
            isPaused = false,
            pausedAtEpochDay = null,
            isArchived = false,
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L
        )
        repo.upsertHabit(habit)
        repo.setHabitCompletion("h_keep", day1, true)
        repo.setHabitCompletion("h_keep", day2, true)

        // Edit habit title & schedule
        repo.upsertHabit(habit.copy(title = "Deep Meditation", weeklyTarget = 5, updatedAtMillis = 2000L))
        // Archive habit
        repo.setArchived("h_keep", true)

        val logsAfterEditAndArchive = repo.observeAllLogs().first()
        assertEquals(2, logsAfterEditAndArchive.size)
        assertTrue(logsAfterEditAndArchive.all { it.completed && it.habitId == "h_keep" })
    }

    @Test
    fun `completing recurring task advances due date and records completion history snapshot`() = runTest {
        val reminderRepo = ReminderRepositoryImpl(
            reminderDao = db.reminderDao(),
            taskDao = db.taskDao(),
            habitLogDao = db.habitLogDao(),
            alarmScheduler = ReminderAlarmScheduler(context)
        )
        val taskRepo = TaskRepositoryImpl(
            db = db,
            taskDao = db.taskDao(),
            reminderRepository = reminderRepo
        )
        val startDay = LocalDate.of(2026, 11, 25).toEpochDay()
        val task = TaskItem(
            id = "t_rec",
            title = "Weekly review",
            description = "",
            priority = TaskPriority.HIGH,
            categoryId = null,
            dueEpochDay = startDay,
            dueTimeMinutes = 540,
            recurrence = TaskRecurrence.WEEKLY,
            recurrenceWeekdays = emptySet(),
            reminderId = null,
            isCompleted = false,
            completedAtMillis = null,
            completionCount = 0,
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L
        )
        taskRepo.upsertTask(task)
        taskRepo.toggleTaskCompletion("t_rec")

        val allTasks = taskRepo.observeAllTasks().first()
        assertEquals(2, allTasks.size)

        val activeRecurring = allTasks.first { it.id == "t_rec" }
        assertFalse(activeRecurring.isCompleted)
        assertEquals(startDay + 7, activeRecurring.dueEpochDay)
        assertEquals(1, activeRecurring.completionCount)

        val historySnapshot = allTasks.first { it.id != "t_rec" }
        assertTrue(historySnapshot.isCompleted)
        assertEquals("Weekly review", historySnapshot.title)
    }

    @Test
    fun `migration 1 to 2 adds new columns while preserving existing tasks and focus sessions`() {
        val dbName = "migration_test.db"
        context.deleteDatabase(dbName)

        // Create v1 database directly via SQLiteOpenHelper
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `colorHex` INTEGER NOT NULL, `iconName` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `priority` TEXT NOT NULL, `categoryId` TEXT, `dueEpochDay` INTEGER, `dueTimeMinutes` INTEGER, `recurrence` TEXT NOT NULL, `recurrenceWeekdaysCsv` TEXT NOT NULL, `reminderId` TEXT, `isCompleted` INTEGER NOT NULL, `completedAtMillis` INTEGER, `createdAtMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                    db.execSQL("CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `plannedDurationSeconds` INTEGER NOT NULL, `actualDurationSeconds` INTEGER NOT NULL, `startedAtMillis` INTEGER NOT NULL, `endedAtMillis` INTEGER NOT NULL, `epochDay` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `linkedTaskId` TEXT, PRIMARY KEY(`id`))")
                    db.execSQL("INSERT INTO tasks VALUES ('t1','Legacy task','','HIGH',NULL,20000,NULL,'NONE','',NULL,0,NULL,1000,1000)")
                    db.execSQL("INSERT INTO focus_sessions VALUES ('f1','POMODORO',1500,1500,1000,2500,20000,1,NULL)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val sqliteDb = helper.writableDatabase
        MyDayDatabase.MIGRATION_1_2.migrate(sqliteDb)

        sqliteDb.query("SELECT title, completionCount FROM tasks WHERE id = 't1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Legacy task", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
        }
        sqliteDb.query("SELECT actualDurationSeconds, note FROM focus_sessions WHERE id = 'f1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1500, cursor.getInt(0))
            assertEquals("", cursor.getString(1))
        }
        sqliteDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `migration 2 to 3 adds reminderTone column with CHIME default`() {
        val dbName = "migration_2_3_test.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `app_settings` (`id` TEXT NOT NULL, `userName` TEXT NOT NULL, `themeMode` TEXT NOT NULL, `notificationsEnabled` INTEGER NOT NULL, `notificationSoundEnabled` INTEGER NOT NULL, `vibrationEnabled` INTEGER NOT NULL, `defaultSnoozeMinutes` INTEGER NOT NULL, `hapticFeedbackEnabled` INTEGER NOT NULL, `defaultPomodoroMinutes` INTEGER NOT NULL, `shortBreakMinutes` INTEGER NOT NULL, `longBreakMinutes` INTEGER NOT NULL, `lastBackupMillis` INTEGER, PRIMARY KEY(`id`))")
                    db.execSQL("INSERT INTO app_settings VALUES ('default','Friend','DARK',1,1,1,10,1,25,5,15,NULL)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val sqliteDb = helper.writableDatabase
        MyDayDatabase.MIGRATION_2_3.migrate(sqliteDb)

        sqliteDb.query("SELECT reminderTone FROM app_settings WHERE id = 'default'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("CHIME", cursor.getString(0))
        }
        sqliteDb.close()
        context.deleteDatabase(dbName)
    }
}
