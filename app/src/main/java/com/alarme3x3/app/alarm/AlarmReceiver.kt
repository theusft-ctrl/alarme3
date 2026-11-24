package com.alarme3x3.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ALARM_TRIGGER) return

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val timeText = intent.getStringExtra(EXTRA_TIME_TEXT) ?: ""
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Alarme 3x3"
        val soundUri = intent.getStringExtra(EXTRA_SOUND_URI)

        val serviceIntent = Intent(context, AlarmRingService::class.java).apply {
            action = AlarmRingService.ACTION_START
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_TIME_TEXT, timeText)
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_SOUND_URI, soundUri)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // Reagenda imediatamente o próximo dia útil da escala
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmScheduler(context.applicationContext).rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.alarme3x3.app.ACTION_ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_TIME_TEXT = "extra_time_text"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_SOUND_URI = "extra_sound_uri"
    }
}
