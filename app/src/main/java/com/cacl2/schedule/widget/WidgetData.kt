package com.cacl2.schedule.widget

data class WidgetCourseInfo(
    val courseName: String,
    val startPeriod: Int,
    val endPeriod: Int,
    val location: String,
    val teacher: String
)

data class WidgetState(
    val dayName: String,
    val weekNumber: Int,
    val dateLabel: String,
    val courses: List<WidgetCourseInfo>,
    val isEmpty: Boolean
)
