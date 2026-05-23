package com.cacl2.schedule.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cacl2.schedule.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Query(
        """
        SELECT * FROM courses
        WHERE startWeek <= :week AND endWeek >= :week
        AND (weekType = 0
            OR (weekType = 1 AND :week % 2 = 1)
            OR (weekType = 2 AND :week % 2 = 0))
        ORDER BY dayOfWeek, startPeriod
        """
    )
    fun getCoursesForWeek(week: Int): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Long): CourseEntity?

    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startPeriod")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<CourseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(course: CourseEntity)

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("DELETE FROM courses")
    suspend fun deleteAll()

    @Transaction
    suspend fun replaceAll(courses: List<CourseEntity>) {
        deleteAll()
        insertAll(courses)
    }
}
