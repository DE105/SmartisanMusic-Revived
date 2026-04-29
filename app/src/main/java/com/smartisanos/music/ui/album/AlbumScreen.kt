@file:OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)

package com.smartisanos.music.ui.album

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import com.smartisanos.music.R
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.await
import com.smartisanos.music.ui.components.SecondaryPageTransition
import com.smartisanos.music.ui.components.SmartisanBlankState
import com.smartisanos.music.ui.components.audioPermission
import com.smartisanos.music.ui.components.hasAudioPermission
import com.smartisanos.music.ui.components.loadArtwork

private val AlbumPageBackground = Color.White
private val AlbumHeaderBackground = Color(0xFFF2F2F2)
private val AlbumActionRowBackground = Color(0xFFFBFBFB)
private val AlbumRowDivider = Color(0xFFE9E9E9)
private val AlbumStrongDivider = Color(0xFFD6D6D6)
private val AlbumTitleColor = Color(0xFF353539)
private val AlbumHeaderTitleColor = Color(0xCC000000)
private val AlbumHeaderSecondaryColor = Color(0x66000000)
private val AlbumSubtitleColor = Color(0xFFA4A7AC)
private val AlbumSelectedColor = Color(0xFFE64040)
private val AlbumCoverBackground = Color(0xFFF0F0F0)
private val AlbumActionColor = Color(0x8F000000)
private val AlbumDurationColor = Color(0x99000000)

private val AlbumTitleStyle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    color = AlbumTitleColor,
)
private val AlbumSubtitleStyle = TextStyle(
    fontSize = 13.sp,
    color = AlbumSubtitleColor,
)
private val AlbumTileTitleStyle = TextStyle(
    fontSize = 13.sp,
    color = Color.Black,
    textAlign = TextAlign.Center,
)
private val AlbumDetailTitleStyle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.Medium,
    color = AlbumHeaderTitleColor,
)
private val AlbumDetailArtistStyle = TextStyle(
    fontSize = 14.sp,
    color = AlbumHeaderSecondaryColor,
)
private val AlbumDetailYearStyle = TextStyle(
    fontSize = 12.sp,
    color = AlbumHeaderSecondaryColor,
)
private val AlbumActionTextStyle = TextStyle(
    fontSize = 14.sp,
    color = AlbumActionColor,
)
private val AlbumTrackIndexStyle = TextStyle(
    fontSize = 14.sp,
    color = AlbumSubtitleColor,
    textAlign = TextAlign.Center,
)
private val AlbumTrackDurationStyle = TextStyle(
    fontSize = 13.sp,
    color = AlbumDurationColor,
)

private val AlbumListRowHeight = 61.dp
private val AlbumListCoverFrameWidth = 64.dp
private val AlbumListCoverSize = 50.dp
private val AlbumGridCellWidth = 116.dp
private val AlbumGridCoverSize = 116.dp
private val AlbumGridCoverPadding = 3.dp
private val AlbumGridTopPadding = 7.dp
private val AlbumDetailHeaderHeight = 139.dp
private val AlbumDetailCoverSize = 116.dp
private val AlbumDetailActionRowHeight = 45.dp
private val AlbumDetailIconSize = 43.dp
private val AlbumTrackIndexWidth = 32.dp
private val MiniPlayerReservedHeight = 73.dp
private const val AlbumViewSwitchBoundsMillis = 240
private const val AlbumViewSwitchContentFadeMillis = 70
private const val AlbumViewSwitchContentFadeDelayMillis = 20

private val AlbumViewSwitchBoundsTransform = BoundsTransform { _, _ ->
    tween(durationMillis = AlbumViewSwitchBoundsMillis, easing = FastOutSlowInEasing)
}

