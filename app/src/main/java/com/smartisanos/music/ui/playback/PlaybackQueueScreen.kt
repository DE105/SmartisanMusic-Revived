package com.smartisanos.music.ui.playback

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import com.smartisanos.music.R
import com.smartisanos.music.ui.components.SmartisanTitleBarSurface
import com.smartisanos.music.ui.components.SmartisanTitleBarSurfaceStyle
import com.smartisanos.music.ui.components.loadEmbeddedArtwork
import kotlinx.coroutines.launch

private val QueueBackground = Color(0xFFF7F7F7)
private val QueueTopBarTitleColor = Color(0xFF6B6B6F)
private val QueueSectionHeaderBackground = Color(0xFFF0F0F0)
private val QueueSectionHeaderTextColor = Color(0xFF8A8A8A)
private val QueueItemBackground = Color.White
private val QueueDraggingItemBackground = Color(0xFFFCFCFC)
private val QueueDividerColor = Color(0xFFEBEBEB)
private val QueueTitleColor = Color(0xFF3A3A3A)
private val QueueSubtitleColor = Color(0xFF8D8D8D)
private val QueueHandleColor = Color(0xFFD1D1D1)
private val QueueArtworkFallback = Color(0xFFE0E0E0)

private val QueueTopBarTitleStyle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.SemiBold,
    color = QueueTopBarTitleColor,
)
private val QueueSectionHeaderStyle = TextStyle(
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    color = QueueSectionHeaderTextColor,
)
private val QueueCurrentTitleStyle = TextStyle(
    fontSize = 17.sp,
    fontWeight = FontWeight.Medium,
    color = QueueTitleColor,
)
private val QueueCurrentSubtitleStyle = TextStyle(
    fontSize = 13.sp,
    color = QueueSubtitleColor,
)
private val QueueItemTitleStyle = TextStyle(
    fontSize = 15.sp,
    color = QueueTitleColor,
)
private val QueueItemSubtitleStyle = TextStyle(
    fontSize = 12.sp,
    color = QueueSubtitleColor,
)

data class PlaybackQueueTrack(
    val id: String,
    val title: String,
    val artist: String,
    val mediaItem: MediaItem? = null,
    val queueIndex: Int = -1,
)

data class PlaybackQueueUiState(
    val currentTrack: PlaybackQueueTrack? = null,
    val upcomingTracks: List<PlaybackQueueTrack> = emptyList(),
    val isCurrentFavorite: Boolean = false,
    val showHeaderAction: Boolean = true,
    val reorderEnabled: Boolean = true,
)

