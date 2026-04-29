package com.smartisanos.music.ui.playback

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.playback.EmbeddedLyrics
import kotlin.math.abs

// Reverse resources:
// - lrc_layout.xml: full-height ListView with 80dp vertical fading edges
// - lrc_item_layout.xml: text_size_lryric=15sp and lineSpacingExtra=6dp
// - values-xxhdpi-v4/dimens.xml: lrc_horizontal_padding=53.599976dp
private val PlaybackLyricsPrimaryStyle = TextStyle(
    fontSize = 15.sp,
    fontWeight = FontWeight.Medium,
    color = Color(0xFF4A69B3),
    textAlign = TextAlign.Center,
)
private val PlaybackLyricsSecondaryStyle = TextStyle(
    fontSize = 15.sp,
    color = Color(0xFF4F5050),
    textAlign = TextAlign.Center,
)
private val PlaybackMoreActionTitleStyle = TextStyle(
    fontSize = 15.sp,
    color = Color(0x99000000),
    textAlign = TextAlign.Center,
)
private val PlaybackMoreActionButtonStyle = TextStyle(
    fontSize = 11.sp,
    color = Color(0x99000000),
    textAlign = TextAlign.Center,
)
private val PlaybackMoreActionSelectedColor = Color(0xFF5C8FE8)
private val PlaybackMoreActionDividerColor = Color(0xFFE0E0E0)
private val PlaybackMoreActionTitleHeight = 51.dp
private val PlaybackMoreActionRowHeight = 72.dp
private val PlaybackMoreActionIconSize = 24.dp
private val PlaybackMoreActionCancelWidth = 54.dp
private val PlaybackMoreActionCancelHeight = 32.dp
private val PlaybackLyricsHorizontalPadding = 53.6.dp
private val PlaybackLyricsLineSpacing = 4.dp
private val PlaybackLyricsRowHeight = 24.dp
private val PlaybackLyricsParagraphGap = 16.dp

@Composable
internal fun PlaybackBottomControls(
    width: Dp,
    bottomInset: Dp,
    state: PlaybackScreenState,
    onRepeatClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .width(width)
                .height(186.dp)
                .padding(bottom = 19.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PlaybackControlButtons(
                isPlaying = state.isPlaying,
                repeatMode = state.repeatMode,
                shuffleEnabled = state.shuffleEnabled,
                scale = width.value / 360f,
                onRepeatClick = onRepeatClick,
                onPreviousClick = onPreviousClick,
                onPlayPauseClick = onPlayPauseClick,
                onNextClick = onNextClick,
                onShuffleClick = onShuffleClick,
            )
            PlaybackVolumeBar(
                modifier = Modifier.padding(top = 18.dp),
                width = width,
                value = state.volume.coerceIn(0f, 1f),
                onValueChange = onVolumeChange,
            )
        }
        Spacer(modifier = Modifier.height((14.dp + bottomInset).coerceAtLeast(16.dp)))
    }
}

