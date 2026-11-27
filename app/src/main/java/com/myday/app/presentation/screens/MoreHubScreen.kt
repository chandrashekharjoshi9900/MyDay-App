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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.myday.app.presentation.components.MyDayCard
import com.myday.app.presentation.components.MyDayStatusDot
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
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                Text(
                    text = "Workspace",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Goals, focus sessions, journal, reminders, and offline backups",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MyDayThemeExtras.colors.mutedText
                )
            }
        }

        item {
            MoreDestinationCard(
                icon = Icons.Rounded.Flag,
                title = "Goals & Milestones",
                subtitle = "Target dates, milestone checklists, and progress tracking",
                onClick = onOpenGoals
            )
        }

        item {
            MoreDestinationCard(
                icon = Icons.Rounded.Timer,
                title = "Focus Timer",
                subtitle = "Pomodoro, breaks, custom durations, and session logs",
                onClick = onOpenFocus
            )
        }

        item {
            MoreDestinationCard(
                icon = Icons.Rounded.AutoStories,
                title = "Daily Journal",
                subtitle = "Achievements, notes, mood, and daily reflections",
                onClick = onOpenJournal
            )
        }

        item {
            MoreDestinationCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "Reminders & Diagnostics",
                subtitle = "Looping alarms, snooze, exact alarm status, and ringtone test",
                onClick = onOpenReminders
            )
        }

        item {
            MoreDestinationCard(
                icon = Icons.Rounded.Settings,
                title = "Settings & Backup",
                subtitle = "Theme, ringtone, haptics, and validated JSON export/restore",
                onClick = onOpenSettings
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            MyDayCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MyDayThemeExtras.colors.mutedText,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline & local only",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "No internet permission, no accounts, and no cloud sync",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MyDayThemeExtras.colors.mutedText
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MyDayStatusDot(color = MyDayThemeExtras.colors.success)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Local",
                            style = MaterialTheme.typography.labelMedium,
                            color = MyDayThemeExtras.colors.success
                        )
                    }
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
    onClick: () -> Unit
) {
    MyDayCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MyDayThemeExtras.colors.mutedText
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MyDayThemeExtras.colors.mutedText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
