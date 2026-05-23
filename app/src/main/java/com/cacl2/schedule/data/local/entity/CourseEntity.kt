package com.cacl2.schedule.data.local.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Immutable
@Entity(
    tableName = "courses",
    indices = [
        Index(value = ["startWeek", "endWeek"]),
        Index(value = ["dayOfWeek", "startPeriod"])
    ]
)
data class CourseEntity(
    @PrimaryKey(autoGenerate = true)
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
