package com.aaronroberts.naturesmusic.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.aaronroberts.naturesmusic.NaturesMusicApplication
import com.aaronroberts.naturesmusic.R
import com.aaronroberts.naturesmusic.data.SoundCatalog

class AlarmService : Service() {

    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                NaturesMusicApplication.instance.alarmController.alarmStop()
                stopAlarmService()
                return START_NOT_STICKY
            }
            ACTION_SNOOZE -> {
                NaturesMusicApplication.instance.alarmController.snooze()
                stopAlarmService()
                return START_NOT_STICKY
            }
        }
        startInForeground()
        NaturesMusicApplication.instance.alarmController.alarmStart()
        vibrate()
        return START_STICKY
    }

    override fun onDestroy() {
        vibrator?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun stopAlarmService() {
        vibrator?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(VibratorManager::class.java)
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        this.vibrator = vibrator
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 400, 500), 0))
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val fullScreen = PendingIntent.getActivity(
            this,
            0,
            Intent(this, AlarmRingingActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val dismiss = PendingIntent.getService(
            this,
            1,
            Intent(this, AlarmService::class.java).setAction(ACTION_DISMISS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val snooze = PendingIntent.getService(
            this,
            2,
            Intent(this, AlarmService::class.java).setAction(ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_wave)
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setContentText(mixLabel(this, NaturesMusicApplication.instance.alarmStore.mix))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .setOngoing(true)
            .addAction(R.drawable.ic_stat_wave, getString(R.string.alarm_stop), dismiss)
            .addAction(R.drawable.ic_stat_wave, getString(R.string.snooze), snooze)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.alarm_channel_desc)
            enableVibration(true)
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "natures_music_alarm"
        const val NOTIFICATION_ID = 42
        const val ACTION_DISMISS = "com.aaronroberts.naturesmusic.DISMISS_ALARM"
        const val ACTION_SNOOZE = "com.aaronroberts.naturesmusic.SNOOZE_ALARM"

        fun mixLabel(context: Context, mix: List<AlarmMixer>): String {
            val names = mix.mapNotNull { item ->
                SoundCatalog.track(item.id)?.displayNameRes?.let(context::getString)
            }
            return if (names.isEmpty()) context.getString(R.string.your_mix) else names.joinToString(", ")
        }
    }
}
