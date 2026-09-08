package com.aaronroberts.naturesmusic.data

/**
 * Ports iOS `defaultSetting*Sounds` presets from ViewController.swift (~1525–1603).
 * Volumes are 0–1; delaySeconds only applies to random / loop+random tracks.
 */
data class DefaultTrackPreset(
    val trackId: String,
    val volume: Float,
    val delaySeconds: Int? = null,
)

object DefaultSettings {

    fun presetsFor(category: SoundCategory): List<DefaultTrackPreset> = when (category) {
        SoundCategory.OCEAN -> listOf(
            DefaultTrackPreset("ocean_wind", 0.1f),
            DefaultTrackPreset("ocean_seagulls", 0.5f, delaySeconds = 10),
            DefaultTrackPreset("ocean_waves_a", 0.5f),
        )
        SoundCategory.LAKE -> listOf(
            DefaultTrackPreset("lake_wind", 0.05f),
            DefaultTrackPreset("lake_birds", 0.5f, delaySeconds = 10),
            DefaultTrackPreset("lake_waves_b", 0.3f, delaySeconds = 6),
        )
        SoundCategory.RIVER -> listOf(
            DefaultTrackPreset("river_a", 0.7f),
        )
        SoundCategory.STORM -> listOf(
            DefaultTrackPreset("rain_c", 0.6f),
            DefaultTrackPreset("thunder_b", 0.6f, delaySeconds = 22),
        )
        SoundCategory.CITY -> listOf(
            // iOS city2 → Android traffic_a (city2.wav loop)
            DefaultTrackPreset("traffic_a", 0.1f),
            // iOS trafficA → Android traffic_b (highway + cars)
            DefaultTrackPreset("traffic_b", 0.1f, delaySeconds = 20),
            DefaultTrackPreset("trains", 0.75f, delaySeconds = 60),
        )
        SoundCategory.EXTRA -> emptyList() // iOS has Stop All only — no Default Setting
    }
}
