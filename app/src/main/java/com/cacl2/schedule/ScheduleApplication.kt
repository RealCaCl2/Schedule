package com.cacl2.schedule

import android.app.Application
import androidx.room.Room
import com.cacl2.schedule.data.local.AppDatabase

class ScheduleApplication : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "schedule_database"
        ).build()
    }
}
