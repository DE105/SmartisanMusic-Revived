package com.smartisanos.music.ui.playback

import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.playback.LocalPlaybackController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val PlaybackPageBackground = Color(0xFFFDFDFB)
private val PlaybackTopBarFill = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFFBFBFB),
        Color(0xFFF6F6F6),
        Color(0xFFF2F2F2),
    ),
)
private val PlaybackTopBarDivider = Color(0xFFE6E6E6)
private val PlaybackTitleColor = Color(0xFF6B6B6F)
private val PlaybackSubtitleColor = Color(0x88333333)
private val PlaybackTimeColor = Color(0x88333333)
private val PlaybackTrackColor = Color(0x1F000000)
private val PlaybackTrackFillColor = Color(0xFFBBBBBB)
private val PlaybackVolumeTrackColor = Color(0x11000000)
private val PlaybackVolumeFillColor = Color(0xFF9A9A9A)
private val PlaybackPanelBorder = Color(0xFFE8E8E8)
private val PlaybackPanelBottomEdge = Color(0xFFFDFDFD)
private val PlaybackPanelShadow = Color(0x14000000)
private val PlaybackMoreButtonShadow = Color(0x12000000)

private val PlaybackTitleStyle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.SemiBold,
    color = PlaybackTitleColor,
)
private val PlaybackArtistStyle = TextStyle(
    fontSize = 11.sp,
    color = PlaybackSubtitleColor,
)
private val PlaybackTimeStyle = TextStyle(
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    color = PlaybackTimeColor,
)

internal data class PlaybackScreenState(
    val mediaItem: MediaItem? = null,
    val isPlaying: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
)

internal enum class PlaybackOutputRoute {
    Speaker,
    Bluetooth,
}

