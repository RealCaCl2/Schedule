package com.cacl2.schedule.data.repository

import com.cacl2.schedule.data.local.dao.CourseDao
import com.cacl2.schedule.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val courseDao: CourseDao) {

    suspend fun getCourseById(id: Long): CourseEntity? {
        return courseDao.getCourseById(id)
    }

    fun getCoursesForWeek(week: Int, semesterId: String): Flow<List<CourseEntity>> {
        return courseDao.getCoursesForWeek(week, semesterId)
    }

    suspend fun getCoursesForWeekOnce(week: Int, semesterId: String): List<CourseEntity> {
        return courseDao.getCoursesForWeekOnce(week, semesterId)
    }

    fun getAllCourses(semesterId: String): Flow<List<CourseEntity>> {
        return courseDao.getAllCourses(semesterId)
    }

    suspend fun insertAll(courses: List<CourseEntity>) {
        courseDao.insertAll(courses)
    }

    suspend fun deleteAll() {
        courseDao.deleteAll()
    }

    suspend fun deleteAllBySemester(semesterId: String) {
        courseDao.deleteAllBySemester(semesterId)
    }

    suspend fun insert(course: CourseEntity) {
        courseDao.insert(course)
    }

    suspend fun update(course: CourseEntity) {
        courseDao.update(course)
    }

    suspend fun delete(course: CourseEntity) {
        courseDao.delete(course)
    }

    suspend fun replaceAll(courses: List<CourseEntity>, semesterId: String) {
        courseDao.replaceAll(courses, semesterId)
    }

    suspend fun findConflictingCourses(
        semesterId: String,
        excludeId: Long,
        dayOfWeek: Int,
        startPeriod: Int,
        endPeriod: Int,
        startWeek: Int,
        endWeek: Int,
        weekType: Int
    ): List<CourseEntity> {
        return courseDao.findConflictingCourses(
            semesterId, excludeId, dayOfWeek,
            startPeriod, endPeriod, startWeek, endWeek, weekType
        )
    }
}
