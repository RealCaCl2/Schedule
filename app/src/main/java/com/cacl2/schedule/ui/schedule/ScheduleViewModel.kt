package com.cacl2.schedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.cacl2.schedule.data.local.entity.CourseEntity
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.model.ScheduleSettings
import com.cacl2.schedule.util.WeekUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class ScheduleViewModel(
    private val courseRepository: CourseRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<ScheduleSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ScheduleSettings())

    private val _currentWeek = MutableStateFlow(1)
    val currentWeek: StateFlow<Int> = _currentWeek.asStateFlow()

    val allCourses: StateFlow<List<CourseEntity>> = courseRepository.getAllCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weekCoursesByWeek: StateFlow<Map<Int, List<CourseEntity>>> = combine(
        allCourses,
        settings
    ) { courses, settingsValue ->
        val totalWeeks = settingsValue.totalWeeks.coerceAtLeast(1)
        val buckets = Array(totalWeeks + 1) { mutableListOf<CourseEntity>() }

        courses.forEach { course ->
            val startWeek = course.startWeek.coerceAtLeast(1)
            val endWeek = course.endWeek.coerceAtMost(totalWeeks)
            if (startWeek > endWeek) return@forEach

            for (week in startWeek..endWeek) {
                val weekTypeMatch = when (course.weekType) {
                    1 -> week % 2 == 1
                    2 -> week % 2 == 0
                    else -> true
                }
                if (weekTypeMatch) {
                    buckets[week].add(course)
                }
            }
        }

        buildMap(totalWeeks) {
            for (week in 1..totalWeeks) {
                put(
                    week,
                    buckets[week].sortedWith(
                        compareBy<CourseEntity> { it.dayOfWeek }
                            .thenBy { it.startPeriod }
                            .thenBy { it.endPeriod }
                    )
                )
            }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun coursesForWeek(week: Int): List<CourseEntity> {
        return weekCoursesByWeek.value[week].orEmpty()
    }

    fun setCurrentWeek(week: Int) {
        _currentWeek.value = week
    }

    fun initCurrentWeek(semesterStartDate: String, totalWeeks: Int) {
        val calculated = WeekUtils.calculateCurrentWeek(semesterStartDate)
        _currentWeek.value = calculated.coerceIn(1, totalWeeks)
    }

    class Factory(
        private val courseRepository: CourseRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScheduleViewModel(courseRepository, settingsRepository) as T
        }
    }
}
