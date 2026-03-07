package com.cacl2.schedule.model

data class ScheduleSettings(
    val totalWeeks: Int = 20,
    val periodsPerDay: Int = 12,
    val semesterStartDate: String = "",
    val showWeekend: Boolean = true,
    val qiangzhiUrl: String = ""
)
