package com.aaronroberts.naturesmusic.timer

import android.os.Handler
import android.os.Looper
import com.aaronroberts.naturesmusic.playback.MixerEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SleepTimerState(
    val remainingSeconds: Int = 0,
    val running: Boolean = false,
    val digits: String = "",
) {
    val displayTime: String
        get() = if (running) formatHms(remainingSeconds) else formatDigits(digits)
}

class SleepTimerStore(private val mixerEngine: MixerEngine) {

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            tick()
            if (_state.value.running) {
                mainHandler.postDelayed(this, 1_000)
            }
        }
    }

    fun appendDigit(digit: Int) {
        val current = _state.value
        if (current.running) return
        if (digit !in 0..9) return
        if (current.digits.length >= 6) return
        _state.value = current.copy(digits = current.digits + digit.toString())
    }

    fun deleteDigit() {
        val current = _state.value
        if (current.running) return
        if (current.digits.isEmpty()) return
        _state.value = current.copy(digits = current.digits.dropLast(1))
    }

    fun start() {
        val current = _state.value
        if (current.running) return
        val seconds = digitsToSeconds(current.digits)
        if (seconds <= 0) return
        mainHandler.removeCallbacks(tickRunnable)
        _state.value = current.copy(
            remainingSeconds = seconds,
            running = true,
            digits = secondsToDigits(seconds),
        )
        mainHandler.postDelayed(tickRunnable, 1_000)
    }

    fun pause() {
        val current = _state.value
        if (!current.running) return
        mainHandler.removeCallbacks(tickRunnable)
        _state.value = current.copy(
            running = false,
            digits = secondsToDigits(current.remainingSeconds),
        )
    }

    fun reset() {
        mainHandler.removeCallbacks(tickRunnable)
        _state.value = SleepTimerState()
    }

    private fun tick() {
        val current = _state.value
        if (!current.running) return
        val next = current.remainingSeconds - 1
        if (next <= 0) {
            _state.value = SleepTimerState()
            mixerEngine.stopAll()
        } else {
            _state.value = current.copy(remainingSeconds = next)
        }
    }
}

internal fun digitsToSeconds(digits: String): Int {
    val padded = digits.padStart(6, '0').takeLast(6)
    val hours = padded.substring(0, 2).toInt()
    val minutes = padded.substring(2, 4).toInt().coerceIn(0, 59)
    val seconds = padded.substring(4, 6).toInt().coerceIn(0, 59)
    return hours * 3600 + minutes * 60 + seconds
}

internal fun secondsToDigits(total: Int): String {
    val clamped = total.coerceAtLeast(0)
    val hours = (clamped / 3600).coerceAtMost(99)
    val minutes = (clamped % 3600) / 60
    val seconds = clamped % 60
    return "%02d%02d%02d".format(hours, minutes, seconds).trimStart('0')
}

internal fun formatDigits(digits: String): String {
    val padded = digits.padStart(6, '0').takeLast(6)
    return "${padded.substring(0, 2)}:${padded.substring(2, 4)}:${padded.substring(4, 6)}"
}

internal fun formatHms(total: Int): String {
    val clamped = total.coerceAtLeast(0)
    val hours = (clamped / 3600).coerceAtMost(99)
    val minutes = (clamped % 3600) / 60
    val seconds = clamped % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}
