package com.smartisanos.music.ui.songs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.smartisanos.music.R
import com.smartisanos.music.ui.components.SmartisanBlankState

@Composable
fun SongsScreen(modifier: Modifier = Modifier) {
    SmartisanBlankState(
        iconRes = R.drawable.blank_song,
        title = stringResource(R.string.no_song),
        subtitle = stringResource(R.string.show_song),
        modifier = modifier,
    )
}
