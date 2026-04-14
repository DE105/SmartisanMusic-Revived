package com.smartisanos.music.ui.artist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.smartisanos.music.ui.components.MusicPlaceholderScreen

@Composable
fun ArtistScreen(modifier: Modifier = Modifier) {
    MusicPlaceholderScreen(
        title = "艺术家",
        modifier = modifier,
    )
}
