package com.myday.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Rounded.Home),
    HABITS("habits", "Habits", Icons.Rounded.Repeat),
    TASKS("tasks", "Tasks", Icons.Rounded.CheckCircle),
    PROGRESS("progress", "Progress", Icons.Rounded.AutoGraph),
    MORE("more", "More", Icons.Rounded.GridView)
}

object SubDestinations {
    const val GOALS = "goals"
    const val FOCUS = "focus"
    const val JOURNAL = "journal"
    const val REMINDERS = "reminders"
    const val SETTINGS = "settings"
}
