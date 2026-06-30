package com.cacl2.schedule.model

import androidx.compose.runtime.Immutable

@Immutable
data class ScheduleSettings(
    val totalWeeks: Int = 20,
    val periodsPerDay: Int = 12,
    val semesterStartDate: String = "",
    val showWeekend: Boolean = true,
    val qiangzhiUrl: String = "",
    val themeMode: Int = 0,
    val activeSemesterId: String = "default",
    val showTeacher: Boolean = true,
    val showLocation: Boolean = true
)