@Composable
fun PlaybackScreen(
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val controller = LocalPlaybackController.current
    val view = LocalView.current
    var state by remember(controller) {
        mutableStateOf(controller.snapshot())
    }
    var showMorePanel by rememberSaveable { mutableStateOf(false) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var keepScreenAwake by rememberSaveable { mutableStateOf(false) }
    var scratchEnabled by rememberSaveable { mutableStateOf(false) }
    var favoriteEnabled by rememberSaveable { mutableStateOf(false) }
    var selectedRoute by rememberSaveable { mutableStateOf(PlaybackOutputRoute.Speaker) }

    BackHandler(enabled = showMorePanel) {
        showMorePanel = false
    }

    DisposableEffect(controller) {
        val playbackController = controller ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                state = playbackController.snapshot()
            }
        }
        playbackController.addListener(listener)
        onDispose {
            playbackController.removeListener(listener)
        }
    }

    DisposableEffect(view, keepScreenAwake) {
        val previousValue = view.keepScreenOn
        view.keepScreenOn = keepScreenAwake
        onDispose {
            view.keepScreenOn = previousValue
        }
    }

    LaunchedEffect(controller, state.mediaItem?.mediaId, state.isPlaying) {
        val playbackController = controller ?: return@LaunchedEffect
        while (isActive) {
            state = playbackController.snapshot()
            delay(if (state.isPlaying) 80L else 240L)
        }
    }

    val mediaMetadata = state.mediaItem?.mediaMetadata
    val title = mediaMetadata?.displayTitle?.toString()
        ?: mediaMetadata?.title?.toString()
        ?: stringResource(R.string.unknown_song_title)
    val artist = mediaMetadata?.subtitle?.toString()
        ?: mediaMetadata?.artist?.toString()
        ?: stringResource(R.string.unknown_artist)
    val durationMs = state.durationMs.takeIf { it > 0L }
        ?: mediaMetadata?.durationMs
        ?: 0L
    val primaryLyricLine = stringResource(R.string.playback_more_primary_line)
    val secondaryLyricLine = stringResource(R.string.playback_more_secondary_line)
    val tertiaryLyricLine = stringResource(R.string.playback_more_tertiary_line)
    val progress = durationMs
        .takeIf { it > 0L }
        ?.let { state.currentPositionMs.toFloat() / it.toFloat() }
        ?.coerceIn(0f, 1f)
        ?: 0f
    val targetNeedleRotation = if (state.mediaItem == null) {
        -12f
    } else {
        3.5f + (progress * 16f)
    }
    val needleRotation by animateFloatAsState(
        targetValue = targetNeedleRotation,
        animationSpec = tween(durationMillis = 220),
        label = "needleRotation",
    )
    val lyricsLines = remember(title, artist, primaryLyricLine, secondaryLyricLine, tertiaryLyricLine) {
        listOf(
            title,
            artist,
            primaryLyricLine,
            secondaryLyricLine,
            tertiaryLyricLine,
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PlaybackPageBackground),
    ) {
        val density = LocalDensity.current
        val topInset = with(density) {
            WindowInsets.safeDrawing.getTop(this).toDp()
        }
        val bottomInset = with(density) {
            WindowInsets.safeDrawing.getBottom(this).toDp()
        }
        val turntableWidth = minOf(maxWidth - 8.dp, 360.dp)
        val scale = turntableWidth.value / 360f

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            PlaybackTopBar(
                title = title,
                artist = artist,
                topInset = topInset,
                onCollapse = onCollapse,
            )
            PlaybackTimeSeekBar(
                durationMs = durationMs,
                currentPositionMs = state.currentPositionMs.coerceIn(0L, durationMs.coerceAtLeast(0L)),
                thumbRes = R.drawable.playing_control_time,
                modifier = Modifier.fillMaxWidth(),
                onSeek = { positionMs ->
                    controller?.seekTo(positionMs)
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                PlaybackTurntableSection(
                    modifier = Modifier.width(turntableWidth),
                    turntableWidth = turntableWidth,
                    scale = scale,
                    showLyrics = showLyrics,
                    keepScreenAwake = keepScreenAwake,
                    lyricsLines = lyricsLines,
                    needleRotation = needleRotation,
                    discRotation = ((state.currentPositionMs % 1_800L).toFloat() / 1_800f) * 360f,
                    onMoreClick = {
                        showMorePanel = true
                    },
                    onKeepScreenToggle = {
                        keepScreenAwake = !keepScreenAwake
                    },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            PlaybackBottomPager(
                width = turntableWidth,
                bottomInset = bottomInset,
                state = state,
                selectedRoute = selectedRoute,
                onRouteSelected = { selectedRoute = it },
                onRepeatClick = {
                    controller?.repeatMode =
                        if (state.repeatMode == Player.REPEAT_MODE_OFF) {
                            Player.REPEAT_MODE_ALL
                        } else {
                            Player.REPEAT_MODE_OFF
                        }
                },
                onPreviousClick = {
                    controller?.seekToPrevious()
                },
                onPlayPauseClick = {
                    if (state.isPlaying) {
                        controller?.pause()
                    } else {
                        controller?.play()
                    }
                },
                onNextClick = {
                    controller?.seekToNext()
                },
                onShuffleClick = {
                    controller?.shuffleModeEnabled = !state.shuffleEnabled
                },
                onVolumeChange = { volume ->
                    controller?.volume = volume
                },
            )
        }

        AnimatedVisibility(
            visible = showMorePanel,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(160)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x12000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        showMorePanel = false
                    },
            ) {
                PlaybackMoreActionPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = 8.dp,
                            end = 8.dp,
                            bottom = 108.dp + bottomInset,
                        )
                        .width(turntableWidth),
                    keepScreenAwake = keepScreenAwake,
                    scratchEnabled = scratchEnabled,
                    favoriteEnabled = favoriteEnabled,
                    showLyrics = showLyrics,
                    onKeepScreenAwakeToggle = { keepScreenAwake = !keepScreenAwake },
                    onScratchToggle = { scratchEnabled = !scratchEnabled },
                    onFavoriteToggle = { favoriteEnabled = !favoriteEnabled },
                    onLyricsToggle = {
                        showLyrics = !showLyrics
                        showMorePanel = false
                    },
                    onDismiss = {
                        showMorePanel = false
                    },
                )
            }
        }
    }
}

@Composable
private fun PlaybackTopBar(
    title: String,
    artist: String,
    topInset: Dp,
    onCollapse: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(topInset + 48.dp)
            .drawWithCache {
                onDrawBehind {
                    drawRect(brush = PlaybackTopBarFill)
                    drawLine(
                        color = PlaybackTopBarDivider,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height - 0.5f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height - 0.5f),
                        strokeWidth = 1f,
                    )
                }
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PressedDrawableButton(
                normalRes = R.drawable.btn_playing_back,
                pressedRes = R.drawable.btn_playing_back_down,
                contentDescription = stringResource(R.string.collapse_player),
                modifier = Modifier
                    .width(40.dp)
                    .height(30.dp),
                onClick = onCollapse,
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = PlaybackTitleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = PlaybackArtistStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            PressedDrawableButton(
                normalRes = R.drawable.btn_playing_list,
                pressedRes = R.drawable.btn_playing_list_down,
                contentDescription = stringResource(R.string.playback_queue),
                modifier = Modifier
                    .width(40.dp)
                    .height(30.dp),
                onClick = { },
            )
        }
    }
}

