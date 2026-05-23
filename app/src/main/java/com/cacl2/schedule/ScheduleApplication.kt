package com.cacl2.schedule

import android.app.Application
import androidx.room.Room
import com.cacl2.schedule.data.local.AppDatabase

class ScheduleApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "schedule_database"
        ).fallbackToDestructiveMigration(true)
            .build()
    }
}
