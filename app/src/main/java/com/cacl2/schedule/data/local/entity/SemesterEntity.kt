package com.cacl2.schedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val startDate: String,
    val totalWeeks: Int = 20,
    val periodsPerDay: Int = 12,
    val createdAt: Long = System.currentTimeMillis()
)