@Composable
private fun PlaybackTimeSeekBar(
    durationMs: Long,
    currentPositionMs: Long,
    @DrawableRes thumbRes: Int,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit,
) {
    val duration = durationMs.coerceAtLeast(0L)
    val currentFraction = if (duration > 0L) {
        currentPositionMs.toFloat() / duration.toFloat()
    } else {
        0f
    }.coerceIn(0f, 1f)
    var trackWidthPx by remember { mutableIntStateOf(0) }
    var dragFraction by remember { mutableFloatStateOf(Float.NaN) }
    val density = LocalDensity.current
    val shownFraction = if (dragFraction.isNaN()) currentFraction else dragFraction.coerceIn(0f, 1f)
    val shownPosition = if (duration > 0L) {
        (shownFraction * duration.toFloat()).roundToLong()
    } else {
        0L
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatPlaybackTime(shownPosition),
                style = PlaybackTimeStyle,
            )
            Text(
                text = "-${formatPlaybackTime((duration - shownPosition).coerceAtLeast(0L))}",
                style = PlaybackTimeStyle,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .padding(horizontal = 12.dp)
                .onSizeChanged { trackWidthPx = it.width }
                .pointerInput(duration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (trackWidthPx > 0) {
                                dragFraction = fractionFromPosition(offset.x, trackWidthPx)
                                if (duration > 0L) {
                                    onSeek((dragFraction * duration.toFloat()).roundToLong())
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (trackWidthPx > 0) {
                                dragFraction = fractionFromPosition(change.position.x, trackWidthPx)
                                if (duration > 0L) {
                                    onSeek((dragFraction * duration.toFloat()).roundToLong())
                                }
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            val finalFraction = dragFraction.takeUnless { it.isNaN() } ?: currentFraction
                            if (duration > 0L) {
                                onSeek((finalFraction * duration.toFloat()).roundToLong())
                            }
                            dragFraction = Float.NaN
                        },
                        onDragCancel = {
                            dragFraction = Float.NaN
                        },
                    )
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.Center)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(PlaybackTrackColor),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(shownFraction)
                    .height(3.dp)
                    .align(Alignment.CenterStart)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(PlaybackTrackFillColor),
            )
            Image(
                painter = painterResource(thumbRes),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.CenterStart)
                    .offset {
                        IntOffset(
                            x = ((trackWidthPx - with(density) { 18.dp.roundToPx() }) * shownFraction)
                                .roundToInt()
                                .coerceAtLeast(0),
                            y = 0,
                        )
                    },
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PlaybackTopBarDivider.copy(alpha = 0.7f)),
        )
    }
}

