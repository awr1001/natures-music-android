package com.aaronroberts.naturesmusic.ui.category

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaronroberts.naturesmusic.NaturesMusicApplication
import com.aaronroberts.naturesmusic.data.SoundCatalog
import com.aaronroberts.naturesmusic.data.SoundCategory
import com.aaronroberts.naturesmusic.playback.PlayStatus
import com.aaronroberts.naturesmusic.playback.TrackPlayback
import com.aaronroberts.naturesmusic.ui.components.PillButton
import com.aaronroberts.naturesmusic.ui.components.ScreenScaffold
import com.aaronroberts.naturesmusic.ui.components.TrackMixerRow
import com.aaronroberts.naturesmusic.ui.help.HelpQuestionButton
import com.aaronroberts.naturesmusic.ui.help.SoundPageHelp
import com.aaronroberts.naturesmusic.ui.theme.Ink

@Composable
fun CategoryScreen(
    category: SoundCategory,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val engine = NaturesMusicApplication.instance.mixerEngine
    val snapshot by engine.tracks.collectAsStateWithLifecycle()
    val set = SoundCatalog.tracksFor(category)
    val othersPlaying = snapshot.values.any { playback ->
        playback.status == PlayStatus.Playing &&
            set.none { it.id == playback.trackId }
    }
    var showHelp by remember { mutableStateOf(false) }
    val showDefaultSetting = category != SoundCategory.EXTRA

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(category.imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        ScreenScaffold(
            title = category.title,
            modifier = Modifier.fillMaxSize(),
            useColorBackground = false,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PillButton(
                    label = "Main Menu",
                    onClick = onBack,
                    fillMaxWidth = false,
                    height = 44.dp,
                    modifier = Modifier.width(180.dp),
                )
                HelpQuestionButton(onClick = { showHelp = true })
            }
            if (showDefaultSetting) {
                PillButton(
                    label = "Default Setting",
                    onClick = { engine.applyDefaultSetting(category) },
                    height = 48.dp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            PillButton(
                label = "Stop All Sounds",
                onClick = { engine.stopCategory(category) },
                height = 48.dp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            if (othersPlaying) {
                Text(
                    text = "Other sounds are still in the mix.",
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                set.forEach { track ->
                    val playback = snapshot[track.id] ?: TrackPlayback(
                        track.id,
                        delaySeconds = track.defaultDelaySeconds.coerceAtLeast(1),
                    )
                    TrackMixerRow(
                        name = track.displayName,
                        playback = playback,
                        onToggle = { engine.toggle(track.id) },
                        onVolume = { engine.setVolume(track.id, it) },
                        showDelay = track.defaultDelaySeconds > 0,
                        onDelay = { engine.setDelay(track.id, it) },
                    )
                }
            }
        }
        SoundPageHelp(visible = showHelp, onDismiss = { showHelp = false })
    }
}
