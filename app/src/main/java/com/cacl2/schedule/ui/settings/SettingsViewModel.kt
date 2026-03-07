package com.cacl2.schedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.model.ScheduleSettings
import com.cacl2.schedule.util.QiangZhiUrlNormalizer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    val settings: StateFlow<ScheduleSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleSettings())

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

    fun normalizeQiangzhiUrl(raw: String): String? {
        return QiangZhiUrlNormalizer.normalizeOrNull(raw)
    }

    fun updateQiangzhiUrl(url: String) {
        viewModelScope.launch { settingsRepository.updateQiangzhiUrl(url) }
    }

    fun clearAllCourses() {
        viewModelScope.launch { courseRepository.deleteAll() }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val courseRepository: CourseRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, courseRepository) as T
        }
    }
}
