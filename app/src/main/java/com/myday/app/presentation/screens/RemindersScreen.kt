package com.myday.app.presentation.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.myday.app.data.reminder.NotificationDiagnostics
import com.myday.app.domain.model.ReminderItem
import com.myday.app.domain.model.ReminderScheduleType
import com.myday.app.domain.model.sortedWeekdaysLabel
import com.myday.app.presentation.components.EmptyStateCard
import com.myday.app.presentation.components.MyDayCard
import com.myday.app.presentation.components.MyDayDatePickerDialog
import com.myday.app.presentation.components.MyDayTimePickerDialog
import com.myday.app.presentation.components.SectionHeader
import com.myday.app.presentation.components.StatusPill
import com.myday.app.presentation.components.WeekdaySelectorRow
import com.myday.app.presentation.components.formatEpochDay
import com.myday.app.presentation.components.formatTimeMinutes
import com.myday.app.presentation.theme.MyDayPalette
import com.myday.app.presentation.theme.MyDayThemeExtras
import com.myday.app.presentation.viewmodel.RemindersUiState
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    state: RemindersUiState,
    diagnosticsHelper: NotificationDiagnostics,
    onBack: () -> Unit,
    onRefreshDiagnostics: () -> Unit,
    onSendTestNotification: () -> Unit,
    onRescheduleAll: () -> Unit,
    onSaveReminder: (
        existing: ReminderItem?,
        title: String,
        notes: String,
        scheduleType: ReminderScheduleType,
        hour: Int,
        minute: Int,
        oneTimeEpochDay: Long?,
        weekdays: Set<Int>,
        dayOfMonth: Int?,
        intervalHours: Int?,
        playSound: Boolean,
        vibrate: Boolean
    ) -> Unit,
    onToggleEnabled: (String, Boolean) -> Unit,
    onSnooze: (String, Int) -> Unit,
    onMarkCompleted: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderItem?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshDiagnostics()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshDiagnostics()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Reminders & Diagnostics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onRescheduleAll) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Reschedule Alarms")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingReminder = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New Reminder")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Notification Diagnostics & Permission Guidance
            item {
                val diag = state.diagnostics
                MyDayCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (diag?.isFullyHealthy == true) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                                contentDescription = null,
                                tint = if (diag?.isFullyHealthy == true) MyDayThemeExtras.colors.success else MyDayPalette.AmberWarning
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Alarm & Notification Health",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        StatusPill(
                            text = if (diag?.isFullyHealthy == true) "Healthy" else "Attention",
                            containerColor = if (diag?.isFullyHealthy == true) {
                                MyDayThemeExtras.colors.successContainer
                            } else {
                                MyDayPalette.AmberWarning.copy(alpha = 0.2f)
                            },
                            contentColor = if (diag?.isFullyHealthy == true) {
                                MyDayThemeExtras.colors.success
                            } else {
                                MyDayPalette.AmberWarning
                            }
                        )
                    }

                    if (diag != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        DiagnosticLine("Notification Permission (Android 13+)", diag.postNotificationsGranted)
                        DiagnosticLine("System Notifications Enabled", diag.notificationsEnabledInSystem)
                        DiagnosticLine("Reminder Channel Active", diag.reminderChannelEnabled)
                        DiagnosticLine(
                            label = if (diag.exactAlarmsAllowed) {
                                "Exact Alarm Access (Precise)"
                            } else {
                                "Exact Alarm Off (Using Inexact Fallback)"
                            },
                            ok = diag.exactAlarmsAllowed
                        )
                        DiagnosticLine("Battery Optimization Exempt", diag.ignoringBatteryOptimizations)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Active alarms: ${diag.activeScheduledRemindersCount}" +
                                (diag.nextAlarmMillis?.let {
                                    " • Next: " + Instant.ofEpochMilli(it)
                                        .atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))
                                } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!diag.postNotificationsGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Button(onClick = {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }) {
                                    Text("Allow Notifications")
                                }
                            }
                            if (!diag.exactAlarmsAllowed) {
                                val exactIntent = diagnosticsHelper.buildExactAlarmSettingsIntent()
                                if (exactIntent != null) {
                                    OutlinedButton(onClick = {
                                        runCatching { context.startActivity(exactIntent) }
                                    }) {
                                        Text("Enable Exact Alarms")
                                    }
                                }
                            }
                            if (!diag.notificationsEnabledInSystem || !diag.reminderChannelEnabled) {
                                OutlinedButton(onClick = {
                                    runCatching {
                                        context.startActivity(diagnosticsHelper.buildAppNotificationSettingsIntent())
                                    }
                                }) {
                                    Text("Notification Settings")
                                }
                            }
                            if (!diag.ignoringBatteryOptimizations) {
                                OutlinedButton(onClick = {
                                    runCatching {
                                        context.startActivity(diagnosticsHelper.buildBatteryOptimizationSettingsIntent())
                                    }
                                }) {
                                    Text("Battery Settings")
                                }
                            }
                            OutlinedButton(onClick = onSendTestNotification) {
                                Text("Send Test Notification")
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Scheduled Reminders",
                    subtitle = "${state.reminders.size} total • Survives reboot & timezone changes"
                )
            }

            if (state.reminders.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "No reminders yet",
                        message = "Tap + to schedule one-time, daily, weekly, monthly, or custom interval reminders.",
                        actionLabel = "Create Reminder",
                        onAction = { editingReminder = null; showEditor = true }
                    )
                }
            } else {
                items(state.reminders, key = { it.id }) { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        defaultSnoozeMinutes = state.defaultSnoozeMinutes,
                        onToggleEnabled = { enabled -> onToggleEnabled(reminder.id, enabled) },
                        onSnooze = { onSnooze(reminder.id, state.defaultSnoozeMinutes) },
                        onComplete = { onMarkCompleted(reminder.id) },
                        onEdit = {
                            editingReminder = reminder
                            showEditor = true
                        },
                        onDelete = { onDelete(reminder.id) }
                    )
                }
            }
        }
    }

    if (showEditor) {
        ReminderEditorDialog(
            existing = editingReminder,
            onDismiss = { showEditor = false },
            onConfirm = { title, notes, type, hour, minute, epochDay, weekdays, dom, intervalH, sound, vib ->
                onSaveReminder(
                    editingReminder, title, notes, type, hour, minute, epochDay, weekdays, dom, intervalH, sound, vib
                )
                showEditor = false
            }
        )
    }
}

