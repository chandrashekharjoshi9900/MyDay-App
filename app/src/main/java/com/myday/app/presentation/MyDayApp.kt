package com.myday.app.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myday.app.presentation.theme.MyDayThemeExtras
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.myday.app.di.AppContainer
import com.myday.app.domain.model.ThemeMode
import com.myday.app.presentation.navigation.SubDestinations
import com.myday.app.presentation.navigation.TopLevelDestination
import com.myday.app.presentation.screens.DashboardScreen
import com.myday.app.presentation.screens.FocusScreen
import com.myday.app.presentation.screens.GoalsScreen
import com.myday.app.presentation.screens.HabitsScreen
import com.myday.app.presentation.screens.JournalScreen
import com.myday.app.presentation.screens.MoreHubScreen
import com.myday.app.presentation.screens.ProgressScreen
import com.myday.app.presentation.screens.RemindersScreen
import com.myday.app.presentation.screens.SettingsScreen
import com.myday.app.presentation.screens.TasksScreen
import com.myday.app.presentation.theme.MyDayTheme
import com.myday.app.presentation.viewmodel.DashboardViewModel
import com.myday.app.presentation.viewmodel.FocusViewModel
import com.myday.app.presentation.viewmodel.GoalsViewModel
import com.myday.app.presentation.viewmodel.HabitsViewModel
import com.myday.app.presentation.viewmodel.JournalViewModel
import com.myday.app.presentation.viewmodel.MyDayViewModelFactory
import com.myday.app.presentation.viewmodel.ProgressViewModel
import com.myday.app.presentation.viewmodel.RemindersViewModel
import com.myday.app.presentation.viewmodel.SettingsViewModel
import com.myday.app.presentation.viewmodel.TasksViewModel

