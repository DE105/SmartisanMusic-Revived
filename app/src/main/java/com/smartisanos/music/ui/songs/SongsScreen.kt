package com.smartisanos.music.ui.songs

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.playback.LocalAudioLibrary
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.await
import com.smartisanos.music.ui.components.SmartisanBlankState
import com.smartisanos.music.ui.components.audioPermission
import com.smartisanos.music.ui.components.hasAudioPermission

private val SongsListBackground = Color(0xFFF8F8F8)
private val SongRowBackground = Color(0xFFFDFDFD)
private val SongRowDivider = Color(0xFFE9E9E9)
private val SongTitleColor = Color(0xC7000000)
private val SongSubtitleColor = Color(0x73000000)
private val SongPlayingColor = Color(0xFFE64040)
private val SongSelectionBorder = Color(0x2E000000)
private val SongSelectionFill = Color(0xFFE95A4E)

private val SongTitleStyle = TextStyle(
    fontSize = 17.sp,
    fontWeight = FontWeight.Medium,
    color = SongTitleColor,
)
private val SongSubtitleStyle = TextStyle(
    fontSize = 13.sp,
    color = SongSubtitleColor,
)

@Composable
fun SongsScreen(modifier: Modifier = Modifier) {
    SongsScreen(
        editMode = false,
        selectedMediaIds = emptySet(),
        onEditSelectionChanged = {},
        modifier = modifier,
    )
}

@Composable
fun SongsScreen(
    editMode: Boolean,
    selectedMediaIds: Set<String>,
    onEditSelectionChanged: (Set<String>) -> Unit,
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

    if (!hasPermission) {
        SongsPermissionState(
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

    if (songs.isEmpty()) {
        SmartisanBlankState(
            iconRes = R.drawable.blank_song,
            title = context.getString(R.string.no_song),
            subtitle = context.getString(R.string.show_song),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SongsListBackground),
    ) {
        itemsIndexed(
            items = songs,
            key = { _, item -> item.mediaId },
        ) { index, item ->
            val selectedInEdit = item.mediaId in selectedMediaIds
            val isCurrent = item.mediaId == currentMediaId
            SongRow(
                mediaItem = item,
                selected = if (editMode) selectedInEdit else isCurrent,
                showPlayingIndicator = !editMode && isCurrent,
                editMode = editMode,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (editMode) {
                        onEditSelectionChanged(selectedMediaIds.toggle(item.mediaId))
                    } else {
                        currentMediaId = item.mediaId
                        playbackBrowser?.setMediaItems(songs, index, 0L)
                        playbackBrowser?.prepare()
                        playbackBrowser?.play()
                    }
                },
                onLongClick = {
                    if (editMode) {
                        onEditSelectionChanged(selectedMediaIds.toggle(item.mediaId))
                    }
                },
            )
        }
    }
}

@Composable
private fun SongsPermissionState(
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
                iconRes = R.drawable.blank_song,
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
private fun SongRow(
    mediaItem: MediaItem,
    selected: Boolean,
    showPlayingIndicator: Boolean,
    editMode: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    val metadata = mediaItem.mediaMetadata
    val title = metadata.displayTitle ?: metadata.title
    val unknownArtistTitle = stringResource(R.string.unknown_artist)
    val unknownAlbumTitle = stringResource(R.string.unknown_album)
    val subtitle = mediaItem.songSubtitle(
        unknownArtistTitle = unknownArtistTitle,
        unknownAlbumTitle = unknownAlbumTitle,
    )
    val qualityBadgeRes = mediaItem.songQualityBadgeRes()

    Column(
        modifier = modifier
            .background(SongRowBackground)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(61.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (editMode) {
                SongSelectionCircle(
                    selected = selected,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title?.toString().orEmpty(),
                    style = SongTitleStyle,
                    color = if (showPlayingIndicator) SongPlayingColor else SongTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                SongSubtitleRow(
                    text = subtitle,
                    qualityBadgeRes = qualityBadgeRes,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (showPlayingIndicator) {
                Box(
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = SongPlayingColor,
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SongRowDivider),
        )
    }
}

@Composable
private fun SongSubtitleRow(
    text: String,
    @DrawableRes qualityBadgeRes: Int?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (qualityBadgeRes != null) {
            Image(
                painter = painterResource(id = qualityBadgeRes),
                contentDescription = null,
                modifier = Modifier.padding(end = 6.dp),
            )
        }
        Text(
            text = text,
            style = SongSubtitleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SongSelectionCircle(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(22.dp)
            .background(
                color = if (selected) SongSelectionFill else Color.Transparent,
                shape = CircleShape,
            )
            .border(
                width = 1.dp,
                color = if (selected) SongSelectionFill else SongSelectionBorder,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Text(
                text = "✓",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

private fun MediaItem.songSubtitle(
    unknownArtistTitle: String,
    unknownAlbumTitle: String,
): String {
    val metadata = mediaMetadata
    val artist = metadata.artist?.toString()?.trim()?.takeIf(String::isNotEmpty) ?: unknownArtistTitle
    val album = metadata.albumTitle?.toString()?.trim()?.takeIf(String::isNotEmpty) ?: unknownAlbumTitle
    return "$artist - $album"
}

@DrawableRes
private fun MediaItem.songQualityBadgeRes(): Int? {
    return when (mediaMetadata.extras?.getString(LocalAudioLibrary.AudioQualityBadgeExtraKey)) {
        LocalAudioLibrary.AudioQualityBadgeFlac -> R.drawable.audio_quality_flac
        LocalAudioLibrary.AudioQualityBadgeApe -> R.drawable.audio_quality_ape
        LocalAudioLibrary.AudioQualityBadgeWav -> R.drawable.audio_quality_wav
        LocalAudioLibrary.AudioQualityBadgeAiff -> R.drawable.audio_quality_aiff
        LocalAudioLibrary.AudioQualityBadgeAlac -> R.drawable.audio_quality_alac
        LocalAudioLibrary.AudioQualityBadgeCue -> R.drawable.audio_quality_cue
        else -> null
    }
}
