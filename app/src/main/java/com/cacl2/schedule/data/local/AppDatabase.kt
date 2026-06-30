package com.cacl2.schedule.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cacl2.schedule.data.local.dao.CourseDao
import com.cacl2.schedule.data.local.dao.SemesterDao
import com.cacl2.schedule.data.local.entity.CourseEntity
import com.cacl2.schedule.data.local.entity.SemesterEntity

@Database(
    entities = [CourseEntity::class, SemesterEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun semesterDao(): SemesterDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS semesters (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        startDate TEXT NOT NULL,
                        totalWeeks INTEGER NOT NULL DEFAULT 20,
                        periodsPerDay INTEGER NOT NULL DEFAULT 12,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "ALTER TABLE courses ADD COLUMN semesterId TEXT NOT NULL DEFAULT 'default'"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_courses_semesterId ON courses(semesterId)"
                )
            }
        }
    }
}
