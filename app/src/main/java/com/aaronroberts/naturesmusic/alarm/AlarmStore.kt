package com.aaronroberts.naturesmusic.alarm

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

enum class AlarmPhase {
    Idle,
    Armed,
    Ringing,
    Snoozed,
}

data class AlarmMixer(
    val id: String,
    val volume: Float,
    val delaySeconds: Int,
)

data class AlarmSettings(
    val enabled: Boolean = false,
    val phase: AlarmPhase = AlarmPhase.Idle,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int = 7,
    val minute: Int = 0,
    val mixJson: String = "[]",
    val snoozeMinutes: Int = 5,
) {
    val mix: List<AlarmMixer> get() = AlarmStore.decodeMix(mixJson)

    val alarmTimeLabel: String
        get() = AlarmStore.formatDateTime(year, month, day, hour, minute)
}

class AlarmStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AlarmSettings> = _settings.asStateFlow()

    val isEnabled: Boolean get() = _settings.value.enabled
    val phase: AlarmPhase get() = _settings.value.phase
    val year: Int get() = _settings.value.year
    val month: Int get() = _settings.value.month
    val day: Int get() = _settings.value.day
    val hour: Int get() = _settings.value.hour
    val minute: Int get() = _settings.value.minute
    val mixJson: String get() = _settings.value.mixJson
    val mix: List<AlarmMixer> get() = _settings.value.mix
    val snoozeMinutes: Int get() = _settings.value.snoozeMinutes

    fun update(transform: (AlarmSettings) -> AlarmSettings) {
        val next = transform(_settings.value)
        prefs.edit()
            .putBoolean(KEY_ENABLED, next.enabled)
            .putString(KEY_PHASE, next.phase.name)
            .putInt(KEY_YEAR, next.year)
            .putInt(KEY_MONTH, next.month)
            .putInt(KEY_DAY, next.day)
            .putInt(KEY_HOUR, next.hour)
            .putInt(KEY_MINUTE, next.minute)
            .putString(KEY_MIX, next.mixJson)
            .putInt(KEY_SNOOZE, next.snoozeMinutes)
            .apply()
        _settings.value = next
    }

    private fun read(): AlarmSettings {
        val now = Calendar.getInstance()
        val phaseName = prefs.getString(KEY_PHASE, null)
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        val phase = try {
            if (phaseName != null) AlarmPhase.valueOf(phaseName)
            else if (enabled) AlarmPhase.Armed else AlarmPhase.Idle
        } catch (_: Exception) {
            if (enabled) AlarmPhase.Armed else AlarmPhase.Idle
        }
        return AlarmSettings(
            enabled = enabled,
            phase = phase,
            year = prefs.getInt(KEY_YEAR, now.get(Calendar.YEAR)),
            month = prefs.getInt(KEY_MONTH, now.get(Calendar.MONTH)),
            day = prefs.getInt(KEY_DAY, now.get(Calendar.DAY_OF_MONTH)),
            hour = prefs.getInt(KEY_HOUR, 7),
            minute = prefs.getInt(KEY_MINUTE, 0),
            mixJson = prefs.getString(KEY_MIX, "[]") ?: "[]",
            snoozeMinutes = prefs.getInt(KEY_SNOOZE, 5).coerceAtLeast(1),
        )
    }

    companion object {
        private const val PREFS = "natures_music_alarm"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PHASE = "phase"
        private const val KEY_YEAR = "year"
        private const val KEY_MONTH = "month"
        private const val KEY_DAY = "day"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"
        private const val KEY_MIX = "mix"
        private const val KEY_SNOOZE = "snooze_minutes"

        fun encodeMix(mix: List<AlarmMixer>): String {
            val arr = JSONArray()
            mix.forEach { item ->
                arr.put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("volume", item.volume.toDouble())
                        put("delaySeconds", item.delaySeconds)
                    },
                )
            }
            return arr.toString()
        }

        fun decodeMix(json: String): List<AlarmMixer> {
            if (json.isBlank()) return emptyList()
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).map { i ->
                    val obj = arr.getJSONObject(i)
                    AlarmMixer(
                        id = obj.getString("id"),
                        volume = obj.getDouble("volume").toFloat(),
                        delaySeconds = obj.getInt("delaySeconds"),
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        /**
         * iOS DateFormatter.localizedString medium date + short time.
         * Minute precision (seconds zeroed) so Current Time == Alarm Time
         * matches iOS updateTime() string compare.
         */
        fun formatDateTime(date: Date): String {
            val cal = Calendar.getInstance().apply { time = date }
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(cal.time)
        }

        fun formatDateTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): String {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, day)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return formatDateTime(cal.time)
        }

        fun copyFields(cal: Calendar): AlarmSettings.() -> AlarmSettings = {
            copy(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH),
                day = cal.get(Calendar.DAY_OF_MONTH),
                hour = cal.get(Calendar.HOUR_OF_DAY),
                minute = cal.get(Calendar.MINUTE),
            )
        }

        /** iOS snooze +/−: +1 until 5 then +5; −1 under 6 then −5; min 1. */
        fun nextSnoozeMinutes(current: Int, plus: Boolean): Int {
            val value = current.coerceAtLeast(1)
            return if (plus) {
                if (value < 5) value + 1 else value + 5
            } else {
                when {
                    value <= 1 -> 1
                    value < 6 -> value - 1
                    else -> value - 5
                }
            }
        }
    }
}
