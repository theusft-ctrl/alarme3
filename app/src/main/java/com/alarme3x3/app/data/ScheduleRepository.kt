package com.alarme3x3.app.data

import com.alarme3x3.app.alarm.AlarmScheduler
import com.alarme3x3.app.domain.AlarmConfig
import com.alarme3x3.app.domain.ExceptionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

sealed class ExceptionSaveResult {
    data object Saved : ExceptionSaveResult()
    data class Conflict(
        val date: LocalDate,
        val existingType: ExceptionType,
        val requestedType: ExceptionType
    ) : ExceptionSaveResult()
}

class ScheduleRepository(
    private val db: AppDatabase,
    private val prefs: UserPreferences,
    private val scheduler: AlarmScheduler
) {
    val startDateFlow: Flow<LocalDate?> = prefs.startDateFlow
    val vibrateEnabledFlow: Flow<Boolean> = prefs.vibrateEnabledFlow

    val alarmsFlow: Flow<List<AlarmConfig>> =
        db.alarmDao().observeAll().map { list -> list.map { it.toDomain() } }

    val exceptionsFlow: Flow<List<ScheduleExceptionEntity>> =
        db.exceptionDao().observeAll()

    suspend fun initializeFirstUse(firstWorkDate: LocalDate) {
        prefs.setStartDate(firstWorkDate)
        if (db.alarmDao().getAllOnce().isEmpty()) {
            val defaults = listOf(
                AlarmEntity(hour = 6, minute = 0, label = "Alarme 1"),
                AlarmEntity(hour = 6, minute = 55, label = "Alarme 2"),
                AlarmEntity(hour = 12, minute = 0, label = "Alarme 3"),
                AlarmEntity(hour = 13, minute = 0, label = "Alarme 4"),
                AlarmEntity(hour = 18, minute = 55, label = "Alarme 5")
            )
            db.alarmDao().insertAll(defaults)
        }
        scheduler.rescheduleAll()
    }

    suspend fun updateStartDate(newStartDate: LocalDate) {
        prefs.setStartDate(newStartDate)
        scheduler.rescheduleAll()
    }

    suspend fun setVibrateEnabled(enabled: Boolean) {
        prefs.setVibrateEnabled(enabled)
    }

    suspend fun saveAlarm(alarm: AlarmConfig) {
        db.alarmDao().upsert(
            AlarmEntity(
                id = alarm.id,
                hour = alarm.hour,
                minute = alarm.minute,
                enabled = alarm.enabled,
                soundUri = alarm.soundUri,
                label = alarm.label
            )
        )
        scheduler.rescheduleAll()
    }

    suspend fun deleteAlarm(alarm: AlarmConfig) {
        scheduler.cancelAlarm(alarm.id)
        db.alarmDao().delete(
            AlarmEntity(alarm.id, alarm.hour, alarm.minute, alarm.enabled, alarm.soundUri, alarm.label)
        )
        scheduler.rescheduleAll()
    }

    suspend fun addException(
        date: LocalDate,
        type: ExceptionType,
        overwrite: Boolean = false
    ): ExceptionSaveResult {
        val iso = date.toString()
        val existing = db.exceptionDao().getByDate(iso)

        if (existing != null && existing.exceptionType() != type && !overwrite) {
            return ExceptionSaveResult.Conflict(
                date = date,
                existingType = existing.exceptionType(),
                requestedType = type
            )
        }

        db.exceptionDao().upsert(ScheduleExceptionEntity(dateIso = iso, type = type.name))
        scheduler.rescheduleAll()
        return ExceptionSaveResult.Saved
    }

    suspend fun removeException(date: LocalDate) {
        db.exceptionDao().deleteByDate(date.toString())
        scheduler.rescheduleAll()
    }

    suspend fun rescheduleAlarms() {
        scheduler.rescheduleAll()
    }
}
