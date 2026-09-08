package com.aaronroberts.naturesmusic.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaronroberts.naturesmusic.NaturesMusicApplication
import com.aaronroberts.naturesmusic.ui.components.CircleGlyphButton
import com.aaronroberts.naturesmusic.ui.components.PillButton
import com.aaronroberts.naturesmusic.ui.components.ScreenScaffold
import com.aaronroberts.naturesmusic.ui.help.HelpQuestionButton
import com.aaronroberts.naturesmusic.ui.help.TimerPageHelp
import com.aaronroberts.naturesmusic.ui.theme.ButtonFill
import com.aaronroberts.naturesmusic.ui.theme.Hairline
import com.aaronroberts.naturesmusic.ui.theme.Ink

@Composable
fun TimerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val store = NaturesMusicApplication.instance.sleepTimerStore
    val state by store.state.collectAsStateWithLifecycle()
    val keypadEnabled = !state.running
    var showHelp by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        ScreenScaffold(title = "Timer", modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleGlyphButton(glyph = "‹", onClick = onBack)
                HelpQuestionButton(onClick = { showHelp = true })
            }
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = state.displayTime,
                    color = Ink,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (keypadEnabled) 1f else 0.35f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    KeypadRow(listOf("1", "2", "3"), keypadEnabled) { store.appendDigit(it) }
                    KeypadRow(listOf("4", "5", "6"), keypadEnabled) { store.appendDigit(it) }
                    KeypadRow(listOf("7", "8", "9"), keypadEnabled) { store.appendDigit(it) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        DigitButton(
                            label = "0",
                            onClick = { store.appendDigit(0) },
                            enabled = keypadEnabled,
                            modifier = Modifier.weight(1f),
                        )
                        DigitButton(
                            label = "Delete",
                            onClick = { store.deleteDigit() },
                            enabled = keypadEnabled,
                            modifier = Modifier.weight(2f),
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                if (state.running) {
                    PillButton(label = "Pause", onClick = { store.pause() })
                } else {
                    PillButton(label = "Start", onClick = { store.start() })
                }
                PillButton(label = "Reset", onClick = { store.reset() })
                Spacer(Modifier.height(16.dp))
            }
        }
        TimerPageHelp(visible = showHelp, onDismiss = { showHelp = false })
    }
}

@Composable
private fun KeypadRow(
    labels: List<String>,
    enabled: Boolean,
    onDigit: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        labels.forEach { label ->
            DigitButton(
                label = label,
                onClick = { onDigit(label.toInt()) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private val DigitShape = RoundedCornerShape(50)

@Composable
private fun DigitButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(DigitShape)
            .background(ButtonFill)
            .border(1.5.dp, Hairline, DigitShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
