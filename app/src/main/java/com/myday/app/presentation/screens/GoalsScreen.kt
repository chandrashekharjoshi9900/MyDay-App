package com.myday.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.myday.app.domain.model.CategoryItem
import com.myday.app.domain.model.GoalItem
import com.myday.app.domain.model.GoalStatus
import com.myday.app.domain.model.GoalWithMilestones
import com.myday.app.presentation.components.CategorySelectorRow
import com.myday.app.presentation.components.ColorPickerRow
import com.myday.app.presentation.components.EmptyStateCard
import com.myday.app.presentation.components.LoadingStateView
import com.myday.app.presentation.components.MyDayCard
import com.myday.app.presentation.components.MyDayDatePickerDialog
import com.myday.app.presentation.components.StatusPill
import com.myday.app.presentation.components.formatEpochDay
import com.myday.app.presentation.theme.MyDayPalette
import com.myday.app.presentation.theme.MyDayThemeExtras
import com.myday.app.presentation.theme.rememberMyDayHaptic
import com.myday.app.presentation.viewmodel.GoalsUiState
import java.time.LocalDate
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    state: GoalsUiState,
    onBack: () -> Unit,
    onSelectFilter: (GoalStatus?) -> Unit,
    onSaveGoal: (
        existing: GoalItem?,
        title: String,
        description: String,
        categoryId: String?,
        colorHex: Long,
        targetEpochDay: Long?,
        manualProgressPercent: Int
    ) -> Unit,
    onUpdateStatus: (String, GoalStatus) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onAddMilestone: (String, String, Int) -> Unit,
    onToggleMilestone: (String) -> Unit,
    onDeleteMilestone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberMyDayHaptic()
    var showEditor by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalItem?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Goals & Milestones") },
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
                    editingGoal = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "New Goal")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.selectedFilter == null,
                    onClick = { onSelectFilter(null) },
                    label = { Text("All (${state.activeCount + state.completedCount}+)") }
                )
                GoalStatus.entries.forEach { status ->
                    FilterChip(
                        selected = state.selectedFilter == status,
                        onClick = { onSelectFilter(status) },
                        label = { Text(status.label) }
                    )
                }
            }

            if (state.isLoading) {
                LoadingStateView()
            } else if (state.goals.isEmpty()) {
                Box(modifier = Modifier.padding(16.dp)) {
                    EmptyStateCard(
                        icon = Icons.Rounded.Flag,
                        title = "Define your next milestone",
                        message = "Create personal goals with target dates and track automatic completion % as milestones finish.",
                        actionLabel = "Create Goal",
                        onAction = { editingGoal = null; showEditor = true }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.goals, key = { it.goal.id }) { item ->
                        GoalCard(
                            item = item,
                            onEdit = {
                                editingGoal = item.goal
                                showEditor = true
                            },
                            onUpdateStatus = { status -> onUpdateStatus(item.goal.id, status) },
                            onDelete = { onDeleteGoal(item.goal.id) },
                            onAddMilestone = { title ->
                                onAddMilestone(item.goal.id, title, item.milestones.size)
                            },
                            onToggleMilestone = { mId ->
                                haptic()
                                onToggleMilestone(mId)
                            },
                            onDeleteMilestone = onDeleteMilestone
                        )
                    }
                }
            }
        }
    }

    if (showEditor) {
        GoalEditorDialog(
            existing = editingGoal,
            categories = state.categories,
            onDismiss = { showEditor = false },
            onConfirm = { title, desc, catId, colorHex, targetDay, manualPct ->
                onSaveGoal(editingGoal, title, desc, catId, colorHex, targetDay, manualPct)
                showEditor = false
            }
        )
    }
}

