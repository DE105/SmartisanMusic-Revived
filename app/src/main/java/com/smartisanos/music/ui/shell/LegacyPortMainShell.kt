package com.smartisanos.music.ui.shell

import android.app.Activity
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.Color
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.SessionResult
import com.smartisanos.music.ExternalAudioExtraKey
import com.smartisanos.music.ExternalAudioLaunchRequest
import com.smartisanos.music.ExternalAudioMediaIdPrefix
import com.smartisanos.music.R
import com.smartisanos.music.data.favorite.FavoriteSongsRepository
import com.smartisanos.music.data.library.LibraryExclusions
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.data.playlist.PlaylistCreateResult
import com.smartisanos.music.data.playlist.PlaylistRepository
import com.smartisanos.music.data.settings.PlaybackSettings
import com.smartisanos.music.data.settings.PlaybackSettingsStore
import com.smartisanos.music.isExternalAudioLaunchItem
import com.smartisanos.music.playback.LocalAudioLibrary
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.LocalPlaybackController
import com.smartisanos.music.playback.ProvidePlaybackController
import com.smartisanos.music.playback.artworkRequestKey
import com.smartisanos.music.playback.await
import com.smartisanos.music.playback.invalidateLibrary
import com.smartisanos.music.playback.loadArtworkBitmap
import com.smartisanos.music.playback.refreshLibrary
import com.smartisanos.music.playback.removeMediaItemsByMediaIds
import com.smartisanos.music.resolveExternalAudioMediaStoreIds
import com.smartisanos.music.resolveExternalAudioArtist
import com.smartisanos.music.ui.components.LegacyTrackActionItem
import com.smartisanos.music.ui.components.LegacyTrackActionsOverlay
import com.smartisanos.music.ui.components.hasAudioPermission
import com.smartisanos.music.ui.album.AlbumViewMode
import com.smartisanos.music.ui.album.buildAlbumSummaries
import com.smartisanos.music.ui.navigation.MusicDestination
import com.smartisanos.music.ui.playlist.PlaylistNameDialog
import com.smartisanos.music.ui.search.GlobalSearchScreen
import com.smartisanos.music.ui.shell.tabs.LegacyPortTabContent
import com.smartisanos.music.ui.shell.titlebar.LegacyPortSearchDetailTitleBar
import com.smartisanos.music.ui.shell.titlebar.LegacyPortTitleBar
import smartisanos.app.MenuDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import smartisanos.widget.tabswitcher.TabSwitcher

private const val LegacySearchTransitionDurationMillis = 300
private const val LegacySearchExitOffsetMultiplier = 1.09f

private sealed interface LegacySearchDrilldownTarget {
    data class Album(
        val albumId: String,
        val albumTitle: String,
    ) : LegacySearchDrilldownTarget

    data class Artist(
        val target: LegacyArtistTarget,
    ) : LegacySearchDrilldownTarget
}

private enum class LegacyTrackActionSource {
    Library,
    Playlist,
}

private val LegacySearchDecelerateEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}

@Composable
fun LegacyPortMainShell(
    playbackLaunchRequest: Int = 0,
    externalAudioLaunchRequest: ExternalAudioLaunchRequest? = null,
    onExternalAudioLaunchConsumed: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    ProvidePlaybackController {
        LegacyPortMainShellContent(
            playbackLaunchRequest = playbackLaunchRequest,
            externalAudioLaunchRequest = externalAudioLaunchRequest,
            onExternalAudioLaunchConsumed = onExternalAudioLaunchConsumed,
            modifier = modifier,
        )
    }
}

