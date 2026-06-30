package com.cacl2.schedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cacl2.schedule.data.local.entity.SemesterEntity
import android.app.Application
import com.cacl2.schedule.data.UpdateManager
import com.cacl2.schedule.data.UpdateResult
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SemesterRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.model.ScheduleSettings
import com.cacl2.schedule.model.SharedScheduleCodec
import com.cacl2.schedule.model.SharedScheduleData
import com.cacl2.schedule.model.ThemeMode
import com.cacl2.schedule.model.UpdateInfo
import com.cacl2.schedule.util.CourseColorMapper
import com.cacl2.schedule.util.QiangZhiUrlNormalizer
import com.cacl2.schedule.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val courseRepository: CourseRepository,
    private val semesterRepository: SemesterRepository,
    private val application: Application
) : ViewModel() {

    val settings: StateFlow<ScheduleSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleSettings())

    val semesters: StateFlow<List<SemesterEntity>> = semesterRepository.getAllSemesters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateTotalWeeks(weeks: Int) {
        viewModelScope.launch { settingsRepository.updateTotalWeeks(weeks) }
    }

    fun updatePeriodsPerDay(periods: Int) {
        viewModelScope.launch { settingsRepository.updatePeriodsPerDay(periods) }
    }

    fun updateSemesterStartDate(date: String) {
        viewModelScope.launch { settingsRepository.updateSemesterStartDate(date) }
    }

    fun updateShowWeekend(show: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowWeekend(show) }
    }

    fun updateShowTeacher(show: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowTeacher(show) }
    }

    fun updateShowLocation(show: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowLocation(show) }
    }

    fun normalizeQiangzhiUrl(raw: String): String? {
        return QiangZhiUrlNormalizer.normalizeOrNull(raw)
    }

    fun updateQiangzhiUrl(url: String) {
        viewModelScope.launch { settingsRepository.updateQiangzhiUrl(url) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.updateThemeMode(mode.value) }
    }

    fun clearAllCourses() {
        viewModelScope.launch {
            courseRepository.deleteAll()
            WidgetUpdater.updateAllWidgets(application)
        }
    }

    fun setActiveSemester(id: String) {
        viewModelScope.launch {
            settingsRepository.setActiveSemester(id)
            WidgetUpdater.updateAllWidgets(application)
        }
    }

    fun createSemester(name: String, startDate: String, totalWeeks: Int, periodsPerDay: Int) {
        viewModelScope.launch {
            val generatedId = startDate.replace("-", "").take(6) + "-" + totalWeeks.toString()
            val semester = SemesterEntity(
                id = generatedId,
                name = name,
                startDate = startDate,
                totalWeeks = totalWeeks,
                periodsPerDay = periodsPerDay
            )
            semesterRepository.insert(semester)
            settingsRepository.setActiveSemester(semester.id)
        }
    }

    fun updateSemester(semester: SemesterEntity) {
        viewModelScope.launch { semesterRepository.update(semester) }
    }

    fun importFromScanResult(encoded: String): SharedScheduleData? {
        val data = SharedScheduleCodec.decode(encoded) ?: return null
        viewModelScope.launch {
            val newId = data.semester.startDate.replace("-", "").take(6) + "-" + data.semester.totalWeeks.toString()
            val semester = SemesterEntity(
                id = newId,
                name = data.semester.name,
                startDate = data.semester.startDate,
                totalWeeks = data.semester.totalWeeks,
                periodsPerDay = data.semester.periodsPerDay
            )
            semesterRepository.insert(semester)
            settingsRepository.setActiveSemester(semester.id)

            val courses = data.courses.map {
                SharedScheduleCodec.toEntity(
                    it,
                    semesterId = semester.id,
                    colorIndex = CourseColorMapper.getColorIndex(it.courseName)
                )
            }
            courseRepository.insertAll(courses)
            WidgetUpdater.updateAllWidgets(application)
        }
        return data
    }

    // ── Update check state ────────────────────────────────────
    private val _updateState = MutableStateFlow<UpdateResult?>(null)
    val updateState: StateFlow<UpdateResult?> = _updateState.asStateFlow()

    private val _updateDownloading = MutableStateFlow(false)
    val updateDownloading: StateFlow<Boolean> = _updateDownloading.asStateFlow()

    private val _updateProgress = MutableStateFlow(0f)
    val updateProgress: StateFlow<Float> = _updateProgress.asStateFlow()

    fun clearUpdateState() {
        _updateState.value = null
    }

    fun checkUpdate() {
        viewModelScope.launch {
            _updateState.value = UpdateResult.Checking
            val manager = UpdateManager(application)
            val result = manager.checkForUpdates()
            _updateState.value = result
        }
    }

    fun downloadUpdate(info: UpdateInfo) {
        viewModelScope.launch {
            _updateDownloading.value = true
            _updateProgress.value = 0f
            val manager = UpdateManager(application)
            val dateStr = java.text.SimpleDateFormat(
                "yyyyMMdd", java.util.Locale.getDefault()
            ).format(java.util.Date())
            val fileName = "Schedule_v${info.versionName}_$dateStr.apk"

            val result = withContext(Dispatchers.IO) {
                manager.downloadApk(
                    url = info.downloadUrl,
                    fileName = fileName,
                    expectedSha256 = info.sha256,
                    totalSizeHint = info.fileSize,
                    onProgress = { progress ->
                        _updateProgress.value = progress
                    }
                )
            }
            _updateDownloading.value = false
            _updateProgress.value = 0f

            result.fold(
                onSuccess = { file ->
                    manager.installApk(file)
                    _updateState.value = null
                },
                onFailure = { e ->
                    _updateState.value = UpdateResult.Error(
                        e.message ?: "Download failed"
                    )
                }
            )
        }
    }

    fun deleteSemester(id: String) {
        viewModelScope.launch {
            courseRepository.deleteAllBySemester(id)
            semesterRepository.deleteById(id)
            WidgetUpdater.updateAllWidgets(application)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val courseRepository: CourseRepository,
        private val semesterRepository: SemesterRepository,
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, courseRepository, semesterRepository, application) as T
        }
    }
}
