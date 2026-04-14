package com.smartisanos.music.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartisanos.music.R
import com.smartisanos.music.playback.ProvidePlaybackController
import com.smartisanos.music.ui.components.GlobalPlaybackBar
import com.smartisanos.music.ui.components.SmartisanBottomBar
import com.smartisanos.music.ui.components.SmartisanTopBar
import com.smartisanos.music.ui.components.SmartisanTopBarIconButton
import com.smartisanos.music.ui.components.SmartisanTopBarShadow
import com.smartisanos.music.ui.components.SmartisanTopBarTextButton
import com.smartisanos.music.ui.navigation.MusicDestination
import com.smartisanos.music.ui.navigation.MusicNavHost

private val ShellBackground = Color(0xFFF7F7F7)
private val SearchIconSize = 34.dp
private val AlbumTileIconSize = 18.dp

@Composable
fun MusicApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: MusicDestination.Playlist.route
    val currentDestination = MusicDestination.entries.firstOrNull { it.route == currentRoute }
        ?: MusicDestination.Playlist
    val crossTextureBrush = rememberCrossTextureBrush()

    ProvidePlaybackController {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ShellBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    },
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                MusicShellTopBar(destination = currentDestination)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(brush = crossTextureBrush),
                ) {
                    MusicNavHost(
                        navController = navController,
                        modifier = Modifier.fillMaxSize(),
                    )
                    GlobalPlaybackBar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                    SmartisanTopBarShadow(
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicShellTopBar(destination: MusicDestination) {
    val showsEdit = destination == MusicDestination.Playlist ||
        destination == MusicDestination.Album ||
        destination == MusicDestination.Songs
    val showsSearch = destination == MusicDestination.Playlist ||
        destination == MusicDestination.Artist ||
        destination == MusicDestination.Album ||
        destination == MusicDestination.Songs
    val showsTile = destination == MusicDestination.Album

    SmartisanTopBar(
        title = destination.label,
        leftContent = if (showsEdit) {
            {
                SmartisanTopBarTextButton(
                    text = stringResource(R.string.edit),
                )
            }
        } else {
            null
        },
        rightContent = if (showsSearch || showsTile) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (showsTile) {
                        SmartisanTopBarIconButton(
                            iconRes = R.drawable.btn_display_tile2,
                            pressedIconRes = R.drawable.btn_display_tile2_down,
                            contentDescription = stringResource(R.string.listview_header_tile),
                            iconSize = AlbumTileIconSize,
                        )
                    }
                    if (showsSearch) {
                        SmartisanTopBarIconButton(
                            iconRes = R.drawable.search_icon,
                            pressedIconRes = R.drawable.search_icon_down,
                            contentDescription = stringResource(R.string.tab_local_search),
                            iconSize = SearchIconSize,
                        )
                    }
                }
            }
        } else {
            null
        },
    )
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
