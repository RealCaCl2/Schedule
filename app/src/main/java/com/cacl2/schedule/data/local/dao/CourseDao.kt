package com.cacl2.schedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    @Query("SELECT * FROM courses ORDER BY dayOfWeek, startPeriod")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(courses: List<CourseEntity>)

    @Query("DELETE FROM courses")
    suspend fun deleteAll()
}
