# MYDAY — Personal Productivity Android Application

> **100% offline, native Android productivity suite** built with Kotlin, Jetpack Compose, Material 3, Room SQLite, DataStore Preferences, and AlarmManager.

## Project Description

**MYDAY** helps you organize daily life in one cohesive, privacy-first Android app without internet access, accounts, or cloud tracking. Designed around a modern dark charcoal interface with soft violet, blue, and green accents, MYDAY brings together:

- **Habit Tracking** with flexible schedules, streaks, pause/archive support, and immutable completion logs
- **Task Management** with priorities, due dates, recurring schedules, and completion history
- **Background Reminders** powered by `AlarmManager` & `BroadcastReceiver` (snooze, complete, dismiss, reboot recovery, and notification diagnostics)
- **Goals & Milestones** with automatic progress calculation
- **Progress Analytics** featuring Daily/Weekly/Monthly reports and a 35-day calendar heatmap
- **Focus Timer** with Pomodoro, Short/Long Break, custom durations, and task linking
- **Daily Journal** for achievements, personal notes, mood tracking, and daily reflections
- **Offline JSON Backup & Restore** via Android Storage Access Framework with SHA-256 checksum validation

---

## Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Language & UI** | Kotlin, Jetpack Compose, Material 3, Material Icons Extended, Navigation Compose |
| **Architecture** | Clean Architecture (`domain` / `data` / `presentation`), MVVM, Repository Pattern |
| **Local Storage** | Room Database (SQLite, 10 entities, migrations, `@Transaction` & `@Upsert`), DataStore Preferences |
| **Concurrency** | Kotlin Coroutines & Flow |
| **Background Alarms** | Android `AlarmManager`, `BroadcastReceiver`, `NotificationManager` |
| **Backup & Restore** | Kotlinx Serialization JSON, SHA-256 validation, Android Storage Access Framework (SAF) |
| **Testing** | JUnit 4, Kotlinx Coroutines Test, Robolectric, Room SQLite migration & DAO tests |

---

## Core Design & Privacy Principles

- **Completely Offline**: Zero `INTERNET` permission, no cloud databases, no Firebase/Supabase, no remote APIs, and no user accounts.
- **Local Persistence**: All user data lives locally in Room SQLite (`myday_database.db`) and Preferences DataStore (`myday_preferences`).
- **Dark Mode Default**: Charcoal surfaces (`#0E1015` / `#161922`), soft violet primary accent (`#8E7CFF`), blue secondary accent (`#5298FF`), and green success indicators (`#2FD180`), with full Light Mode support.
- **Clean Architecture + MVVM**: Strict separation between `domain` (models, repository interfaces, pure calculators), `data` (Room DAOs/entities/migrations, repositories, AlarmManager receivers, SAF JSON backup), and `presentation` (Compose UI, Material 3 theme, ViewModels).

---

## Implemented Modules

1. **Dashboard (`Home`)**
   - Time-of-day personalized greeting and current date
   - Daily productivity completion ring
   - Today's scheduled habits with one-tap completion
   - Pending and completed tasks overview
   - Upcoming scheduled reminders
   - Active habit streak and today's focus duration
   - 7-day weekly progress bar chart
   - Quick-add shortcuts

2. **Habit Tracker (`Habits`)**
   - Create, edit, pause, resume, archive, unarchive, and delete habits
   - Daily, selected weekdays, and weekly frequency target schedules
   - Categories, accent colors, and optional daily reminder sync
   - Historical completion tracking (date picker) and 14-day interactive history sheet
   - Current & longest streak calculation + recent missed day tracking
   - Preserves all historical `HabitLog` records when editing or archiving habits

3. **Task Manager (`Tasks`)**
   - Create, edit, and delete tasks
   - High, Medium, and Low priority levels
   - Due dates, due times, and optional synchronized background reminders
   - Recurring tasks (`One-time`, `Daily`, `Weekly`, `Monthly`, `Custom days`)
   - Pending, Completed, and History views with completion timestamps

