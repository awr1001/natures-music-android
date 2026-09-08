package com.aaronroberts.naturesmusic.ui.help

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaronroberts.naturesmusic.R
import com.aaronroberts.naturesmusic.ui.components.CircleGlyphButton
import com.aaronroberts.naturesmusic.ui.components.PillButton
import com.aaronroberts.naturesmusic.ui.components.YouTubeLinkButton
import com.aaronroberts.naturesmusic.ui.theme.Ink

private const val DemoYouTubeUrl = "https://www.youtube.com/watch?v=hut-tR-XoO0"

/** One help section: iOS order is bullet text, then optional screenshot crop. */
data class HelpSection(
    val bullet: String,
    val imageResId: Int? = null,
)

@Composable
fun HelpQuestionButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    CircleGlyphButton(glyph = "?", onClick = onClick, modifier = modifier)
}

/** Decorative ??? row matching iOS help chrome (not tappable). */
@Composable
private fun HelpQuestionMarksRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) {
            Text(
                text = "?",
                color = Ink,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun PageHelpOverlay(
    title: String,
    sections: List<HelpSection>,
    dismissLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BackHandler(onBack = onDismiss)
    Box(
        modifier = modifier
            .fillMaxSize()
            // iOS help panels use opaque white + 0.3s alpha fade (AnimatedVisibility).
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            HelpQuestionMarksRow()
            Text(
                text = title,
                color = Ink,
                fontSize = 35.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 24.dp, start = 8.dp, end = 8.dp),
            )
            sections.forEach { section ->
                Text(
                    text = "- ${section.bullet}",
                    color = Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 5.dp, vertical = 8.dp),
                )
                val imageRes = section.imageResId
                if (imageRes != null) {
                    Image(
                        painter = painterResource(imageRes),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .padding(bottom = 16.dp),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Spacer(Modifier.height(12.dp))
                }
            }
            Text(
                text = "* A demonstration video can be viewed by tapping on the YouTube link button below *",
                color = Ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp, start = 4.dp, end = 4.dp),
            )
            YouTubeLinkButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DemoYouTubeUrl))
                    context.startActivity(intent)
                },
            )
            Spacer(Modifier.height(28.dp))
            HelpQuestionMarksRow()
            Spacer(Modifier.height(20.dp))
            // iOS Oval_Button: 195×41, corner 15, black border, dismiss label per page.
            PillButton(
                label = dismissLabel,
                onClick = onDismiss,
                modifier = Modifier.width(195.dp),
                height = 41.dp,
                fillMaxWidth = false,
                cornerRadius = 15.dp,
                fontSize = 25.sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
fun HomePageHelp(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        PageHelpOverlay(
            title = "Main Menu Explained",
            sections = listOf(
                HelpSection(
                    bullet = "Click on the button with the name of the sound category you want to hear to display the sound options.",
                    imageResId = R.drawable.help_menu_1,
                ),
                HelpSection(
                    bullet = "Click on the 'Stop All Sounds' button to stop all sounds playing for the entire app.",
                    imageResId = R.drawable.help_menu_2,
                ),
            ),
            dismissLabel = "Main Menu",
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun SoundPageHelp(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        PageHelpOverlay(
            title = "Sound Settings Explained",
            sections = listOf(
                HelpSection(
                    bullet = "Each sound is played by clicking on the corresponding Play button with the sound name displayed above it. The sound is stopped by clicking the same button, which reads Stop while the mixer is playing.",
                    imageResId = R.drawable.help_sound_1,
                ),
                HelpSection(
                    bullet = "Adjust the volume for each sound by moving the slider to the left or right.",
                    imageResId = R.drawable.help_sound_2,
                ),
                HelpSection(
                    bullet = "Some sounds offer volume range and delay range options. Adjust the range settings by moving the sliders left or right.",
                    imageResId = R.drawable.help_sound_3,
                ),
                // iOS has screenshot crops 4–5; Android crops not present — text-only sections.
                HelpSection(
                    bullet = "Click on the 'Default Setting' button to play a suggested set of sounds at recommended settings.",
                ),
                HelpSection(
                    bullet = "Clicking on the 'Stop All Sounds' button at the top of the screen will stop all the sounds playing for the sound category.",
                ),
            ),
            dismissLabel = "Return",
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun AlarmPageHelp(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        PageHelpOverlay(
            title = "Alarm Controlls Explained",
            sections = listOf(
                HelpSection(
                    bullet = "The 'Current Time' label is displaying the present time to you.",
                    imageResId = R.drawable.help_alarm_1,
                ),
                HelpSection(
                    bullet = "To set the alarm: 1 Play all of the sounds you want to hear at the desired volume and delay settings. 2 Then select the alarm time and date with the 'Time and Date Wheel' at the bottom of the screen. 3 Click on the 'Set Alarm' button. The sounds will stop and the alarm time that you set will be displayed at the 'Alarm Time' label. The sounds and settings are saved and replayed when the alarm time arrives.",
                    imageResId = R.drawable.help_alarm_2,
                ),
                HelpSection(
                    bullet = "Clicking on the 'Snooze Button' will temporarily silence the alarm. The alarm will return after the selected snooze time runs out.",
                    imageResId = R.drawable.help_alarm_3,
                ),
                HelpSection(
                    bullet = "The 'Alarm Time' label will display the alarm time selected. Clicking on the label will stop the alarm sounds and the alarm function.",
                    imageResId = R.drawable.help_alarm_4,
                ),
                HelpSection(
                    bullet = "The 'Time and Date Wheel' is used to select the date and time of the alarm before clicking on 'Set Alarm'.",
                    imageResId = R.drawable.help_alarm_5,
                ),
            ),
            dismissLabel = "Alarm",
            onDismiss = onDismiss,
        )
    }
}

@Composable
fun TimerPageHelp(visible: Boolean, onDismiss: () -> Unit) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        PageHelpOverlay(
            title = "Timer Controlls Explained",
            sections = listOf(
                HelpSection(
                    bullet = "Use the number keys on the keypad to enter the time you want the timer to countdown from.",
                    imageResId = R.drawable.help_timer_1,
                ),
                HelpSection(
                    // Android chrome uses Delete; iOS copy says "back arrow".
                    bullet = "Use the Delete button to erase numbers.",
                    imageResId = R.drawable.help_timer_2,
                ),
                HelpSection(
                    bullet = "Click on the 'Start' button to start the timer. Once the timer reaches zero all of the sounds will be stopped. The 'Pause' button will stop the timer and can be restarted by clicking 'Start' again. Click the 'Reset' button to erase the time.",
                    imageResId = R.drawable.help_timer_3,
                ),
            ),
            dismissLabel = "Timer",
            onDismiss = onDismiss,
        )
    }
}