@Composable
fun AlbumScreen(
    libraryRefreshVersion: Int,
    viewMode: AlbumViewMode,
    selectedAlbumId: String?,
    onAlbumSelected: (String, String) -> Unit,
    onAlbumBack: () -> Unit,
    onAddToPlaylistRequest: (List<MediaItem>) -> Unit = {},
    onAddToQueueRequest: (List<MediaItem>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playbackBrowser = LocalPlaybackBrowser.current
    val exclusionsStore = remember(context.applicationContext) {
        LibraryExclusionsStore(context.applicationContext)
    }
    val libraryRevision by exclusionsStore.revision.collectAsState(initial = 0)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }
    var permissionVersion by remember { mutableIntStateOf(0) }
    var songs by remember(playbackBrowser) { mutableStateOf(emptyList<MediaItem>()) }
    var currentMediaId by remember(playbackBrowser) {
        mutableStateOf(playbackBrowser?.currentMediaItem?.mediaId)
    }
    val hasPermission = hasAudioPermission(context)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionVersion++
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

    LaunchedEffect(playbackBrowser, permissionVersion, libraryRevision, libraryRefreshVersion, hasPermission) {
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

    if (!hasPermission) {
        AlbumPermissionState(
            modifier = modifier,
            onGrantPermission = {
                permissionLauncher.launch(audioPermission())
            },
            onOpenSettings = {
                context.openAppSettings()
            },
        )
        return
    }

    val unknownAlbumTitle = stringResource(R.string.unknown_album)
    val multipleArtistsTitle = stringResource(R.string.many_artist)
    val albums = remember(songs, unknownAlbumTitle, multipleArtistsTitle) {
        buildAlbumSummaries(
            mediaItems = songs,
            unknownAlbumTitle = unknownAlbumTitle,
            multipleArtistsTitle = multipleArtistsTitle,
        )
    }

    if (albums.isEmpty()) {
        SmartisanBlankState(
            iconRes = R.drawable.blank_album,
            title = stringResource(R.string.no_album),
            subtitle = stringResource(R.string.show_album),
            modifier = modifier,
        )
        return
    }

    BackHandler(enabled = selectedAlbumId != null, onBack = onAlbumBack)
    SecondaryPageTransition(
        secondaryKey = selectedAlbumId,
        modifier = modifier.fillMaxSize(),
        label = "album detail page",
        primaryContent = {
            AlbumOverview(
                viewMode = viewMode,
                albums = albums,
                currentMediaId = currentMediaId,
                onAlbumSelected = onAlbumSelected,
                modifier = Modifier.fillMaxSize(),
            )
        },
        secondaryContent = { targetAlbumId ->
            val album = albums.firstOrNull { it.id == targetAlbumId }
            if (album != null) {
                AlbumDetail(
                    album = album,
                    currentMediaId = currentMediaId,
                    playbackBrowser = playbackBrowser,
                    onAddToPlaylistRequest = onAddToPlaylistRequest,
                    onAddToQueueRequest = onAddToQueueRequest,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    )
}

@Composable
private fun AlbumOverview(
    viewMode: AlbumViewMode,
    albums: List<AlbumSummary>,
    currentMediaId: String?,
    onAlbumSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val artworkCache = remember(albums) {
        mutableStateMapOf<String, ImageBitmap?>()
    }

    SharedTransitionLayout(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = viewMode,
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(
                        durationMillis = AlbumViewSwitchContentFadeMillis,
                        delayMillis = AlbumViewSwitchContentFadeDelayMillis,
                    ),
                ) togetherWith fadeOut(
                    animationSpec = tween(durationMillis = AlbumViewSwitchContentFadeMillis),
                ) using SizeTransform(clip = false)
            },
            label = "album view mode",
        ) { targetViewMode ->
            when (targetViewMode) {
                AlbumViewMode.List -> AlbumList(
                    albums = albums,
                    currentMediaId = currentMediaId,
                    artworkCache = artworkCache,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                    onAlbumSelected = onAlbumSelected,
                    modifier = Modifier.fillMaxSize(),
                )
                AlbumViewMode.Tile -> AlbumGrid(
                    albums = albums,
                    currentMediaId = currentMediaId,
                    artworkCache = artworkCache,
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent,
                    onAlbumSelected = onAlbumSelected,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun AlbumGrid(
    albums: List<AlbumSummary>,
    currentMediaId: String?,
    artworkCache: MutableMap<String, ImageBitmap?>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAlbumSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(AlbumGridCellWidth),
        modifier = modifier
            .fillMaxSize()
            .background(AlbumPageBackground),
        contentPadding = PaddingValues(
            start = 9.dp,
            top = AlbumGridTopPadding,
            end = 9.dp,
            bottom = MiniPlayerReservedHeight,
        ),
        horizontalArrangement = Arrangement.spacedBy(1.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(
            items = albums,
            key = { it.id },
        ) { album ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                AlbumTile(
                    album = album,
                    selected = album.songs.any { it.mediaId == currentMediaId },
                    artworkCache = artworkCache,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = {
                        onAlbumSelected(album.id, album.title)
                    },
                )
            }
        }
    }
}

@Composable
private fun AlbumTile(
    album: AlbumSummary,
    selected: Boolean,
    artworkCache: MutableMap<String, ImageBitmap?>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(AlbumGridCellWidth)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AlbumArtwork(
            mediaItem = album.representative,
            fallbackRes = R.drawable.noalbumcover_220,
            artworkCache = artworkCache,
            modifier = Modifier
                .size(AlbumGridCoverSize)
                .albumSharedBounds(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    key = album.artworkSharedKey,
                    zIndexInOverlay = 2f,
                ),
            contentPadding = AlbumGridCoverPadding,
        )
        Text(
            text = album.title,
            style = AlbumTileTitleStyle,
            color = if (selected) AlbumSelectedColor else Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp, start = 4.dp, end = 4.dp),
        )
    }
}

@Composable
private fun AlbumList(
    albums: List<AlbumSummary>,
    currentMediaId: String?,
    artworkCache: MutableMap<String, ImageBitmap?>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onAlbumSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AlbumPageBackground),
        contentPadding = PaddingValues(bottom = MiniPlayerReservedHeight),
    ) {
        items(
            items = albums,
            key = { it.id },
        ) { album ->
            AlbumListRow(
                album = album,
                selected = album.songs.any { it.mediaId == currentMediaId },
                artworkCache = artworkCache,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onClick = {
                    onAlbumSelected(album.id, album.title)
                },
            )
        }
    }
}

@Composable
private fun AlbumListRow(
    album: AlbumSummary,
    selected: Boolean,
    artworkCache: MutableMap<String, ImageBitmap?>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
) {
    val trackCount = stringResource(R.string.album_track_count, album.trackCount)

    Column(
        modifier = Modifier
            .background(AlbumPageBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AlbumListRowHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(AlbumListCoverFrameWidth)
                    .height(AlbumListRowHeight),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(AlbumListCoverSize)) {
                    AlbumArtwork(
                        mediaItem = album.representative,
                        fallbackRes = R.drawable.noalbumcover_120,
                        artworkCache = artworkCache,
                        modifier = Modifier
                            .matchParentSize()
                            .albumSharedBounds(
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope,
                                key = album.artworkSharedKey,
                                zIndexInOverlay = 2f,
                            ),
                        showListMask = true,
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 18.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = album.title,
                    style = AlbumTitleStyle,
                    color = if (selected) AlbumSelectedColor else AlbumTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${album.artist} · $trackCount",
                    style = AlbumSubtitleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AlbumRowDivider),
        )
    }
}

@Composable
private fun Modifier.albumSharedBounds(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    key: Any,
    zIndexInOverlay: Float = 0f,
): Modifier {
    return with(sharedTransitionScope) {
        sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animatedVisibilityScope,
            enter = fadeIn(
                animationSpec = tween(durationMillis = AlbumViewSwitchContentFadeMillis),
            ),
            exit = fadeOut(
                animationSpec = tween(durationMillis = AlbumViewSwitchContentFadeMillis),
            ),
            boundsTransform = AlbumViewSwitchBoundsTransform,
            resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.Crop),
            zIndexInOverlay = zIndexInOverlay,
        )
    }
}

