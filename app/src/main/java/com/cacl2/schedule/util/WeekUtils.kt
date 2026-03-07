package com.cacl2.schedule.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object WeekUtils {

    fun calculateCurrentWeek(semesterStartDate: String): Int {
        if (semesterStartDate.isBlank()) return 1
        return try {
            val start = LocalDate.parse(semesterStartDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val today = LocalDate.now()
            val daysDiff = ChronoUnit.DAYS.between(start, today)
            if (daysDiff < 0) return 1
            (daysDiff / 7 + 1).toInt()
        } catch (_: Exception) {
            1
        }
    }

    fun getWeekDateRange(semesterStartDate: String, week: Int): String {
        if (semesterStartDate.isBlank()) return ""
        return try {
            val start = LocalDate.parse(semesterStartDate, DateTimeFormatter.ISO_LOCAL_DATE)
            val weekStart = start.plusDays(((week - 1) * 7).toLong())
            val weekEnd = weekStart.plusDays(6)
            val formatter = DateTimeFormatter.ofPattern("MM/dd")
            "${weekStart.format(formatter)}-${weekEnd.format(formatter)}"
        } catch (_: Exception) {
            ""
        }
    }
}
