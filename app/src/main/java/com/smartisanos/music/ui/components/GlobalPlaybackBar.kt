package com.smartisanos.music.ui.components

import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.data.favorite.FavoriteSongsRepository
import com.smartisanos.music.playback.LocalPlaybackController
import kotlinx.coroutines.launch

private val PlaybackBarTitleColor = Color(0xCC000000)
private val PlaybackBarSubtitleColor = Color(0x66000000)
private val PlaybackBarContentHeight = 61.dp
private val PlaybackBarShadowHeight = 6.dp
private val PlaybackBarArtworkSize = 50.dp
private val PlaybackBarSidePadding = 12.dp
private val PlaybackBarRightPadding = 3.dp
private val PlaybackBarButtonSpacing = 3.dp
private val PlaybackBarTextWidth = 143.3.dp

private val PlaybackBarTitleStyle = TextStyle(
    fontSize = 16.sp,
    color = PlaybackBarTitleColor,
)
private val PlaybackBarSubtitleStyle = TextStyle(
    fontSize = 13.sp,
    color = PlaybackBarSubtitleColor,
)

private data class PlaybackBarSnapshot(
    val mediaItem: MediaItem? = null,
    val isPlaying: Boolean = false,
)

@Composable
fun GlobalPlaybackBar(
    modifier: Modifier = Modifier,
    onOpenPlayback: () -> Unit = {},
) {
    val context = LocalContext.current
    val controller = LocalPlaybackController.current ?: return
    val favoriteRepository = remember(context.applicationContext) {
        FavoriteSongsRepository.getInstance(context.applicationContext)
    }
    val favoriteIds by favoriteRepository.observeFavoriteIds().collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    var snapshot by remember(controller) {
        mutableStateOf(
            PlaybackBarSnapshot(
                mediaItem = controller.currentMediaItem,
                isPlaying = controller.isPlaying,
            ),
        )
    }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                snapshot = PlaybackBarSnapshot(
                    mediaItem = player.currentMediaItem,
                    isPlaying = player.isPlaying,
                )
            }
        }
        controller.addListener(listener)
        onDispose {
            controller.removeListener(listener)
        }
    }

    val mediaItem = snapshot.mediaItem ?: return
    val mediaId = mediaItem.mediaId
    val isFavorite = mediaId in favoriteIds
    val title = mediaItem.mediaMetadata.displayTitle?.toString()
        ?: mediaItem.mediaMetadata.title?.toString()
        ?: context.getString(R.string.unknown_song_title)
    val subtitle = mediaItem.mediaMetadata.subtitle?.toString()
        ?: mediaItem.mediaMetadata.artist?.toString()
        ?: context.getString(R.string.unknown_artist)

    Column(modifier = modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.now_playing_bar_shadow),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxWidth()
                .height(PlaybackBarShadowHeight),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlaybackBarContentHeight),
        ) {
            AndroidView(
                factory = { viewContext ->
                    ImageView(viewContext).apply {
                        scaleType = ImageView.ScaleType.FIT_XY
                        setImageResource(R.drawable.floatplay_bg)
                    }
                },
                modifier = Modifier.matchParentSize(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(PlaybackBarContentHeight)
                    .padding(start = PlaybackBarSidePadding, end = PlaybackBarRightPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onOpenPlayback,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaybackBarArtwork(
                        mediaItem = mediaItem,
                        modifier = Modifier.size(PlaybackBarArtworkSize),
                    )
                    Column(
                        modifier = Modifier
                            .padding(start = PlaybackBarSidePadding)
                            .width(PlaybackBarTextWidth),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        TextLine(
                            text = title,
                            style = PlaybackBarTitleStyle,
                            marquee = true,
                        )
                        TextLine(
                            text = subtitle,
                            style = PlaybackBarSubtitleStyle,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaybackBarButton(
                        normalRes = if (isFavorite) {
                            R.drawable.floatplay_btn_favorite_cancel
                        } else {
                            R.drawable.floatplay_btn_favorite_add
                        },
                        pressedRes = if (isFavorite) {
                            R.drawable.floatplay_btn_favorite_cancel_down
                        } else {
                            R.drawable.floatplay_btn_favorite_add_down
                        },
                        contentDescription = stringResource(R.string.favorite),
                        onClick = {
                            scope.launch {
                                favoriteRepository.toggle(mediaId)
                            }
                        },
                    )
                    PlaybackBarButton(
                        normalRes = R.drawable.thumbnail_floatplay_btn_prev,
                        pressedRes = R.drawable.thumbnail_floatplay_btn_prev_down,
                        contentDescription = stringResource(R.string.previous_song),
                        onClick = controller::seekToPrevious,
                    )
                    PlaybackBarButton(
                        normalRes = if (snapshot.isPlaying) {
                            R.drawable.floatplay_btn_pause
                        } else {
                            R.drawable.floatplay_btn_play
                        },
                        pressedRes = if (snapshot.isPlaying) {
                            R.drawable.floatplay_btn_pause_down
                        } else {
                            R.drawable.floatplay_btn_play_down
                        },
                        contentDescription = if (snapshot.isPlaying) {
                            stringResource(R.string.pause)
                        } else {
                            stringResource(R.string.play)
                        },
                        onClick = {
                            if (snapshot.isPlaying) {
                                controller.pause()
                            } else {
                                controller.play()
                            }
                        },
                    )
                    PlaybackBarButton(
                        normalRes = R.drawable.floatplay_btn_next,
                        pressedRes = R.drawable.floatplay_btn_next_down,
                        contentDescription = stringResource(R.string.next_song),
                        isLast = true,
                        onClick = controller::seekToNext,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaybackBarArtwork(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val artwork by produceState<ImageBitmap?>(initialValue = null, mediaItem.mediaId) {
        value = loadEmbeddedArtwork(context, mediaItem)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFFF0F0F0)),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork!!,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Image(
                painter = painterResource(R.drawable.playing_cover_lp),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun PlaybackBarButton(
    normalRes: Int,
    pressedRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    isLast: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .padding(end = if (isLast) 0.dp else PlaybackBarButtonSpacing)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(if (pressed) pressedRes else normalRes),
            contentDescription = contentDescription,
        )
    }
}

@Composable
private fun TextLine(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    marquee: Boolean = false,
) {
    androidx.compose.material3.Text(
        text = text,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (marquee) {
                    Modifier.basicMarquee()
                } else {
                    Modifier
                }
            ),
    )
}
