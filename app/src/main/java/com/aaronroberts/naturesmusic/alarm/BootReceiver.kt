package com.aaronroberts.naturesmusic.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.aaronroberts.naturesmusic.NaturesMusicApplication
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? NaturesMusicApplication ?: return
        val store = app.alarmStore
        when (store.phase) {
            AlarmPhase.Idle -> return
            AlarmPhase.Armed -> {
                app.alarmScheduler.schedule(
                    store.year,
                    store.month,
                    store.day,
                    store.hour,
                    store.minute,
                )
            }
            AlarmPhase.Snoozed -> {
                val target = Calendar.getInstance().apply {
                    set(Calendar.YEAR, store.year)
                    set(Calendar.MONTH, store.month)
                    set(Calendar.DAY_OF_MONTH, store.day)
                    set(Calendar.HOUR_OF_DAY, store.hour)
                    set(Calendar.MINUTE, store.minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                app.alarmScheduler.scheduleAt(target.timeInMillis)
            }
            AlarmPhase.Ringing -> {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, AlarmService::class.java),
                )
            }
        }
    }
}
