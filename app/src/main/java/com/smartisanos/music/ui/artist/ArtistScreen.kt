package com.smartisanos.music.ui.artist

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.lazy.items
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

private val ArtistPageBackground = Color.White
private val ArtistHeaderBackground = Color(0xFFF2F2F2)
private val ArtistActionRowBackground = Color(0xFFFBFBFB)
private val ArtistRowDivider = Color(0xFFE9E9E9)
private val ArtistTitleColor = Color(0xFF353539)
private val ArtistSubtitleColor = Color(0xFFA4A7AC)
private val ArtistSelectedColor = Color(0xFFE64040)
private val ArtistHeaderTitleColor = Color(0xCC000000)
private val ArtistHeaderSecondaryColor = Color(0x66000000)
private val ArtistCoverBackground = Color(0xFFF0F0F0)
private val ArtistActionColor = Color(0x8F000000)
private val ArtistDurationColor = Color(0x99000000)

private val ArtistTitleStyle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    color = ArtistTitleColor,
)
private val ArtistSubtitleStyle = TextStyle(
    fontSize = 13.sp,
    color = ArtistSubtitleColor,
)
private val ArtistDetailTitleStyle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.Medium,
    color = ArtistHeaderTitleColor,
)
private val ArtistDetailSubtitleStyle = TextStyle(
    fontSize = 14.sp,
    color = ArtistHeaderSecondaryColor,
)
private val ArtistActionTextStyle = TextStyle(
    fontSize = 14.sp,
    color = ArtistActionColor,
)
private val ArtistTrackIndexStyle = TextStyle(
    fontSize = 14.sp,
    color = ArtistSubtitleColor,
    textAlign = TextAlign.Center,
)
private val ArtistTrackDurationStyle = TextStyle(
    fontSize = 13.sp,
    color = ArtistDurationColor,
)

private val ArtistListRowHeight = 61.dp
private val ArtistListHorizontalPadding = 14.dp
private val ArtistDetailHeaderHeight = 139.dp
private val ArtistDetailCoverSize = 116.dp
private val ArtistDetailActionRowHeight = 45.dp
private val ArtistDetailIconSize = 43.dp
private val ArtistTrackIndexWidth = 32.dp
private val ArtistCoverPadding = 3.dp
private val MiniPlayerReservedHeight = 73.dp

