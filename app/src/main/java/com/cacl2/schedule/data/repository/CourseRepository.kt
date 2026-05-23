package com.cacl2.schedule.data.repository

import com.cacl2.schedule.data.local.dao.CourseDao
import com.cacl2.schedule.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val courseDao: CourseDao) {

    suspend fun getCourseById(id: Long): CourseEntity? {
        return courseDao.getCourseById(id)
    }

    fun getCoursesForWeek(week: Int): Flow<List<CourseEntity>> {
        return courseDao.getCoursesForWeek(week)
    }

    fun getAllCourses(): Flow<List<CourseEntity>> {
        return courseDao.getAllCourses()
    }

    suspend fun insertAll(courses: List<CourseEntity>) {
        courseDao.insertAll(courses)
    }

    suspend fun deleteAll() {
        courseDao.deleteAll()
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

    suspend fun replaceAll(courses: List<CourseEntity>) {
        courseDao.replaceAll(courses)
    }
}
