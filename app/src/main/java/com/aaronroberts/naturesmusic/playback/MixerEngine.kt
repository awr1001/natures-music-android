package com.aaronroberts.naturesmusic.playback

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
import com.aaronroberts.naturesmusic.R
import com.aaronroberts.naturesmusic.alarm.AlarmMixer
import com.aaronroberts.naturesmusic.data.PlayStyle
import com.aaronroberts.naturesmusic.data.DefaultSettings
import com.aaronroberts.naturesmusic.data.SoundCatalog
import com.aaronroberts.naturesmusic.data.SoundCategory
import com.aaronroberts.naturesmusic.data.SoundTrack
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PlayStatus { Stopped, Preparing, Playing, Error }

data class TrackPlayback(
    val trackId: String,
    val status: PlayStatus = PlayStatus.Stopped,
    val volume: Float = 0.7f,
    val error: String? = null,
    val delaySeconds: Int = 10,
    val heardVolume: Float = volume,
    val heardDelaySeconds: Int = delaySeconds,
)

/**
 * Mixes looping beds and random one-shots on a dedicated audio thread.
 * Starts [MixerPlaybackService] so sleep-style playback can continue after
 * the activity is backgrounded (Android 8+ foreground-service requirement).
 */
class MixerEngine(private val app: Context) {