@Composable
fun MyDayApp(container: AppContainer) {
    val factory = MyDayViewModelFactory(container)
    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val settingsState by settingsVm.uiState.collectAsStateWithLifecycle()

    val isDark = settingsState.settings.themeMode == ThemeMode.DARK

    MyDayTheme(
        darkTheme = isDark,
        hapticsEnabled = settingsState.settings.hapticFeedbackEnabled
    ) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()
        val showBottomBar = currentRoute in topLevelRoutes

        fun navigateTopLevel(route: String) {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showBottomBar) {
                    Column {
                        HorizontalDivider(thickness = 1.dp, color = MyDayThemeExtras.colors.cardBorder)
                        NavigationBar(
                            containerColor = MyDayThemeExtras.colors.navBarSurface,
                            tonalElevation = 0.dp
                        ) {
                            TopLevelDestination.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(dest.route) },
                                    icon = {
                                        Icon(
                                            imageVector = dest.icon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label, style = MaterialTheme.typography.labelMedium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unselectedIconColor = MyDayThemeExtras.colors.mutedText,
                                        unselectedTextColor = MyDayThemeExtras.colors.mutedText
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = TopLevelDestination.HOME.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(TopLevelDestination.HOME.route) {
                    val vm: DashboardViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    DashboardScreen(
                        state = state,
                        onToggleHabit = vm::toggleHabitToday,
                        onToggleTask = vm::toggleTask,
                        onNavigateToHabits = { navigateTopLevel(TopLevelDestination.HABITS.route) },
                        onNavigateToTasks = { navigateTopLevel(TopLevelDestination.TASKS.route) },
                        onNavigateToProgress = { navigateTopLevel(TopLevelDestination.PROGRESS.route) },
                        onNavigateToReminders = { navController.navigate(SubDestinations.REMINDERS) },
                        onNavigateToFocus = { navController.navigate(SubDestinations.FOCUS) },
                        onNavigateToJournal = { navController.navigate(SubDestinations.JOURNAL) }
                    )
                }

                composable(TopLevelDestination.HABITS.route) {
                    val vm: HabitsViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    HabitsScreen(
                        state = state,
                        onSelectTab = vm::selectTab,
                        onSelectDate = vm::selectDate,
                        onToggleCompletion = vm::toggleCompletionForSelectedDate,
                        onToggleDayCompletion = vm::toggleCompletionForDay,
                        onSaveHabit = vm::saveHabit,
                        onSetPaused = vm::setPaused,
                        onSetArchived = vm::setArchived,
                        onDeleteHabit = vm::deleteHabit,
                        onOpenHistory = vm::openHistory
                    )
                }

                composable(TopLevelDestination.TASKS.route) {
                    val vm: TasksViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    TasksScreen(
                        state = state,
                        onSelectTab = vm::selectTab,
                        onSelectPriority = vm::selectPriorityFilter,
                        onToggleTask = vm::toggleTask,
                        onSaveTask = vm::saveTask,
                        onDeleteTask = vm::deleteTask
                    )
                }

                composable(TopLevelDestination.PROGRESS.route) {
                    val vm: ProgressViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    ProgressScreen(
                        state = state,
                        onSelectPeriod = vm::selectPeriod
                    )
                }

                composable(TopLevelDestination.MORE.route) {
                    MoreHubScreen(
                        onOpenGoals = { navController.navigate(SubDestinations.GOALS) },
                        onOpenFocus = { navController.navigate(SubDestinations.FOCUS) },
                        onOpenJournal = { navController.navigate(SubDestinations.JOURNAL) },
                        onOpenReminders = { navController.navigate(SubDestinations.REMINDERS) },
                        onOpenSettings = { navController.navigate(SubDestinations.SETTINGS) }
                    )
                }

                composable(SubDestinations.GOALS) {
                    val vm: GoalsViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    GoalsScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                        onSelectFilter = vm::selectFilter,
                        onSaveGoal = vm::saveGoal,
                        onUpdateStatus = vm::updateStatus,
                        onDeleteGoal = vm::deleteGoal,
                        onAddMilestone = vm::addMilestone,
                        onToggleMilestone = vm::toggleMilestone,
                        onDeleteMilestone = vm::deleteMilestone
                    )
                }

                composable(SubDestinations.FOCUS) {
                    val vm: FocusViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    FocusScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                        onSelectMode = vm::selectMode,
                        onSetCustomMinutes = vm::setCustomMinutes,
                        onLinkTask = vm::linkTask,
                        onStartOrResume = vm::startOrResume,
                        onPause = vm::pause,
                        onReset = vm::reset,
                        onFinishEarly = vm::finishEarly,
                        onDeleteSession = vm::deleteSession,
                        onClearBanner = vm::clearBanner
                    )
                }

                composable(SubDestinations.JOURNAL) {
                    val vm: JournalViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    JournalScreen(
                        state = state,
                        onBack = { navController.popBackStack() },
                        onSearchChange = vm::updateSearchQuery,
                        onSaveEntry = vm::saveEntry,
                        onDeleteEntry = vm::deleteEntry
                    )
                }

                composable(SubDestinations.REMINDERS) {
                    val vm: RemindersViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsStateWithLifecycle()
                    RemindersScreen(
                        state = state,
                        diagnosticsHelper = vm.notificationDiagnostics,
                        onBack = { navController.popBackStack() },
                        onRefreshDiagnostics = vm::refreshDiagnostics,
                        onSendTestNotification = vm::sendTestNotification,
                        onPreviewSound = vm::previewTone,
                        onRescheduleAll = vm::rescheduleAll,
                        onSaveReminder = vm::saveReminder,
                        onToggleEnabled = vm::toggleEnabled,
                        onSnooze = vm::snoozeReminder,
                        onMarkCompleted = vm::markCompleted,
                        onDelete = vm::deleteReminder,
                        onClearMessage = vm::clearStatusMessage
                    )
                }

                composable(SubDestinations.SETTINGS) {
                    SettingsScreen(
                        state = settingsState,
                        onBack = { navController.popBackStack() },
                        onSetTheme = settingsVm::setThemeMode,
                        onSetUserName = settingsVm::setUserName,
                        onSetNotificationsEnabled = settingsVm::setNotificationsEnabled,
                        onSetSoundEnabled = settingsVm::setNotificationSoundEnabled,
                        onSetReminderTone = settingsVm::setReminderTone,
                        onPreviewTone = settingsVm::previewReminderTone,
                        onSetVibrationEnabled = settingsVm::setVibrationEnabled,
                        onSetSnoozeMinutes = settingsVm::setDefaultSnoozeMinutes,
                        onSetHapticEnabled = settingsVm::setHapticFeedbackEnabled,
                        onExportUriSelected = settingsVm::exportBackupToUri,
                        onImportUriSelected = settingsVm::inspectBackupUri,
                        onConfirmRestore = settingsVm::confirmRestore,
                        onCancelRestore = settingsVm::cancelRestorePreview,
                        onClearMessage = settingsVm::clearMessage
                    )
                }
            }
        }
    }
}
