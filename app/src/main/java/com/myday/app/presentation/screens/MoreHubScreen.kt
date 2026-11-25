package com.myday.app.presentation.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.myday.app.presentation.components.MyDayCard
import com.myday.app.presentation.components.StatusPill
import com.myday.app.presentation.theme.MyDayPalette
import com.myday.app.presentation.theme.MyDayThemeExtras

@Composable
fun MoreHubScreen(
    onOpenGoals: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Workspace & Tools",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Goals, focus sessions, daily journal, background reminders, and offline backups.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            MoreDestinationCard(
                icon = Icons.Rounded.Flag,
                title = "Goals & Milestones",
                subtitle = "Set target dates, break down milestones, and track completion %",
                accent = MaterialTheme.colorScheme.primary,
                onClick = onOpenGoals
            )
        }

        item {
            MoreDestinationCard(
                icon = Icons.Rounded.Timer,
                title = "Focus Timer",
                subtitle = "Pomodoro 25m, short/long breaks, custom durations & session logs",
                accent = MyDayThemeExtras.colors.success,
                onClick = onOpenFocus
            )
        }

        item {
            MoreDestinationCard(
                icon = Icons.Rounded.AutoStories,
                title = "Daily Journal",
                subtitle = "Achievements, personal notes, and daily reflections",
                accent = Color(0xFFE879F9),
                onClick = onOpenJournal
            )
        }

        item {
            MoreDestinationCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "Reminders & Diagnostics",
                subtitle = "Background alarms, snooze, exact alarm & permission health",
                accent = MyDayPalette.AmberWarning,
                onClick = onOpenReminders
            )
        }

        item {
            MoreDestinationCard(
                icon = Icons.Rounded.Settings,
                title = "Settings & JSON Backup",
                subtitle = "Themes, haptics, notification preferences, offline export & restore",
                accent = MaterialTheme.colorScheme.secondary,
                onClick = onOpenSettings
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            MyDayCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MyDayThemeExtras.colors.success
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "100% Offline & Private",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "MYDAY runs without internet permission, cloud sync, or accounts. Your data stays on your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        text = "Local",
                        containerColor = MyDayThemeExtras.colors.successContainer,
                        contentColor = MyDayThemeExtras.colors.success
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreDestinationCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    MyDayCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
