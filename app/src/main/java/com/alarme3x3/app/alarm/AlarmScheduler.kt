package com.alarme3x3.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.alarme3x3.app.MainActivity
import com.alarme3x3.app.data.AppDatabase
import com.alarme3x3.app.data.UserPreferences
import com.alarme3x3.app.domain.ExceptionType
import com.alarme3x3.app.domain.ScaleEngine
import java.time.LocalDate
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    suspend fun rescheduleAll() {
        val prefs = UserPreferences(context)
        val startDate = prefs.getStartDateOnce() ?: return
        val db = AppDatabase.getInstance(context)

        val alarms = db.alarmDao().getAllOnce().map { it.toDomain() }
        val exceptions = db.exceptionDao().getAllOnce()

        val customOff = exceptions
            .filter { it.exceptionType() == ExceptionType.CUSTOM_OFF }
            .map { LocalDate.parse(it.dateIso) }
            .toSet()

        val extraWork = exceptions
            .filter { it.exceptionType() == ExceptionType.EXTRA_WORK }
            .map { LocalDate.parse(it.dateIso) }
            .toSet()

        val now = ZonedDateTime.now()

        for (alarm in alarms) {
            cancelAlarm(alarm.id)
            if (!alarm.enabled) continue

            val next = ScaleEngine.findNextOccurrenceForAlarm(
                alarm = alarm,
                startDate = startDate,
                customOffDates = customOff,
                extraWorkDates = extraWork,
                now = now
            ) ?: continue

            scheduleExactAlarm(
                alarmId = alarm.id,
                triggerAtMillis = next.triggerDateTime.toInstant().toEpochMilli(),
                timeText = alarm.formattedTime(),
                label = alarm.label,
                soundUri = alarm.soundUri
            )
        }
    }

    private fun scheduleExactAlarm(
        alarmId: Long,
        triggerAtMillis: Long,
        timeText: String,
        label: String,
        soundUri: String?
    ) {
        if (!canScheduleExactAlarms()) return

        val fireIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_TIME_TEXT, timeText)
            putExtra(AlarmReceiver.EXTRA_LABEL, label)
            putExtra(AlarmReceiver.EXTRA_SOUND_URI, soundUri)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            fireIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt() + 100_000,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)
        alarmManager.setAlarmClock(clockInfo, pendingIntent)
    }

    fun cancelAlarm(alarmId: Long) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
