package com.smartisanos.music.ui.loved

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.data.favorite.FavoriteSongsRepository
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.await
import com.smartisanos.music.ui.components.SmartisanBlankState
import com.smartisanos.music.ui.components.SmartisanTopBarIconButton
import com.smartisanos.music.ui.components.SmartisanTopBarIconButtonStyle
import com.smartisanos.music.ui.components.audioPermission
import com.smartisanos.music.ui.components.hasAudioPermission
import com.smartisanos.music.ui.components.loadEmbeddedArtwork
import com.smartisanos.music.ui.playlist.PlaylistTrackActionDialog
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.launch

private val LovedSongsPageBackground = Color(0xFFF8F8F8)
private val LovedSongsRowBackground = Color(0xFFFDFDFD)
private val LovedSongsRowPressedBackground = Color(0xFFF1F1F1)
private val LovedSongsRowDivider = Color(0xFFE9E9E9)
private val LovedSongsTitleColor = Color(0xCC000000)
private val LovedSongsSubtitleColor = Color(0x73000000)
private val LovedSongsPlayingColor = Color(0xFFBDBDBD)
private val LovedSongsArtworkFallback = Color(0xFFF0F0F0)

private val LovedSongsTitleStyle = TextStyle(
    fontSize = 17.sp,
    fontWeight = FontWeight.Medium,
    color = LovedSongsTitleColor,
)
private val LovedSongsSubtitleStyle = TextStyle(
    fontSize = 13.sp,
    color = LovedSongsSubtitleColor,
)

private val LovedSongsActionBarHeight = 45.dp
private val LovedSongsActionBarHorizontalPadding = 5.dp
private val LovedSongsActionSideButtonWidth = 42.dp
private val LovedSongsActionMiddleSpacing = 5.dp
private val LovedSongsShuffleIconSize = 18.dp
private val LovedSongsPlayIconSize = 14.dp
private val LovedSongsRowHeight = 61.dp
private val LovedSongsCoverFrameWidth = 64.dp
private val LovedSongsCoverSize = 50.dp
private val LovedSongsRowHorizontalPadding = 11.dp
private val LovedSongsTextSpacing = 12.dp
private val LovedSongsMoreButtonWidth = 40.dp
private val LovedSongsMiniPlayerReservedHeight = 73.dp