@Composable
private fun PlaybackTurntableSection(
    turntableWidth: Dp,
    scale: Float,
    showLyrics: Boolean,
    keepScreenAwake: Boolean,
    lyricsLines: List<String>,
    needleRotation: Float,
    discRotation: Float,
    onMoreClick: () -> Unit,
    onKeepScreenToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val turntableHeight = 356.5938.dp * scale
    val needleWidth = 73.3.dp * scale
    val needleHeight = 310.dp * scale
    val needleTopMargin = 25.5.dp * scale
    val needleRightMargin = 2.5.dp * scale
    val needleShadowRightMargin = 2.dp * scale
    val moreButtonMargin = 12.dp * scale
    val moreButtonTopMargin = 38.dp * scale

    Box(
        modifier = modifier.height(turntableHeight + 52.dp * scale),
    ) {
        PressedDrawableButton(
            normalRes = R.drawable.more_btn,
            pressedRes = R.drawable.more_btn_down,
            contentDescription = stringResource(R.string.player_more_actions),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = moreButtonMargin, top = moreButtonTopMargin)
                .size(28.dp * scale),
            shadowColor = PlaybackMoreButtonShadow,
            onClick = onMoreClick,
        )
        if (showLyrics) {
            PressedDrawableButton(
                normalRes = R.drawable.sun_btn_off,
                pressedRes = R.drawable.sun_btn_off_down,
                contentDescription = stringResource(R.string.always_on),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = moreButtonMargin, top = moreButtonTopMargin)
                    .size(28.dp * scale)
                    .graphicsLayer {
                        alpha = if (keepScreenAwake) 1f else 0.72f
                    },
                shadowColor = PlaybackMoreButtonShadow,
                onClick = onKeepScreenToggle,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(turntableWidth)
                .height(turntableHeight),
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        rotationZ = discRotation
                    },
            ) {
                Image(
                    painter = painterResource(R.drawable.playing_lp),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.matchParentSize(),
                )
                Image(
                    painter = painterResource(R.drawable.playing_cover_lp),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.matchParentSize(),
                )
            }
            AnimatedVisibility(
                visible = showLyrics,
                enter = fadeIn(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(180)),
                modifier = Modifier.matchParentSize(),
            ) {
                PlaybackLyricsOverlay(
                    lyricsLines = lyricsLines,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = needleTopMargin, end = needleShadowRightMargin)
                    .width(needleWidth)
                    .height(needleHeight)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.82f, 0.08f)
                        rotationZ = needleRotation
                    },
            ) {
                AndroidDrawableImage(
                    drawableRes = R.drawable.needle_shadow2,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = needleTopMargin, end = needleRightMargin)
                    .width(needleWidth)
                    .height(needleHeight)
                    .graphicsLayer {
                        transformOrigin = TransformOrigin(0.82f, 0.08f)
                        rotationZ = needleRotation
                    },
            ) {
                AndroidDrawableImage(
                    drawableRes = R.drawable.playing_stylus_lp_bg,
                    modifier = Modifier.matchParentSize(),
                )
                AndroidDrawableImage(
                    drawableRes = R.drawable.playing_stylus_lp,
                    modifier = Modifier.matchParentSize(),
                )
                AndroidDrawableImage(
                    drawableRes = R.drawable.playing_stylus_lp_top,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}

@Composable
internal fun PlaybackControlButtons(
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    scale: Float,
    onRepeatClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PressedDrawableButton(
            normalRes = if (repeatMode == Player.REPEAT_MODE_OFF) {
                R.drawable.btn_playing_cycle_off
            } else {
                R.drawable.btn_playing_cycle_on
            },
            pressedRes = if (repeatMode == Player.REPEAT_MODE_OFF) {
                R.drawable.btn_playing_cycle_off
            } else {
                R.drawable.btn_playing_cycle_on
            },
            contentDescription = stringResource(R.string.repeat),
            modifier = Modifier
                .width(67.3.dp * scale)
                .height(87.dp * scale),
            onClick = onRepeatClick,
        )
        PressedDrawableButton(
            normalRes = R.drawable.btn_playing_prev,
            pressedRes = R.drawable.btn_playing_prev_down,
            contentDescription = stringResource(R.string.previous_song),
            modifier = Modifier
                .width(70.dp * scale)
                .height(87.dp * scale),
            onClick = onPreviousClick,
        )
        PressedDrawableButton(
            normalRes = if (isPlaying) {
                R.drawable.btn_playing_pause
            } else {
                R.drawable.btn_playing_play
            },
            pressedRes = if (isPlaying) {
                R.drawable.btn_playing_pause_down
            } else {
                R.drawable.btn_playing_play_down
            },
            contentDescription = if (isPlaying) {
                stringResource(R.string.pause)
            } else {
                stringResource(R.string.play)
            },
            modifier = Modifier
                .width(85.3.dp * scale)
                .height(87.dp * scale),
            onClick = onPlayPauseClick,
        )
        PressedDrawableButton(
            normalRes = R.drawable.btn_playing_next,
            pressedRes = R.drawable.btn_playing_next_down,
            contentDescription = stringResource(R.string.next_song),
            modifier = Modifier
                .width(70.dp * scale)
                .height(87.dp * scale),
            onClick = onNextClick,
        )
        PressedDrawableButton(
            normalRes = if (shuffleEnabled) {
                R.drawable.btn_playing_shuffle_on
            } else {
                R.drawable.btn_playing_shuffle_off
            },
            pressedRes = if (shuffleEnabled) {
                R.drawable.btn_playing_shuffle_on
            } else {
                R.drawable.btn_playing_shuffle_off
            },
            contentDescription = stringResource(R.string.shuffle),
            modifier = Modifier
                .width(67.3.dp * scale)
                .height(87.dp * scale),
            onClick = onShuffleClick,
        )
    }
}

