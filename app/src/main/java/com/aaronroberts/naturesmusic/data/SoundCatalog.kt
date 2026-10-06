package com.aaronroberts.naturesmusic.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.RawRes
import com.aaronroberts.naturesmusic.R

enum class SoundCategory(
    val id: String,
    @StringRes val titleRes: Int,
    @DrawableRes val imageRes: Int,
) {
    OCEAN("ocean", R.string.category_ocean, R.drawable.category_ocean),
    LAKE("lake", R.string.category_lake, R.drawable.category_lake),
    RIVER("river", R.string.category_river, R.drawable.category_river),
    STORM("storm", R.string.category_storm, R.drawable.category_storm),
    CITY("city", R.string.category_city, R.drawable.category_city),
    EXTRA("extra", R.string.category_extra, R.drawable.category_extra),
}

enum class PlayStyle { Loop, Random, LoopAndRandom }

data class SoundTrack(
    val id: String,
    @StringRes val displayNameRes: Int,
    val category: SoundCategory,
    @RawRes val rawResIds: List<Int>,
    val style: PlayStyle,
    val defaultDelaySeconds: Int = 0,
    val replaceHint: String = "",
) {
    @get:RawRes
    val rawResId: Int get() = rawResIds.first()
}

object SoundCatalog {

    val homeCategories: List<SoundCategory> = listOf(
        SoundCategory.OCEAN,
        SoundCategory.LAKE,
        SoundCategory.RIVER,
        SoundCategory.STORM,
        SoundCategory.CITY,
        SoundCategory.EXTRA,
    )

    private val oceanSeagulls = listOf(
        R.raw.seagulls1,
        R.raw.seagulls2,
        R.raw.seagulls3,
        R.raw.seagulls4,
        R.raw.seagulls6,
        R.raw.seagulls7,
        R.raw.babyseagulls1,
        R.raw.babyseagulls2,
        R.raw.babyseagulls3,
    )

    private val lakeBirds = listOf(
        R.raw.bird1,
        R.raw.bird2,
        R.raw.bird3,
        R.raw.bird4l,
        R.raw.bird5l,
        R.raw.bird5r,
        R.raw.tweet1,
        R.raw.tweet2l,
        R.raw.tweet3r,
        R.raw.tweet4,
    )

    private val lakeLoons = listOf(R.raw.loon1, R.raw.loon2, R.raw.loon3)

    private val lakeWavesA = listOf(
        R.raw.waves1, R.raw.waves2, R.raw.waves3, R.raw.waves4, R.raw.waves5,
        R.raw.waves6, R.raw.waves7, R.raw.waves8, R.raw.waves9, R.raw.waves10,
        R.raw.waves11,
    )

    private val lakeWavesB = listOf(
        R.raw.lwaves1, R.raw.lwaves2, R.raw.lwaves3, R.raw.lwaves4, R.raw.lwaves5,
        R.raw.lwaves6, R.raw.lwaves7, R.raw.lwaves8, R.raw.lwaves9, R.raw.lwaves10,
    )

    private val lakeWavesC = listOf(
        R.raw.swaves1, R.raw.swaves2, R.raw.swaves3, R.raw.swaves4, R.raw.swaves5,
        R.raw.swaves6, R.raw.swaves7, R.raw.swaves8, R.raw.swaves9, R.raw.swaves10,
        R.raw.swaves11, R.raw.swaves12, R.raw.swaves13, R.raw.swaves14, R.raw.swaves15,
    )

    private val thunderA = listOf(
        R.raw.dthunder1, R.raw.dthunder2, R.raw.dthunder3, R.raw.dthunder4, R.raw.dthunder5,
        R.raw.dthunder6, R.raw.dthunder7, R.raw.dthunder8, R.raw.dthunder9, R.raw.dthunder10,
        R.raw.dthunder11, R.raw.dthunder12, R.raw.dthunder13, R.raw.dthunder14, R.raw.dthunder15,
    )

