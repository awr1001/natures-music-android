package com.aaronroberts.naturesmusic.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaronroberts.naturesmusic.R
import com.aaronroberts.naturesmusic.playback.PlayStatus
import com.aaronroberts.naturesmusic.playback.TrackPlayback
import com.aaronroberts.naturesmusic.ui.theme.Hairline
import com.aaronroberts.naturesmusic.ui.theme.Ink
import kotlin.math.roundToInt

@Composable
fun TrackMixerRow(
    name: String,
    playback: TrackPlayback,
    onToggle: () -> Unit,
    onVolume: (Float) -> Unit,
    modifier: Modifier = Modifier,
    showDelay: Boolean = false,
    onDelay: (Int) -> Unit = {},
) {
    val stopLabel = stringResource(R.string.stop)
    val startingLabel = stringResource(R.string.starting)
    val tryAgainLabel = stringResource(R.string.try_again)
    val playLabel = stringResource(R.string.play)
    val label = when (playback.status) {
        PlayStatus.Playing -> stopLabel
        PlayStatus.Preparing -> startingLabel
        PlayStatus.Error -> tryAgainLabel
        PlayStatus.Stopped -> playLabel
    }
    val live = playback.status == PlayStatus.Playing || playback.status == PlayStatus.Preparing
    val shownVolume = if (live) playback.heardVolume else playback.volume
    val shownDelay = if (live) playback.heardDelaySeconds else playback.delaySeconds
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = name,
            color = Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = stringResource(R.string.volume_percent, (shownVolume * 100).roundToInt()),
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        HairlineSlider(
            value = shownVolume,
            onValueChange = onVolume,
            modifier = Modifier.fillMaxWidth(),
        )
        if (showDelay) {
            Text(
                text = stringResource(R.string.delay_seconds, shownDelay),
                color = Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
            )
            HairlineSlider(
                value = (shownDelay - 1) / 89f,
                onValueChange = { onDelay((it * 89f).roundToInt() + 1) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        PillButton(
            label = label,
            onClick = onToggle,
            enabled = playback.status != PlayStatus.Preparing,
            height = 48.dp,
            modifier = Modifier.padding(top = 10.dp),
        )
        playback.error?.let {
            Text(
                text = it,
                color = Ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
fun HairlineSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf(false) }
    var last by remember { mutableFloatStateOf(value) }
    val shown = if (dragging) last else value
    fun apply(x: Float, width: Float) {
        val next = (x / width).coerceIn(0f, 1f)
        last = next
        onValueChange(next)
    }
    Canvas(
        modifier = modifier
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset -> apply(offset.x, size.width.toFloat()) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { _ -> dragging = true },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                ) { change, _ ->
                    apply(change.position.x, size.width.toFloat())
                }
            },
    ) {
        val trackH = 4.dp.toPx()
        val y = (size.height - trackH) / 2f
        drawRoundRect(
            color = Hairline,
            topLeft = Offset(0f, y),
            size = Size(size.width, trackH),
            cornerRadius = CornerRadius(trackH / 2f),
        )
        val thumbR = 8.dp.toPx()
        val cx = (shown.coerceIn(0f, 1f) * size.width).coerceIn(thumbR, size.width - thumbR)
        drawCircle(color = Ink, radius = thumbR, center = Offset(cx, size.height / 2f))
    }
}
