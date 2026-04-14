package com.smartisanos.music.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartisanos.music.R
import com.smartisanos.music.ui.components.SmartisanBottomBar
import com.smartisanos.music.ui.navigation.MusicDestination
import com.smartisanos.music.ui.navigation.MusicNavHost

private val ShellBackground = Color(0xFFF7F7F7)

@Composable
fun MusicApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: MusicDestination.Playlist.route
    val crossTextureBrush = rememberCrossTextureBrush()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ShellBackground,
        bottomBar = {
            SmartisanBottomBar(
                currentRoute = currentRoute,
                onDestinationSelected = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(brush = crossTextureBrush)
        ) {
            MusicNavHost(
                navController = navController,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun rememberCrossTextureBrush(): ShaderBrush {
    val texture = ImageBitmap.imageResource(id = R.drawable.ablum_crosstexture_bg)
    return remember(texture) {
        ShaderBrush(
            ImageShader(
                image = texture,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated,
            )
        )
    }
}