    private val thunderB = listOf(
        R.raw.ethunder1, R.raw.ethunder2, R.raw.ethunder3, R.raw.ethunder4, R.raw.ethunder5,
        R.raw.ethunder6, R.raw.ethunder7, R.raw.ethunder8, R.raw.ethunder9, R.raw.ethunder10,
        R.raw.ethunder11, R.raw.ethunder12, R.raw.ethunder13, R.raw.ethunder14, R.raw.ethunder15,
    )

    private val trafficB = listOf(
        R.raw.highwaytraffic,
        R.raw.carslow1, R.raw.carslow2, R.raw.carslow3, R.raw.carslow4, R.raw.carslow5,
        R.raw.carslow6, R.raw.carslow7, R.raw.carslow8, R.raw.carslow9, R.raw.carslow10,
        R.raw.carslow11, R.raw.carslow12, R.raw.carslow13, R.raw.carslow14, R.raw.carslow15,
        R.raw.carslow16, R.raw.carslow17, R.raw.carslow18, R.raw.carslow19, R.raw.carslow20,
        R.raw.truckslow1, R.raw.truckslow2, R.raw.truckslow3, R.raw.truckslow4, R.raw.truckslow5,
        R.raw.truckslow6, R.raw.truckslow7, R.raw.truckslow8, R.raw.truckslow9,
        R.raw.bigrigslow1, R.raw.bigrigslow2, R.raw.bigrigslow3,
    )

    private val trains = listOf(R.raw.train1, R.raw.train2, R.raw.train3)

    private val flies = listOf(
        R.raw.fly1, R.raw.fly2, R.raw.fly3, R.raw.fly4, R.raw.fly5, R.raw.fly6,
    )