@Composable
internal fun PlaybackLyricsOverlay(
    mediaId: String?,
    lyrics: EmbeddedLyrics?,
    fallbackLines: List<String>,
    currentPositionMs: Long,
    modifier: Modifier = Modifier,
) {
    val lyricsTimingKey = if (lyrics?.isTimeSynced == true) currentPositionMs else Long.MIN_VALUE
    val renderModel = remember(lyrics, fallbackLines, lyricsTimingKey) {
        buildPlaybackLyricsRenderModel(
            lyrics = lyrics,
            fallbackLines = fallbackLines,
            currentPositionMs = currentPositionMs,
        )
    }

    key(mediaId, lyrics, fallbackLines) {
        val listState = rememberLazyListState()

        BoxWithConstraints(modifier = modifier) {
            val centerPadding = remember(maxHeight) {
                ((maxHeight - PlaybackLyricsRowHeight) / 2f).coerceAtLeast(0.dp)
            }
            val visualCenterIndex by remember(listState, renderModel) {
                derivedStateOf {
                    if (renderModel.mode != PlaybackLyricsMode.Static) {
                        renderModel.alphaAnchorIndex
                    } else {
                        val layoutInfo = listState.layoutInfo
                        val visibleItems = layoutInfo.visibleItemsInfo
                        if (visibleItems.isEmpty()) {
                            renderModel.alphaAnchorIndex
                        } else {
                            val viewportCenter = (
                                layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset
                            ) / 2
                            visibleItems.minByOrNull { item ->
                                abs((item.offset + (item.size / 2)) - viewportCenter)
                            }?.index ?: renderModel.alphaAnchorIndex
                        }
                    }
                }
            }

            LaunchedEffect(renderModel.focusIndex, renderModel.mode) {
                if (renderModel.mode != PlaybackLyricsMode.Static) {
                    listState.scrollToItem(index = renderModel.focusIndex)
                }
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape),
            ) {
                Image(
                    painter = painterResource(R.drawable.mask_playing_lyric),
                    contentDescription = stringResource(R.string.lyrics),
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.matchParentSize(),
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = PlaybackLyricsHorizontalPadding),
                state = listState,
                userScrollEnabled = renderModel.mode == PlaybackLyricsMode.Static,
                contentPadding = PaddingValues(vertical = centerPadding),
                verticalArrangement = Arrangement.spacedBy(PlaybackLyricsLineSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(
                    items = renderModel.lines,
                    key = { index, text -> "${renderModel.mode}-$index-$text" },
                ) { index, text ->
                    val highlighted = renderModel.highlightedIndex == index
                    val style = if (highlighted) {
                        PlaybackLyricsPrimaryStyle
                    } else {
                        PlaybackLyricsSecondaryStyle
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(
                                if (text.isBlank()) {
                                    PlaybackLyricsParagraphGap
                                } else {
                                    PlaybackLyricsRowHeight
                                },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (text.isNotBlank()) {
                            Text(
                                text = text,
                                style = style.copy(
                                    color = style.color.copy(
                                        alpha = alphaForDistance(abs(index - visualCenterIndex)),
                                    ),
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

internal enum class PlaybackLyricsMode {
    Timed,
    Static,
    Fallback,
}

internal data class PlaybackLyricsRenderModel(
    val mode: PlaybackLyricsMode,
    val lines: List<String>,
    val focusIndex: Int,
    val alphaAnchorIndex: Int,
    val highlightedIndex: Int?,
)

internal fun buildPlaybackLyricsRenderModel(
    lyrics: EmbeddedLyrics?,
    fallbackLines: List<String>,
    currentPositionMs: Long,
): PlaybackLyricsRenderModel {
    if (lyrics == null || lyrics.lines.isEmpty()) {
        return buildFallbackPlaybackLyricsRenderModel(fallbackLines)
    }

    return if (lyrics.isTimeSynced) {
        buildTimedPlaybackLyricsRenderModel(lyrics, currentPositionMs)
    } else {
        buildStaticPlaybackLyricsRenderModel(lyrics)
    }
}

private fun buildFallbackPlaybackLyricsRenderModel(
    fallbackLines: List<String>,
): PlaybackLyricsRenderModel {
    val focusIndex = fallbackLines.lastIndex
        .coerceAtLeast(0)
        .coerceAtMost(2)
    return PlaybackLyricsRenderModel(
        mode = PlaybackLyricsMode.Fallback,
        lines = fallbackLines,
        focusIndex = focusIndex,
        alphaAnchorIndex = focusIndex,
        highlightedIndex = focusIndex,
    )
}

private fun buildTimedPlaybackLyricsRenderModel(
    lyrics: EmbeddedLyrics,
    currentPositionMs: Long,
): PlaybackLyricsRenderModel {
    val activeIndex = lyrics.lines
        .indexOfLast { (it.timestampMs ?: Long.MAX_VALUE) <= currentPositionMs }
    val focusIndex = activeIndex.takeIf { it >= 0 } ?: 0
    return PlaybackLyricsRenderModel(
        mode = PlaybackLyricsMode.Timed,
        lines = lyrics.lines.map { it.text },
        focusIndex = focusIndex,
        alphaAnchorIndex = focusIndex,
        highlightedIndex = activeIndex.takeIf { it >= 0 },
    )
}

private fun buildStaticPlaybackLyricsRenderModel(
    lyrics: EmbeddedLyrics,
): PlaybackLyricsRenderModel = PlaybackLyricsRenderModel(
    mode = PlaybackLyricsMode.Static,
    lines = lyrics.lines.map { it.text },
    focusIndex = 0,
    alphaAnchorIndex = 0,
    highlightedIndex = null,
)

private fun alphaForDistance(distance: Int): Float =
    when (distance) {
        0 -> 1f
        1 -> 0.84f
        2 -> 0.68f
        3 -> 0.52f
        4 -> 0.36f
        else -> 0.2f
    }

@Composable
internal fun PlaybackMoreActionPanel(
    favoriteEnabled: Boolean,
    visualPage: PlaybackVisualPage,
    scratchEnabled: Boolean,
    sleepTimerActive: Boolean,
    bottomInset: Dp,
    addToPlaylistEnabled: Boolean = true,
    onAddToPlaylistClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSetRingtoneClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onLyricsToggle: () -> Unit,
    onScratchToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.background(Color.White),
    ) {
        PlaybackMoreActionTitleBar(onDismiss = onDismiss)
        PlaybackMoreActionGrid(
            favoriteEnabled = favoriteEnabled,
            visualPage = visualPage,
            scratchEnabled = scratchEnabled,
            sleepTimerActive = sleepTimerActive,
            addToPlaylistEnabled = addToPlaylistEnabled,
            onAddToPlaylistClick = onAddToPlaylistClick,
            onAddToQueueClick = onAddToQueueClick,
            onFavoriteToggle = onFavoriteToggle,
            onSetRingtoneClick = onSetRingtoneClick,
            onSleepTimerClick = onSleepTimerClick,
            onLyricsToggle = onLyricsToggle,
            onScratchToggle = onScratchToggle,
            onDeleteClick = onDeleteClick,
            onDismiss = onDismiss,
        )
        Spacer(modifier = Modifier.height(bottomInset))
    }
}

@Composable
private fun PlaybackMoreActionTitleBar(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlaybackMoreActionTitleHeight),
        contentAlignment = Alignment.Center,
    ) {
        AndroidDrawableImage(
            drawableRes = R.drawable.more_select_titlebar_bg,
            modifier = Modifier.matchParentSize(),
        )
        Text(
            text = stringResource(R.string.select_action),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = PlaybackMoreActionTitleStyle,
        )
        PlaybackCancelButton(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .width(PlaybackMoreActionCancelWidth)
                .height(PlaybackMoreActionCancelHeight),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun PlaybackMoreActionGrid(
    favoriteEnabled: Boolean,
    visualPage: PlaybackVisualPage,
    scratchEnabled: Boolean,
    sleepTimerActive: Boolean,
    addToPlaylistEnabled: Boolean,
    onAddToPlaylistClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSetRingtoneClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onLyricsToggle: () -> Unit,
    onScratchToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlaybackMoreActionRowHeight * 2),
    ) {
        AndroidDrawableImage(
            drawableRes = R.drawable.more_select_btn_bg,
            modifier = Modifier.matchParentSize(),
        )
        AndroidDrawableImage(
            drawableRes = R.drawable.more_select_titlebar_bg_shadow,
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopCenter),
        )
        Column(modifier = Modifier.fillMaxSize()) {
            PlaybackMoreActionRow {
                PlaybackMoreActionButton(
                    label = stringResource(R.string.add_to_playlist),
                    normalRes = R.drawable.more_select_icon_addlist,
                    pressedRes = R.drawable.more_select_icon_addlist_down,
                    enabled = addToPlaylistEnabled,
                    onClick = onAddToPlaylistClick,
                )
                PlaybackMoreActionDivider(vertical = true)
                PlaybackMoreActionButton(
                    label = stringResource(R.string.add_to_queue),
                    normalRes = R.drawable.more_select_icon_addplay,
                    pressedRes = R.drawable.more_select_icon_addplay_down,
                    onClick = onAddToQueueClick,
                )
                PlaybackMoreActionDivider(vertical = true)
                PlaybackMoreActionButton(
                    label = stringResource(R.string.love),
                    normalRes = if (favoriteEnabled) R.drawable.more_select_icon_favorite_cancel else R.drawable.more_select_icon_favorite_add,
                    pressedRes = if (favoriteEnabled) R.drawable.more_select_icon_favorite_cancel_down else R.drawable.more_select_icon_favorite_add_down,
                    onClick = onFavoriteToggle,
                )
                PlaybackMoreActionDivider(vertical = true)
                PlaybackMoreActionButton(
                    label = stringResource(R.string.lyrics),
                    normalRes = R.drawable.more_select_icon_lyric,
                    pressedRes = R.drawable.more_select_icon_lyric,
                    selected = visualPage == PlaybackVisualPage.Lyrics,
                    onClick = onLyricsToggle,
                )
            }
            PlaybackMoreActionDivider(vertical = false)
            PlaybackMoreActionRow {
                PlaybackMoreActionButton(
                    label = stringResource(R.string.set_ringtone),
                    normalRes = R.drawable.more_select_icon_ringtone,
                    pressedRes = R.drawable.more_select_icon_ringtone,
                    onClick = onSetRingtoneClick,
                )
                PlaybackMoreActionDivider(vertical = true)
                PlaybackMoreActionButton(
                    label = stringResource(R.string.sleep_timer),
                    normalRes = R.drawable.more_select_icon_timer,
                    pressedRes = R.drawable.more_select_icon_timer,
                    selected = sleepTimerActive,
                    selectedTextColor = PlaybackMoreActionSelectedColor,
                    onClick = onSleepTimerClick,
                )
                PlaybackMoreActionDivider(vertical = true)
                PlaybackMoreActionButton(
                    label = stringResource(R.string.djing),
                    normalRes = if (scratchEnabled) R.drawable.more_select_icon_djing_on else R.drawable.more_select_icon_djing,
                    pressedRes = if (scratchEnabled) R.drawable.more_select_icon_djing_on else R.drawable.more_select_icon_djing,
                    selected = scratchEnabled,
                    selectedTextColor = PlaybackMoreActionSelectedColor,
                    onClick = onScratchToggle,
                )
                PlaybackMoreActionDivider(vertical = true)
                PlaybackMoreActionButton(
                    label = stringResource(R.string.delete),
                    normalRes = R.drawable.more_select_icon_delete,
                    pressedRes = R.drawable.more_select_icon_delete,
                    onClick = onDeleteClick,
                )
            }
        }
    }
}

@Composable
private fun PlaybackMoreActionRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlaybackMoreActionRowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun PlaybackMoreActionDivider(vertical: Boolean) {
    Box(
        modifier = if (vertical) {
            Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(PlaybackMoreActionDividerColor)
        } else {
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PlaybackMoreActionDividerColor)
        },
    )
}

@Composable
private fun PlaybackCancelButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        AndroidDrawableImage(
            drawableRes = if (pressed) R.drawable.btn_cancel_down else R.drawable.btn_cancel,
            modifier = Modifier.matchParentSize(),
        )
        Text(
            text = stringResource(R.string.cancel),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = PlaybackMoreActionButtonStyle,
        )
    }
}

@Composable
private fun RowScope.PlaybackMoreActionButton(
    label: String,
    normalRes: Int,
    pressedRes: Int,
    selected: Boolean = false,
    selectedTextColor: Color = PlaybackMoreActionButtonStyle.color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .alpha(if (enabled) 1f else 0.38f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(if (enabled && pressed) pressedRes else normalRes),
            contentDescription = label,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(PlaybackMoreActionIconSize),
        )
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = PlaybackMoreActionButtonStyle.copy(
                color = if (selected) selectedTextColor else PlaybackMoreActionButtonStyle.color,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, start = 4.dp, end = 4.dp),
        )
    }
}
