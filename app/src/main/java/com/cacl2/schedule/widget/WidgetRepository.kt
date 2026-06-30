package com.cacl2.schedule.widget

import android.content.Context
import com.cacl2.schedule.ScheduleApplication
import com.cacl2.schedule.data.repository.SettingsRepository
import com.cacl2.schedule.util.WeekUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class WidgetRepository(context: Context) {
    private val app = context.applicationContext as ScheduleApplication
    private val db = app.database
    private val settingsRepo = SettingsRepository(context.applicationContext)

    fun getTodayState(): WidgetState {
        val today = LocalDate.now()
        val todayDay = today.dayOfWeek.value

        val activeSemesterId = runBlocking { settingsRepo.activeSemesterId.first() }
        val activeSemester = runBlocking { db.semesterDao().getSemesterById(activeSemesterId) }
        val startDate = activeSemester?.startDate ?: ""
        val totalWeeks = activeSemester?.totalWeeks ?: 20

        val currentWeek = WeekUtils.calculateCurrentWeek(startDate).coerceIn(1, totalWeeks)

        val courses = runBlocking {
            db.courseDao().getCoursesForWeekOnce(currentWeek, activeSemesterId)
        }.filter { it.dayOfWeek == todayDay }
            .sortedBy { it.startPeriod }

        val dayLabel = when (todayDay) {
            1 -> "一"
            2 -> "二"
            3 -> "三"
            4 -> "四"
            5 -> "五"
            6 -> "六"
            7 -> "日"
            else -> ""
        }
        val dateLabel = today.format(DateTimeFormatter.ofPattern("MM/dd"))

        return WidgetState(
            dayName = "周$dayLabel",
            weekNumber = currentWeek,
            dateLabel = dateLabel,
            courses = courses.map {
                WidgetCourseInfo(it.courseName, it.startPeriod, it.endPeriod, it.location, it.teacher)
            },
            isEmpty = courses.isEmpty()
        )
    }
}
