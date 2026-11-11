package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Autopilot : Screen("autopilot", "Autopilot", Icons.Default.Terminal)
    object Tools : Screen("tools", "Tools", Icons.Default.Build)
    object Memory : Screen("memory", "Memory", Icons.Default.Psychology)
    object Scheduler : Screen("scheduler", "Schedule", Icons.Default.DateRange)
    object Security : Screen("security", "Security", Icons.Default.Security)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val navigationItems = listOf(
    Screen.Autopilot,
    Screen.Tools,
    Screen.Memory,
    Screen.Scheduler,
    Screen.Security,
    Screen.Settings
)