    val tracks: List<SoundTrack> = listOf(
        SoundTrack("ocean_wind", R.string.sound_wind, SoundCategory.OCEAN, listOf(R.raw.wind8), PlayStyle.Loop),
        SoundTrack("ocean_seagulls", R.string.sound_seagulls, SoundCategory.OCEAN, oceanSeagulls, PlayStyle.Random, defaultDelaySeconds = 10),
        SoundTrack("ocean_waves_a", R.string.sound_waves_a, SoundCategory.OCEAN, listOf(R.raw.owaves1), PlayStyle.Loop),
        SoundTrack("ocean_waves_b", R.string.sound_waves_b, SoundCategory.OCEAN, listOf(R.raw.owaves2), PlayStyle.Loop),
        SoundTrack("ocean_waves_c", R.string.sound_waves_c, SoundCategory.OCEAN, listOf(R.raw.owaves3), PlayStyle.Loop),
        SoundTrack("ocean_waves_d", R.string.sound_waves_d, SoundCategory.OCEAN, listOf(R.raw.owaves4), PlayStyle.Loop),

        SoundTrack("lake_wind", R.string.sound_wind, SoundCategory.LAKE, listOf(R.raw.swindb), PlayStyle.Loop),
        SoundTrack("lake_birds", R.string.sound_birds, SoundCategory.LAKE, lakeBirds, PlayStyle.Random, defaultDelaySeconds = 10),
        SoundTrack("lake_loons", R.string.sound_loons, SoundCategory.LAKE, lakeLoons, PlayStyle.Random, defaultDelaySeconds = 10),
        SoundTrack("lake_waves_a", R.string.sound_waves_a, SoundCategory.LAKE, lakeWavesA, PlayStyle.Random, defaultDelaySeconds = 10),
        SoundTrack("lake_waves_b", R.string.sound_waves_b, SoundCategory.LAKE, lakeWavesB, PlayStyle.Random, defaultDelaySeconds = 6),
        SoundTrack("lake_waves_c", R.string.sound_waves_c, SoundCategory.LAKE, lakeWavesC, PlayStyle.Random, defaultDelaySeconds = 10),

        SoundTrack("river_a", R.string.sound_river_a, SoundCategory.RIVER, listOf(R.raw.river1), PlayStyle.Loop),
        SoundTrack("river_b", R.string.sound_river_b, SoundCategory.RIVER, listOf(R.raw.river2), PlayStyle.Loop),
        SoundTrack("river_c", R.string.sound_river_c, SoundCategory.RIVER, listOf(R.raw.river3), PlayStyle.Loop),

        SoundTrack("rain_a", R.string.sound_rain_a, SoundCategory.STORM, listOf(R.raw.rain1), PlayStyle.Loop),
        SoundTrack("rain_b", R.string.sound_rain_b, SoundCategory.STORM, listOf(R.raw.rain2), PlayStyle.Loop),
        SoundTrack("rain_c", R.string.sound_rain_c, SoundCategory.STORM, listOf(R.raw.rain3), PlayStyle.Loop),
        SoundTrack("thunder_a", R.string.sound_thunder_a, SoundCategory.STORM, thunderA, PlayStyle.Random, defaultDelaySeconds = 15),
        SoundTrack("thunder_b", R.string.sound_thunder_b, SoundCategory.STORM, thunderB, PlayStyle.Random, defaultDelaySeconds = 22),

        SoundTrack("city_a", R.string.sound_city_a, SoundCategory.CITY, listOf(R.raw.city1), PlayStyle.Loop),
        SoundTrack("traffic_a", R.string.sound_traffic_a, SoundCategory.CITY, listOf(R.raw.city2), PlayStyle.Loop),
        SoundTrack("traffic_b", R.string.sound_traffic_b, SoundCategory.CITY, trafficB, PlayStyle.LoopAndRandom, defaultDelaySeconds = 20),
        SoundTrack("trains", R.string.sound_trains, SoundCategory.CITY, trains, PlayStyle.Random, defaultDelaySeconds = 60),
        SoundTrack("industrial_a", R.string.sound_industrial_a, SoundCategory.CITY, listOf(R.raw.industrial), PlayStyle.Loop),
        SoundTrack("industrial_b", R.string.sound_industrial_b, SoundCategory.CITY, listOf(R.raw.industrial2), PlayStyle.Loop),
        SoundTrack("steampunk", R.string.sound_steam_punk, SoundCategory.CITY, listOf(R.raw.steampunkmachine), PlayStyle.Loop),

        SoundTrack("campfire", R.string.sound_campfire, SoundCategory.EXTRA, listOf(R.raw.campfire1), PlayStyle.Loop),
        SoundTrack("crickets_a", R.string.sound_crickets_a, SoundCategory.EXTRA, listOf(R.raw.cricket1), PlayStyle.Loop),
        SoundTrack("crickets_b", R.string.sound_crickets_b, SoundCategory.EXTRA, listOf(R.raw.crickets), PlayStyle.Loop),
        SoundTrack("fly", R.string.sound_fly, SoundCategory.EXTRA, flies, PlayStyle.Random, defaultDelaySeconds = 10),
        SoundTrack("frogs", R.string.sound_frogs, SoundCategory.EXTRA, listOf(R.raw.frogs), PlayStyle.Loop),
        SoundTrack("bomber", R.string.sound_bomber, SoundCategory.EXTRA, listOf(R.raw.bomber), PlayStyle.Loop),
    )

    val alarmBell = SoundTrack(
        id = "alarm_bell",
        displayNameRes = R.string.sound_bell,
        category = SoundCategory.EXTRA,
        rawResIds = listOf(R.raw.alarm_bell),
        style = PlayStyle.Loop,
        replaceHint = "alarm",
    )

    val alarmChoices: List<SoundTrack> = listOf(alarmBell)

    private val byId = (tracks + alarmBell).associateBy { it.id }

    fun track(id: String): SoundTrack? = byId[id]

    fun tracksFor(category: SoundCategory): List<SoundTrack> =
        tracks.filter { it.category == category }

    fun category(id: String): SoundCategory? = SoundCategory.entries.find { it.id == id }
}
