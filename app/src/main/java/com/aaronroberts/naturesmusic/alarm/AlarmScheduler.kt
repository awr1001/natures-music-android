package com.aaronroberts.naturesmusic.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedule AlarmManager at the picker's datetime. Does **not** roll +1 day.
     * If that instant is already past but still the same displayed minute (or
     * within ~60s), fire 1 second from now. If clearly in the past, do not
     * schedule — iOS would not fire either.
     */
    fun schedule(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar {
        return schedule(pickerCalendar(year, month, day, hour, minute))
    }

    fun schedule(trigger: Calendar): Calendar {
        val cal = trigger.clone() as Calendar
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val backupAt = backupTriggerMillis(cal.timeInMillis, System.currentTimeMillis())
        if (backupAt != null) {
            setClock(backupAt)
        } else {
            cancel()
        }
        return cal
    }

    fun scheduleAt(timeInMillis: Long): Calendar {
        var trigger = timeInMillis
        val now = System.currentTimeMillis()
        if (trigger <= now) {
            trigger = now + 1_000L
        }
        setClock(trigger)
        return Calendar.getInstance().apply { this.timeInMillis = trigger }
    }

    fun cancel() {
        alarmManager.cancel(operation())
    }

    private fun setClock(triggerAtMillis: Long) {
        val op = operation()
        val show = PendingIntent.getActivity(
            context,
            SHOW_REQUEST,
            Intent(context, com.aaronroberts.naturesmusic.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val info = AlarmManager.AlarmClockInfo(triggerAtMillis, show)
        try {
            alarmManager.setAlarmClock(info, op)
        } catch (_: SecurityException) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    op,
                )
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    op,
                )
            }
        }
    }

    private fun operation(): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_FIRE)
        return PendingIntent.getBroadcast(
            context,
            FIRE_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        private const val FIRE_REQUEST = 1001
        private const val SHOW_REQUEST = 1002
        const val SAME_MINUTE_GRACE_MS = 60_000L

        fun pickerCalendar(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            now: Calendar = Calendar.getInstance(),
        ): Calendar {
            val cal = now.clone() as Calendar
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal
        }

        /** Kept name: picker fields as chosen, never rolled +1 day. */
        fun triggerCalendar(
            year: Int,
            month: Int,
            day: Int,
            hour: Int,
            minute: Int,
            now: Calendar = Calendar.getInstance(),
        ): Calendar = pickerCalendar(year, month, day, hour, minute, now)

        /**
         * AlarmManager backup instant, or null if the picker is clearly in the past
         * (different minute/day) — do not secretly jump a day.
         */
        fun backupTriggerMillis(pickerMillis: Long, nowMillis: Long): Long? {
            val picker = Calendar.getInstance().apply { timeInMillis = pickerMillis }
            picker.set(Calendar.SECOND, 0)
            picker.set(Calendar.MILLISECOND, 0)
            val aligned = picker.timeInMillis
            if (aligned > nowMillis) return aligned
            val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val sameMinute =
                picker.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    picker.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) &&
                    picker.get(Calendar.HOUR_OF_DAY) == now.get(Calendar.HOUR_OF_DAY) &&
                    picker.get(Calendar.MINUTE) == now.get(Calendar.MINUTE)
            if (sameMinute || nowMillis - aligned <= SAME_MINUTE_GRACE_MS) {
                return nowMillis + 1_000L
            }
            return null
        }
    }
}