@Composable
fun LovedSongsScreen(
    modifier: Modifier = Modifier,
    onRequestAddToPlaylist: (List<MediaItem>) -> Unit = {},
    onRequestAddToQueue: (List<MediaItem>) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playbackBrowser = LocalPlaybackBrowser.current
    val exclusionsStore = remember(context.applicationContext) {
        LibraryExclusionsStore(context.applicationContext)
    }
    val favoriteRepository = remember(context.applicationContext) {
        FavoriteSongsRepository.getInstance(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    val libraryRevision by exclusionsStore.revision.collectAsState(initial = 0)
    val favorites by favoriteRepository.observeFavorites().collectAsState(initial = emptyList())
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }
    var permissionVersion by remember { mutableIntStateOf(0) }
    var songs by remember(playbackBrowser) { mutableStateOf(emptyList<MediaItem>()) }
    var currentMediaId by remember(playbackBrowser) {
        mutableStateOf(playbackBrowser?.currentMediaItem?.mediaId)
    }
    var pendingActionMediaId by rememberSaveable { mutableStateOf<String?>(null) }
    var sortMode by rememberSaveable { mutableStateOf(LovedSongsSortMode.Time) }
    val hasPermission = hasAudioPermission(context)
    val titleComparator = remember {
        val collator = Collator.getInstance(Locale.getDefault())
        Comparator<String> { left, right ->
            collator.compare(left, right)
        }
    }
    val sortedEntries = remember(favorites, songs, sortMode, titleComparator) {
        sortLovedSongEntries(
            entries = buildLovedSongEntries(
                favorites = favorites,
                visibleSongs = songs,
            ),
            sortMode = sortMode,
            titleComparator = titleComparator,
        )
    }
    val pendingActionEntry = remember(pendingActionMediaId, sortedEntries) {
        sortedEntries.firstOrNull { it.mediaItem.mediaId == pendingActionMediaId }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionVersion += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(playbackBrowser) {
        val browser = playbackBrowser ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                currentMediaId = player.currentMediaItem?.mediaId
            }
        }
        browser.addListener(listener)
        currentMediaId = browser.currentMediaItem?.mediaId
        onDispose {
            browser.removeListener(listener)
        }
    }

    LaunchedEffect(playbackBrowser, permissionVersion, libraryRevision, hasPermission) {
        val browser = playbackBrowser ?: run {
            songs = emptyList()
            return@LaunchedEffect
        }
        if (!hasPermission) {
            songs = emptyList()
            return@LaunchedEffect
        }

        val rootResult = browser.getLibraryRoot(null).await(context)
        val rootItem = rootResult.value ?: run {
            songs = emptyList()
            return@LaunchedEffect
        }
        val childrenResult = browser.getChildren(rootItem.mediaId, 0, Int.MAX_VALUE, null).await(context)
        songs = childrenResult.value?.toList().orEmpty()
    }

    if (pendingActionEntry != null) {
        PlaylistTrackActionDialog(
            thirdActionText = stringResource(R.string.cancel_love),
            thirdActionIconRes = R.drawable.more_select_icon_favorite_cancel,
            thirdActionPressedIconRes = R.drawable.more_select_icon_favorite_cancel_down,
            onDismiss = {
                pendingActionMediaId = null
            },
            onAddToPlaylistClick = {
                pendingActionMediaId = null
                onRequestAddToPlaylist(listOf(pendingActionEntry.mediaItem))
            },
            onAddToQueueClick = {
                pendingActionMediaId = null
                onRequestAddToQueue(listOf(pendingActionEntry.mediaItem))
            },
            onThirdActionClick = {
                val mediaId = pendingActionEntry.mediaItem.mediaId
                pendingActionMediaId = null
                scope.launch {
                    favoriteRepository.remove(mediaId)
                }
            },
        )
    }

    if (!hasPermission) {
        LovedSongsPermissionState(
            modifier = modifier,
            onGrantPermission = {
                permissionLauncher.launch(audioPermission())
            },
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            },
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LovedSongsPageBackground),
    ) {
        LovedSongsActionBar(
            sortMode = sortMode,
            hasSongs = sortedEntries.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
            onSortModeChange = { sortMode = it },
            onPlayClick = {
                val request = buildLovedSongsPlayRequest(sortedEntries)
                if (request != null) {
                    playbackBrowser?.setMediaItems(request.mediaItems, request.startIndex, 0L)
                    playbackBrowser?.prepare()
                    playbackBrowser?.play()
                }
            },
            onShuffleClick = {
                val request = buildLovedSongsShuffleRequest(sortedEntries)
                if (request != null) {
                    playbackBrowser?.setMediaItems(request.mediaItems, request.startIndex, 0L)
                    playbackBrowser?.prepare()
                    playbackBrowser?.play()
                }
            },
        )
        if (sortedEntries.isEmpty()) {
            SmartisanBlankState(
                iconRes = R.drawable.blank_playlist,
                title = stringResource(R.string.no_saved_song),
                subtitle = stringResource(R.string.show_saved_song),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LovedSongsPageBackground),
                contentPadding = PaddingValues(bottom = LovedSongsMiniPlayerReservedHeight),
            ) {
                itemsIndexed(
                    items = sortedEntries,
                    key = { _, item -> item.mediaItem.mediaId },
                ) { index, entry ->
                    LovedSongRow(
                        mediaItem = entry.mediaItem,
                        isCurrent = entry.mediaItem.mediaId == currentMediaId,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val request = buildLovedSongsPlayRequest(
                                entries = sortedEntries,
                                startMediaId = entry.mediaItem.mediaId,
                            )
                            if (request != null) {
                                playbackBrowser?.setMediaItems(request.mediaItems, request.startIndex, 0L)
                                playbackBrowser?.prepare()
                                playbackBrowser?.play()
                            }
                        },
                        onMoreClick = {
                            pendingActionMediaId = sortedEntries[index].mediaItem.mediaId
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LovedSongsPermissionState(
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SmartisanBlankState(
                iconRes = R.drawable.blank_playlist,
                title = stringResource(R.string.audio_permission_title),
                subtitle = stringResource(R.string.audio_permission_subtitle),
            )
            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onGrantPermission) {
                    Text(text = stringResource(R.string.audio_permission_action))
                }
                Button(onClick = onOpenSettings) {
                    Text(text = stringResource(R.string.audio_permission_settings))
                }
            }
        }
    }
}

