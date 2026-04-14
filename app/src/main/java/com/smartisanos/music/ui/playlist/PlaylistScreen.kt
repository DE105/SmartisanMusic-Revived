package com.smartisanos.music.ui.playlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.smartisanos.music.ui.components.MusicPlaceholderScreen

@Composable
fun PlaylistScreen(modifier: Modifier = Modifier) {
    MusicPlaceholderScreen(
        title = "播放列表",
        modifier = modifier,
    )
}
