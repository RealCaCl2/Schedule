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
        WHERE semesterId = :semesterId
          AND startWeek <= :week AND endWeek >= :week
          AND (weekType = 0
              OR (weekType = 1 AND :week % 2 = 1)
              OR (weekType = 2 AND :week % 2 = 0))
        ORDER BY dayOfWeek, startPeriod
        """
    )
    fun getCoursesForWeek(week: Int, semesterId: String): Flow<List<CourseEntity>>

    @Query(
        """
        SELECT * FROM courses
        WHERE semesterId = :semesterId
          AND startWeek <= :week AND endWeek >= :week
          AND (weekType = 0
              OR (weekType = 1 AND :week % 2 = 1)
              OR (weekType = 2 AND :week % 2 = 0))
        ORDER BY dayOfWeek, startPeriod
        """
    )
    suspend fun getCoursesForWeekOnce(week: Int, semesterId: String): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Long): CourseEntity?

    @Query("SELECT * FROM courses WHERE semesterId = :semesterId ORDER BY dayOfWeek, startPeriod")
    fun getAllCourses(semesterId: String): Flow<List<CourseEntity>>

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

    @Query("DELETE FROM courses WHERE semesterId = :semesterId")
    suspend fun deleteAllBySemester(semesterId: String)

    @Transaction
    suspend fun replaceAll(courses: List<CourseEntity>, semesterId: String) {
        deleteAllBySemester(semesterId)
        insertAll(courses.map { it.copy(semesterId = semesterId) })
    }

    @Query(
        """
        SELECT * FROM courses
        WHERE semesterId = :semesterId
          AND id != :excludeId
          AND dayOfWeek = :dayOfWeek
          AND startPeriod <= :endPeriod AND endPeriod >= :startPeriod
          AND startWeek <= :endWeek AND endWeek >= :startWeek
          AND (weekType = 0 OR :weekType = 0
               OR (weekType = 1 AND :weekType = 1)
               OR (weekType = 2 AND :weekType = 2))
        """
    )
    suspend fun findConflictingCourses(
        semesterId: String,
        excludeId: Long,
        dayOfWeek: Int,
        startPeriod: Int,
        endPeriod: Int,
        startWeek: Int,
        endWeek: Int,
        weekType: Int
    ): List<CourseEntity>
}
