package com.cacl2.schedule.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import android.app.Application
import com.cacl2.schedule.data.local.entity.CourseEntity
import com.cacl2.schedule.data.repository.CourseRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.util.CourseColorMapper
import com.cacl2.schedule.widget.WidgetUpdater
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
data class EditFormState(
    val courseName: String = "",
    val teacher: String = "",
    val location: String = "",
    val dayOfWeek: Int = 1,
    val startPeriod: String = "",
    val endPeriod: String = "",
    val startWeek: String = "",
    val endWeek: String = "",
    val weekType: Int = 0
)

@Immutable
sealed class EditSaveState {
    data object Idle : EditSaveState()
    data object Saving : EditSaveState()
    data object Success : EditSaveState()
    data class Error(val message: String) : EditSaveState()
}

@Immutable
sealed class EditValidationError {
    data object CourseNameEmpty : EditValidationError()
    data object InvalidDayOfWeek : EditValidationError()
    data object InvalidPeriodRange : EditValidationError()
    data object InvalidWeekRange : EditValidationError()
}

class CourseEditViewModel(
    private val courseRepository: CourseRepository,
    private val settingsRepository: SettingsRepository,
    private val application: Application
) : ViewModel() {

    private var courseId: Long = 0

    private val _formState = MutableStateFlow(EditFormState())
    val formState: StateFlow<EditFormState> = _formState.asStateFlow()

    private val _saveState = MutableStateFlow<EditSaveState>(EditSaveState.Idle)
    val saveState: StateFlow<EditSaveState> = _saveState.asStateFlow()

    private val _conflicts = MutableStateFlow<List<CourseEntity>>(emptyList())
    val conflicts: StateFlow<List<CourseEntity>> = _conflicts.asStateFlow()

    private var conflictJob: Job? = null

    fun loadCourse(id: Long) {
        courseId = id
        if (id == 0L) return
        viewModelScope.launch {
            val course = courseRepository.getCourseById(id) ?: return@launch
            _formState.value = EditFormState(
                courseName = course.courseName,
                teacher = course.teacher,
                location = course.location,
                dayOfWeek = course.dayOfWeek,
                startPeriod = course.startPeriod.toString(),
                endPeriod = course.endPeriod.toString(),
                startWeek = course.startWeek.toString(),
                endWeek = course.endWeek.toString(),
                weekType = course.weekType
            )
        }
    }

    fun updateCourseName(value: String) {
        _formState.update { it.copy(courseName = value) }
        checkConflicts()
    }

    fun updateTeacher(value: String) {
        _formState.update { it.copy(teacher = value) }
    }

    fun updateLocation(value: String) {
        _formState.update { it.copy(location = value) }
    }

    fun updateDayOfWeek(value: Int) {
        _formState.update { it.copy(dayOfWeek = value) }
        checkConflicts()
    }

    fun updateStartPeriod(value: String) {
        _formState.update { it.copy(startPeriod = value) }
        checkConflicts()
    }

    fun updateEndPeriod(value: String) {
        _formState.update { it.copy(endPeriod = value) }
        checkConflicts()
    }

    fun updateStartWeek(value: String) {
        _formState.update { it.copy(startWeek = value) }
        checkConflicts()
    }

    fun updateEndWeek(value: String) {
        _formState.update { it.copy(endWeek = value) }
        checkConflicts()
    }

    fun updateWeekType(value: Int) {
        _formState.update { it.copy(weekType = value) }
        checkConflicts()
    }

    fun resetSaveState() {
        _saveState.value = EditSaveState.Idle
    }

    private fun checkConflicts() {
        val state = _formState.value
        val sp = state.startPeriod.toIntOrNull()
        val ep = state.endPeriod.toIntOrNull()
        val sw = state.startWeek.toIntOrNull()
        val ew = state.endWeek.toIntOrNull()
        if (sp == null || ep == null || sw == null || ew == null || sp < 1 || ep < 1 || sw < 1 || ew < 1) {
            _conflicts.value = emptyList()
            return
        }

        conflictJob?.cancel()
        conflictJob = viewModelScope.launch {
            val activeSemesterId = settingsRepository.activeSemesterId.first()
            _conflicts.value = courseRepository.findConflictingCourses(
                semesterId = activeSemesterId,
                excludeId = courseId,
                dayOfWeek = state.dayOfWeek,
                startPeriod = sp,
                endPeriod = ep,
                startWeek = sw,
                endWeek = ew,
                weekType = state.weekType
            )
        }
    }

    fun save(): EditValidationError? {
        val state = _formState.value
        val name = state.courseName.trim()
        if (name.isBlank()) return EditValidationError.CourseNameEmpty

        val day = state.dayOfWeek
        if (day !in 1..7) return EditValidationError.InvalidDayOfWeek

        val sp = state.startPeriod.toIntOrNull()
        val ep = state.endPeriod.toIntOrNull()
        if (sp == null || ep == null || sp < 1 || ep < 1 || sp > ep) {
            return EditValidationError.InvalidPeriodRange
        }

        val sw = state.startWeek.toIntOrNull()
        val ew = state.endWeek.toIntOrNull()
        if (sw == null || ew == null || sw < 1 || ew < 1 || sw > ew) {
            return EditValidationError.InvalidWeekRange
        }

        _saveState.value = EditSaveState.Saving
        viewModelScope.launch {
            try {
                val semesterId = settingsRepository.activeSemesterId.first()
                val course = CourseEntity(
                    id = courseId,
                    courseName = name,
                    teacher = state.teacher.trim(),
                    location = state.location.trim(),
                    dayOfWeek = day,
                    startPeriod = sp,
                    endPeriod = ep,
                    startWeek = sw,
                    endWeek = ew,
                    weekType = state.weekType,
                    colorIndex = CourseColorMapper.getColorIndex(name),
                    semesterId = semesterId
                )

                if (courseId == 0L) {
                    courseRepository.insert(course)
                } else {
                    courseRepository.update(course)
                }
                WidgetUpdater.updateAllWidgets(application)
                _saveState.value = EditSaveState.Success
            } catch (e: Exception) {
                _saveState.value = EditSaveState.Error(e.message ?: "保存失败")
            }
        }

        return null
    }

    class Factory(
        private val courseRepository: CourseRepository,
        private val settingsRepository: SettingsRepository,
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CourseEditViewModel(courseRepository, settingsRepository, application) as T
        }
    }
}