@Composable
private fun LovedSongsActionBar(
    sortMode: LovedSongsSortMode,
    hasSongs: Boolean,
    modifier: Modifier = Modifier,
    onSortModeChange: (LovedSongsSortMode) -> Unit,
    onPlayClick: () -> Unit,
    onShuffleClick: () -> Unit,
) {
    val sortTabsHeight = rememberDrawableMinHeightDp(R.drawable.btn_category_songname)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LovedSongsActionBarHeight),
    ) {
        DrawableBackground(
            drawableRes = R.drawable.list_item,
            modifier = Modifier.matchParentSize(),
        )
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = LovedSongsActionBarHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SmartisanTopBarIconButton(
                enabled = hasSongs,
                iconRes = R.drawable.btn_shuffle3,
                pressedIconRes = R.drawable.btn_shuffle3_down,
                iconSize = LovedSongsShuffleIconSize,
                contentDescription = stringResource(R.string.play_shuffle),
                width = LovedSongsActionSideButtonWidth,
                buttonStyle = SmartisanTopBarIconButtonStyle.Filled,
                onClick = onShuffleClick,
            )
            Spacer(modifier = Modifier.width(LovedSongsActionMiddleSpacing))
            LovedSongsSortTabs(
                sortMode = sortMode,
                modifier = Modifier
                    .weight(1f)
                    .height(sortTabsHeight),
                onSortModeChange = onSortModeChange,
            )
            Spacer(modifier = Modifier.width(LovedSongsActionMiddleSpacing))
            SmartisanTopBarIconButton(
                enabled = hasSongs,
                iconRes = R.drawable.album_btn_play,
                pressedIconRes = R.drawable.album_btn_play_down,
                iconSize = LovedSongsPlayIconSize,
                contentDescription = stringResource(R.string.play),
                width = LovedSongsActionSideButtonWidth,
                buttonStyle = SmartisanTopBarIconButtonStyle.Filled,
                onClick = onPlayClick,
            )
        }
    }
}

@Composable
private fun LovedSongsSortTabs(
    sortMode: LovedSongsSortMode,
    modifier: Modifier = Modifier,
    onSortModeChange: (LovedSongsSortMode) -> Unit,
) {
    Row(modifier = modifier) {
        LovedSongsSortTab(
            text = stringResource(R.string.time),
            selected = sortMode == LovedSongsSortMode.Time,
            normalBackgroundRes = R.drawable.btn_category_songname,
            selectedBackgroundRes = R.drawable.btn_category_songname_down,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onClick = {
                onSortModeChange(LovedSongsSortMode.Time)
            },
        )
        LovedSongsSortTab(
            text = stringResource(R.string.song_name),
            selected = sortMode == LovedSongsSortMode.SongName,
            normalBackgroundRes = R.drawable.btn_category_views,
            selectedBackgroundRes = R.drawable.btn_category_views_down,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onClick = {
                onSortModeChange(LovedSongsSortMode.SongName)
            },
        )
    }
}

