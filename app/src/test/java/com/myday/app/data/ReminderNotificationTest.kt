package com.myday.app.data

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.myday.app.data.local.db.MyDayDatabase
import com.myday.app.data.reminder.NotificationChannels
import com.myday.app.data.reminder.NotificationHelper
import com.myday.app.data.reminder.ReminderAlarmScheduler
import com.myday.app.domain.model.ReminderTone
import com.myday.app.data.repository.HabitRepositoryImpl
import com.myday.app.data.repository.ReminderRepositoryImpl
import com.myday.app.data.repository.TaskRepositoryImpl
import com.myday.app.domain.model.HabitItem
import com.myday.app.domain.model.HabitScheduleType
import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.ReminderScheduleType
import com.myday.app.domain.model.ReminderTargetType
import com.myday.app.domain.model.TaskItem
import com.myday.app.domain.model.TaskPriority
import com.myday.app.domain.model.TaskRecurrence
import com.myday.app.presentation.viewmodel.HabitsViewModel
import com.myday.app.presentation.viewmodel.TasksViewModel
import com.myday.app.data.repository.CategoryRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class ReminderNotificationTest {

    private lateinit var context: Context
    private lateinit var db: MyDayDatabase
    private lateinit var alarmScheduler: ReminderAlarmScheduler
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var reminderRepository: ReminderRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        db = Room.inMemoryDatabaseBuilder(context, MyDayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        alarmScheduler = ReminderAlarmScheduler(context)
        notificationHelper = NotificationHelper(context)
        reminderRepository = ReminderRepositoryImpl(
            reminderDao = db.reminderDao(),
            taskDao = db.taskDao(),
            habitLogDao = db.habitLogDao(),
            alarmScheduler = alarmScheduler
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `scheduling same reminder twice prevents duplicate alarms`() = runTest {
        val future = System.currentTimeMillis() + 3_600_000L
        val reminder = ReminderItem(
            id = "rem_dup_test",
            title = "Stand up",
            notes = "Stretch",
            scheduleType = ReminderScheduleType.DAILY,
            hour = 14,
            minute = 30,
            oneTimeEpochDay = null,
            weekdays = emptySet(),
            dayOfMonth = null,
            intervalHours = null,
            nextTriggerMillis = future,
            isEnabled = true,
            isCompleted = false,
            snoozedUntilMillis = null,
            playSound = true,
            vibrate = true,
            targetType = ReminderTargetType.STANDALONE,
            targetId = null,
            lastTriggeredMillis = null,
            createdAtMillis = System.currentTimeMillis(),
            updatedAtMillis = System.currentTimeMillis()
        )

        reminderRepository.upsertAndSchedule(reminder)
        reminderRepository.upsertAndSchedule(reminder.copy(title = "Stand up & stretch"))

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        reminderRepository.deleteReminder("rem_dup_test")
        assertEquals(0, shadowAlarmManager.scheduledAlarms.size)
    }

    @Test
    fun `snooze reschedules alarm and markCompleted completes linked task`() = runTest {
        val taskRepo = TaskRepositoryImpl(db, db.taskDao(), reminderRepository)
        val now = System.currentTimeMillis()
        val task = TaskItem(
            id = "t_linked",
            title = "Submit report",
            description = "",
            priority = TaskPriority.HIGH,
            categoryId = null,
            dueEpochDay = null,
            dueTimeMinutes = null,
            recurrence = TaskRecurrence.NONE,
            recurrenceWeekdays = emptySet(),
            reminderId = "rem_linked",
            isCompleted = false,
            completedAtMillis = null,
            completionCount = 0,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        taskRepo.upsertTask(task)

        val reminder = ReminderItem(
            id = "rem_linked",
            title = "Task: Submit report",
            notes = "",
            scheduleType = ReminderScheduleType.ONE_TIME,
            hour = 23,
            minute = 59,
            oneTimeEpochDay = java.time.LocalDate.now().plusDays(1).toEpochDay(),
            weekdays = emptySet(),
            dayOfMonth = null,
            intervalHours = null,
            nextTriggerMillis = now + 86_400_000L,
            isEnabled = true,
            isCompleted = false,
            snoozedUntilMillis = null,
            playSound = true,
            vibrate = true,
            targetType = ReminderTargetType.TASK,
            targetId = "t_linked",
            lastTriggeredMillis = null,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        reminderRepository.upsertAndSchedule(reminder)

        reminderRepository.snoozeReminder("rem_linked", 15)
        val snoozed = reminderRepository.getReminderById("rem_linked")
        assertNotNull(snoozed?.snoozedUntilMillis)

        reminderRepository.markCompleted("rem_linked")
        val completedReminder = reminderRepository.getReminderById("rem_linked")
        val completedTask = taskRepo.getTaskById("t_linked")
        assertTrue(completedReminder?.isCompleted == true)
        assertFalse(completedReminder?.isEnabled == true)
        assertTrue(completedTask?.isCompleted == true)
    }

    @Test
    fun `notification helper builds reminder notification with complete snooze and dismiss actions`() {
        val now = System.currentTimeMillis()
        val reminder = ReminderItem(
            id = "rem_notif",
            title = "Evening Review",
            notes = "Check goals",
            scheduleType = ReminderScheduleType.DAILY,
            hour = 20,
            minute = 0,
            oneTimeEpochDay = null,
            weekdays = emptySet(),
            dayOfMonth = null,
            intervalHours = null,
            nextTriggerMillis = now + 3_600_000L,
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

        notificationHelper.showReminderNotification(
            reminder = reminder,
            soundEnabled = true,
            vibrateEnabled = true,
            snoozeMinutes = 10
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(nm)
        assertEquals(1, shadowNm.allNotifications.size)
        val posted = shadowNm.allNotifications.first()
        assertEquals(3, posted.actions.size)
        assertEquals("Complete", posted.actions[0].title.toString())
        assertEquals("Snooze 10m", posted.actions[1].title.toString())
        assertEquals("Dismiss", posted.actions[2].title.toString())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `task and habit reminder times persist exact hour and minute without resetting`() = runTest {
        val mainDispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(mainDispatcher)
        try {
            val categoryRepo = CategoryRepositoryImpl(db.categoryDao())
            val taskRepo = TaskRepositoryImpl(db, db.taskDao(), reminderRepository)
            val habitRepo = HabitRepositoryImpl(db.habitDao(), db.habitLogDao(), db.categoryDao())

            val tasksVm = TasksViewModel(taskRepo, categoryRepo, reminderRepository)
            val habitsVm = HabitsViewModel(habitRepo, categoryRepo, reminderRepository)

            tasksVm.saveTask(
                existing = null,
                title = "Night prep",
                description = "",
                priority = TaskPriority.MEDIUM,
                categoryId = null,
                dueEpochDay = java.time.LocalDate.now().plusDays(1).toEpochDay(),
                dueTimeMinutes = null,
                recurrence = TaskRecurrence.NONE,
                recurrenceWeekdays = emptySet(),
                hasReminder = true,
                reminderMinutes = 21 * 60 + 45
            )

            habitsVm.saveHabit(
                existing = null,
                title = "Morning walk",
                description = "",
                categoryId = null,
                colorHex = 0xFF8E7CFFL,
                scheduleType = HabitScheduleType.DAILY,
                selectedWeekdays = emptySet(),
                weeklyTarget = 7,
                reminderTimeMinutes = 6 * 60 + 15
            )

            var reminders = reminderRepository.getAllReminders()
            var attempts = 0
            while (reminders.size < 2 && attempts < 30) {
                Thread.sleep(50)
                reminders = reminderRepository.getAllReminders()
                attempts++
            }

            val taskRem = reminders.first { it.targetType == ReminderTargetType.TASK }
            val habitRem = reminders.first { it.targetType == ReminderTargetType.HABIT }

            assertEquals(21, taskRem.hour)
            assertEquals(45, taskRem.minute)
            assertEquals(6, habitRem.hour)
            assertEquals(15, habitRem.minute)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `reminder notification attaches chime sound and selects tone-specific channel`() {
        val now = System.currentTimeMillis()
        val reminder = ReminderItem(
            id = "rem_sound",
            title = "Drink Water",
            notes = "With chime",
            scheduleType = ReminderScheduleType.DAILY,
            hour = 11,
            minute = 0,
            oneTimeEpochDay = null,
            weekdays = emptySet(),
            dayOfMonth = null,
            intervalHours = null,
            nextTriggerMillis = now + 60_000L,
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

        assertEquals(NotificationChannels.REMINDERS_CHIME, NotificationChannels.channelIdFor(true, ReminderTone.CHIME))
        assertEquals(NotificationChannels.REMINDERS_ALARM, NotificationChannels.channelIdFor(true, ReminderTone.ALARM_RINGTONE))
        assertEquals(NotificationChannels.REMINDERS_SILENT, NotificationChannels.channelIdFor(false, ReminderTone.CHIME))
        val expectedUri = notificationHelper.soundUriFor(ReminderTone.CHIME)
        assertTrue(expectedUri.toString().contains(com.myday.app.R.raw.myday_reminder_tone.toString()))

        notificationHelper.showReminderNotification(
            reminder = reminder,
            soundEnabled = true,
            vibrateEnabled = true,
            snoozeMinutes = 10,
            tone = ReminderTone.CHIME
        )

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val posted = Shadows.shadowOf(nm).allNotifications.first()
        assertEquals(NotificationChannels.REMINDERS_CHIME, posted.channelId)
        assertEquals(expectedUri, nm.getNotificationChannel(NotificationChannels.REMINDERS_CHIME)?.sound)
    }
}