private val AlbumSummary.artworkSharedKey: String
    get() = "album:${id}:artwork"

@Composable
private fun AlbumArtwork(
    mediaItem: MediaItem,
    fallbackRes: Int,
    artworkCache: MutableMap<String, ImageBitmap?>? = null,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = 0.dp,
    showListMask: Boolean = false,
) {
    val context = LocalContext.current
    val artworkCacheKey = mediaItem.mediaId
    val hasCachedArtwork = artworkCache?.containsKey(artworkCacheKey) == true
    val cachedArtwork = artworkCache?.get(artworkCacheKey)
    val artwork by produceState<ImageBitmap?>(
        initialValue = cachedArtwork,
        mediaItem.mediaId,
        artworkCache,
    ) {
        if (hasCachedArtwork) {
            return@produceState
        }

        val loadedArtwork = loadArtwork(context, mediaItem)
        artworkCache?.put(artworkCacheKey, loadedArtwork)
        value = loadedArtwork
    }

    Box(
        modifier = modifier
            .background(AlbumCoverBackground)
            .clipToBounds()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(
                painter = painterResource(fallbackRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (showListMask) {
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
private fun AlbumDetail(
    album: AlbumSummary,
    currentMediaId: String?,
    playbackBrowser: MediaBrowser?,
    onAddToPlaylistRequest: (List<MediaItem>) -> Unit,
    onAddToQueueRequest: (List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AlbumPageBackground),
        contentPadding = PaddingValues(bottom = MiniPlayerReservedHeight),
    ) {
        item(key = "${album.id}:header") {
            AlbumDetailHeader(
                album = album,
                onAddToPlaylistClick = { onAddToPlaylistRequest(album.songs) },
                onAddToQueueClick = { onAddToQueueRequest(album.songs) },
            )
            AlbumDetailPlayActions(
                onPlayAll = {
                    playbackBrowser?.playAlbum(album)
                },
                onShuffle = {
                    playbackBrowser?.playAlbum(album, shuffled = true)
                },
            )
        }
        items(
            items = album.songs,
            key = { it.mediaId },
        ) { item ->
            val index = album.songs.indexOf(item) + 1
            AlbumTrackRow(
                mediaItem = item,
                index = index,
                selected = item.mediaId == currentMediaId,
                onClick = {
                    playbackBrowser?.playAlbumTrack(album, index - 1)
                },
            )
        }
    }
}

@Composable
private fun AlbumDetailHeader(
    album: AlbumSummary,
    onAddToPlaylistClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AlbumDetailHeaderHeight)
            .background(AlbumHeaderBackground)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlbumArtwork(
            mediaItem = album.representative,
            fallbackRes = R.drawable.noalbumcover_220,
            modifier = Modifier.size(AlbumDetailCoverSize),
            contentPadding = AlbumGridCoverPadding,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
        ) {
            Text(
                text = album.title,
                style = AlbumDetailTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = album.artist,
                style = AlbumDetailArtistStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                text = album.year?.toString().orEmpty(),
                style = AlbumDetailYearStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlbumHeaderIconButton(
                    iconRes = R.drawable.ablum_btn_addlist,
                    contentDescription = stringResource(R.string.add_to_playlist),
                    onClick = onAddToPlaylistClick,
                )
                AlbumHeaderIconButton(
                    iconRes = R.drawable.ablum_btn_addplay,
                    contentDescription = stringResource(R.string.add_to_queue),
                    onClick = onAddToQueueClick,
                )
            }
        }
    }
}

@Composable
private fun AlbumHeaderIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(AlbumDetailIconSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.matchParentSize(),
        )
    }
}

