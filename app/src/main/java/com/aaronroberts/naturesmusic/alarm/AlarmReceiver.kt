package com.aaronroberts.naturesmusic.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val service = Intent(context, AlarmService::class.java)
        ContextCompat.startForegroundService(context, service)
    }

    companion object {
        const val ACTION_FIRE = "com.aaronroberts.naturesmusic.ALARM_FIRE"
    }
}