@Composable
private fun GoalCard(
    item: GoalWithMilestones,
    onEdit: () -> Unit,
    onUpdateStatus: (GoalStatus) -> Unit,
    onDelete: () -> Unit,
    onAddMilestone: (String) -> Unit,
    onToggleMilestone: (String) -> Unit,
    onDeleteMilestone: (String) -> Unit
) {
    val goal = item.goal
    val accent = Color(goal.colorHex)
    var newMilestoneText by remember { mutableStateOf("") }

    MyDayCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val (bg, fg) = when (goal.status) {
                        GoalStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
                        GoalStatus.PAUSED -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
                        GoalStatus.COMPLETED -> MyDayThemeExtras.colors.successContainer to MyDayThemeExtras.colors.success
                    }
                    StatusPill(text = goal.status.label, containerColor = bg, contentColor = fg)
                }
                val meta = buildString {
                    item.category?.let { append(it.name) }
                    goal.targetEpochDay?.let {
                        if (isNotEmpty()) append(" • ")
                        append("Target: ${formatEpochDay(it)}")
                    }
                }
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Goal", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete Goal",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (goal.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = goal.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (item.milestones.isEmpty()) "Manual Progress" else "${item.milestones.count { it.isCompleted }}/${item.milestones.size} milestones",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${item.progressPercent}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (item.progressPercent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (goal.status == GoalStatus.COMPLETED) MyDayThemeExtras.colors.success else accent,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Milestones list
        if (item.milestones.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item.milestones.forEach { milestone ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleMilestone(milestone.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (milestone.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (milestone.isCompleted) MyDayThemeExtras.colors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = milestone.title,
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (milestone.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onDeleteMilestone(milestone.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Remove milestone",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Add Milestone inline input
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newMilestoneText,
                onValueChange = { newMilestoneText = it },
                placeholder = { Text("Add milestone...") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = {
                    if (newMilestoneText.isNotBlank()) {
                        onAddMilestone(newMilestoneText)
                        newMilestoneText = ""
                    }
                },
                enabled = newMilestoneText.isNotBlank()
            ) {
                Text("Add")
            }
        }

        // Goal lifecycle actions: Pause / Resume / Complete
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (goal.status == GoalStatus.ACTIVE) {
                OutlinedButton(onClick = { onUpdateStatus(GoalStatus.PAUSED) }) {
                    Icon(Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pause")
                }
                Button(onClick = { onUpdateStatus(GoalStatus.COMPLETED) }) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Complete")
                }
            } else {
                Button(onClick = { onUpdateStatus(GoalStatus.ACTIVE) }) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resume")
                }
            }
        }
    }
}

@Composable
private fun GoalEditorDialog(
    existing: GoalItem?,
    categories: List<CategoryItem>,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        categoryId: String?,
        colorHex: Long,
        targetEpochDay: Long?,
        manualProgressPercent: Int
    ) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var categoryId by remember { mutableStateOf(existing?.categoryId) }
    var colorHex by remember { mutableLongStateOf(existing?.colorHex ?: MyDayPalette.AccentSwatches.first()) }
    var targetEpochDay by remember { mutableStateOf(existing?.targetEpochDay ?: LocalDate.now().plusDays(30).toEpochDay()) }
    var manualProgress by remember { mutableIntStateOf(existing?.manualProgressPercent ?: 0) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Goal" else "Edit Goal") },
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
                    label = { Text("Goal Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Why this matters (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                CategorySelectorRow(
                    categories = categories,
                    selectedCategoryId = categoryId,
                    onSelectCategory = { categoryId = it }
                )
                Text("Accent Color", style = MaterialTheme.typography.labelLarge)
                ColorPickerRow(
                    selectedColorHex = colorHex,
                    onSelectColor = { colorHex = it }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Target Date", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(onClick = { showDatePicker = true }) {
                        Text(targetEpochDay?.let { formatEpochDay(it) } ?: "Set Target")
                    }
                }
                Column {
                    Text("Manual Progress (if no milestones): $manualProgress%", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = manualProgress.toFloat(),
                        onValueChange = { manualProgress = it.roundToInt() },
                        valueRange = 0f..100f
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(title, description, categoryId, colorHex, targetEpochDay, manualProgress)
                },
                enabled = title.isNotBlank()
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
            initialEpochDay = targetEpochDay,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                targetEpochDay = it
                showDatePicker = false
            }
        )
    }
}