@Composable
fun PlaybackQueueScreen(
    state: PlaybackQueueUiState,
    onExitFullScreenClick: () -> Unit,
    onReturnToPlaybackClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onFavoriteCurrentClick: () -> Unit,
    onClearUpcomingClick: () -> Unit,
    onMoveRequest: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val bottomInset = with(density) {
        WindowInsets.safeDrawing.getBottom(this).toDp()
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var upcomingTracks by remember { mutableStateOf(state.upcomingTracks) }
    var draggingTrackKey by remember { mutableStateOf<String?>(null) }
    var draggingIndex by remember { mutableIntStateOf(-1) }
    var draggingOffsetY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(state.upcomingTracks) {
        if (draggingTrackKey == null) {
            upcomingTracks = state.upcomingTracks
        }
    }

    fun endDragging() {
        draggingTrackKey = null
        draggingIndex = -1
        draggingOffsetY = 0f
    }

    LaunchedEffect(state.reorderEnabled) {
        if (!state.reorderEnabled) {
            upcomingTracks = state.upcomingTracks
            endDragging()
        }
    }

    fun updateDragging(deltaY: Float) {
        if (!state.reorderEnabled) {
            return
        }
        val activeTrackKey = draggingTrackKey ?: return
        val fromIndex = draggingIndex
        if (fromIndex !in upcomingTracks.indices) {
            endDragging()
            return
        }

        draggingOffsetY += deltaY

        val visibleItems = listState.layoutInfo.visibleItemsInfo
        val currentItemInfo = visibleItems.firstOrNull { item ->
            item.key == activeTrackKey
        } ?: return

        val draggedTop = currentItemInfo.offset.toFloat() + draggingOffsetY
        val draggedBottom = draggedTop + currentItemInfo.size
        val targetCenter = draggedTop + (currentItemInfo.size / 2f)

        val targetItemInfo = visibleItems
            .filter { item ->
                val key = item.key as? String ?: return@filter false
                key.startsWith(PlaybackQueueTrackKeyPrefix) && key != currentItemInfo.key
            }
            .firstOrNull { item ->
                targetCenter in item.offset.toFloat()..(item.offset + item.size).toFloat()
            }

        if (targetItemInfo != null) {
            val targetKey = targetItemInfo.key as? String
            val toIndex = upcomingTracks.indexOfFirst { track ->
                playbackQueueTrackKey(track) == targetKey
            }
            if (toIndex >= 0 && toIndex != fromIndex) {
                val movedTrack = upcomingTracks[fromIndex]
                val targetTrack = upcomingTracks[toIndex]
                val oldOffset = currentItemInfo.offset
                val newOffset = targetItemInfo.offset
                val reorderedTracks = upcomingTracks.toMutableList().apply {
                    add(toIndex, removeAt(fromIndex))
                }
                val updatedTracks = reorderedTracks.map { track ->
                    track.withQueueIndexAfterMove(
                        fromIndex = movedTrack.queueIndex,
                        toIndex = targetTrack.queueIndex,
                    )
                }
                upcomingTracks = updatedTracks
                draggingIndex = toIndex
                draggingTrackKey = updatedTracks.getOrNull(toIndex)?.let(::playbackQueueTrackKey)
                draggingOffsetY += oldOffset - newOffset
                if (movedTrack.queueIndex >= 0 && targetTrack.queueIndex >= 0) {
                    onMoveRequest(movedTrack.queueIndex, targetTrack.queueIndex)
                }
            }
        }

        val viewportStart = listState.layoutInfo.viewportStartOffset.toFloat()
        val viewportEnd = listState.layoutInfo.viewportEndOffset.toFloat()
        val scrollDelta = when {
            draggedTop < viewportStart -> draggedTop - viewportStart
            draggedBottom > viewportEnd -> draggedBottom - viewportEnd
            else -> 0f
        }
        if (scrollDelta != 0f) {
            scope.launch {
                listState.scrollBy(scrollDelta)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = QueueBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            PlaybackQueueTopBar(
                showReturnButton = state.showHeaderAction,
                onExitFullScreenClick = {
                    endDragging()
                    onExitFullScreenClick()
                },
                onReturnToPlaybackClick = {
                    endDragging()
                    onReturnToPlaybackClick()
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = bottomInset + 16.dp),
        ) {
            state.currentTrack?.let { currentTrack ->
                item(key = "header_now_playing") {
                    QueueSectionHeader(title = "正在播放")
                }
                item(key = "current_track") {
                    CurrentPlayingCard(
                        track = currentTrack,
                        isFavorite = state.isCurrentFavorite,
                        onFavoriteClick = onFavoriteCurrentClick,
                    )
                }
            }

            if (upcomingTracks.isNotEmpty()) {
                item(key = "header_upcoming") {
                    QueueSectionHeaderWithClear(
                        title = "继续播放",
                        onClearClick = {
                            endDragging()
                            onClearUpcomingClick()
                        },
                    )
                }
                itemsIndexed(
                    items = upcomingTracks,
                    key = { _, track -> playbackQueueTrackKey(track) },
                    contentType = { _, _ -> "track_item" },
                ) { index, track ->
                    val trackKey = playbackQueueTrackKey(track)
                    val isDragging = draggingTrackKey == trackKey
                    UpcomingTrackItem(
                        track = track,
                        dragKey = trackKey,
                        reorderEnabled = state.reorderEnabled,
                        isDragging = isDragging,
                        dragOffsetY = if (isDragging) draggingOffsetY else 0f,
                        onClick = {
                            onItemClick(track.queueIndex.takeIf { it >= 0 } ?: index)
                        },
                        onDragStart = {
                            if (state.reorderEnabled) {
                                draggingTrackKey = trackKey
                                draggingIndex = index
                                draggingOffsetY = 0f
                            }
                        },
                        onDrag = ::updateDragging,
                        onDragEnd = ::endDragging,
                    )
                    if (index < upcomingTracks.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp),
                            color = QueueDividerColor,
                            thickness = 0.5.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackQueueTopBar(
    showReturnButton: Boolean,
    onExitFullScreenClick: () -> Unit,
    onReturnToPlaybackClick: () -> Unit,
) {
    val density = LocalDensity.current
    val topInset = with(density) {
        WindowInsets.safeDrawing.getTop(this).toDp()
    }

    SmartisanTitleBarSurface(
        style = SmartisanTitleBarSurfaceStyle.Playback,
        modifier = Modifier
            .fillMaxWidth()
            .height(topInset + 48.dp),
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
                onClick = onExitFullScreenClick,
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "播放队列",
                    style = QueueTopBarTitleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showReturnButton) {
                PressedDrawableButton(
                    normalRes = R.drawable.btn_playing_list,
                    pressedRes = R.drawable.btn_playing_list_down,
                    contentDescription = "返回全屏播放页",
                    modifier = Modifier
                        .width(40.dp)
                        .height(30.dp),
                    onClick = onReturnToPlaybackClick,
                )
            } else {
                Spacer(
                    modifier = Modifier
                        .width(40.dp)
                        .height(30.dp),
                )
            }
        }
    }
}

@Composable
private fun QueueSectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(QueueSectionHeaderBackground)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(text = title, style = QueueSectionHeaderStyle)
    }
}

@Composable
private fun QueueSectionHeaderWithClear(
    title: String,
    onClearClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(QueueSectionHeaderBackground)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = QueueSectionHeaderStyle)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onClearClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "×",
                color = QueueSectionHeaderTextColor,
                fontSize = 18.sp,
            )
        }
    }
}