    private val audioThread = HandlerThread("natures-music-mixer").apply { start() }
    private val audioHandler = Handler(audioThread.looper)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val players = mutableMapOf<String, MediaPlayer>()
    private val oneShotPlayers = mutableMapOf<String, MediaPlayer>()
    private val randomTasks = mutableMapOf<String, Runnable>()
    private val volumes = mutableMapOf<String, Float>()
    private val lastOneShotRes = mutableMapOf<String, Int>()
    private val firstShotPending = mutableSetOf<String>()
    private var pausedForAlarm = false
    private var pausedIds: Set<String> = emptySet()
    private var hasAudioFocus = false
    private var alarmFocusHeld = false
    @Volatile
    private var restoreForAlarm = false

    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val focusRequest: AudioFocusRequest? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(mediaAttributes)
                .setOnAudioFocusChangeListener { /* keep mixing for sleep use */ }
                .build()
        } else {
            null
        }

    private val _tracks = MutableStateFlow(
        SoundCatalog.tracks.associate {
            it.id to TrackPlayback(
                it.id,
                delaySeconds = it.defaultDelaySeconds.coerceAtLeast(1),
            )
        },
    )
    val tracks: StateFlow<Map<String, TrackPlayback>> = _tracks.asStateFlow()

    val anyPlaying: Boolean
        get() = _tracks.value.values.any { it.status == PlayStatus.Playing || it.status == PlayStatus.Preparing }

    fun play(trackId: String) {
        audioHandler.post { playInternal(trackId) }
    }

    fun stop(trackId: String) {
        audioHandler.post { stopInternal(trackId) }
    }

    fun toggle(trackId: String) {
        audioHandler.post {
            val current = _tracks.value[trackId]
            if (current?.status == PlayStatus.Playing || current?.status == PlayStatus.Preparing) {
                stopInternal(trackId)
            } else {
                playInternal(trackId)
            }
        }
    }

    fun setVolume(trackId: String, volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        volumes[trackId] = clamped
        _tracks.update { map ->
            val existing = map[trackId] ?: TrackPlayback(trackId)
            map + (trackId to existing.copy(volume = clamped, heardVolume = clamped))
        }
        audioHandler.post {
            players[trackId]?.setVolume(clamped, clamped)
            oneShotPlayers[trackId]?.setVolume(clamped, clamped)
        }
    }

    fun setDelay(trackId: String, seconds: Int) {
        val clamped = seconds.coerceIn(1, 90)
        _tracks.update { map ->
            val existing = map[trackId] ?: TrackPlayback(trackId)
            map + (trackId to existing.copy(delaySeconds = clamped, heardDelaySeconds = clamped))
        }
    }

    fun stopAll() {
        restoreForAlarm = false
        mainHandler.removeCallbacks(ensureServiceRunnable)
        audioHandler.post {
            activeTrackIds().forEach { stopInternal(it) }
            mainHandler.post { MixerPlaybackService.stopIfRunning(app) }
        }
    }

    /** Stops every mixer that belongs to [category] (iOS stopAll*Sounds). */
    fun stopCategory(category: SoundCategory) {
        val ids = SoundCatalog.tracksFor(category).map { it.id }.toSet()
        audioHandler.post {
            ids.forEach { stopInternal(it) }
            if (!anyActivePlayers()) {
                mainHandler.post { MixerPlaybackService.stopIfRunning(app) }
            }
        }
    }

    /**
     * Applies iOS defaultSetting*Sounds: set recommended volume/delay, then play.
     * Extra has no default on iOS — no-op.
     */
    fun applyDefaultSetting(category: SoundCategory) {
        val presets = DefaultSettings.presetsFor(category)
        if (presets.isEmpty()) return
        audioHandler.post {
            presets.forEach { preset ->
                val volume = preset.volume.coerceIn(0f, 1f)
                volumes[preset.trackId] = volume
                val delay = preset.delaySeconds
                _tracks.update { map ->
                    val existing = map[preset.trackId] ?: TrackPlayback(preset.trackId)
                    val withVol = existing.copy(volume = volume, heardVolume = volume)
                    val next = if (delay != null) {
                        val clamped = delay.coerceIn(1, 90)
                        withVol.copy(delaySeconds = clamped, heardDelaySeconds = clamped)
                    } else {
                        withVol
                    }
                    map + (preset.trackId to next)
                }
                // Restart so an already-playing track picks up new settings (iOS re-fires Play).
                stopInternal(preset.trackId)
                playInternal(preset.trackId)
            }
        }
    }

    fun snapshotPlaying(): List<AlarmMixer> {
        return _tracks.value.values
            .filter { it.status == PlayStatus.Playing || it.status == PlayStatus.Preparing }
            .map { AlarmMixer(id = it.trackId, volume = it.volume, delaySeconds = it.delaySeconds) }
    }

    fun restore(mix: List<AlarmMixer>, asAlarm: Boolean) {
        restoreForAlarm = asAlarm
        mix.forEach { item ->
            setVolume(item.id, item.volume)
            setDelay(item.id, item.delaySeconds)
            play(item.id)
        }
    }

    fun pauseForAlarm() {
        audioHandler.post {
            if (pausedForAlarm) return@post
            pausedIds = _tracks.value.filter { (_, playback) ->
                playback.status == PlayStatus.Playing || playback.status == PlayStatus.Preparing
            }.keys.toSet()
            pausedForAlarm = true
            pausedIds.forEach { cancelRandom(it) }
            (players.values + oneShotPlayers.values).forEach { player ->
                try {
                    if (player.isPlaying) player.pause()
                } catch (_: IllegalStateException) {
                }
            }
        }
    }

    fun resumeAfterAlarm() {
        audioHandler.post {
            if (!pausedForAlarm) return@post
            pausedForAlarm = false
            pausedIds.forEach { id ->
                val track = SoundCatalog.track(id)
                try {
                    players[id]?.start()
                } catch (_: IllegalStateException) {
                    if (track != null && track.style != PlayStyle.Random) {
                        playInternal(id)
                        return@forEach
                    }
                }
                try {
                    oneShotPlayers[id]?.start()
                } catch (_: IllegalStateException) {
                    oneShotPlayers.remove(id)?.release()
                }
                if (track != null &&
                    (track.style == PlayStyle.Random || track.style == PlayStyle.LoopAndRandom) &&
                    oneShotPlayers[id] == null
                ) {
                    scheduleNextOneShot(track)
                }
            }
            pausedIds = emptySet()
            if (anyActivePlayers()) {
                ensureService()
            }
        }
    }

    fun release() {
        audioHandler.post {
            activeTrackIds().forEach { stopInternal(it) }
            audioThread.quitSafely()
        }
    }

    private fun playInternal(trackId: String) {
        val track = SoundCatalog.track(trackId) ?: return
        val current = _tracks.value[trackId]
        if (current?.status == PlayStatus.Playing || current?.status == PlayStatus.Preparing) return

        stopInternal(trackId)
        requestFocus()
        val volume = volumes[trackId] ?: current?.volume ?: 0.7f
        volumes[trackId] = volume
        firstShotPending.add(trackId)
        setState(trackId, PlayStatus.Preparing, volume, null)

        when (track.style) {
            PlayStyle.Loop -> startLoopBed(track, volume)
            PlayStyle.Random -> startRandomOnly(track, volume)
            PlayStyle.LoopAndRandom -> {
                startLoopBed(track, volume)
                playOneShot(track.id)
            }
        }
    }

    private fun startLoopBed(track: SoundTrack, volume: Float) {
        val player = try {
            createLoopingPlayer(track.rawResIds.first(), volume)
        } catch (e: Exception) {
            setState(track.id, PlayStatus.Error, volume, e.message ?: app.getString(R.string.error_couldnt_start))
            return
        }

        player.setOnErrorListener { _, what, extra ->
            audioHandler.post {
                setState(track.id, PlayStatus.Error, volume, app.getString(R.string.error_couldnt_start_code, what, extra))
                releasePlayer(track.id)
            }
            true
        }
        players[track.id] = player
        try {
            player.start()
            setState(track.id, PlayStatus.Playing, volume, null)
            ensureService()
        } catch (e: Exception) {
            setState(track.id, PlayStatus.Error, volume, e.message ?: app.getString(R.string.error_couldnt_start))
            releasePlayer(track.id)
        }
    }

    private fun startRandomOnly(track: SoundTrack, volume: Float) {
        setState(track.id, PlayStatus.Playing, volume, null)
        ensureService()
        playOneShot(track.id)
    }

    private fun scheduleNextOneShot(track: SoundTrack, delayMs: Long? = null) {
        cancelRandom(track.id)
        val ids = oneShotIds(track)
        if (ids.isEmpty()) return
        if (!isTrackLive(track.id)) return
        val task = Runnable { playOneShot(track.id) }
        randomTasks[track.id] = task
        audioHandler.postDelayed(task, delayMs ?: randomDelayMs(track.id))
    }

    private fun playOneShot(trackId: String) {
        randomTasks.remove(trackId)
        val track = SoundCatalog.track(trackId) ?: return
        if (!isTrackLive(trackId)) return
        val ids = oneShotIds(track)
        if (ids.isEmpty()) return

        val userVolume = volumes[trackId] ?: _tracks.value[trackId]?.volume ?: 0.7f
        val firstClip = trackId in firstShotPending
        val shotVolume = if (firstClip) userVolume else jitterVolume(userVolume)
        val rawResId = pickOneShotId(ids, lastOneShotRes[trackId])
        lastOneShotRes[trackId] = rawResId

        // One player per track: cut off a still-playing clip if the next gap is due.
        releaseOneShot(trackId)
        val player = try {
            createOneShotPlayer(rawResId, shotVolume)
        } catch (e: Exception) {
            failOneShot(track, trackId, userVolume, e.message ?: app.getString(R.string.error_couldnt_start))
            return
        }

        player.setOnCompletionListener {
            audioHandler.post {
                if (oneShotPlayers[trackId] === player) {
                    releaseOneShot(trackId)
                } else {
                    releaseQuietly(player)
                }
            }
        }
        player.setOnErrorListener { _, _, _ ->
            audioHandler.post {
                releaseOneShot(trackId)
                if (isTrackLive(trackId)) {
                    scheduleNextOneShot(track)
                }
            }
            true
        }
        oneShotPlayers[trackId] = player
        try {
            player.start()
            firstShotPending.remove(trackId)
            val nextDelayMs = randomDelayMs(trackId)
            val heardDelay = ((nextDelayMs + 500L) / 1000L).toInt().coerceIn(1, 90)
            setHeard(trackId, shotVolume, heardDelay)
            scheduleNextOneShot(track, nextDelayMs)
        } catch (e: Exception) {
            releaseOneShot(trackId)
            failOneShot(track, trackId, userVolume, e.message ?: app.getString(R.string.error_couldnt_start))
        }
    }

    private fun failOneShot(track: SoundTrack, trackId: String, userVolume: Float, message: String) {
        if (track.style == PlayStyle.Random && players[trackId] == null) {
            setState(trackId, PlayStatus.Error, userVolume, message)
        } else if (isTrackLive(trackId)) {
            scheduleNextOneShot(track)
        }
    }

    private fun stopInternal(trackId: String) {
        val volume = volumes[trackId] ?: _tracks.value[trackId]?.volume ?: 0.7f
        cancelRandom(trackId)
        releaseOneShot(trackId)
        releasePlayer(trackId)
        firstShotPending.remove(trackId)
        lastOneShotRes.remove(trackId)
        setState(trackId, PlayStatus.Stopped, volume, null)
        if (!anyActivePlayers()) {
            abandonFocus()
        }
    }

    private fun releasePlayer(trackId: String) {
        players.remove(trackId)?.let { releaseQuietly(it) }
    }

    private fun releaseOneShot(trackId: String) {
        oneShotPlayers.remove(trackId)?.let { releaseQuietly(it) }
    }

    private fun cancelRandom(trackId: String) {
        randomTasks.remove(trackId)?.let { audioHandler.removeCallbacks(it) }
    }

    private fun releaseQuietly(player: MediaPlayer) {
        try {
            if (player.isPlaying) player.stop()
        } catch (_: Exception) {
        }
        try {
            player.release()
        } catch (_: Exception) {
        }
    }

    private fun createLoopingPlayer(@RawRes rawResId: Int, volume: Float): MediaPlayer {
        return createPlayer(rawResId, volume, looping = true)
    }

    private fun createOneShotPlayer(@RawRes rawResId: Int, volume: Float): MediaPlayer {
        return createPlayer(rawResId, volume, looping = false)
    }

    /**
     * Builds a player that is already prepared. The AssetFileDescriptor must stay
     * open through [MediaPlayer.prepare] — closing it after [MediaPlayer.setDataSource]
     * is why short one-shots never fired.
     */
    private fun createPlayer(@RawRes rawResId: Int, volume: Float, looping: Boolean): MediaPlayer {
        val player = MediaPlayer()
        val afd = app.resources.openRawResourceFd(rawResId)
            ?: throw IllegalStateException("Missing raw resource $rawResId")
        try {
            player.setAudioAttributes(if (restoreForAlarm) alarmAttributes else mediaAttributes)
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            player.isLooping = looping
            player.setVolume(volume, volume)
            player.prepare()
        } catch (e: Exception) {
            releaseQuietly(player)
            throw e
        } finally {
            try {
                afd.close()
            } catch (_: Exception) {
            }
        }
        return player
    }

    private fun oneShotIds(track: SoundTrack): List<Int> = when (track.style) {
        PlayStyle.Loop -> emptyList()
        PlayStyle.Random -> track.rawResIds
        PlayStyle.LoopAndRandom -> track.rawResIds.drop(1)
    }

    private fun isTrackLive(trackId: String): Boolean {
        val status = _tracks.value[trackId]?.status
        return status == PlayStatus.Playing || status == PlayStatus.Preparing
    }

    private fun randomDelayMs(trackId: String): Long {
        val delaySeconds = (_tracks.value[trackId]?.delaySeconds ?: 10).coerceIn(1, 90)
        val factor = 0.75 + Random.nextDouble() * 0.50
        return (delaySeconds * 1_000L * factor).toLong().coerceAtLeast(1_000L)
    }

    private fun jitterVolume(volume: Float): Float {
        val factor = 0.75f + Random.nextFloat() * 0.50f
        return (volume * factor).coerceIn(0f, 1f)
    }

    private fun activeTrackIds(): List<String> {
        val live = _tracks.value.filter { (_, playback) ->
            playback.status == PlayStatus.Playing || playback.status == PlayStatus.Preparing
        }.keys
        return (players.keys + oneShotPlayers.keys + randomTasks.keys + live).toSet().toList()
    }

    private fun anyActivePlayers(): Boolean =
        players.isNotEmpty() || oneShotPlayers.isNotEmpty() || randomTasks.isNotEmpty()

    private fun pickOneShotId(ids: List<Int>, lastId: Int?): Int {
        if (ids.size <= 1) return ids.first()
        val choices = if (lastId != null) ids.filter { it != lastId } else ids
        return choices[Random.nextInt(choices.size)]
    }

    private fun setHeard(trackId: String, heardVolume: Float, heardDelaySeconds: Int) {
        _tracks.update { map ->
            val existing = map[trackId] ?: TrackPlayback(trackId)
            map + (
                trackId to existing.copy(
                    heardVolume = heardVolume,
                    heardDelaySeconds = heardDelaySeconds,
                )
            )
        }
    }

    private fun setState(trackId: String, status: PlayStatus, volume: Float, error: String?) {
        _tracks.update { map ->
            val existing = map[trackId] ?: TrackPlayback(trackId)
            val heardVol = if (status == PlayStatus.Stopped) volume else existing.heardVolume
            val heardDelay = if (status == PlayStatus.Stopped) existing.delaySeconds else existing.heardDelaySeconds
            map + (
                trackId to existing.copy(
                    status = status,
                    volume = volume,
                    error = error,
                    heardVolume = heardVol,
                    heardDelaySeconds = heardDelay,
                )
            )
        }
    }

    private val ensureServiceRunnable = Runnable {
        if (!anyPlaying) return@Runnable
        val intent = Intent(app, MixerPlaybackService::class.java)
        ContextCompat.startForegroundService(app, intent)
    }

    private fun ensureService() {
        mainHandler.removeCallbacks(ensureServiceRunnable)
        mainHandler.post(ensureServiceRunnable)
    }

    private fun requestFocus() {
        if (hasAudioFocus) return
        val result = if (restoreForAlarm) {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && focusRequest != null) {
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            )
        }
        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        alarmFocusHeld = hasAudioFocus && restoreForAlarm
    }

    private fun abandonFocus() {
        if (!hasAudioFocus) return
        if (alarmFocusHeld || Build.VERSION.SDK_INT < Build.VERSION_CODES.O || focusRequest == null) {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        } else {
            audioManager.abandonAudioFocusRequest(focusRequest)
        }
        hasAudioFocus = false
        alarmFocusHeld = false
    }

    companion object {
        private val mediaAttributes: AudioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

        private val alarmAttributes: AudioAttributes =
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setLegacyStreamType(AudioManager.STREAM_ALARM)
                .build()
    }
}