@Composable
private fun AlbumDetailPlayActions(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AlbumRowDivider),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AlbumDetailActionRowHeight)
                .background(AlbumActionRowBackground),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlbumTextAction(
                text = stringResource(R.string.play_all),
                onClick = onPlayAll,
                leadingContent = {
                    PlayTriangleIcon()
                },
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(AlbumRowDivider),
            )
            AlbumTextAction(
                text = stringResource(R.string.play_shuffle),
                onClick = onShuffle,
                leadingContent = {
                    Image(
                        painter = painterResource(R.drawable.btn_shuffle3),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AlbumRowDivider),
        )
    }
}

@Composable
private fun AlbumTextAction(
    text: String,
    onClick: () -> Unit,
    leadingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingContent()
        Text(
            text = text,
            style = AlbumActionTextStyle,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

@Composable
private fun PlayTriangleIcon() {
    Canvas(modifier = Modifier.size(width = 8.dp, height = 10.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path = path, color = AlbumActionColor)
    }
}

@Composable
private fun AlbumTrackRow(
    mediaItem: MediaItem,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val metadata = mediaItem.mediaMetadata
    val title = metadata.displayTitle ?: metadata.title
    val duration = formatDuration(metadata.durationMs ?: 0L)

    Column(
        modifier = Modifier
            .background(AlbumPageBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(AlbumListRowHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = mediaItem.displayTrackNumber(index),
                style = AlbumTrackIndexStyle,
                modifier = Modifier.width(AlbumTrackIndexWidth),
            )
            Text(
                text = title?.toString().orEmpty(),
                style = AlbumTitleStyle,
                color = if (selected) AlbumSelectedColor else AlbumTitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 5.dp, end = 8.dp),
            )
            Text(
                text = duration,
                style = AlbumTrackDurationStyle,
                maxLines = 1,
                modifier = Modifier.padding(end = 14.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AlbumRowDivider),
        )
    }
}

@Composable
private fun AlbumPermissionState(
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
                iconRes = R.drawable.blank_album,
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

private fun MediaBrowser.playAlbum(
    album: AlbumSummary,
    shuffled: Boolean = false,
) {
    setMediaItems(if (shuffled) album.songs.shuffled() else album.songs, 0, 0L)
    prepare()
    play()
}

private fun MediaBrowser.playAlbumTrack(
    album: AlbumSummary,
    index: Int,
) {
    setMediaItems(album.songs, index, 0L)
    prepare()
    play()
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ),
    )
}
