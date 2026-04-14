package com.smartisanos.music.ui.playlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.R
import com.smartisanos.music.ui.components.SmartisanBlankState

private val PlaylistControlText = Color(0x80000000)
private val PlaylistControlPressedText = Color(0xFF515257)
private val PlaylistRowBackground = Color(0xFFF6F6F6)

private val HeaderRowHeight = 45.dp
private val AddButtonIconSize = 27.dp

@Composable
fun PlaylistScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        PlaylistEmptyState(
            modifier = Modifier
                .fillMaxSize(),
        )
        NewPlaylistRow(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun NewPlaylistRow(modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .height(HeaderRowHeight)
            .background(PlaylistRowBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
            ),
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(
                    if (pressed) R.drawable.btn_add_down else R.drawable.btn_add
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(AddButtonIconSize)
                    .alpha(if (pressed) 1f else 0.72f),
            )
            Text(
                text = stringResource(R.string.new_playlist),
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = if (pressed) PlaylistControlPressedText else PlaylistControlText,
            )
        }
    }
}

@Composable
private fun PlaylistEmptyState(modifier: Modifier = Modifier) {
    SmartisanBlankState(
        iconRes = R.drawable.blank_playlist,
        title = stringResource(R.string.no_playlist),
        subtitle = stringResource(R.string.create_playlist),
        modifier = modifier,
    )
}
