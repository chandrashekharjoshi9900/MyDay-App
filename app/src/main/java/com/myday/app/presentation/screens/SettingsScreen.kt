package com.myday.app.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.rounded.MusicNote
import com.myday.app.BuildConfig
import com.myday.app.domain.model.ReminderTone
import com.myday.app.domain.model.ThemeMode
import com.myday.app.presentation.components.MyDayCard
import com.myday.app.presentation.components.SectionHeader
import com.myday.app.presentation.viewmodel.SettingsUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onSetTheme: (ThemeMode) -> Unit,
    onSetUserName: (String) -> Unit,
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSetSoundEnabled: (Boolean) -> Unit,
    onSetReminderTone: (ReminderTone) -> Unit,
    onPreviewTone: (ReminderTone) -> Unit,
    onSetVibrationEnabled: (Boolean) -> Unit,
    onSetSnoozeMinutes: (Int) -> Unit,
    onSetHapticEnabled: (Boolean) -> Unit,
    onExportUriSelected: (android.content.ContentResolver, android.net.Uri) -> Unit,
    onImportUriSelected: (android.content.ContentResolver, android.net.Uri) -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var nameInput by remember(state.settings.userName) { mutableStateOf(state.settings.userName) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            onExportUriSelected(context.contentResolver, uri)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onImportUriSelected(context.contentResolver, uri)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { msg ->
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
                title = { Text("Settings & backup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isBusy) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            // Personalization & Theme
            item {
                SectionHeader(title = "Appearance & Profile")
            }

            item {
                MyDayCard {
                    Text("Display Name", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { onSetUserName(nameInput) }) {
                            Text("Save")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Theme", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(
                                selected = state.settings.themeMode == mode,
                                onClick = { onSetTheme(mode) },
                                label = { Text(mode.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    SettingSwitchRow(
                        title = "Haptic Feedback",
                        subtitle = "Subtle tactile response on check-ins and timer actions",
                        checked = state.settings.hapticFeedbackEnabled,
                        onCheckedChange = onSetHapticEnabled
                    )
                }
            }

            // Notification Preferences
            item {
                SectionHeader(title = "Notification Preferences")
            }

            item {
                MyDayCard {
                    SettingSwitchRow(
                        title = "Enable Notifications",
                        subtitle = "Master switch for reminders and focus alerts",
                        checked = state.settings.notificationsEnabled,
                        onCheckedChange = onSetNotificationsEnabled
                    )
                    SettingSwitchRow(
                        title = "Notification Sound",
                        subtitle = "Play reminder ringtone/chime when alerts fire",
                        checked = state.settings.notificationSoundEnabled,
                        onCheckedChange = onSetSoundEnabled
                    )
                    if (state.settings.notificationSoundEnabled) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Reminder Ringtone", style = MaterialTheme.typography.labelLarge)
                            TextButton(onClick = { onPreviewTone(state.settings.reminderTone) }) {
                                Icon(Icons.Rounded.MusicNote, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Preview")
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReminderTone.entries.forEach { tone ->
                                FilterChip(
                                    selected = state.settings.reminderTone == tone,
                                    onClick = { onSetReminderTone(tone) },
                                    label = { Text(tone.label) }
                                )
                            }
                        }
                    }
                    SettingSwitchRow(
                        title = "Vibration",
                        subtitle = "Vibrate when reminders fire",
                        checked = state.settings.vibrationEnabled,
                        onCheckedChange = onSetVibrationEnabled
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Default Snooze Duration", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(5, 10, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = state.settings.defaultSnoozeMinutes == mins,
                                onClick = { onSetSnoozeMinutes(mins) },
                                label = { Text("${mins}m") }
                            )
                        }
                    }
                }
            }

            // Offline JSON Backup & Restore
            item {
                SectionHeader(
                    title = "Offline Backup & Restore",
                    subtitle = "Export or import all MYDAY data as validated JSON"
                )
            }

            item {
                MyDayCard {
                    val lastBackupText = state.settings.lastBackupMillis?.let {
                        "Last backup: " + Instant.ofEpochMilli(it)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a"))
                    } ?: "No backup created on this device yet."

                    Text(
                        text = lastBackupText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val fileName = "myday_backup_${LocalDate.now()}.json"
                                exportLauncher.launch(fileName)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isBusy
                        ) {
                            Icon(Icons.Rounded.Upload, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isBusy
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Restore JSON")
                        }
                    }
                }
            }

            // App & Privacy Information
            item {
                SectionHeader(title = "App & Privacy")
            }

            item {
                MyDayCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("MYDAY v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Native offline personal productivity suite built with Kotlin, Jetpack Compose, Room SQLite, and AlarmManager.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Privacy Guarantee", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "No INTERNET permission, no analytics SDKs, no cloud accounts, and no remote servers. All habits, tasks, reminders, goals, focus logs, and journal entries remain strictly on your device.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Restore Confirmation Dialog after validation succeeds
    state.pendingRestorePreview?.let { preview ->
        val exportedAt = Instant.ofEpochMilli(preview.exportedAtMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a"))

        AlertDialog(
            onDismissRequest = onCancelRestore,
            title = { Text("Confirm Backup Restore") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Backup verified (SHA-256 checksum OK). Restoring will replace current local data with:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Exported: $exportedAt", fontWeight = FontWeight.Medium)
                    Text("• Habits: ${preview.habitsCount} (${preview.habitLogsCount} completion logs)")
                    Text("• Tasks: ${preview.tasksCount}")
                    Text("• Reminders: ${preview.remindersCount}")
                    Text("• Goals: ${preview.goalsCount} (${preview.milestonesCount} milestones)")
                    Text("• Focus sessions: ${preview.focusSessionsCount}")
                    Text("• Journal entries: ${preview.journalEntriesCount}")
                }
            },
            confirmButton = {
                Button(onClick = onConfirmRestore) {
                    Text("Restore Data")
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelRestore) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
