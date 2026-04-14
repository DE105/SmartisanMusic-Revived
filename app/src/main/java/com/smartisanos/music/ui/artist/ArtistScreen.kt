package com.smartisanos.music.ui.artist

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.smartisanos.music.R
import com.smartisanos.music.ui.components.SmartisanBlankState

@Composable
fun ArtistScreen(modifier: Modifier = Modifier) {
    SmartisanBlankState(
        iconRes = R.drawable.blank_artist,
        title = stringResource(R.string.no_artist),
        subtitle = stringResource(R.string.show_artist),
        modifier = modifier,
    )
}
