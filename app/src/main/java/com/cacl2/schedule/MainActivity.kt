package com.cacl2.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.ui.navigation.NavGraph
import com.cacl2.schedule.ui.theme.ScheduleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ScheduleApplication
        val courseRepository = CourseRepository(app.database.courseDao())
        val settingsRepository = SettingsRepository(applicationContext)

        setContent {
            ScheduleTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    courseRepository = courseRepository,
                    settingsRepository = settingsRepository
                )
            }
        }
    }
}
