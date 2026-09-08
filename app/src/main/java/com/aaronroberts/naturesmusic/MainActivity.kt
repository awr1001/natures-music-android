package com.aaronroberts.naturesmusic

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aaronroberts.naturesmusic.ui.NaturesMusicApp
import com.aaronroberts.naturesmusic.ui.theme.NaturesMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bar = AndroidColor.parseColor("#D6D6D6")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(bar, bar),
            navigationBarStyle = SystemBarStyle.light(bar, bar),
        )
        setContent {
            NaturesMusicTheme {
                NaturesMusicApp()
            }
        }
    }
}