@Composable
private fun CurrentPlayingCard(
    track: PlaybackQueueTrack,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
) {
    val context = LocalContext.current
    val artwork by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = track.mediaItem?.mediaId,
    ) {
        value = track.mediaItem?.let { loadEmbeddedArtwork(context, it) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(QueueItemBackground)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QueueArtwork(
            artwork = artwork,
            artworkSize = 76.dp,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = track.title,
                style = QueueCurrentTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = QueueCurrentSubtitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Image(
            painter = painterResource(
                id = if (isFavorite) {
                    R.drawable.playing_btn_favorite_cancel
                } else {
                    R.drawable.playing_btn_favorite_add
                },
            ),
            contentDescription = stringResource(R.string.favorite),
            modifier = Modifier
                .size(34.dp)
                .clickable(onClick = onFavoriteClick),
        )
    }
}

@Composable
private fun UpcomingTrackItem(
    track: PlaybackQueueTrack,
    dragKey: String,
    reorderEnabled: Boolean,
    isDragging: Boolean,
    dragOffsetY: Float,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val context = LocalContext.current
    val artwork by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = track.mediaItem?.mediaId,
    ) {
        value = track.mediaItem?.let { loadEmbeddedArtwork(context, it) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .background(if (isDragging) QueueDraggingItemBackground else QueueItemBackground)
            .graphicsLayer {
                translationY = dragOffsetY
            }
            .zIndex(if (isDragging) 1f else 0f)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QueueArtwork(
            artwork = artwork,
            artworkSize = 40.dp,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, end = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = track.title,
                style = QueueItemTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = QueueItemSubtitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (reorderEnabled) {
            QueueDragHandle(
                modifier = Modifier.pointerInput(dragKey) {
                    detectDragGestures(
                        onDragStart = {
                            onDragStart()
                        },
                        onDragEnd = onDragEnd,
                        onDragCancel = onDragEnd,
                    ) { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.y)
                    }
                },
            )
        }
    }
}

@Composable
private fun QueueArtwork(
    artwork: ImageBitmap?,
    artworkSize: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .size(artworkSize)
            .clip(RoundedCornerShape(4.dp))
            .background(QueueArtworkFallback),
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Image(
            painter = painterResource(id = R.drawable.mask_albumcover_list),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
private fun QueueDragHandle(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(36.dp)
            .height(36.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(1.5.dp)
                    .background(QueueHandleColor),
            )
            if (it < 2) {
                Spacer(modifier = Modifier.height(3.dp))
            }
        }
    }
}

private const val PlaybackQueueTrackKeyPrefix = "upcoming_"

private fun playbackQueueTrackKey(track: PlaybackQueueTrack): String {
    return "$PlaybackQueueTrackKeyPrefix${track.queueIndex}_${track.id}"
}

private fun PlaybackQueueTrack.withQueueIndexAfterMove(
    fromIndex: Int,
    toIndex: Int,
): PlaybackQueueTrack {
    if (queueIndex < 0 || fromIndex < 0 || toIndex < 0 || fromIndex == toIndex) {
        return this
    }
    val nextQueueIndex = when {
        queueIndex == fromIndex -> toIndex
        fromIndex < toIndex && queueIndex in (fromIndex + 1)..toIndex -> queueIndex - 1
        fromIndex > toIndex && queueIndex in toIndex until fromIndex -> queueIndex + 1
        else -> queueIndex
    }
    return if (nextQueueIndex == queueIndex) {
        this
    } else {
        copy(queueIndex = nextQueueIndex)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF7F7F7, widthDp = 360)
@Composable
private fun PlaybackQueueScreenPreview() {
    PlaybackQueueScreen(
        state = PlaybackQueueUiState(
            currentTrack = PlaybackQueueTrack(
                id = "current",
                title = "生如夏花",
                artist = "朴树",
            ),
            upcomingTracks = listOf(
                PlaybackQueueTrack("1", "苏珊的舞鞋", "朴树"),
                PlaybackQueueTrack("2", "今夜的滋味", "朴树"),
                PlaybackQueueTrack("3", "Colorful Days", "朴树"),
                PlaybackQueueTrack("4", "来不及", "朴树"),
            ),
        ),
        onExitFullScreenClick = {},
        onReturnToPlaybackClick = {},
        onItemClick = {},
        onFavoriteCurrentClick = {},
        onClearUpcomingClick = {},
        onMoveRequest = { _, _ -> },
    )
}
