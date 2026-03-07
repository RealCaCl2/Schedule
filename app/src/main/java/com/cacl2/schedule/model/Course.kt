package com.cacl2.schedule.model

data class Course(
    val id: Long = 0,
    val courseName: String,
    val teacher: String,
    val location: String,
    val dayOfWeek: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val startWeek: Int,
    val endWeek: Int,
    val weekType: Int = 0,
    val colorIndex: Int = 0
)
