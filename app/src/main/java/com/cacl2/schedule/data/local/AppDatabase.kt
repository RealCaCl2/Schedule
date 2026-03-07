package com.cacl2.schedule.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cacl2.schedule.data.local.dao.CourseDao
import com.cacl2.schedule.data.local.entity.CourseEntity

@Database(entities = [CourseEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
}
