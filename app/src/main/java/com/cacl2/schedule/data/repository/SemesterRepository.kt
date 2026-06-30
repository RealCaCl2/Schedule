package com.cacl2.schedule.data.repository

import com.cacl2.schedule.data.local.dao.SemesterDao
import com.cacl2.schedule.data.local.entity.SemesterEntity
import kotlinx.coroutines.flow.Flow

class SemesterRepository(private val semesterDao: SemesterDao) {

    fun getAllSemesters(): Flow<List<SemesterEntity>> = semesterDao.getAllSemesters()

    suspend fun getSemesterById(id: String): SemesterEntity? = semesterDao.getSemesterById(id)

    suspend fun insert(semester: SemesterEntity) = semesterDao.insert(semester)

    suspend fun update(semester: SemesterEntity) = semesterDao.update(semester)

    suspend fun deleteById(id: String) = semesterDao.deleteById(id)

    suspend fun count(): Int = semesterDao.count()
}
