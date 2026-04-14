package com.smartisanos.music.ui.album

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.smartisanos.music.R
import com.smartisanos.music.ui.components.SmartisanBlankState

@Composable
fun AlbumScreen(modifier: Modifier = Modifier) {
    SmartisanBlankState(
        iconRes = R.drawable.blank_album,
        title = stringResource(R.string.no_album),
        subtitle = stringResource(R.string.show_album),
        modifier = modifier,
    )
}
