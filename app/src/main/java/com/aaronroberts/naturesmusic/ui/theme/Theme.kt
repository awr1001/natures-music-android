package com.aaronroberts.naturesmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val AppGray = Color(0xFFD6D6D6)
val ButtonFill = Color(0xFFEFEFEF)
val Ink = Color(0xFF111111)
val Hairline = Color(0xFF000000)

/** iOS UIColor.blue / .red / .green on the alarm time pill. */
val AlarmBlue = Color(0xFF0000FF)
val AlarmRed = Color(0xFFFF0000)
val AlarmGreen = Color(0xFF00C800)

/** iOS YouTube help-link pill (~#FF645A). */
val YouTubeRed = Color(0xFFFF645A)

private val Scheme = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    background = AppGray,
    onBackground = Ink,
    surface = AppGray,
    onSurface = Ink,
    secondary = Ink,
    onSecondary = Color.White,
    outline = Hairline,
)

@Composable
fun NaturesMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = MaterialTheme.typography.copy(
            titleLarge = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = Ink,
            ),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = Ink,
            ),
        ),
        content = content,
    )
}