@Composable
internal fun PlaybackVolumeBar(
    value: Float,
    width: Dp,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit,
) {
    var trackWidthPx by remember { mutableIntStateOf(0) }
    var dragFraction by remember { mutableFloatStateOf(Float.NaN) }
    val density = LocalDensity.current
    val shownFraction = if (dragFraction.isNaN()) value else dragFraction.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .width(width)
            .height(31.dp)
            .drawWithCache {
                onDrawBehind {
                    drawRoundRect(
                        color = Color.White,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    )
                    drawRoundRect(
                        color = PlaybackPanelShadow,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        size = size.copy(height = size.height - 1.dp.toPx()),
                    )
                    drawRoundRect(
                        color = PlaybackPanelBorder,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                    )
                    drawLine(
                        color = PlaybackPanelBottomEdge,
                        start = androidx.compose.ui.geometry.Offset(4.dp.toPx(), size.height - 1.dp.toPx()),
                        end = androidx.compose.ui.geometry.Offset(size.width - 4.dp.toPx(), size.height - 1.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            }
            .padding(horizontal = 9.dp, vertical = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(23.dp)
                .onSizeChanged { trackWidthPx = it.width }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (trackWidthPx > 0) {
                                dragFraction = fractionFromPosition(offset.x, trackWidthPx)
                                onValueChange(dragFraction)
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (trackWidthPx > 0) {
                                dragFraction = fractionFromPosition(change.position.x, trackWidthPx)
                                onValueChange(dragFraction)
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            val finalFraction = dragFraction.takeUnless { it.isNaN() } ?: value
                            onValueChange(finalFraction)
                            dragFraction = Float.NaN
                        },
                        onDragCancel = {
                            dragFraction = Float.NaN
                        },
                    )
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.Center)
                    .padding(horizontal = 17.5.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(PlaybackVolumeTrackColor),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(shownFraction)
                    .height(6.dp)
                    .align(Alignment.CenterStart)
                    .padding(start = 17.5.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(PlaybackVolumeFillColor),
            )
            Image(
                painter = painterResource(R.drawable.playing_control_volume),
                contentDescription = stringResource(R.string.volume),
                modifier = Modifier
                    .widthIn(min = 12.dp)
                    .height(23.dp)
                    .align(Alignment.CenterStart)
                    .offset {
                        IntOffset(
                            x = ((trackWidthPx - with(density) { 14.dp.roundToPx() }) * shownFraction)
                                .roundToInt()
                                .coerceAtLeast(0),
                            y = 0,
                        )
                    },
            )
        }
    }
}

@Composable
internal fun PressedDrawableButton(
    @DrawableRes normalRes: Int,
    @DrawableRes pressedRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shadowColor: Color = Color.Transparent,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .then(
                if (shadowColor.alpha > 0f) {
                    Modifier.drawWithCache {
                        onDrawBehind {
                            drawCircle(
                                color = shadowColor,
                                radius = size.minDimension * 0.48f,
                                center = androidx.compose.ui.geometry.Offset(
                                    x = size.width / 2f,
                                    y = size.height / 2f + 1.5.dp.toPx(),
                                ),
                            )
                        }
                    }
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AndroidDrawableImage(
            drawableRes = if (pressed) pressedRes else normalRes,
            modifier = Modifier.matchParentSize(),
            contentDescription = contentDescription,
        )
    }
}

@Composable
internal fun AndroidDrawableImage(
    @DrawableRes drawableRes: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_XY
                this.contentDescription = contentDescription
            }
        },
        modifier = modifier,
        update = { imageView ->
            imageView.setImageResource(drawableRes)
            imageView.contentDescription = contentDescription
        },
    )
}

private fun Player?.snapshot(): PlaybackScreenState {
    val player = this ?: return PlaybackScreenState()
    return PlaybackScreenState(
        mediaItem = player.currentMediaItem,
        isPlaying = player.isPlaying,
        repeatMode = player.repeatMode,
        shuffleEnabled = player.shuffleModeEnabled,
        currentPositionMs = player.currentPosition.coerceAtLeast(0L),
        durationMs = player.duration.takeIf { it > 0L } ?: 0L,
        volume = player.volume,
    )
}

private fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = (positionMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private fun fractionFromPosition(positionX: Float, trackWidthPx: Int): Float {
    if (trackWidthPx <= 0) return 0f
    return (positionX / trackWidthPx.toFloat()).coerceIn(0f, 1f)
}
