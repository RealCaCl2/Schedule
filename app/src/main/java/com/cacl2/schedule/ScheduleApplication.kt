package com.cacl2.schedule

import android.app.Application
import androidx.room.Room
import com.cacl2.schedule.data.local.AppDatabase
import com.cacl2.schedule.data.repository.SemesterRepository
import com.cacl2.schedule.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class ScheduleApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "schedule_database"
        ).addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        // Eagerly initialize database and ensure default semester exists
        // BEFORE any Activity starts, to avoid race conditions in UI data loading.
        // Using runBlocking on a background dispatcher to avoid main-thread IO warnings
        // while still completing before onCreate returns.
        runBlocking(Dispatchers.IO) {
            val db = database
            val semesterRepository = SemesterRepository(db.semesterDao())
            val settingsRepository = SettingsRepository(this@ScheduleApplication)
            settingsRepository.ensureDefaultSemesterIfNeeded(semesterRepository)
        }
    }
}
