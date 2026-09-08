package com.aaronroberts.naturesmusic

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.aaronroberts.naturesmusic.alarm.AlarmController
import com.aaronroberts.naturesmusic.alarm.AlarmPhase
import com.aaronroberts.naturesmusic.alarm.AlarmScheduler
import com.aaronroberts.naturesmusic.alarm.AlarmService
import com.aaronroberts.naturesmusic.alarm.AlarmStore
import com.aaronroberts.naturesmusic.playback.MixerEngine
import com.aaronroberts.naturesmusic.timer.SleepTimerStore
import java.util.Calendar
import java.util.Date

class NaturesMusicApplication : Application() {

    lateinit var mixerEngine: MixerEngine
        private set

    lateinit var alarmStore: AlarmStore
        private set

    lateinit var alarmScheduler: AlarmScheduler
        private set

    lateinit var alarmController: AlarmController
        private set

    lateinit var sleepTimerStore: SleepTimerStore
        private set

    private val tickHandler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            onAlarmTick()
            tickHandler.postDelayed(this, 1_000L)
        }
    }

    @Volatile
    private var tickFiredLabel: String? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        mixerEngine = MixerEngine(this)
        sleepTimerStore = SleepTimerStore(mixerEngine)
        alarmStore = AlarmStore(this)
        alarmScheduler = AlarmScheduler(this)
        alarmController = AlarmController(this, alarmStore, alarmScheduler, mixerEngine)
        when (alarmStore.phase) {
            AlarmPhase.Idle -> Unit
            AlarmPhase.Armed -> {
                alarmScheduler.schedule(
                    alarmStore.year,
                    alarmStore.month,
                    alarmStore.day,
                    alarmStore.hour,
                    alarmStore.minute,
                )
            }
            AlarmPhase.Snoozed -> {
                val target = Calendar.getInstance().apply {
                    set(Calendar.YEAR, alarmStore.year)
                    set(Calendar.MONTH, alarmStore.month)
                    set(Calendar.DAY_OF_MONTH, alarmStore.day)
                    set(Calendar.HOUR_OF_DAY, alarmStore.hour)
                    set(Calendar.MINUTE, alarmStore.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                alarmScheduler.scheduleAt(target.timeInMillis)
            }
            AlarmPhase.Ringing -> {
                ContextCompat.startForegroundService(
                    this,
                    Intent(this, AlarmService::class.java),
                )
            }
        }
        tickHandler.post(tickRunnable)
    }

    /**
     * iOS updateTime(): every 1s, if Armed and Current Time string equals the
     * Alarm Time pill string, fire. App-wide, not only while AlarmScreen is visible.
     */
    private fun onAlarmTick() {
        val settings = alarmStore.settings.value
        if (settings.phase != AlarmPhase.Armed) {
            tickFiredLabel = null
            return
        }
        val nowLabel = AlarmStore.formatDateTime(Date())
        if (nowLabel != settings.alarmTimeLabel) return
        if (tickFiredLabel == nowLabel) return
        tickFiredLabel = nowLabel
        ContextCompat.startForegroundService(
            this,
            Intent(this, AlarmService::class.java),
        )
    }

    override fun onTerminate() {
        tickHandler.removeCallbacks(tickRunnable)
        mixerEngine.release()
        super.onTerminate()
    }

    companion object {
        lateinit var instance: NaturesMusicApplication
            private set
    }
}
