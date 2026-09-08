package com.aaronroberts.naturesmusic.alarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aaronroberts.naturesmusic.NaturesMusicApplication
import com.aaronroberts.naturesmusic.ui.components.PillButton
import com.aaronroberts.naturesmusic.ui.theme.AppGray
import com.aaronroberts.naturesmusic.ui.theme.Ink
import com.aaronroberts.naturesmusic.ui.theme.NaturesMusicTheme

class AlarmRingingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = NaturesMusicApplication.instance.alarmStore
        val sound = AlarmService.mixLabel(store.mix)
        setContent {
            NaturesMusicTheme {
                val settings by store.settings.collectAsStateWithLifecycle()
                LaunchedEffect(settings.phase) {
                    if (settings.phase == AlarmPhase.Idle || settings.phase == AlarmPhase.Snoozed) {
                        finish()
                    }
                }
                AlarmRingingScreen(
                    time = settings.alarmTimeLabel,
                    sound = sound,
                    onDismiss = {
                        startService(
                            Intent(this, AlarmService::class.java)
                                .setAction(AlarmService.ACTION_DISMISS),
                        )
                    },
                    onSnooze = {
                        startService(
                            Intent(this, AlarmService::class.java)
                                .setAction(AlarmService.ACTION_SNOOZE),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun AlarmRingingScreen(
    time: String,
    sound: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppGray)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Alarm Active",
            color = Ink,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = time,
            color = Ink,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        Text(
            text = sound,
            color = Ink,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 36.dp),
        )
        PillButton(label = "Snooze", onClick = onSnooze)
        Spacer(Modifier.height(12.dp))
        PillButton(label = "Dismiss", onClick = onDismiss)
    }
}
