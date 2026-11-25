package com.myday.app.presentation.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myday.app.domain.model.FocusSessionItem
import com.myday.app.domain.model.FocusSessionType
import com.myday.app.presentation.components.CircularProgressRing
import com.myday.app.presentation.components.EmptyStateCard
import com.myday.app.presentation.components.MyDayCard
import com.myday.app.presentation.components.SectionHeader
import com.myday.app.presentation.components.StatusPill
import com.myday.app.presentation.theme.MyDayThemeExtras
import com.myday.app.presentation.theme.rememberMyDayHaptic
import com.myday.app.presentation.viewmodel.FocusUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(
    state: FocusUiState,
    onBack: () -> Unit,
    onSelectMode: (FocusSessionType) -> Unit,
    onSetCustomMinutes: (Int) -> Unit,
    onLinkTask: (String?) -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onFinishEarly: () -> Unit,
    onDeleteSession: (String) -> Unit,
    onClearBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberMyDayHaptic()
    val snackbarHostState = remember { SnackbarHostState() }
    val timer = state.timer

    LaunchedEffect(timer.statusBanner) {
        timer.statusBanner?.let {
            snackbarHostState.showSnackbar(it)
            onClearBanner()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Focus Timer") },
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FocusSessionType.entries.forEach { mode ->
                        FilterChip(
                            selected = timer.selectedType == mode,
                            onClick = { onSelectMode(mode) },
                            enabled = !timer.isRunning,
                            label = { Text(mode.label) }
                        )
                    }
                }
            }

            // Timer Card
            item {
                val progress = if (timer.totalSeconds > 0) {
                    1f - (timer.remainingSeconds.toFloat() / timer.totalSeconds.toFloat())
                } else 0f
                val mins = timer.remainingSeconds / 60
                val secs = timer.remainingSeconds % 60

                MyDayCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = timer.selectedType.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        CircularProgressRing(
                            progress = progress,
                            size = 200.dp,
                            strokeWidth = 12.dp,
                            accentColor = if (timer.selectedType == FocusSessionType.POMODORO ||
                                timer.selectedType == FocusSessionType.CUSTOM
                            ) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MyDayThemeExtras.colors.success
                            }
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "%02d:%02d".format(mins, secs),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (timer.isRunning) "Focusing..." else "Ready",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (timer.selectedType == FocusSessionType.CUSTOM && !timer.isRunning) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Custom duration: ${timer.customMinutes} min",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Slider(
                                value = timer.customMinutes.toFloat(),
                                onValueChange = { onSetCustomMinutes(it.roundToInt()) },
                                valueRange = 5f..120f
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (timer.isRunning) {
                                Button(onClick = {
                                    haptic()
                                    onPause()
                                }) {
                                    Icon(Icons.Rounded.Pause, null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause")
                                }
                                OutlinedButton(onClick = {
                                    haptic()
                                    onFinishEarly()
                                }) {
                                    Icon(Icons.Rounded.Check, null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Finish")
                                }
                            } else {
                                Button(onClick = {
                                    haptic()
                                    onStartOrResume()
                                }) {
                                    Icon(Icons.Rounded.PlayArrow, null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (timer.sessionStartedAtMillis != null) "Resume" else "Start")
                                }
                                if (timer.sessionStartedAtMillis != null || timer.remainingSeconds != timer.totalSeconds) {
                                    OutlinedButton(onClick = onReset) {
                                        Icon(Icons.Rounded.Refresh, null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Reset")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Optional Task Link
            if (state.pendingTasks.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Link to Task (optional)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = timer.linkedTaskId == null,
                                onClick = { onLinkTask(null) },
                                label = { Text("No Task") }
                            )
                            state.pendingTasks.take(8).forEach { task ->
                                FilterChip(
                                    selected = timer.linkedTaskId == task.id,
                                    onClick = { onLinkTask(task.id) },
                                    label = { Text(task.title) }
                                )
                            }
                        }
                    }
                }
            }

            // Daily Focus Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MyDayCard(modifier = Modifier.weight(1f)) {
                        Text("Today", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${state.todayFocusMinutes} min", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${state.todaySessionCount} sessions", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    MyDayCard(modifier = Modifier.weight(1f)) {
                        Text("All-Time", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${state.totalFocusMinutes} min", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${state.recentSessions.size} recent logs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Session History
            item {
                SectionHeader(
                    title = "Session History",
                    subtitle = "Recorded completed focus & break blocks"
                )
            }

            if (state.recentSessions.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Rounded.Timer,
                        title = "No focus sessions yet",
                        message = "Start a 25-minute Pomodoro or custom session to record your focus history."
                    )
                }
            } else {
                items(state.recentSessions, key = { it.id }) { session ->
                    FocusSessionRow(
                        session = session,
                        onDelete = { onDeleteSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FocusSessionRow(
    session: FocusSessionItem,
    onDelete: () -> Unit
) {
    val timeLabel = Instant.ofEpochMilli(session.startedAtMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))

    MyDayCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.type.label,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusPill(
                        text = "${(session.actualDurationSeconds / 60).coerceAtLeast(1)} min",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$timeLabel • ${session.note}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete Session",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