@Composable
private fun LovedSongsSortTab(
    text: String,
    selected: Boolean,
    @DrawableRes normalBackgroundRes: Int,
    @DrawableRes selectedBackgroundRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val backgroundRes = if (selected || pressed) {
        selectedBackgroundRes
    } else {
        normalBackgroundRes
    }
    val textColor = if (selected || pressed) {
        Color.White
    } else {
        Color(0x9A000000)
    }

    Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        ),
        contentAlignment = Alignment.Center,
    ) {
        DrawableBackground(
            drawableRes = backgroundRes,
            modifier = Modifier.matchParentSize(),
        )
        Text(
            text = text,
            style = LovedSongsSubtitleStyle.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DrawableBackground(
    @DrawableRes drawableRes: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Canvas(modifier = modifier) {
        val drawable = context.getDrawable(drawableRes) ?: return@Canvas
        drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
        drawIntoCanvas { canvas ->
            drawable.draw(canvas.nativeCanvas)
        }
    }
}

@Composable
private fun rememberDrawableMinHeightDp(
    @DrawableRes drawableRes: Int,
): androidx.compose.ui.unit.Dp {
    val context = LocalContext.current
    val density = LocalDensity.current

    return remember(context, density, drawableRes) {
        val minHeightPx = context.getDrawable(drawableRes)?.minimumHeight ?: 0
        with(density) { minHeightPx.toDp() }
    }
}

@Composable
private fun LovedSongRow(
    mediaItem: MediaItem,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val title = mediaItem.mediaMetadata.displayTitle?.toString()
        ?: mediaItem.mediaMetadata.title?.toString()
        ?: ""
    val subtitle = mediaItem.mediaMetadata.subtitle?.toString()
        ?: mediaItem.mediaMetadata.artist?.toString()
        ?: ""

    Box(
        modifier = modifier
            .background(if (pressed) LovedSongsRowPressedBackground else LovedSongsRowBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(LovedSongsRowHeight)
                .padding(start = LovedSongsRowHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LovedSongsArtwork(
                mediaItem = mediaItem,
                modifier = Modifier
                    .width(LovedSongsCoverFrameWidth)
                    .fillMaxHeight(),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = LovedSongsTextSpacing),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = LovedSongsTitleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = LovedSongsSubtitleStyle,
                    color = if (isCurrent) LovedSongsPlayingColor else LovedSongsSubtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            LovedSongsMoreButton(
                modifier = Modifier
                    .width(LovedSongsMoreButtonWidth)
                    .fillMaxHeight(),
                onClick = onMoreClick,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LovedSongsRowDivider)
                .align(Alignment.BottomStart),
        )
    }
}

@Composable
private fun LovedSongsArtwork(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val artwork by produceState<ImageBitmap?>(initialValue = null, mediaItem.mediaId) {
        value = loadEmbeddedArtwork(context, mediaItem)
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .padding(start = 1.dp)
                .size(LovedSongsCoverSize)
                .background(LovedSongsArtworkFallback),
        ) {
            if (artwork != null) {
                Image(
                    bitmap = artwork!!,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
            Image(
                painter = painterResource(R.drawable.mask_albumcover_list),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

@Composable
private fun LovedSongsMoreButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(
                if (pressed) R.drawable.btn_more_white else R.drawable.btn_more,
            ),
            contentDescription = stringResource(R.string.select_action),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun LovedSongRowPreview() {
    LovedSongRow(
        mediaItem = MediaItem.Builder()
            .setMediaId("preview")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("程艾影")
                    .setDisplayTitle("程艾影")
                    .setArtist("赵雷")
                    .setSubtitle("赵雷")
                    .build(),
            )
            .build(),
        isCurrent = true,
        onClick = {},
        onMoreClick = {},
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F8F8, widthDp = 360)
@Composable
private fun LovedSongsActionBarPreview() {
    LovedSongsActionBar(
        sortMode = LovedSongsSortMode.Time,
        hasSongs = true,
        onSortModeChange = {},
        onPlayClick = {},
        onShuffleClick = {},
    )
}
