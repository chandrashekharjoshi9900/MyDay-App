package com.myday.app.data.local.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.myday.app.domain.model.AppSettingsModel

@Database(
    entities = [
        CategoryEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        TaskEntity::class,
        ReminderEntity::class,
        GoalEntity::class,
        MilestoneEntity::class,
        FocusSessionEntity::class,
        JournalEntryEntity::class,
        AppSettingsEntity::class
    ],
    version = 3,
    exportSchema = true
)
abstract class MyDayDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun goalDao(): GoalDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun journalEntryDao(): JournalEntryDao
    abstract fun appSettingsDao(): AppSettingsDao

    suspend fun replaceAllDataAtomically(
        categories: List<CategoryEntity>,
        habits: List<HabitEntity>,
        habitLogs: List<HabitLogEntity>,
        tasks: List<TaskEntity>,
        reminders: List<ReminderEntity>,
        goals: List<GoalEntity>,
        milestones: List<MilestoneEntity>,
        focusSessions: List<FocusSessionEntity>,
        journalEntries: List<JournalEntryEntity>,
        appSettings: AppSettingsEntity
    ) {
        withTransaction {
            milestoneDao().clearAll()
            habitLogDao().clearAll()
            focusSessionDao().clearAll()
            journalEntryDao().clearAll()
            reminderDao().clearAll()
            taskDao().clearAll()
            habitDao().clearAll()
            goalDao().clearAll()
            categoryDao().clearAll()
            appSettingsDao().clearAll()

            if (categories.isNotEmpty()) categoryDao().upsertAll(categories)
            if (habits.isNotEmpty()) habitDao().upsertAll(habits)
            if (habitLogs.isNotEmpty()) habitLogDao().upsertAll(habitLogs)
            if (tasks.isNotEmpty()) taskDao().upsertAll(tasks)
            if (reminders.isNotEmpty()) reminderDao().upsertAll(reminders)
            if (goals.isNotEmpty()) goalDao().upsertAll(goals)
            if (milestones.isNotEmpty()) milestoneDao().upsertAll(milestones)
            if (focusSessions.isNotEmpty()) focusSessionDao().upsertAll(focusSessions)
            if (journalEntries.isNotEmpty()) journalEntryDao().upsertAll(journalEntries)
            appSettingsDao().upsert(appSettings)
        }
    }

    companion object {
        const val DB_NAME = "myday_database.db"

        /**
         * Schema migration from v1 to v2:
         * - Adds completionCount column on tasks to preserve recurring completion history counts
         * - Adds note column on focus_sessions
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN completionCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN note TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN reminderTone TEXT NOT NULL DEFAULT 'CHIME'")
            }
        }

        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity("cat_health", "Health", 0xFF2FD180L, "favorite", true),
            CategoryEntity("cat_work", "Work", 0xFF5298FFL, "work", true),
            CategoryEntity("cat_learning", "Learning", 0xFF8E7CFFL, "school", true),
            CategoryEntity("cat_mindfulness", "Mindfulness", 0xFF38BDF8L, "self_improvement", true),
            CategoryEntity("cat_personal", "Personal", 0xFFF59E0BL, "person", true)
        )

        fun build(context: Context): MyDayDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MyDayDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedDefaultsSync(db)
                    }
                })
                .build()
        }

        fun seedDefaultsSync(db: SupportSQLiteDatabase) {
            DEFAULT_CATEGORIES.forEach { cat ->
                val cv = ContentValues().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("colorHex", cat.colorHex)
                    put("iconName", cat.iconName)
                    put("isDefault", if (cat.isDefault) 1 else 0)
                }
                db.insert("categories", SQLiteDatabase.CONFLICT_IGNORE, cv)
            }
            val defaults = AppSettingsModel()
            val settingsCv = ContentValues().apply {
                put("id", "default")
                put("userName", defaults.userName)
                put("themeMode", defaults.themeMode.name)
                put("notificationsEnabled", if (defaults.notificationsEnabled) 1 else 0)
                put("notificationSoundEnabled", if (defaults.notificationSoundEnabled) 1 else 0)
                put("reminderTone", defaults.reminderTone.name)
                put("vibrationEnabled", if (defaults.vibrationEnabled) 1 else 0)
                put("defaultSnoozeMinutes", defaults.defaultSnoozeMinutes)
                put("hapticFeedbackEnabled", if (defaults.hapticFeedbackEnabled) 1 else 0)
                put("defaultPomodoroMinutes", defaults.defaultPomodoroMinutes)
                put("shortBreakMinutes", defaults.shortBreakMinutes)
                put("longBreakMinutes", defaults.longBreakMinutes)
                putNull("lastBackupMillis")
            }
            db.insert("app_settings", SQLiteDatabase.CONFLICT_IGNORE, settingsCv)
        }

        suspend fun seedDefaults(db: MyDayDatabase) {
            if (db.categoryDao().getAll().isEmpty()) {
                db.categoryDao().upsertAll(DEFAULT_CATEGORIES)
            }
            if (db.appSettingsDao().get() == null) {
                db.appSettingsDao().upsert(AppSettingsModel().toEntity())
            }
        }
    }
}
