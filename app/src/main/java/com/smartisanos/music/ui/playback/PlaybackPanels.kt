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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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

private val PlaybackPanelBackground = Color(0xFFF9F9F7)
private val PlaybackPanelLineColor = Color(0x12000000)
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
private val PlaybackMoreActionButtonWidth = 56.dp
private val PlaybackMoreActionIconBoxSize = 36.dp
private val PlaybackMoreActionIconSize = 30.dp

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
    keepScreenAwake: Boolean,
    favoriteEnabled: Boolean,
    showLyrics: Boolean,
    onKeepScreenAwakeToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onLyricsToggle: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(PlaybackPanelBackground)
            .drawWithCache {
                onDrawBehind {
                    drawRoundRect(
                        color = Color(0x14000000),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()),
                    )
                    drawRoundRect(
                        color = PlaybackPanelLineColor,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()),
                        style = Stroke(width = 1.dp.toPx()),
                    )
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PlaybackMoreActionButton(
                stringResource(R.string.favorite),
                if (favoriteEnabled) R.drawable.playing_btn_favorite_cancel else R.drawable.playing_btn_favorite_add,
                if (favoriteEnabled) R.drawable.playing_btn_favorite_cancel_down else R.drawable.playing_btn_favorite_add_down,
                favoriteEnabled,
                onFavoriteToggle,
            )
            PlaybackMoreActionButton(stringResource(R.string.share), R.drawable.playing_btn_share, R.drawable.playing_btn_share_down, false, onDismiss)
            PlaybackMoreActionButton(stringResource(R.string.lyrics), R.drawable.playing_btn_lyrics, R.drawable.playing_btn_lyrics_down, showLyrics, onLyricsToggle)
            PlaybackMoreActionButton(stringResource(R.string.always_on), R.drawable.sun_btn_off, R.drawable.sun_btn_off_down, keepScreenAwake, onKeepScreenAwakeToggle)
        }
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

@Composable
private fun PlaybackMoreActionButton(
    label: String,
    normalRes: Int,
    pressedRes: Int,
    highlighted: Boolean,
    onClick: () -> Unit,
    iconBoxSize: Dp = PlaybackMoreActionIconBoxSize,
    iconWidth: Dp = PlaybackMoreActionIconSize,
    iconHeight: Dp = PlaybackMoreActionIconSize,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Column(
        modifier = Modifier
            .width(PlaybackMoreActionButtonWidth)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(iconBoxSize),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(if (pressed) pressedRes else normalRes),
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(iconWidth)
                    .height(iconHeight),
            )
        }
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = PlaybackPanelLabelStyle.copy(
                color = if (highlighted) Color(0xCC333333) else PlaybackPanelLabelStyle.color,
            ),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
