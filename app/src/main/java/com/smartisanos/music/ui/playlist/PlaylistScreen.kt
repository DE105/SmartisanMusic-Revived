package com.smartisanos.music.ui.playlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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

private val PlaylistControlText = Color(0x80000000)
private val PlaylistControlPressedText = Color(0xFF515257)
private val PlaylistRowBackground = Color(0xFFF6F6F6)
private val EmptyTitleColor = Color(0x32000000)
private val EmptySubtitleColor = Color(0x2A000000)
private val EmptyTitleHighlightColor = Color(0x80FFFFFF)
private val EmptySubtitleHighlightColor = Color(0x73FFFFFF)

private val HeaderRowHeight = 45.dp
private val AddButtonIconSize = 27.dp
private val EmptyIconSize = 132.dp
private val EmptyIconAlpha = 0.34f
private val EmptyStateOffsetY = (-56).dp
private val EmptyTextHighlightOffset = 1.dp

private val EmptyTitleStyle = TextStyle(
    fontSize = 23.sp,
    fontWeight = FontWeight.SemiBold,
    color = EmptyTitleColor,
)
private val EmptySubtitleStyle = TextStyle(
    fontSize = 15.sp,
    color = EmptySubtitleColor,
)

@Composable
fun PlaylistScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        NewPlaylistRow()
        PlaylistEmptyState(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@Composable
private fun NewPlaylistRow() {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
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
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.offset(y = EmptyStateOffsetY),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.blank_playlist),
                contentDescription = null,
                modifier = Modifier
                    .size(EmptyIconSize)
                    .alpha(EmptyIconAlpha),
            )
            EngravedHintText(
                text = stringResource(R.string.no_playlist),
                style = EmptyTitleStyle,
                color = EmptyTitleColor,
                highlightColor = EmptyTitleHighlightColor,
                modifier = Modifier.padding(top = 8.dp),
            )
            EngravedHintText(
                text = stringResource(R.string.create_playlist),
                style = EmptySubtitleStyle,
                color = EmptySubtitleColor,
                highlightColor = EmptySubtitleHighlightColor,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun EngravedHintText(
    text: String,
    style: TextStyle,
    color: Color,
    highlightColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Text(
            text = text,
            style = style,
            color = highlightColor,
            modifier = Modifier.offset(y = EmptyTextHighlightOffset),
        )
        Text(
            text = text,
            style = style,
            color = color,
        )
    }
}
