package com.aaronroberts.naturesmusic.ui

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aaronroberts.naturesmusic.data.SoundCatalog
import com.aaronroberts.naturesmusic.ui.alarm.AlarmScreen
import com.aaronroberts.naturesmusic.ui.category.CategoryScreen
import com.aaronroberts.naturesmusic.ui.home.HomeScreen
import com.aaronroberts.naturesmusic.ui.timer.TimerScreen

@Composable
fun NaturesMusicApp() {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = "home",
        modifier = Modifier.safeDrawingPadding(),
    ) {
        composable("home") {
            HomeScreen(
                onOpenCategory = { id -> nav.navigate("category/$id") },
                onOpenTimer = { nav.navigate("timer") },
                onOpenAlarm = { nav.navigate("alarm") },
            )
        }
        composable(
            route = "category/{categoryId}",
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("categoryId").orEmpty()
            val category = SoundCatalog.category(id)
            if (category == null) {
                LaunchedEffect(id) { nav.popBackStack() }
            } else {
                CategoryScreen(category = category, onBack = { nav.popBackStack() })
            }
        }
        composable("timer") {
            TimerScreen(onBack = { nav.popBackStack() })
        }
        composable("alarm") {
            AlarmScreen(onBack = { nav.popBackStack() })
        }
    }
}