@Composable
private fun LegacyPortMainShellContent(
    playbackLaunchRequest: Int,
    externalAudioLaunchRequest: ExternalAudioLaunchRequest?,
    onExternalAudioLaunchConsumed: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val controller = LocalPlaybackController.current
    val scope = rememberCoroutineScope()
    val favoriteRepository = remember(context.applicationContext) {
        FavoriteSongsRepository.getInstance(context.applicationContext)
    }
    val playlistRepository = remember(context.applicationContext) {
        PlaylistRepository.getInstance(context.applicationContext)
    }
    val libraryExclusionsStore = remember(context.applicationContext) {
        LibraryExclusionsStore(context.applicationContext)
    }
    val playbackSettingsStore = remember(context.applicationContext) {
        PlaybackSettingsStore(context.applicationContext)
    }
    val favoriteIds by favoriteRepository.observeFavoriteIds().collectAsState(initial = emptySet())
    val libraryExclusions by libraryExclusionsStore.exclusions.collectAsState(initial = LibraryExclusions())
    val playbackSettings by playbackSettingsStore.settings.collectAsState(initial = PlaybackSettings())
    val playlists by playlistRepository.playlists.collectAsState(initial = emptyList())
    var playbackVisible by remember { mutableStateOf(false) }
    var searchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchDrilldownTarget by remember { mutableStateOf<LegacySearchDrilldownTarget?>(null) }
    var currentDestination by remember { mutableStateOf(MusicDestination.Playlist) }
    var playlistAddModeActive by remember { mutableStateOf(false) }
    var moreSettingsPageActive by remember { mutableStateOf(false) }
    var songsEditMode by remember { mutableStateOf(false) }
    var selectedSongIds by remember { mutableStateOf(emptySet<String>()) }
    var albumViewMode by remember { mutableStateOf(AlbumViewMode.List) }
    var albumEditMode by remember { mutableStateOf(false) }
    var selectedAlbumIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedAlbumId by remember { mutableStateOf<String?>(null) }
    var selectedAlbumTitle by remember { mutableStateOf<String?>(null) }
    var artistAlbumViewMode by remember { mutableStateOf(AlbumViewMode.List) }
    var selectedArtistTarget by remember { mutableStateOf<LegacyArtistTarget?>(null) }
    var libraryRefreshVersion by remember { mutableStateOf(0) }
    var libraryRefreshing by remember { mutableStateOf(false) }
    var showSongDeleteConfirm by remember { mutableStateOf(false) }
    var pendingSongDeleteMediaIds by remember { mutableStateOf(emptySet<String>()) }
    var pendingSystemDeleteSongIds by remember { mutableStateOf(emptySet<String>()) }
    var pendingPlaylistPickerMediaItems by remember { mutableStateOf<List<MediaItem>?>(null) }
    var pendingTrackActionMediaId by remember { mutableStateOf<String?>(null) }
    var pendingTrackActionSource by remember { mutableStateOf(LegacyTrackActionSource.Library) }
    var showPlaybackPlaylistCreateDialog by remember { mutableStateOf(false) }
    var playbackPlaylistCreateInitialValue by remember { mutableStateOf("") }
    var snapshot by remember(controller) {
        mutableStateOf(
            LegacyPlaybackBarSnapshot(
                mediaItem = controller?.currentMediaItem,
                isPlaying = controller?.isPlaying == true,
            ),
        )
    }
    val legacyLibrary = rememberLegacyLibraryMediaState(libraryRefreshVersion)
    val pendingTrackActionItem = remember(pendingTrackActionMediaId, legacyLibrary.items) {
        pendingTrackActionMediaId?.let { mediaId ->
            legacyLibrary.items.firstOrNull { item -> item.mediaId == mediaId }
        }
    }
    val artworkRequestKey = snapshot.mediaItem?.artworkRequestKey()
    val artworkBitmap by produceState<Bitmap?>(initialValue = null, artworkRequestKey) {
        value = snapshot.mediaItem?.let { mediaItem ->
            loadLegacyArtworkBitmap(context.applicationContext, mediaItem)
        }
    }
    val openSearchOverlay = {
        searchQuery = ""
        searchDrilldownTarget = null
        searchVisible = true
    }
    val closeSearchOverlay = {
        searchVisible = false
        searchDrilldownTarget = null
    }

    fun closeAlbumDetail() {
        selectedAlbumId = null
        selectedAlbumTitle = null
    }

    fun closeArtistDetail() {
        selectedArtistTarget = selectedArtistTarget?.parentTarget()
    }

    DisposableEffect(controller) {
        if (controller == null) {
            snapshot = LegacyPlaybackBarSnapshot()
            return@DisposableEffect onDispose { }
        }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                snapshot = LegacyPlaybackBarSnapshot(
                    mediaItem = player.currentMediaItem,
                    isPlaying = player.isPlaying,
                )
            }
        }
        controller.addListener(listener)
        snapshot = LegacyPlaybackBarSnapshot(
            mediaItem = controller.currentMediaItem,
            isPlaying = controller.isPlaying,
        )
        onDispose {
            controller.removeListener(listener)
        }
    }

    fun cleanupDeletedSongs(mediaIds: Set<String>, hideFromLibrary: Boolean) {
        if (mediaIds.isEmpty()) {
            return
        }
        controller.removeMediaItemsByMediaIds(mediaIds)
        scope.launch {
            if (hideFromLibrary) {
                libraryExclusionsStore.hideMediaIds(mediaIds)
            }
            favoriteRepository.removeAll(mediaIds)
            playlistRepository.removeMediaIdsFromAll(mediaIds)
            runCatching {
                controller?.invalidateLibrary()?.await(context)
            }
            libraryRefreshVersion += 1
        }
    }

    fun reclaimHiddenMediaIds(mediaIds: Set<String>) {
        if (mediaIds.isEmpty()) {
            return
        }
        controller.removeMediaItemsByMediaIds(mediaIds)
    }

    fun enqueueMediaItems(items: List<MediaItem>) {
        if (items.isEmpty()) {
            return
        }
        if (controller?.repeatMode == Player.REPEAT_MODE_ONE) {
            Toast.makeText(context, R.string.can_not_add_to_queue_single_repeat, Toast.LENGTH_SHORT).show()
        } else {
            controller?.addMediaItems(items)
            Toast.makeText(context, R.string.add_to_queue_success, Toast.LENGTH_SHORT).show()
        }
    }

    fun requestAddToPlaylist(items: List<MediaItem>) {
        val candidates = items.filter { item ->
            item.mediaId.isNotBlank() && !item.isExternalAudioLaunchItem()
        }
        if (candidates.isNotEmpty()) {
            pendingPlaylistPickerMediaItems = candidates
        }
    }

    fun showTrackActions(
        item: MediaItem,
        source: LegacyTrackActionSource,
    ) {
        if (item.mediaId.isBlank()) {
            return
        }
        pendingTrackActionMediaId = item.mediaId
        pendingTrackActionSource = source
    }

    fun dismissTrackActions() {
        pendingTrackActionMediaId = null
    }

    fun requestSongDeleteConfirmation(mediaIds: Set<String>) {
        if (mediaIds.isEmpty()) {
            return
        }
        pendingSongDeleteMediaIds = mediaIds
        showSongDeleteConfirm = true
    }

    fun refreshLegacyLibrary() {
        if (libraryRefreshing) {
            return
        }
        val playbackController = controller
        if (playbackController == null) {
            Toast.makeText(context, R.string.library_refresh_failed, Toast.LENGTH_SHORT).show()
            return
        }
        libraryRefreshing = true
        scope.launch {
            val result = runCatching {
                playbackController.refreshLibrary().await(context)
            }.getOrNull()
            libraryRefreshing = false
            if (result?.resultCode == SessionResult.RESULT_SUCCESS) {
                libraryRefreshVersion += 1
                Toast.makeText(context, R.string.library_refresh_success, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.library_refresh_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val deleteMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val mediaIds = pendingSystemDeleteSongIds
        pendingSystemDeleteSongIds = emptySet()
        if (result.resultCode == Activity.RESULT_OK) {
            cleanupDeletedSongs(mediaIds, hideFromLibrary = false)
        }
    }

    fun requestSystemDeleteMediaIds(mediaIds: Set<String>) {
        if (mediaIds.isEmpty()) {
            return
        }
        val deleteUris = mediaIds.mapNotNull { mediaId ->
            mediaId.toLegacyMediaStoreDeleteUri()
        }
        if (deleteUris.isEmpty()) {
            cleanupDeletedSongs(mediaIds, hideFromLibrary = true)
            return
        }
        val request = runCatching {
            val pendingIntent = MediaStore.createDeleteRequest(
                context.contentResolver,
                deleteUris,
            )
            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        }.getOrElse {
            cleanupDeletedSongs(mediaIds, hideFromLibrary = true)
            return
        }
        pendingSystemDeleteSongIds = mediaIds
        runCatching {
            deleteMediaLauncher.launch(request)
        }.onFailure {
            pendingSystemDeleteSongIds = emptySet()
            cleanupDeletedSongs(mediaIds, hideFromLibrary = true)
        }
    }

    LaunchedEffect(playbackLaunchRequest) {
        if (playbackLaunchRequest > 0) {
            playbackVisible = true
        }
    }

    LaunchedEffect(currentDestination) {
        if (currentDestination != MusicDestination.Songs) {
            songsEditMode = false
            selectedSongIds = emptySet()
            showSongDeleteConfirm = false
            pendingSongDeleteMediaIds = emptySet()
        }
        if (currentDestination != MusicDestination.Album) {
            albumEditMode = false
            selectedAlbumIds = emptySet()
            selectedAlbumId = null
            selectedAlbumTitle = null
        }
        if (currentDestination != MusicDestination.Artist) {
            selectedArtistTarget = null
        }
        if (currentDestination != MusicDestination.Playlist) {
            playlistAddModeActive = false
        }
        dismissTrackActions()
    }

    BackHandler(enabled = currentDestination == MusicDestination.Album && selectedAlbumId != null) {
        closeAlbumDetail()
    }

    BackHandler(enabled = currentDestination == MusicDestination.Artist && selectedArtistTarget != null) {
        closeArtistDetail()
    }

    LaunchedEffect(externalAudioLaunchRequest, controller) {
        val request = externalAudioLaunchRequest ?: return@LaunchedEffect
        playbackVisible = true
        val playbackController = controller ?: return@LaunchedEffect
        val (artist, mediaStoreIds) = withContext(Dispatchers.IO) {
            request.resolveExternalAudioArtist(context.applicationContext) to
                request.resolveExternalAudioMediaStoreIds(context.applicationContext)
        }
        val mediaItem = request.toExternalAudioMediaItem(
            fallbackTitle = context.getString(R.string.unknown_song_title),
            artist = artist,
            mediaStoreId = mediaStoreIds.mediaStoreId,
            albumId = mediaStoreIds.albumId,
        )
        playbackController.setMediaItem(mediaItem)
        playbackController.prepare()
        playbackController.play()
        onExternalAudioLaunchConsumed(request.requestId)
    }

    val realTabContentBottomMargin = dimensionResource(R.dimen.realtabcontent_margin_bottom) - 6.dp
    val hideBottomChrome = currentDestination == MusicDestination.More && moreSettingsPageActive

    LaunchedEffect(currentDestination) {
        if (currentDestination != MusicDestination.More) {
            moreSettingsPageActive = false
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                View(viewContext).apply {
                    setBackgroundResource(R.drawable.account_background)
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (hideBottomChrome) 0.dp else realTabContentBottomMargin),
        ) {
            val titleContentHeight = dimensionResource(R.dimen.title_bar_height)
            val titleAreaHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + titleContentHeight
            val titleBarContent: @Composable (String?, LegacyArtistTarget?, Modifier) -> Unit = { albumDetailTitle, artistTarget, titleModifier ->
                LegacyPortTitleBar(
                    destination = currentDestination,
                    songsEditMode = currentDestination == MusicDestination.Songs && songsEditMode,
                    selectedSongCount = selectedSongIds.size,
                    albumEditMode = currentDestination == MusicDestination.Album && albumEditMode,
                    selectedAlbumCount = selectedAlbumIds.size,
                    albumDetailTitle = albumDetailTitle,
                    albumViewMode = albumViewMode,
                    artistTarget = artistTarget,
                    artistAlbumViewMode = artistAlbumViewMode,
                    onEnterSongsEditMode = {
                        songsEditMode = true
                        selectedSongIds = emptySet()
                    },
                    onExitSongsEditMode = {
                        songsEditMode = false
                        selectedSongIds = emptySet()
                        showSongDeleteConfirm = false
                    },
                    onRequestDeleteSelected = {
                        if (selectedSongIds.isNotEmpty()) {
                            requestSongDeleteConfirmation(selectedSongIds)
                        }
                    },
                    onEnterAlbumEditMode = {
                        albumEditMode = true
                        selectedAlbumIds = emptySet()
                    },
                    onExitAlbumEditMode = {
                        albumEditMode = false
                        selectedAlbumIds = emptySet()
                    },
                    onToggleAlbumViewMode = {
                        albumViewMode = if (albumViewMode == AlbumViewMode.List) {
                            AlbumViewMode.Tile
                        } else {
                            AlbumViewMode.List
                        }
                    },
                    onAlbumDetailBack = {
                        closeAlbumDetail()
                    },
                    onArtistBack = {
                        closeArtistDetail()
                    },
                    onToggleArtistAlbumViewMode = {
                        artistAlbumViewMode = if (artistAlbumViewMode == AlbumViewMode.List) {
                            AlbumViewMode.Tile
                        } else {
                            AlbumViewMode.List
                        }
                    },
                    onSearchClick = openSearchOverlay,
                    modifier = titleModifier,
                )
            }
            if (currentDestination == MusicDestination.Playlist || currentDestination == MusicDestination.More) {
                // 播放列表页和更多二级页需要复刻原版自身的标题栏、详情栈和加歌/文件夹过渡。
            } else if (currentDestination == MusicDestination.Album) {
                LegacyPortPageStackTransition(
                    secondaryKey = selectedAlbumTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(titleAreaHeight),
                    label = "legacy album title transition",
                    primaryContent = {
                        titleBarContent(null, null, Modifier.fillMaxSize())
                    },
                    secondaryContent = { detailTitle ->
                        titleBarContent(detailTitle, null, Modifier.fillMaxSize())
                    },
                )
            } else if (currentDestination == MusicDestination.Artist) {
                LegacyPortArtistTitleStack(
                    selectedTarget = selectedArtistTarget,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(titleAreaHeight),
                ) { artistTarget, titleModifier ->
                    titleBarContent(null, artistTarget, titleModifier)
                }
            } else {
                titleBarContent(null, null, Modifier.fillMaxWidth())
            }
            LegacyPortTabContent(
                destination = currentDestination,
                mediaItems = legacyLibrary.items,
                libraryLoaded = legacyLibrary.loaded,
                songsEditMode = currentDestination == MusicDestination.Songs && songsEditMode,
                selectedSongIds = selectedSongIds,
                albumViewMode = albumViewMode,
                albumEditMode = currentDestination == MusicDestination.Album && albumEditMode,
                selectedAlbumId = selectedAlbumId,
                selectedAlbumIds = selectedAlbumIds,
                artistAlbumViewMode = artistAlbumViewMode,
                selectedArtistTarget = selectedArtistTarget,
                hiddenMediaIds = libraryExclusions.hiddenMediaIds,
                libraryRefreshVersion = libraryRefreshVersion,
                libraryRefreshing = libraryRefreshing,
                playbackSettings = playbackSettings,
                onRefreshLibrary = ::refreshLegacyLibrary,
                onRequestAddToPlaylist = ::requestAddToPlaylist,
                onRequestAddToQueue = ::enqueueMediaItems,
                onScratchEnabledChange = { enabled ->
                    scope.launch {
                        playbackSettingsStore.setScratchEnabled(enabled)
                    }
                },
                onHidePlayerAxisEnabledChange = { enabled ->
                    scope.launch {
                        playbackSettingsStore.setHidePlayerAxisEnabled(enabled)
                    }
                },
                onPopcornSoundEnabledChange = { enabled ->
                    scope.launch {
                        playbackSettingsStore.setPopcornSoundEnabled(enabled)
                    }
                },
                onMediaIdsHidden = ::reclaimHiddenMediaIds,
                onRequestDeleteMediaIds = ::requestSystemDeleteMediaIds,
                onLibraryTrackMoreClick = { item ->
                    showTrackActions(item, LegacyTrackActionSource.Library)
                },
                onPlaylistTrackMoreClick = { item ->
                    showTrackActions(item, LegacyTrackActionSource.Playlist)
                },
                onMoreSettingsPageActiveChanged = { active ->
                    moreSettingsPageActive = active
                },
                onToggleSongSelected = { mediaId ->
                    selectedSongIds = if (mediaId in selectedSongIds) {
                        selectedSongIds - mediaId
                    } else {
                        selectedSongIds + mediaId
                    }
                },
                onToggleAlbumSelected = { albumId ->
                    selectedAlbumIds = if (albumId in selectedAlbumIds) {
                        selectedAlbumIds - albumId
                    } else {
                        selectedAlbumIds + albumId
                    }
                },
                onAlbumSelected = { albumId, albumTitle ->
                    albumEditMode = false
                    selectedAlbumIds = emptySet()
                    selectedAlbumId = albumId
                    selectedAlbumTitle = albumTitle
                },
                onArtistTargetChanged = { target ->
                    selectedArtistTarget = target
                },
                onPlaylistAddModeActiveChanged = { active ->
                    playlistAddModeActive = active
                },
                onSearchClick = openSearchOverlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
        if (!hideBottomChrome) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
            ) {
                LegacyPortPlaybackBar(
                    snapshot = snapshot,
                    favoriteIds = favoriteIds,
                    artworkBitmap = artworkBitmap,
                    onOpenPlayback = {
                        playbackVisible = true
                    },
                    onToggleFavorite = { mediaItem ->
                        if (mediaItem.isExternalAudioLaunchItem()) {
                            return@LegacyPortPlaybackBar
                        }
                        val mediaId = mediaItem.mediaId.takeIf(String::isNotBlank) ?: return@LegacyPortPlaybackBar
                        scope.launch {
                            favoriteRepository.toggle(mediaId)
                        }
                    },
                    onPrevious = {
                        controller?.seekToPrevious()
                    },
                    onPlayPause = {
                        if (controller?.isPlaying == true) {
                            controller.pause()
                        } else {
                            controller?.play()
                        }
                    },
                    onNext = {
                        controller?.seekToNext()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(67.dp),
                )
                LegacyPortBottomBar(
                    currentDestination = if (playlistAddModeActive) MusicDestination.Songs else currentDestination,
                    onDestinationSelected = { destination ->
                        currentDestination = destination
                    },
                )
            }
        }
        val trackActionItems = pendingTrackActionItem?.let { actionItem ->
            val mediaId = actionItem.mediaId
            val isFavorite = mediaId in favoriteIds
            val canPersist = mediaId.isNotBlank() && !actionItem.isExternalAudioLaunchItem()
            val actions = mutableListOf(
                LegacyTrackActionItem(
                    labelRes = R.string.add_to_playlist,
                    iconRes = R.drawable.more_select_icon_addlist,
                    pressedIconRes = R.drawable.more_select_icon_addlist_down,
                    enabled = canPersist,
                    onClick = {
                        dismissTrackActions()
                        requestAddToPlaylist(listOf(actionItem))
                    },
                ),
                LegacyTrackActionItem(
                    labelRes = R.string.add_to_queue,
                    iconRes = R.drawable.more_select_icon_addplay,
                    pressedIconRes = R.drawable.more_select_icon_addplay_down,
                    onClick = {
                        dismissTrackActions()
                        enqueueMediaItems(listOf(actionItem))
                    },
                ),
                LegacyTrackActionItem(
                    labelRes = if (isFavorite) R.string.cancel_love else R.string.love,
                    iconRes = if (isFavorite) {
                        R.drawable.more_select_icon_favorite_cancel
                    } else {
                        R.drawable.more_select_icon_favorite_add
                    },
                    pressedIconRes = if (isFavorite) {
                        R.drawable.more_select_icon_favorite_cancel_down
                    } else {
                        R.drawable.more_select_icon_favorite_add_down
                    },
                    enabled = canPersist,
                    selected = isFavorite,
                    onClick = {
                        dismissTrackActions()
                        if (canPersist) {
                            scope.launch {
                                favoriteRepository.toggle(mediaId)
                            }
                        }
                    },
                ),
            )
            if (pendingTrackActionSource == LegacyTrackActionSource.Library) {
                actions += LegacyTrackActionItem(
                    labelRes = R.string.delete,
                    iconRes = R.drawable.more_select_icon_delete,
                    onClick = {
                        dismissTrackActions()
                        requestSongDeleteConfirmation(setOf(mediaId))
                    },
                )
            }
            actions
        }.orEmpty()
        LegacyTrackActionsOverlay(
            visible = pendingTrackActionItem != null,
            actions = trackActionItems,
            onDismissRequest = ::dismissTrackActions,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2.4f),
        )
        LegacyPortPlaybackOverlay(
            visible = playbackVisible,
            playbackSettings = playbackSettings,
            onRequestAddToPlaylist = ::requestAddToPlaylist,
            onRequestAddToQueue = ::enqueueMediaItems,
            onScratchEnabledChange = { enabled ->
                scope.launch {
                    playbackSettingsStore.setScratchEnabled(enabled)
                }
            },
            onCollapse = {
                playbackVisible = false
            },
            modifier = Modifier.zIndex(3f),
        )
        LegacyPortSearchOverlay(
            visible = searchVisible,
            query = searchQuery,
            mediaItems = legacyLibrary.items,
            hiddenMediaIds = libraryExclusions.hiddenMediaIds,
            drilldownTarget = searchDrilldownTarget,
            libraryRefreshVersion = libraryRefreshVersion,
            onQueryChange = { value ->
                searchQuery = value
            },
            onDismiss = closeSearchOverlay,
            onOpenPlayback = {
                playbackVisible = true
            },
            onRequestAddToPlaylist = ::requestAddToPlaylist,
            onRequestAddToQueue = ::enqueueMediaItems,
            onTrackMoreClick = { item ->
                showTrackActions(item, LegacyTrackActionSource.Library)
            },
            onDrilldownTargetChanged = { target ->
                searchDrilldownTarget = target
            },
            onAlbumClick = { albumId, albumTitle ->
                searchDrilldownTarget = LegacySearchDrilldownTarget.Album(
                    albumId = albumId,
                    albumTitle = albumTitle,
                )
            },
            onArtistClick = { artistId, artistName ->
                searchDrilldownTarget = LegacySearchDrilldownTarget.Artist(
                    target = LegacyArtistTarget.Albums(
                        artistId = artistId,
                        artistName = artistName,
                    ),
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
        )
        LegacyPlaybackPlaylistPickerOverlay(
            visible = pendingPlaylistPickerMediaItems != null && !showPlaybackPlaylistCreateDialog,
            playlists = playlists,
            onDismiss = {
                pendingPlaylistPickerMediaItems = null
            },
            onCreateNewPlaylist = {
                scope.launch {
                    playbackPlaylistCreateInitialValue = playlistRepository.suggestNextUntitledName()
                    showPlaybackPlaylistCreateDialog = true
                }
            },
            onPlaylistSelected = { playlistId ->
                val mediaIds = pendingPlaylistPickerMediaItems?.map(MediaItem::mediaId).orEmpty()
                scope.launch {
                    val result = playlistRepository.addMediaIds(playlistId, mediaIds)
                    when {
                        result.addedCount > 0 -> {
                            Toast.makeText(context, R.string.playlist_added, Toast.LENGTH_SHORT).show()
                        }
                        result.duplicateCount > 0 -> {
                            Toast.makeText(context, R.string.playlist_song_exists, Toast.LENGTH_SHORT).show()
                        }
                    }
                    pendingPlaylistPickerMediaItems = null
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(2f),
        )
        if (showPlaybackPlaylistCreateDialog) {
            PlaylistNameDialog(
                title = androidx.compose.ui.res.stringResource(R.string.new_playlist),
                initialValue = playbackPlaylistCreateInitialValue,
                selectAllOnOpen = true,
                onDismiss = {
                    showPlaybackPlaylistCreateDialog = false
                },
                onConfirm = { input ->
                    val mediaIds = pendingPlaylistPickerMediaItems?.map(MediaItem::mediaId).orEmpty()
                    scope.launch {
                        when (val result = playlistRepository.createPlaylist(input, mediaIds)) {
                            PlaylistCreateResult.EmptyName -> {
                                Toast.makeText(context, R.string.playlist_create_failed, Toast.LENGTH_SHORT).show()
                            }
                            PlaylistCreateResult.DuplicateName -> {
                                Toast.makeText(context, R.string.playlist_duplicate_name, Toast.LENGTH_SHORT).show()
                            }
                            is PlaylistCreateResult.Success -> {
                                showPlaybackPlaylistCreateDialog = false
                                pendingPlaylistPickerMediaItems = null
                                Toast.makeText(context, R.string.playlist_added, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
            )
        }
        if (showSongDeleteConfirm) {
            LegacySongDeleteConfirmOverlay(
                onDismiss = {
                    showSongDeleteConfirm = false
                    pendingSongDeleteMediaIds = emptySet()
                },
                onConfirm = {
                    val mediaIds = pendingSongDeleteMediaIds
                    if (mediaIds.isEmpty()) {
                        showSongDeleteConfirm = false
                        pendingSongDeleteMediaIds = emptySet()
                        return@LegacySongDeleteConfirmOverlay
                    }
                    showSongDeleteConfirm = false
                    pendingSongDeleteMediaIds = emptySet()
                    songsEditMode = false
                    selectedSongIds = emptySet()
                    requestSystemDeleteMediaIds(mediaIds)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f),
            )
        }
    }
}

@Composable
private fun LegacyPortBottomBar(
    currentDestination: MusicDestination,
    onDestinationSelected: (MusicDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            factory = { viewContext ->
                TabSwitcher(viewContext)
            },
            update = { tabSwitcher ->
                tabSwitcher.setOnDestinationSelectedListener(onDestinationSelected)
                tabSwitcher.setCurrentDestination(currentDestination)
            },
        )
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars),
        )
    }
}

@Composable
private fun LegacySongDeleteConfirmOverlay(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestOnConfirm by rememberUpdatedState(onConfirm)
    DisposableEffect(context) {
        val dialog = MenuDialog(context).apply {
            setTitle(R.string.delete_song_title_text)
            setNegativeButton(
                View.OnClickListener {
                    latestOnDismiss()
                },
            )
            setPositiveButton(R.string.dialog_delete_conform) {
                latestOnConfirm()
            }
            setOnCancelListener {
                latestOnDismiss()
            }
        }
        dialog.show()
        onDispose {
            dialog.setOnCancelListener(null)
            if (dialog.isShowing) {
                dialog.dismiss()
            }
        }
    }
}

@Composable
private fun LegacyPortSearchOverlay(
    visible: Boolean,
    query: String,
    mediaItems: List<MediaItem>,
    hiddenMediaIds: Set<String>,
    drilldownTarget: LegacySearchDrilldownTarget?,
    libraryRefreshVersion: Int,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onOpenPlayback: () -> Unit,
    onRequestAddToPlaylist: (List<MediaItem>) -> Unit,
    onRequestAddToQueue: (List<MediaItem>) -> Unit,
    onTrackMoreClick: (MediaItem) -> Unit,
    onDrilldownTargetChanged: (LegacySearchDrilldownTarget?) -> Unit,
    onAlbumClick: (String, String) -> Unit,
    onArtistClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier.fillMaxSize(),
        enter = slideInVertically(
            animationSpec = tween(
                durationMillis = LegacySearchTransitionDurationMillis,
                easing = LegacySearchDecelerateEasing,
            ),
            initialOffsetY = { fullHeight -> fullHeight },
        ),
        exit = slideOutVertically(
            animationSpec = tween(
                durationMillis = LegacySearchTransitionDurationMillis,
                easing = LegacySearchDecelerateEasing,
            ),
            targetOffsetY = { fullHeight ->
                (fullHeight * LegacySearchExitOffsetMultiplier).toInt()
            },
        ),
    ) {
        LegacyPortPageStackTransition(
            secondaryKey = drilldownTarget,
            modifier = Modifier.fillMaxSize(),
            label = "legacy search detail transition",
            primaryContent = {
                GlobalSearchScreen(
                    query = query,
                    libraryRefreshVersion = libraryRefreshVersion,
                    onQueryChange = onQueryChange,
                    onDismiss = onDismiss,
                    onOpenPlayback = onOpenPlayback,
                    onAlbumClick = onAlbumClick,
                    onArtistClick = onArtistClick,
                    modifier = Modifier.fillMaxSize(),
                )
            },
            secondaryContent = { target ->
                LegacyPortSearchDrilldownPage(
                    target = target,
                    mediaItems = mediaItems,
                    hiddenMediaIds = hiddenMediaIds,
                    onBack = {
                        when (target) {
                            is LegacySearchDrilldownTarget.Album -> onDrilldownTargetChanged(null)
                            is LegacySearchDrilldownTarget.Artist -> {
                                val parentTarget = target.target.parentTarget()
                                onDrilldownTargetChanged(
                                    parentTarget?.let(LegacySearchDrilldownTarget::Artist),
                                )
                            }
                        }
                    },
                    onRequestAddToPlaylist = onRequestAddToPlaylist,
                    onRequestAddToQueue = onRequestAddToQueue,
                    onTrackMoreClick = onTrackMoreClick,
                    onArtistTargetChanged = { artistTarget ->
                        onDrilldownTargetChanged(
                            artistTarget?.let(LegacySearchDrilldownTarget::Artist),
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            },
        )
    }
}

@Composable
private fun LegacyPortSearchDrilldownPage(
    target: LegacySearchDrilldownTarget,
    mediaItems: List<MediaItem>,
    hiddenMediaIds: Set<String>,
    onBack: () -> Unit,
    onRequestAddToPlaylist: (List<MediaItem>) -> Unit,
    onRequestAddToQueue: (List<MediaItem>) -> Unit,
    onTrackMoreClick: (MediaItem) -> Unit,
    onArtistTargetChanged: (LegacyArtistTarget?) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val visibleSongs = remember(mediaItems, hiddenMediaIds) {
        mediaItems.filterNot { mediaItem -> mediaItem.mediaId in hiddenMediaIds }
    }
    val albums = remember(visibleSongs, context) {
        buildAlbumSummaries(
            mediaItems = visibleSongs,
            unknownAlbumTitle = context.getString(R.string.unknown_album),
            multipleArtistsTitle = context.getString(R.string.many_artist),
        )
    }
    val titleContentHeight = dimensionResource(R.dimen.title_bar_height)
    val titleAreaHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + titleContentHeight

    Column(
        modifier = modifier.background(ComposeColor.White),
    ) {
        when (target) {
            is LegacySearchDrilldownTarget.Album -> {
                val album = remember(albums, target.albumId) {
                    albums.firstOrNull { album -> album.id == target.albumId }
                }
                LegacyPortSearchDetailTitleBar(
                    destination = MusicDestination.Album,
                    albumDetailTitle = album?.title ?: target.albumTitle,
                    artistTarget = null,
                    onBack = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(titleAreaHeight),
                )
                if (album != null) {
                    LegacyPortAlbumDetailPage(
                        album = album,
                        onRequestAddToPlaylist = onRequestAddToPlaylist,
                        onRequestAddToQueue = onRequestAddToQueue,
                        onTrackMoreClick = onTrackMoreClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(ComposeColor.White),
                    )
                }
            }
            is LegacySearchDrilldownTarget.Artist -> {
                LegacyPortArtistTitleStack(
                    selectedTarget = target.target,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(titleAreaHeight),
                ) { artistTarget, titleModifier ->
                    LegacyPortSearchDetailTitleBar(
                        destination = MusicDestination.Artist,
                        albumDetailTitle = null,
                        artistTarget = artistTarget,
                        onBack = onBack,
                        modifier = titleModifier,
                    )
                }
                LegacyPortArtistPage(
                    mediaItems = visibleSongs,
                    active = true,
                    selectedTarget = target.target,
                    albumViewMode = AlbumViewMode.List,
                    hiddenMediaIds = emptySet(),
                    onTargetChanged = onArtistTargetChanged,
                    onRequestAddToPlaylist = onRequestAddToPlaylist,
                    onRequestAddToQueue = onRequestAddToQueue,
                    onTrackMoreClick = onTrackMoreClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}

@Composable
internal fun rememberLegacyLibraryMediaState(
    libraryRefreshVersion: Int = 0,
): LegacyLibraryMediaState {
    val context = LocalContext.current
    val browser = LocalPlaybackBrowser.current
    val hasPermission = hasAudioPermission(context)
    var state by remember(browser) { mutableStateOf(LegacyLibraryMediaState()) }

    LaunchedEffect(browser, hasPermission, libraryRefreshVersion) {
        val playbackBrowser = browser ?: run {
            state = LegacyLibraryMediaState(loaded = true)
            return@LaunchedEffect
        }
        if (!hasPermission) {
            state = LegacyLibraryMediaState(loaded = true)
            return@LaunchedEffect
        }
        val rootItem = playbackBrowser.getLibraryRoot(null).await(context).value ?: run {
            state = LegacyLibraryMediaState(loaded = true)
            return@LaunchedEffect
        }
        state = LegacyLibraryMediaState(
            items = playbackBrowser.getChildren(rootItem.mediaId, 0, Int.MAX_VALUE, null)
                .await(context)
                .value
                ?.toList()
                .orEmpty(),
            loaded = true,
        )
    }

    return state
}

internal data class LegacyLibraryMediaState(
    val items: List<MediaItem> = emptyList(),
    val loaded: Boolean = false,
)

private fun String.toLegacyMediaStoreDeleteUri(): Uri? {
    val mediaStoreId = trim().toLongOrNull() ?: return null
    return ContentUris.withAppendedId(
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
        mediaStoreId,
    )
}

@Composable
private fun LegacyPortPlaybackBar(
    snapshot: LegacyPlaybackBarSnapshot,
    favoriteIds: Set<String>,
    artworkBitmap: Bitmap?,
    onOpenPlayback: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val root = LayoutInflater.from(context).inflate(R.layout.playback_bar, null, false)

            root.setBackgroundColor(Color.TRANSPARENT)
            root.findViewById<ImageView>(R.id.album_art)?.setImageResource(R.drawable.noalbumcover_220)
            root.findViewById<View>(R.id.playback_bar_shadow)?.setBackgroundResource(R.drawable.now_playing_bar_shadow)

            root
        },
        update = { root ->
            val mediaItem = snapshot.mediaItem
            val mediaId = mediaItem?.mediaId.orEmpty()
            val isExternalAudio = mediaItem?.isExternalAudioLaunchItem() == true
            val isFavorite = !isExternalAudio && mediaId in favoriteIds
            val title = mediaItem?.mediaMetadata?.displayTitle?.toString()
                ?: mediaItem?.mediaMetadata?.title?.toString()
                ?: context.getString(R.string.unknown_song_title)
            val artist = mediaItem?.mediaMetadata?.subtitle?.toString()
                ?: mediaItem?.mediaMetadata?.artist?.toString()
                ?: context.getString(R.string.unknown_artist)

            root.findViewById<TextView>(R.id.track_name)?.text = title
            root.findViewById<TextView>(R.id.artist_name)?.text = artist
            root.findViewById<ImageButton>(R.id.left_btn)?.apply {
                setImageResource(
                    if (isFavorite) {
                        R.drawable.float_favor_cancel_selector
                    } else {
                        R.drawable.float_favor_add_selector
                    },
                )
                isEnabled = mediaItem != null && !isExternalAudio
                setOnClickListener {
                    if (mediaItem != null) {
                        onToggleFavorite(mediaItem)
                    }
                }
            }
            root.findViewById<ImageButton>(R.id.prev_btn)?.apply {
                setImageResource(R.drawable.float_btn_prev_selector)
                setOnClickListener { onPrevious() }
            }
            root.findViewById<ImageButton>(R.id.play_btn)?.apply {
                setImageResource(
                    if (snapshot.isPlaying) {
                        R.drawable.float_btn_pause_selector
                    } else {
                        R.drawable.float_btn_play_selector
                    },
                )
                setOnClickListener { onPlayPause() }
            }
            root.findViewById<ImageButton>(R.id.next_btn)?.apply {
                setImageResource(R.drawable.float_btn_next_selector)
                setOnClickListener { onNext() }
            }
            root.findViewById<View>(R.id.song_info_zone)?.setOnClickListener {
                onOpenPlayback()
            }
            root.findViewById<ImageView>(R.id.album_art)?.apply {
                if (artworkBitmap != null) {
                    setImageBitmap(artworkBitmap)
                } else {
                    setImageResource(R.drawable.noalbumcover_220)
                }
                setOnClickListener {
                    onOpenPlayback()
                }
            }
        },
    )
}

private data class LegacyPlaybackBarSnapshot(
    val mediaItem: MediaItem? = null,
    val isPlaying: Boolean = false,
)

private suspend fun loadLegacyArtworkBitmap(
    context: android.content.Context,
    mediaItem: MediaItem,
): Bitmap? = loadArtworkBitmap(context, mediaItem, LegacyPlaybackBarArtworkDecodeSize)

private val LegacyPlaybackBarArtworkDecodeSize = android.util.Size(128, 128)

private fun ExternalAudioLaunchRequest.toExternalAudioMediaItem(
    fallbackTitle: String,
    artist: String?,
    mediaStoreId: Long?,
    albumId: Long?,
): MediaItem {
    val uriTitle = uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.substringBeforeLast('.', missingDelimiterValue = uri.lastPathSegment.orEmpty())
        ?.takeIf(String::isNotBlank)
    val title = displayName
        ?.substringBeforeLast('.', missingDelimiterValue = displayName)
        ?.takeIf(String::isNotBlank)
        ?: uriTitle
        ?: fallbackTitle
    val normalizedMimeType = mimeType
        ?.lowercase()
        ?.takeUnless { it.endsWith("/*") }
    val extras = Bundle().apply {
        putBoolean(ExternalAudioExtraKey, true)
        if (albumId != null) {
            putLong(LocalAudioLibrary.AlbumIdExtraKey, albumId)
        }
    }
    val metadataBuilder = MediaMetadata.Builder()
        .setTitle(title)
        .setDisplayTitle(title)
        .setIsPlayable(true)
        .setIsBrowsable(false)
        .setExtras(extras)
    if (mediaStoreId != null) {
        metadataBuilder.setArtworkUri(LocalAudioLibrary.trackArtworkUri(mediaStoreId))
    }
    artist?.takeIf(String::isNotBlank)?.let { externalArtist ->
        metadataBuilder
            .setArtist(externalArtist)
            .setSubtitle(externalArtist)
    }

    return MediaItem.Builder()
        .setMediaId("$ExternalAudioMediaIdPrefix$requestId")
        .setUri(uri)
        .setMimeType(normalizedMimeType)
        .setMediaMetadata(metadataBuilder.build())
        .build()
}