4. **Background Reminder System (`More -> Reminders & Diagnostics`)**
   - Built on Android `AlarmManager` + `BroadcastReceiver` + `NotificationManager` (not WorkManager)
   - Supports One-time, Daily, Weekly, Custom weekdays, Monthly, and Custom interval schedules
   - Notification actions for **Complete**, **Snooze**, and **Dismiss**
   - Reschedules alarms after `BOOT_COMPLETED`, `LOCKED_BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`, and `DATE_CHANGED`
   - Duplicate alarm prevention via deterministic `PendingIntent` request codes
   - Android 13+ `POST_NOTIFICATIONS` permission handling and Android 12+ `SCHEDULE_EXACT_ALARM` check with inexact `setAndAllowWhileIdle` fallback
   - Built-in Notification Diagnostics screen and test notification trigger

5. **Goal Tracker (`More -> Goals & Milestones`)**
   - Personal goals with categories, accent colors, and target dates
   - Milestone checklist with automatic completion percentage calculation (or manual progress slider)
   - Pause, resume, and complete goal lifecycle

6. **Progress Analytics (`Progress`)**
   - Switchable Daily, Weekly, and Monthly reports
   - Habit completion rate, task completion & overdue stats, streaks, and focus time totals
   - 35-day calendar activity heatmap and trend charts
   - Transparent calculations that exclude rest/unscheduled days from penalty ratios

7. **Focus Timer (`More -> Focus Timer`)**
   - 25-minute Pomodoro, 5-minute Short Break, 15-minute Long Break, and Custom duration (5–120 min)
   - Optional task linking, live circular countdown, daily & all-time focus statistics, and session history

8. **Daily Journal (`More -> Daily Journal`)**
   - Daily notes, achievements, reflections, mood rating (1–5), date selection, search, edit, and delete

9. **Settings & Offline JSON Backup (`More -> Settings & Backup`)**
   - Dark / Light theme toggle, display name, haptic feedback toggle, and notification preferences
   - Storage Access Framework (`CreateDocument` / `OpenDocument`) JSON export & restore
   - SHA-256 checksum & structural validation with restore preview confirmation dialog

---

## Project Structure

```text
app/src/main/java/com/myday/app/
├── MyDayApplication.kt
├── MainActivity.kt
├── di/AppContainer.kt
├── domain/
│   ├── model/Models.kt
│   ├── repository/Repositories.kt
│   └── calculator/
│       ├── StreakCalculator.kt
│       ├── GoalProgressCalculator.kt
│       ├── ReminderScheduleCalculator.kt
│       └── AnalyticsCalculator.kt
├── data/
│   ├── local/
│   │   ├── db/{Entities.kt, Daos.kt, Mappers.kt, MyDayDatabase.kt}
│   │   └── datastore/UserPreferencesDataStore.kt
│   ├── reminder/
│   │   ├── NotificationHelper.kt
│   │   ├── ReminderAlarmScheduler.kt
│   │   ├── ReminderAlarmReceiver.kt
│   │   ├── ReminderActionReceiver.kt
│   │   ├── ReminderRescheduleReceiver.kt
│   │   └── NotificationDiagnostics.kt
│   ├── repository/RepositoryImpls.kt
│   └── backup/BackupRepositoryImpl.kt
└── presentation/
    ├── theme/Theme.kt
    ├── navigation/MyDayDestinations.kt
    ├── components/CommonComponents.kt
    ├── viewmodel/
    ├── screens/
    └── MyDayApp.kt
```

---

## Setup & Build Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat or newer (JDK 17)
- Android SDK Platform 35 (`compileSdk = 35`, `minSdk = 26`, `targetSdk = 35`)

### Build & Test Commands

```powershell
# Run all unit, Room database, migration, and Robolectric notification tests
.\gradlew.bat testDebugUnitTest

# Build Debug APK
.\gradlew.bat assembleDebug

# Build Release APK (unsigned unless keystore.properties is configured)
.\gradlew.bat assembleRelease
```

### Signed Release APK Configuration
Create a `keystore.properties` file in the root directory (do not commit secrets):

```properties
storeFile=path/to/myday-release.jks
storePassword=yourStorePassword
keyAlias=myday
keyPassword=yourKeyPassword
```

Then run:
```powershell
.\gradlew.bat assembleRelease
```

Generated APKs are placed under `app/build/outputs/apk/debug/` and `app/build/outputs/apk/release/`.
