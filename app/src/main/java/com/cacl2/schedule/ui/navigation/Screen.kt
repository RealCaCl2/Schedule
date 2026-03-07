package com.cacl2.schedule.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.cacl2.schedule.R

sealed class Screen(
    val route: String,
    val icon: ImageVector,
    val labelRes: Int
) {
    data object Home : Screen("home", Icons.Default.Home, R.string.nav_home)
    data object Schedule : Screen("schedule", Icons.Default.CalendarMonth, R.string.nav_schedule)
    data object Import : Screen("import", Icons.Default.Download, R.string.nav_import)
    data object Settings : Screen("settings", Icons.Default.Settings, R.string.nav_settings)
    data object Onboarding : Screen("onboarding", Icons.Default.Settings, R.string.nav_settings)

    companion object {
        val bottomNavItems = listOf(Home, Schedule, Settings)
    }
}

