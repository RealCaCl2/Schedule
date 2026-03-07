package com.cacl2.schedule.util

import kotlin.math.abs

object CourseColorMapper {
    fun getColorIndex(courseName: String): Int {
        return abs(courseName.hashCode()) % 10
    }
}
