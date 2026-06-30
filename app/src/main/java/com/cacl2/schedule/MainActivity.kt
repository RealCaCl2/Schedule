package com.cacl2.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.cacl2.schedule.data.UpdateManager
import com.cacl2.schedule.data.UpdateResult
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SemesterRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.model.ScheduleSettings
import com.cacl2.schedule.model.ThemeMode
import com.cacl2.schedule.ui.navigation.NavGraph
import com.cacl2.schedule.ui.theme.ScheduleTheme
import com.cacl2.schedule.ui.update.UpdateAvailableDialog
import com.cacl2.schedule.ui.update.UpdateErrorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as ScheduleApplication
        val courseRepository = CourseRepository(app.database.courseDao())
        val semesterRepository = SemesterRepository(app.database.semesterDao())
        val settingsRepository = SettingsRepository(applicationContext)

        setContent {
            val settings by settingsRepository.settings
                .collectAsState(initial = ScheduleSettings())
            val themeMode = ThemeMode.fromValue(settings.themeMode)

            val isDark = when (themeMode) {
                ThemeMode.Dark -> true
                ThemeMode.Light -> false
                ThemeMode.System -> isSystemInDarkTheme()
            }

            var autoUpdateResult by remember { mutableStateOf<UpdateResult?>(null) }
            var autoUpdateDownloading by remember { mutableStateOf(false) }
            var autoUpdateProgress by remember { mutableStateOf(0f) }

            LaunchedEffect(Unit) {
                val manager = UpdateManager(this@MainActivity)
                val result = manager.autoCheck()
                if (result is UpdateResult.Available) {
                    autoUpdateResult = result
                }
            }

            val window = this@MainActivity.window
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark

            ScheduleTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    courseRepository = courseRepository,
                    settingsRepository = settingsRepository,
                    semesterRepository = semesterRepository
                )

                // Auto-check update dialog
                when (val state = autoUpdateResult) {
                    is UpdateResult.Available -> {
                        UpdateAvailableDialog(
                            info = state.info,
                            isDownloading = autoUpdateDownloading,
                            downloadProgress = autoUpdateProgress,
                            onDownload = {
                                autoUpdateDownloading = true
                                autoUpdateProgress = 0f
                                MainScope().launch {
                                    val manager = UpdateManager(this@MainActivity)
                                    val dateStr = java.text.SimpleDateFormat(
                                        "yyyyMMdd", java.util.Locale.getDefault()
                                    ).format(java.util.Date())
                                    val fileName = "Schedule_v${state.info.versionName}_$dateStr.apk"
                                    val file = withContext(Dispatchers.IO) {
                                        manager.downloadApk(
                                            url = state.info.downloadUrl,
                                            fileName = fileName,
                                            expectedSha256 = state.info.sha256,
                                            totalSizeHint = state.info.fileSize,
                                            onProgress = { progress ->
                                                autoUpdateProgress = progress
                                            }
                                        )
                                    }
                                    autoUpdateDownloading = false
                                    autoUpdateProgress = 0f
                                    file.onSuccess { manager.installApk(it) }
                                        .onFailure { e ->
                                            autoUpdateResult = UpdateResult.Error(
                                                e.message ?: "Download failed"
                                            )
                                        }
                                }
                            },
                            onDismiss = { autoUpdateResult = null }
                        )
                    }

                    is UpdateResult.Error -> {
                        UpdateErrorDialog(
                            message = state.message,
                            onDismiss = { autoUpdateResult = null }
                        )
                    }

                    else -> {}
                }
            }
        }
    }
}
