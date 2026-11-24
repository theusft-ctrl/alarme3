package com.alarme3x3.app

import android.app.Application
import com.alarme3x3.app.alarm.AlarmScheduler
import com.alarme3x3.app.data.AppDatabase
import com.alarme3x3.app.data.ScheduleRepository
import com.alarme3x3.app.data.UserPreferences

class Alarme3x3App : Application() {
    val repository: ScheduleRepository by lazy {
        ScheduleRepository(
            db = AppDatabase.getInstance(this),
            prefs = UserPreferences(this),
            scheduler = AlarmScheduler(this)
        )
    }
}
