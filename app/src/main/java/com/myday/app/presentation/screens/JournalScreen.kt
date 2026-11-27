package com.myday.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myday.app.domain.model.JournalEntryItem
import com.myday.app.presentation.components.EmptyStateCard
import com.myday.app.presentation.components.LoadingStateView
import com.myday.app.presentation.components.MyDayCard
import com.myday.app.presentation.components.MyDayDatePickerDialog
import com.myday.app.presentation.components.StatusPill
import com.myday.app.presentation.components.formatEpochDay
import com.myday.app.presentation.theme.MyDayThemeExtras
import com.myday.app.presentation.viewmodel.JournalUiState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    state: JournalUiState,
    onBack: () -> Unit,
    onSearchChange: (String) -> Unit,
    onSaveEntry: (
        existing: JournalEntryItem?,
        epochDay: Long,
        title: String,
        content: String,
        achievements: String,
        reflection: String,
        moodLevel: Int
    ) -> Unit,
    onDeleteEntry: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditor by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<JournalEntryItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Journal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
                    editingEntry = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New Journal Entry")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchChange,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )

            if (state.isLoading) {
                LoadingStateView()
            } else if (state.entries.isEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Rounded.AutoStories,
                        title = "Your private journal",
                        message = "Capture daily wins, personal notes, and evening reflections completely offline.",
                        actionLabel = "Write Today's Entry",
                        onAction = { editingEntry = null; showEditor = true }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.entries, key = { it.id }) { entry ->
                        JournalEntryCard(
                            entry = entry,
                            onEdit = {
                                editingEntry = entry
                                showEditor = true
                            },
                            onDelete = { onDeleteEntry(entry.id) }
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        JournalEditorDialog(
            existing = editingEntry,
            onDismiss = { showEditor = false },
            onConfirm = { epochDay, title, content, achievements, reflection, mood ->
                onSaveEntry(editingEntry, epochDay, title, content, achievements, reflection, mood)
                showEditor = false
            }
        )
    }
}

@Composable
private fun JournalEntryCard(
    entry: JournalEntryItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val moodLabels = mapOf(
        1 to "Low",
        2 to "Okay",
        3 to "Steady",
        4 to "Good",
        5 to "Great"
    )
    MyDayCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatEpochDay(entry.epochDay),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusPill(
                        text = "Mood: ${moodLabels[entry.moodLevel] ?: "Steady"}",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Entry", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete Entry",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (entry.content.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Notes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(text = entry.content, style = MaterialTheme.typography.bodyMedium)
        }
        if (entry.achievements.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Achievements", style = MaterialTheme.typography.labelLarge, color = MyDayThemeExtras.colors.success)
            Text(text = entry.achievements, style = MaterialTheme.typography.bodyMedium)
        }
        if (entry.reflection.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Daily Reflection", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(text = entry.reflection, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun JournalEditorDialog(
    existing: JournalEntryItem?,
    onDismiss: () -> Unit,
    onConfirm: (
        epochDay: Long,
        title: String,
        content: String,
        achievements: String,
        reflection: String,
        moodLevel: Int
    ) -> Unit
) {
    var epochDay by remember { mutableLongStateOf(existing?.epochDay ?: LocalDate.now().toEpochDay()) }
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var content by remember { mutableStateOf(existing?.content.orEmpty()) }
    var achievements by remember { mutableStateOf(existing?.achievements.orEmpty()) }
    var reflection by remember { mutableStateOf(existing?.reflection.orEmpty()) }
    var mood by remember { mutableIntStateOf(existing?.moodLevel ?: 4) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Journal Entry" else "Edit Journal Entry") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Date", fontWeight = FontWeight.Medium)
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text(formatEpochDay(epochDay))
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Headline / Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = achievements,
                    onValueChange = { achievements = it },
                    label = { Text("Today's Achievements") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Personal Notes") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reflection,
                    onValueChange = { reflection = it },
                    label = { Text("Daily Reflection") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Mood", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(1 to "1", 2 to "2", 3 to "3", 4 to "4", 5 to "5").forEach { (level, label) ->
                        FilterChip(
                            selected = mood == level,
                            onClick = { mood = level },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(epochDay, title, content, achievements, reflection, mood) },
                enabled = title.isNotBlank() || content.isNotBlank() || achievements.isNotBlank() || reflection.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

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
