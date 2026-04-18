package com.smartisanos.music.ui.playback

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlin.math.absoluteValue

private const val RouterPageIndex = 0
private const val ControllerPageIndex = 1

private val PlaybackRouterTextColor = Color(0x66333333)
private val PlaybackRouterTextSelectedColor = Color(0xFF4A4A4A)
private val PlaybackLyricsPrimaryStyle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    color = Color(0xB01B1B1B),
    textAlign = TextAlign.Center,
)
private val PlaybackLyricsSecondaryStyle = TextStyle(
    fontSize = 14.sp,
    color = Color(0x66333333),
    textAlign = TextAlign.Center,
)
private val PlaybackPanelLabelStyle = TextStyle(
    fontSize = 11.sp,
    color = Color(0x66333333),
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

@Composable
internal fun PlaybackBottomPager(
    width: Dp,
    bottomInset: Dp,
    state: PlaybackScreenState,
    selectedRoute: PlaybackOutputRoute,
    onRouteSelected: (PlaybackOutputRoute) -> Unit,
    onRepeatClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onVolumeChange: (Float) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = ControllerPageIndex,
        pageCount = { 2 },
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(186.dp),
            beyondViewportPageCount = 1,
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                .absoluteValue
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = 1f - pageOffset.coerceIn(0f, 1f) * 0.18f
                    },
                contentAlignment = Alignment.TopCenter,
            ) {
                if (page == ControllerPageIndex) {
                    Column(
                        modifier = Modifier
                            .width(width)
                            .padding(bottom = 15.dp),
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
                } else {
                    PlaybackRouterPage(
                        width = width,
                        selectedRoute = selectedRoute,
                        onRouteSelected = onRouteSelected,
                    )
                }
            }
        }
        PlaybackPagerIndicator(
            currentPage = pagerState.currentPage,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(modifier = Modifier.height((10.dp + bottomInset).coerceAtLeast(12.dp)))
    }
}

@Composable
internal fun PlaybackLyricsOverlay(
    lyricsLines: List<String>,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 54.dp, vertical = 80.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            lyricsLines.forEachIndexed { index, line ->
                val style = if (index == 2) PlaybackLyricsPrimaryStyle else PlaybackLyricsSecondaryStyle
                val alpha = when (index) {
                    0, 4 -> 0.28f
                    1, 3 -> 0.46f
                    else -> 0.92f
                }
                Text(
                    text = line,
                    style = style.copy(color = style.color.copy(alpha = alpha)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
internal fun PlaybackMoreActionPanel(
    favoriteEnabled: Boolean,
    showLyrics: Boolean,
    scratchEnabled: Boolean,
    bottomInset: Dp,
    onFavoriteToggle: () -> Unit,
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
            showLyrics = showLyrics,
            scratchEnabled = scratchEnabled,
            onFavoriteToggle = onFavoriteToggle,
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
    showLyrics: Boolean,
    scratchEnabled: Boolean,
    onFavoriteToggle: () -> Unit,
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
                    onClick = onDismiss,
                )
                PlaybackMoreActionDivider(vertical = true)
                PlaybackMoreActionButton(
                    label = stringResource(R.string.add_to_queue),
                    normalRes = R.drawable.more_select_icon_addplay,
                    pressedRes = R.drawable.more_select_icon_addplay_down,
                    onClick = onDismiss,
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
                    selected = showLyrics,
                    onClick = onLyricsToggle,
                )
            }
            PlaybackMoreActionDivider(vertical = false)
            PlaybackMoreActionRow {
                PlaybackMoreActionButton(
                    label = stringResource(R.string.set_ringtone),
                    normalRes = R.drawable.more_select_icon_ringtone,
                    pressedRes = R.drawable.more_select_icon_ringtone,
                    onClick = onDismiss,
                )
                PlaybackMoreActionDivider(vertical = true)
                PlaybackMoreActionButton(
                    label = stringResource(R.string.sleep_timer),
                    normalRes = R.drawable.more_select_icon_timer,
                    pressedRes = R.drawable.more_select_icon_timer,
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
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(if (pressed) pressedRes else normalRes),
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

@Composable
private fun PlaybackRouterPage(
    width: Dp,
    selectedRoute: PlaybackOutputRoute,
    onRouteSelected: (PlaybackOutputRoute) -> Unit,
) {
    Column(
        modifier = Modifier.width(width).fillMaxHeight().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.switch_audio_output_source), style = PlaybackPanelLabelStyle)
        AndroidDrawableImage(
            drawableRes = R.drawable.playing_divide_line,
            modifier = Modifier.fillMaxWidth().height(1.dp).padding(top = 14.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaybackRouteOption(stringResource(R.string.speaker), selectedRoute == PlaybackOutputRoute.Speaker) { onRouteSelected(PlaybackOutputRoute.Speaker) }
            Spacer(modifier = Modifier.width(37.dp))
            PlaybackRouteOption(stringResource(R.string.bluetooth), selectedRoute == PlaybackOutputRoute.Bluetooth) { onRouteSelected(PlaybackOutputRoute.Bluetooth) }
        }
    }
}

@Composable
private fun PlaybackRouteOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        style = PlaybackPanelLabelStyle.copy(fontSize = 13.sp, color = if (selected) PlaybackRouterTextSelectedColor else PlaybackRouterTextColor),
        modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
    )
}

@Composable
private fun PlaybackPagerIndicator(currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(if (currentPage == RouterPageIndex) R.drawable.playing_dot_bluetooth_black else R.drawable.playing_dot_bluetooth_white),
            contentDescription = null,
            modifier = Modifier.width(13.dp).height(12.dp),
        )
        Image(
            painter = painterResource(if (currentPage == ControllerPageIndex) R.drawable.playing_dot_black else R.drawable.playing_dot_white),
            contentDescription = null,
            modifier = Modifier.width(13.dp).height(12.dp),
        )
    }
}