@Composable
fun ArtistScreen(
    selectedArtistId: String?,
    onArtistSelected: (String, String) -> Unit,
    onArtistBack: () -> Unit,
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

    LaunchedEffect(playbackBrowser, permissionVersion, libraryRevision) {
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
        ArtistPermissionState(
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

    val unknownArtistTitle = stringResource(R.string.unknown_artist)
    val unknownAlbumTitle = stringResource(R.string.unknown_album)
    val artists = remember(songs, unknownArtistTitle, unknownAlbumTitle) {
        buildArtistSummaries(
            mediaItems = songs,
            unknownArtistTitle = unknownArtistTitle,
            unknownAlbumTitle = unknownAlbumTitle,
        )
    }

    if (artists.isEmpty()) {
        SmartisanBlankState(
            iconRes = R.drawable.blank_artist,
            title = stringResource(R.string.no_artist),
            subtitle = stringResource(R.string.show_artist),
            modifier = modifier,
        )
        return
    }

    BackHandler(enabled = selectedArtistId != null, onBack = onArtistBack)
    SecondaryPageTransition(
        secondaryKey = selectedArtistId,
        modifier = modifier.fillMaxSize(),
        label = "artist detail page",
        primaryContent = {
            ArtistOverview(
                artists = artists,
                currentMediaId = currentMediaId,
                onArtistSelected = onArtistSelected,
                modifier = Modifier.fillMaxSize(),
            )
        },
        secondaryContent = { targetArtistId ->
            val artist = artists.firstOrNull { it.id == targetArtistId }
            if (artist != null) {
                ArtistDetail(
                    artist = artist,
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
private fun ArtistOverview(
    artists: List<ArtistSummary>,
    currentMediaId: String?,
    onArtistSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = MiniPlayerReservedHeight),
    ) {
        items(
            items = artists,
            key = { it.id },
        ) { artist ->
            ArtistListRow(
                artist = artist,
                selected = artist.songs.any { it.mediaId == currentMediaId },
                onClick = {
                    onArtistSelected(artist.id, artist.name)
                },
            )
        }
    }
}

@Composable
private fun ArtistListRow(
    artist: ArtistSummary,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val subtitle = stringResource(
        R.string.artist_album_song_count,
        artist.albumCount,
        artist.trackCount,
    )

    Column(
        modifier = Modifier
            .background(ArtistPageBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(ArtistListRowHeight)
                .padding(horizontal = ArtistListHorizontalPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = artist.name,
                style = ArtistTitleStyle,
                color = if (selected) ArtistSelectedColor else ArtistTitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = ArtistSubtitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ArtistRowDivider),
        )
    }
}

@Composable
private fun ArtistDetail(
    artist: ArtistSummary,
    currentMediaId: String?,
    playbackBrowser: MediaBrowser?,
    onAddToPlaylistRequest: (List<MediaItem>) -> Unit,
    onAddToQueueRequest: (List<MediaItem>) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ArtistPageBackground),
        contentPadding = PaddingValues(bottom = MiniPlayerReservedHeight),
    ) {
        item(key = "${artist.id}:header") {
            ArtistDetailHeader(
                artist = artist,
                onAddToPlaylistClick = { onAddToPlaylistRequest(artist.songs) },
                onAddToQueueClick = { onAddToQueueRequest(artist.songs) },
            )
            ArtistDetailPlayActions(
                onPlayAll = {
                    playbackBrowser?.playArtist(artist)
                },
                onShuffle = {
                    playbackBrowser?.playArtist(artist, shuffled = true)
                },
            )
        }
        items(
            items = artist.songs,
            key = { it.mediaId },
        ) { item ->
            val index = artist.songs.indexOf(item) + 1
            ArtistTrackRow(
                mediaItem = item,
                index = index,
                selected = item.mediaId == currentMediaId,
                onClick = {
                    playbackBrowser?.playArtistTrack(artist, index - 1)
                },
            )
        }
    }
}

@Composable
private fun ArtistDetailHeader(
    artist: ArtistSummary,
    onAddToPlaylistClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
) {
    val subtitle = stringResource(
        R.string.artist_album_song_count,
        artist.albumCount,
        artist.trackCount,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(ArtistDetailHeaderHeight)
            .background(ArtistHeaderBackground)
            .padding(start = 12.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtistArtwork(
            mediaItem = artist.representative,
            fallbackRes = R.drawable.noalbumcover_220,
            modifier = Modifier.size(ArtistDetailCoverSize),
            contentPadding = ArtistCoverPadding,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
        ) {
            Text(
                text = artist.name,
                style = ArtistDetailTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = ArtistDetailSubtitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtistHeaderIconButton(
                    iconRes = R.drawable.ablum_btn_addlist,
                    contentDescription = stringResource(R.string.add_to_playlist),
                    onClick = onAddToPlaylistClick,
                )
                ArtistHeaderIconButton(
                    iconRes = R.drawable.ablum_btn_addplay,
                    contentDescription = stringResource(R.string.add_to_queue),
                    onClick = onAddToQueueClick,
                )
            }
        }
    }
}

@Composable
private fun ArtistArtwork(
    mediaItem: MediaItem,
    @DrawableRes fallbackRes: Int,
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val context = LocalContext.current
    val artwork by produceState<ImageBitmap?>(initialValue = null, mediaItem.mediaId) {
        value = loadArtwork(context, mediaItem)
    }

    Box(
        modifier = modifier
            .background(ArtistCoverBackground)
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
    }
}

@Composable
private fun ArtistHeaderIconButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(ArtistDetailIconSize)
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
private fun ArtistDetailPlayActions(
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ArtistRowDivider),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ArtistDetailActionRowHeight)
                .background(ArtistActionRowBackground),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtistTextAction(
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
                    .background(ArtistRowDivider),
            )
            ArtistTextAction(
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
                .background(ArtistRowDivider),
        )
    }
}

@Composable
private fun ArtistTextAction(
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
            style = ArtistActionTextStyle,
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
        drawPath(path = path, color = ArtistActionColor)
    }
}

@Composable
private fun ArtistTrackRow(
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
            .background(ArtistPageBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ArtistListRowHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = index.toString(),
                style = ArtistTrackIndexStyle,
                modifier = Modifier.width(ArtistTrackIndexWidth),
            )
            Text(
                text = title?.toString().orEmpty(),
                style = ArtistTitleStyle,
                color = if (selected) ArtistSelectedColor else ArtistTitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 5.dp, end = 8.dp),
            )
            Text(
                text = duration,
                style = ArtistTrackDurationStyle,
                maxLines = 1,
                modifier = Modifier.padding(end = 14.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(ArtistRowDivider),
        )
    }
}

@Composable
private fun ArtistPermissionState(
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
                iconRes = R.drawable.blank_artist,
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

private fun MediaBrowser.playArtist(
    artist: ArtistSummary,
    shuffled: Boolean = false,
) {
    setMediaItems(if (shuffled) artist.songs.shuffled() else artist.songs, 0, 0L)
    prepare()
    play()
}

private fun MediaBrowser.playArtistTrack(
    artist: ArtistSummary,
    index: Int,
) {
    setMediaItems(artist.songs, index, 0L)
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
