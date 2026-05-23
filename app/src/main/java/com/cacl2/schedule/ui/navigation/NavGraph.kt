package com.cacl2.schedule.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.ui.edit.CourseEditScreen
import com.cacl2.schedule.ui.home.HomeScreen
import com.cacl2.schedule.ui.import_.ImportScreen
import com.cacl2.schedule.ui.onboarding.OnboardingScreen
import com.cacl2.schedule.ui.schedule.ScheduleScreen
import com.cacl2.schedule.ui.schedule.ScheduleViewModel
import com.cacl2.schedule.ui.settings.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    courseRepository: CourseRepository,
    settingsRepository: SettingsRepository
) {
    val onboardingCompleted by settingsRepository.onboardingCompleted.collectAsState(initial = null)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = Screen.bottomNavItems.any { it.route == currentRoute }

    val activity = LocalContext.current as ComponentActivity
    val sharedScheduleViewModel: ScheduleViewModel = viewModel(
        factory = ScheduleViewModel.Factory(courseRepository, settingsRepository),
        viewModelStoreOwner = activity
    )

    var initialStartDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(onboardingCompleted) {
        val state = onboardingCompleted
        if (initialStartDestination == null && state != null) {
            initialStartDestination = if (state) Screen.Home.route else Screen.Onboarding.route
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    tonalElevation = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(screen.icon, contentDescription = null) },
                            label = { Text(stringResource(screen.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val startDestination = initialStartDestination
        if (startDestination == null) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding))
            return@Scaffold
        }

        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    settingsRepository = settingsRepository,
                    onComplete = {
                        navController.navigate(Screen.Import.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    courseRepository = courseRepository,
                    settingsRepository = settingsRepository,
                    viewModel = sharedScheduleViewModel
                )
            }
            composable(Screen.Schedule.route) {
                ScheduleScreen(
                    courseRepository = courseRepository,
                    settingsRepository = settingsRepository,
                    viewModel = sharedScheduleViewModel,
                    onEditCourse = { courseId ->
                        navController.navigate("edit/$courseId")
                    }
                )
            }
            composable(
                route = Screen.Edit.route,
                arguments = listOf(navArgument("courseId") { type = NavType.LongType; defaultValue = 0L })
            ) { backStackEntry ->
                val courseId = backStackEntry.arguments?.getLong("courseId") ?: 0L
                CourseEditScreen(
                    courseId = courseId,
                    courseRepository = courseRepository,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Import.route) {
                ImportScreen(
                    courseRepository = courseRepository,
                    settingsRepository = settingsRepository,
                    onImportSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    settingsRepository = settingsRepository,
                    courseRepository = courseRepository,
                    onImportClick = {
                        navController.navigate(Screen.Import.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