@Composable
private fun DiagnosticLine(label: String, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (ok) "OK" else "Check",
            style = MaterialTheme.typography.labelLarge,
            color = if (ok) MyDayThemeExtras.colors.success else MyDayPalette.AmberWarning
        )
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderItem,
    defaultSnoozeMinutes: Int,
    onToggleEnabled: (Boolean) -> Unit,
    onSnooze: () -> Unit,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val triggerAt = reminder.snoozedUntilMillis ?: reminder.nextTriggerMillis
    val formattedNext = Instant.ofEpochMilli(triggerAt)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))

    MyDayCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.title,
                    style = MaterialTheme.typography.titleMedium
                )
                if (reminder.notes.isNotBlank()) {
                    Text(
                        text = reminder.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                val scheduleDetails = when (reminder.scheduleType) {
                    ReminderScheduleType.ONE_TIME -> "One-time"
                    ReminderScheduleType.DAILY -> "Daily"
                    ReminderScheduleType.WEEKLY -> "Weekly (${reminder.weekdays.sortedWeekdaysLabel()})"
                    ReminderScheduleType.CUSTOM_WEEKDAYS -> reminder.weekdays.sortedWeekdaysLabel()
                    ReminderScheduleType.MONTHLY -> "Monthly (Day ${reminder.dayOfMonth ?: 1})"
                    ReminderScheduleType.CUSTOM_INTERVAL -> "Every ${reminder.intervalHours ?: 6} hours"
                }
                Text(
                    text = "${formatTimeMinutes(reminder.hour * 60 + reminder.minute)} • $scheduleDetails",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = when {
                        reminder.isCompleted -> "Completed"
                        !reminder.isEnabled -> "Paused / Disabled"
                        reminder.snoozedUntilMillis != null -> "Snoozed until $formattedNext"
                        else -> "Next: $formattedNext"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = reminder.isEnabled && !reminder.isCompleted,
                onCheckedChange = onToggleEnabled
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!reminder.isCompleted && reminder.isEnabled) {
                OutlinedButton(
                    onClick = onSnooze,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.Snooze, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Snooze ${defaultSnoozeMinutes}m")
                }
                OutlinedButton(
                    onClick = onComplete,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Done")
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.Edit, contentDescription = "Edit Reminder", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete Reminder",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ReminderEditorDialog(
    existing: ReminderItem?,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        notes: String,
        scheduleType: ReminderScheduleType,
        hour: Int,
        minute: Int,
        oneTimeEpochDay: Long?,
        weekdays: Set<Int>,
        dayOfMonth: Int?,
        intervalHours: Int?,
        playSound: Boolean,
        vibrate: Boolean
    ) -> Unit
) {
    val defaultTime = remember { LocalTime.now().plusMinutes(15) }
    val dialogKey = existing?.id ?: "new_reminder"
    var title by remember(dialogKey) { mutableStateOf(existing?.title.orEmpty()) }
    var notes by remember(dialogKey) { mutableStateOf(existing?.notes.orEmpty()) }
    var scheduleType by remember(dialogKey) { mutableStateOf(existing?.scheduleType ?: ReminderScheduleType.ONE_TIME) }
    var hour by remember(dialogKey) { mutableIntStateOf(existing?.hour ?: defaultTime.hour) }
    var minute by remember(dialogKey) { mutableIntStateOf(existing?.minute ?: defaultTime.minute) }
    var epochDay by remember(dialogKey) { mutableStateOf(existing?.oneTimeEpochDay ?: LocalDate.now().toEpochDay()) }
    var weekdays by remember(dialogKey) { mutableStateOf(existing?.weekdays ?: setOf(LocalDate.now().dayOfWeek.value)) }
    var dayOfMonth by remember(dialogKey) { mutableIntStateOf(existing?.dayOfMonth ?: LocalDate.now().dayOfMonth) }
    var intervalHours by remember(dialogKey) { mutableIntStateOf(existing?.intervalHours ?: 6) }
    var playSound by remember(dialogKey) { mutableStateOf(existing?.playSound ?: true) }
    var vibrate by remember(dialogKey) { mutableStateOf(existing?.vibrate ?: true) }

    var showTimePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Reminder" else "Edit Reminder") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Reminder Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Schedule Type", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ReminderScheduleType.entries.forEach { type ->
                        FilterChip(
                            selected = scheduleType == type,
                            onClick = { scheduleType = type },
                            label = { Text(type.label) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) {
                        Text(formatTimeMinutes(hour * 60 + minute))
                    }
                    if (scheduleType == ReminderScheduleType.ONE_TIME || scheduleType == ReminderScheduleType.CUSTOM_INTERVAL) {
                        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) {
                            Text(epochDay?.let { formatEpochDay(it) } ?: "Today")
                        }
                    }
                }

                if (scheduleType == ReminderScheduleType.WEEKLY || scheduleType == ReminderScheduleType.CUSTOM_WEEKDAYS) {
                    WeekdaySelectorRow(
                        selectedWeekdays = weekdays,
                        onToggleWeekday = { d ->
                            weekdays = if (scheduleType == ReminderScheduleType.WEEKLY) {
                                setOf(d)
                            } else {
                                if (weekdays.contains(d)) weekdays - d else weekdays + d
                            }
                        }
                    )
                }

                if (scheduleType == ReminderScheduleType.MONTHLY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Day of month: $dayOfMonth", fontWeight = FontWeight.Medium)
                        Row {
                            OutlinedButton(onClick = { if (dayOfMonth > 1) dayOfMonth-- }) { Text("-") }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = { if (dayOfMonth < 31) dayOfMonth++ }) { Text("+") }
                        }
                    }
                }

                if (scheduleType == ReminderScheduleType.CUSTOM_INTERVAL) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Repeat every $intervalHours hr(s)", fontWeight = FontWeight.Medium)
                        Row {
                            OutlinedButton(onClick = { if (intervalHours > 1) intervalHours-- }) { Text("-") }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = { if (intervalHours < 168) intervalHours++ }) { Text("+") }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notification Sound")
                    Switch(checked = playSound, onCheckedChange = { playSound = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Vibration")
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title, notes, scheduleType, hour, minute,
                        epochDay, weekdays, dayOfMonth, intervalHours, playSound, vibrate
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Text("Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showTimePicker) {
        MyDayTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                hour = h
                minute = m
                showTimePicker = false
            }
        )
    }

    if (showDatePicker) {
        MyDayDatePickerDialog(
            initialEpochDay = epochDay,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                epochDay = it
                showDatePicker = false
            }
        )
    }
}
