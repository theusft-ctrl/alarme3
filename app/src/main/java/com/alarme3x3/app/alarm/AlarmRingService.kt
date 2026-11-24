package com.alarme3x3.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.alarme3x3.app.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AlarmRingService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) {
            _dismissEvents.tryEmit(Unit)
            stopSelf()
            return START_NOT_STICKY
        }

        val timeText = intent?.getStringExtra(AlarmReceiver.EXTRA_TIME_TEXT) ?: ""
        val label = intent?.getStringExtra(AlarmReceiver.EXTRA_LABEL) ?: "Alarme 3x3"
        val soundUriString = intent?.getStringExtra(AlarmReceiver.EXTRA_SOUND_URI)

        ensureChannel()

        val fullScreenIntent = Intent(this, AlarmRingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            putExtra(AlarmReceiver.EXTRA_TIME_TEXT, timeText)
            putExtra(AlarmReceiver.EXTRA_LABEL, label)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            1001,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(this, AlarmRingService::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            1002,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Alarme 3x3 • $timeText")
            .setContentText(label.ifBlank { "Dia de trabalho na sua escala" })
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Desligar",
                dismissPendingIntent
            )
            .build()

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)

        startAlarmAudio(soundUriString)
        CoroutineScope(Dispatchers.IO).launch {
            if (UserPreferences(applicationContext).isVibrateEnabledOnce()) {
                startVibration()
            }
        }

        return START_NOT_STICKY
    }

    private fun startAlarmAudio(soundUriString: String?) {
        stopAlarmAudio()
        val primaryUri = soundUriString?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        if (primaryUri == null) return

        try {
            playUri(primaryUri)
        } catch (_: Exception) {
            val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            runCatching { playUri(fallback) }
        }
    }

    private fun playUri(uri: Uri) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(applicationContext, uri)
            isLooping = true
            prepare()
            start()
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 600, 400, 600, 400)
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun stopAlarmAudio() {
        runCatching {
            mediaPlayer?.run {
                if (isPlaying) stop()
                release()
            }
        }
        mediaPlayer = null
        vibrator?.cancel()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Disparo de Alarmes",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Canal nativo para disparo dos alarmes da escala 3x3"
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopAlarmAudio()
        _dismissEvents.tryEmit(Unit)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.alarme3x3.app.RING_START"
        const val ACTION_DISMISS = "com.alarme3x3.app.RING_DISMISS"
        private const val CHANNEL_ID = "alarme_3x3_ring_channel"
        private const val NOTIFICATION_ID = 9001

        private val _dismissEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val dismissEvents = _dismissEvents.asSharedFlow()
    }
}
