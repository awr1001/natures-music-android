package com.aaronroberts.naturesmusic.alarm

import android.content.Context
import android.content.Intent
import com.aaronroberts.naturesmusic.playback.MixerEngine
import java.util.Calendar

/**
 * iOS ViewController alarmStart / alarmStop / alarmPause / snooze, for Android.
 * Mix snapshot lives in [AlarmStore]; playback is restored with asAlarm=true.
 */
class AlarmController(
    private val app: Context,
    private val store: AlarmStore,
    private val scheduler: AlarmScheduler,
    private val mixer: MixerEngine,
) {

    /**
     * iOS setAlarmButton: copy the wheel datetime into the Alarm Time pill
     * (medium date + short time). Sounds are not required to update the pill.
     * Always store the wheel year/month/day/hour/minute, phase=Armed, and
     * schedule. Do not roll +1 day.
     *
     * Snapshot the mix if anything is playing (stopAll after). If the mix is
     * empty, still arm the datetime and keep previous mixJson if any, or [].
     *
     * @return false only when there is no mix to wake to (empty snapshot and
     * no previous mix) so the UI can show a start-sounds hint. The datetime
     * is still stored and the pill still changes.
     */
    fun setAlarm(picker: Calendar): Boolean {
        val mix = mixer.snapshotPlaying()
        val mixJson = if (mix.isNotEmpty()) {
            mixer.stopAll()
            AlarmStore.encodeMix(mix)
        } else {
            val previous = store.mixJson
            if (previous.isNotBlank() && previous != "[]") previous else "[]"
        }
        val year = picker.get(Calendar.YEAR)
        val month = picker.get(Calendar.MONTH)
        val day = picker.get(Calendar.DAY_OF_MONTH)
        val hour = picker.get(Calendar.HOUR_OF_DAY)
        val minute = picker.get(Calendar.MINUTE)
        scheduler.schedule(year, month, day, hour, minute)
        store.update {
            it.copy(
                enabled = true,
                phase = AlarmPhase.Armed,
                year = year,
                month = month,
                day = day,
                hour = hour,
                minute = minute,
                mixJson = mixJson,
            )
        }
        return mixJson != "[]"
    }

    /** iOS alarmStart: restore mix and mark the pill Alarm Active. */
    @Synchronized
    fun alarmStart() {
        if (store.phase == AlarmPhase.Ringing) return
        scheduler.cancel()
        mixer.restore(store.mix, asAlarm = true)
        store.update { it.copy(enabled = true, phase = AlarmPhase.Ringing) }
    }

    /** iOS alarmStop: stopAll, clear alarm, pill back to "Alarm". */
    fun alarmStop() {
        mixer.stopAll()
        scheduler.cancel()
        store.update { it.copy(enabled = false, phase = AlarmPhase.Idle) }
        app.stopService(Intent(app, AlarmService::class.java))
    }

    /**
     * iOS snoozeButton / notification snooze: pill "Snoozed...", alarmPause,
     * then alarmStart after snoozeMinutes.
     */
    fun snooze() {
        mixer.stopAll()
        val minutes = store.snoozeMinutes.coerceAtLeast(1)
        val cal = scheduler.scheduleAt(System.currentTimeMillis() + minutes * 60_000L)
        store.update {
            it.copy(
                enabled = true,
                phase = AlarmPhase.Snoozed,
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH),
                day = cal.get(Calendar.DAY_OF_MONTH),
                hour = cal.get(Calendar.HOUR_OF_DAY),
                minute = cal.get(Calendar.MINUTE),
            )
        }
        app.stopService(Intent(app, AlarmService::class.java))
    }

    fun bumpSnooze(plus: Boolean) {
        store.update {
            it.copy(snoozeMinutes = AlarmStore.nextSnoozeMinutes(it.snoozeMinutes, plus))
        }
    }
}
