package com.aaronroberts.naturesmusic.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aaronroberts.naturesmusic.NaturesMusicApplication
import com.aaronroberts.naturesmusic.data.SoundCatalog
import com.aaronroberts.naturesmusic.ui.components.PillButton
import com.aaronroberts.naturesmusic.ui.components.ScreenScaffold
import com.aaronroberts.naturesmusic.ui.help.HelpQuestionButton
import com.aaronroberts.naturesmusic.ui.help.HomePageHelp

@Composable
fun HomeScreen(
    onOpenCategory: (String) -> Unit,
    onOpenTimer: () -> Unit,
    onOpenAlarm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showHelp by remember { mutableStateOf(false) }
    val engine = NaturesMusicApplication.instance.mixerEngine
    Box(modifier = modifier.fillMaxSize()) {
        ScreenScaffold(title = "Nature's Music", modifier = Modifier.fillMaxSize()) {
            HelpQuestionButton(
                onClick = { showHelp = true },
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 10.dp),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SoundCatalog.homeCategories.forEach { category ->
                    PillButton(
                        label = category.title,
                        onClick = { onOpenCategory(category.id) },
                    )
                }
                // iOS order: categories → Alarm → Timer → Stop All Sounds
                PillButton(label = "Alarm", onClick = onOpenAlarm)
                PillButton(label = "Timer", onClick = onOpenTimer)
                PillButton(label = "Stop All Sounds", onClick = { engine.stopAll() })
                Spacer(Modifier.height(16.dp))
            }
        }
        HomePageHelp(visible = showHelp, onDismiss = { showHelp = false })
    }
}
