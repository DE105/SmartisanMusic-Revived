package com.smartisanos.music.ui.shell

import android.app.Activity
import android.content.ContentUris
import android.net.Uri
import android.os.Bundle
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.icu.text.Transliterator
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import android.util.Size
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import com.smartisanos.music.ExternalAudioExtraKey
import com.smartisanos.music.ExternalAudioLaunchRequest
import com.smartisanos.music.ExternalAudioMediaIdPrefix
import com.smartisanos.music.R
import com.smartisanos.music.data.favorite.FavoriteSongsRepository
import com.smartisanos.music.data.library.LibraryExclusions
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.data.playlist.PlaylistRepository
import com.smartisanos.music.data.playlist.UserPlaylistSummary
import com.smartisanos.music.data.settings.PlaybackSettings
import com.smartisanos.music.data.settings.PlaybackSettingsStore
import com.smartisanos.music.isExternalAudioLaunchItem
import com.smartisanos.music.playback.LocalAudioLibrary
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.LocalPlaybackController
import com.smartisanos.music.playback.ProvidePlaybackController
import com.smartisanos.music.playback.await
import com.smartisanos.music.playback.invalidateLibrary
import com.smartisanos.music.playback.removeMediaItemsByMediaIds
import com.smartisanos.music.resolveExternalAudioAlbumId
import com.smartisanos.music.resolveExternalAudioArtist
import com.smartisanos.music.ui.components.hasAudioPermission
import com.smartisanos.music.ui.album.AlbumViewMode
import com.smartisanos.music.ui.navigation.MusicDestination
import com.smartisanos.music.ui.widgets.EditableListViewItem
import com.smartisanos.music.ui.widgets.StretchTextView
import smartisanos.app.MenuDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale
import smartisanos.widget.ActionButtonGroup
import smartisanos.widget.ListContentItemText
import smartisanos.widget.TitleBar
import smartisanos.widget.letters.QuickBarEx
import smartisanos.widget.tabswitcher.TabSwitcher
import kotlin.random.Random

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
    val legacyLibrary = rememberLegacyLibraryMediaState()
    var playbackVisible by remember { mutableStateOf(false) }
    var currentDestination by remember { mutableStateOf(MusicDestination.Songs) }
    var playlistAddModeActive by remember { mutableStateOf(false) }
    var songsEditMode by remember { mutableStateOf(false) }
    var selectedSongIds by remember { mutableStateOf(emptySet<String>()) }
    var albumViewMode by remember { mutableStateOf(AlbumViewMode.List) }
    var albumEditMode by remember { mutableStateOf(false) }
    var selectedAlbumIds by remember { mutableStateOf(emptySet<String>()) }
    var selectedAlbumId by remember { mutableStateOf<String?>(null) }
    var selectedAlbumTitle by remember { mutableStateOf<String?>(null) }
    var artistAlbumViewMode by remember { mutableStateOf(AlbumViewMode.List) }
    var selectedArtistTarget by remember { mutableStateOf<LegacyArtistTarget?>(null) }
    var showSongDeleteConfirm by remember { mutableStateOf(false) }
    var pendingSystemDeleteSongIds by remember { mutableStateOf(emptySet<String>()) }
    var snapshot by remember(controller) {
        mutableStateOf(
            LegacyPlaybackBarSnapshot(
                mediaItem = controller?.currentMediaItem,
                isPlaying = controller?.isPlaying == true,
            ),
        )
    }
    val artworkBitmap by produceState<Bitmap?>(initialValue = null, snapshot.mediaItem?.mediaId) {
        value = snapshot.mediaItem?.let { mediaItem ->
            loadLegacyArtworkBitmap(context.applicationContext, mediaItem)
        }
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
    }

    BackHandler(enabled = currentDestination == MusicDestination.Album && selectedAlbumId != null) {
        selectedAlbumId = null
        selectedAlbumTitle = null
    }

    BackHandler(enabled = currentDestination == MusicDestination.Artist && selectedArtistTarget != null) {
        selectedArtistTarget = selectedArtistTarget?.parentTarget()
    }

    LaunchedEffect(externalAudioLaunchRequest, controller) {
        val request = externalAudioLaunchRequest ?: return@LaunchedEffect
        playbackVisible = true
        val playbackController = controller ?: return@LaunchedEffect
        val (artist, albumId) = withContext(Dispatchers.IO) {
            request.resolveExternalAudioArtist(context.applicationContext) to
                request.resolveExternalAudioAlbumId(context.applicationContext)
        }
        val mediaItem = request.toExternalAudioMediaItem(
            fallbackTitle = context.getString(R.string.unknown_song_title),
            artist = artist,
            albumId = albumId,
        )
        playbackController.setMediaItem(mediaItem)
        playbackController.prepare()
        playbackController.play()
        onExternalAudioLaunchConsumed(request.requestId)
    }

    val realTabContentBottomMargin = dimensionResource(R.dimen.realtabcontent_margin_bottom) - 6.dp

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
                .padding(bottom = realTabContentBottomMargin),
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
                            showSongDeleteConfirm = true
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
                        selectedAlbumId = null
                        selectedAlbumTitle = null
                    },
                    onArtistBack = {
                        selectedArtistTarget = selectedArtistTarget?.parentTarget()
                    },
                    onToggleArtistAlbumViewMode = {
                        artistAlbumViewMode = if (artistAlbumViewMode == AlbumViewMode.List) {
                            AlbumViewMode.Tile
                        } else {
                            AlbumViewMode.List
                        }
                    },
                    modifier = titleModifier,
                )
            }
            if (currentDestination == MusicDestination.Playlist) {
                // 播放列表页需要复刻原版自身的标题栏、详情栈和加歌 Activity 过渡。
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
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
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
        LegacyPortPlaybackOverlay(
            visible = playbackVisible,
            playbackSettings = playbackSettings,
            onScratchEnabledChange = { enabled ->
                scope.launch {
                    playbackSettingsStore.setScratchEnabled(enabled)
                }
            },
            onCollapse = {
                playbackVisible = false
            },
            modifier = Modifier.zIndex(1f),
        )
        if (showSongDeleteConfirm) {
            LegacySongDeleteConfirmOverlay(
                onDismiss = {
                    showSongDeleteConfirm = false
                },
                onConfirm = {
                    val mediaIds = selectedSongIds
                    if (mediaIds.isEmpty()) {
                        showSongDeleteConfirm = false
                        return@LegacySongDeleteConfirmOverlay
                    }
                    showSongDeleteConfirm = false
                    songsEditMode = false
                    selectedSongIds = emptySet()
                    val deleteUris = mediaIds.mapNotNull { mediaId ->
                        mediaId.toLegacyMediaStoreDeleteUri()
                    }
                    if (deleteUris.isEmpty()) {
                        cleanupDeletedSongs(mediaIds, hideFromLibrary = true)
                        return@LegacySongDeleteConfirmOverlay
                    }
                    val request = runCatching {
                        val pendingIntent = MediaStore.createDeleteRequest(
                            context.contentResolver,
                            deleteUris,
                        )
                        IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                    }.getOrElse {
                        cleanupDeletedSongs(mediaIds, hideFromLibrary = true)
                        return@LegacySongDeleteConfirmOverlay
                    }
                    pendingSystemDeleteSongIds = mediaIds
                    runCatching {
                        deleteMediaLauncher.launch(request)
                    }.onFailure {
                        pendingSystemDeleteSongIds = emptySet()
                        cleanupDeletedSongs(mediaIds, hideFromLibrary = true)
                    }
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
private fun LegacyPortTabContent(
    destination: MusicDestination,
    mediaItems: List<MediaItem>,
    libraryLoaded: Boolean,
    songsEditMode: Boolean,
    selectedSongIds: Set<String>,
    albumViewMode: AlbumViewMode,
    albumEditMode: Boolean,
    selectedAlbumId: String?,
    selectedAlbumIds: Set<String>,
    artistAlbumViewMode: AlbumViewMode,
    selectedArtistTarget: LegacyArtistTarget?,
    hiddenMediaIds: Set<String>,
    onToggleSongSelected: (String) -> Unit,
    onToggleAlbumSelected: (String) -> Unit,
    onAlbumSelected: (String, String) -> Unit,
    onArtistTargetChanged: (LegacyArtistTarget?) -> Unit,
    onPlaylistAddModeActiveChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        MusicDestination.Songs -> LegacyPortSongsPage(
            mediaItems = mediaItems,
            libraryLoaded = libraryLoaded,
            active = true,
            editMode = songsEditMode,
            selectedSongIds = selectedSongIds,
            hiddenMediaIds = hiddenMediaIds,
            onToggleSongSelected = onToggleSongSelected,
            modifier = modifier,
        )
        MusicDestination.Album -> LegacyPortAlbumPage(
            mediaItems = mediaItems,
            active = true,
            viewMode = albumViewMode,
            editMode = albumEditMode,
            selectedAlbumId = selectedAlbumId,
            selectedAlbumIds = selectedAlbumIds,
            hiddenMediaIds = hiddenMediaIds,
            onAlbumSelected = onAlbumSelected,
            onToggleAlbumSelected = onToggleAlbumSelected,
            modifier = modifier,
        )
        MusicDestination.Artist -> LegacyPortArtistPage(
            mediaItems = mediaItems,
            active = true,
            selectedTarget = selectedArtistTarget,
            albumViewMode = artistAlbumViewMode,
            hiddenMediaIds = hiddenMediaIds,
            onTargetChanged = onArtistTargetChanged,
            modifier = modifier,
        )
        MusicDestination.Playlist -> LegacyPortPlaylistPage(
            mediaItems = mediaItems,
            active = true,
            hiddenMediaIds = hiddenMediaIds,
            onAddModeActiveChanged = onPlaylistAddModeActiveChanged,
            modifier = modifier,
        )
        MusicDestination.More -> LegacyPortMoreList(
            active = true,
            modifier = modifier,
        )
    }
}

@Composable
private fun LegacyPortSongsPage(
    mediaItems: List<MediaItem>,
    libraryLoaded: Boolean,
    active: Boolean,
    editMode: Boolean,
    selectedSongIds: Set<String>,
    hiddenMediaIds: Set<String>,
    onToggleSongSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val browser = LocalPlaybackBrowser.current
    var selectedSortIndex by remember { mutableStateOf(0) }
    val visibleSongs = remember(mediaItems, hiddenMediaIds) {
        mediaItems.filterNot { mediaItem -> mediaItem.mediaId in hiddenMediaIds }
    }
    val sortedSongs = remember(visibleSongs, selectedSortIndex) {
        visibleSongs.sortedForLegacySort(selectedSortIndex)
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            FrameLayout(viewContext).apply {
                setBackgroundColor(Color.TRANSPARENT)
                LayoutInflater.from(viewContext).inflate(R.layout.f_all_track, this, true)
                (getChildAt(1) as? RelativeLayout)?.setBackgroundColor(Color.TRANSPARENT)
                findViewById<FrameLayout>(R.id.all_track_fragment_container)?.apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    LayoutInflater.from(viewContext).inflate(R.layout.smart_pinnedlist, this, true)
                }
                findViewById<ListView>(R.id.list)?.apply {
                    divider = ColorDrawable(viewContext.getColor(R.color.listview_divider_color))
                    dividerHeight = viewContext.resources.getDimensionPixelSize(R.dimen.listview_dividerHeight)
                    selector = viewContext.getDrawable(R.drawable.listview_selector)
                    cacheColorHint = Color.TRANSPARENT
                    setBackgroundColor(Color.TRANSPARENT)
                    layoutAnimation = AnimationUtils.loadLayoutAnimation(viewContext, R.anim.list_anim_layout)
                }
            }
        },
        update = { root ->
            root.visibility = if (active) View.VISIBLE else View.INVISIBLE
            val hasSongs = sortedSongs.isNotEmpty()
            val showEmptyState = libraryLoaded && !hasSongs
            val playActionsEnabled = hasSongs && !editMode
            root.findViewById<View>(R.id.fl_null_artist)?.visibility = if (showEmptyState) View.VISIBLE else View.GONE
            root.findViewById<ActionButtonGroup>(R.id.l_alltrack_header)?.apply {
                visibility = if (hasSongs) View.VISIBLE else View.GONE
                setupLegacySongsSortHeader(selectedSortIndex) { index ->
                    selectedSortIndex = index
                }
            }
            root.findViewById<View>(R.id.play_container)?.apply {
                visibility = if (hasSongs) View.VISIBLE else View.GONE
                alpha = if (editMode) 0.22f else 1f
            }
            root.findViewById<View>(R.id.view_divider)?.visibility = if (hasSongs) View.VISIBLE else View.GONE
            root.findViewById<View>(R.id.bt_play)?.apply {
                isEnabled = playActionsEnabled
                setOnClickListener {
                    if (!playActionsEnabled || sortedSongs.isEmpty()) {
                        return@setOnClickListener
                    }
                    browser?.shuffleModeEnabled = false
                    browser?.setMediaItems(sortedSongs, 0, 0L)
                    browser?.prepare()
                    browser?.play()
                }
            }
            root.findViewById<View>(R.id.bt_shuffle)?.apply {
                isEnabled = playActionsEnabled
                setOnClickListener {
                    if (!playActionsEnabled || sortedSongs.isEmpty()) {
                        return@setOnClickListener
                    }
                    val startIndex = Random.nextInt(sortedSongs.size)
                    browser?.shuffleModeEnabled = true
                    browser?.setMediaItems(sortedSongs, startIndex, 0L)
                    browser?.prepare()
                    browser?.play()
                }
            }
            val listView = root.findViewById<ListView>(R.id.list) ?: return@AndroidView
            listView.visibility = if (hasSongs || libraryLoaded) View.VISIBLE else View.INVISIBLE
            val adapter = listView.adapter as? LegacySongsAdapter ?: LegacySongsAdapter().also { adapter ->
                listView.adapter = adapter
            }
            val previousEditMode = listView.getTag(R.id.elvitem) as? Boolean
            val animateEditMode = previousEditMode != null && previousEditMode != editMode
            listView.setTag(R.id.elvitem, editMode)
            val listContentChanged = adapter.updateItems(
                nextItems = sortedSongs,
                nextCurrentMediaId = browser?.currentMediaItem?.mediaId,
                nextCurrentIsPlaying = browser?.isPlaying == true,
                nextSectioned = selectedSortIndex == 0,
                nextEditMode = editMode,
                nextSelectedMediaIds = selectedSongIds,
            )
            if (!listContentChanged) {
                adapter.updateVisibleSongRows(
                    listView = listView,
                    animateEditMode = animateEditMode,
                )
            }
            if (listView.getTag(R.id.list) !== browser) {
                (listView.getTag(R.id.text) as? Player.Listener)?.let { oldListener ->
                    (listView.getTag(R.id.list) as? Player)?.removeListener(oldListener)
                }
                if (browser != null) {
                    val playbackListener = object : Player.Listener {
                        override fun onEvents(player: Player, events: Player.Events) {
                            adapter.setPlaybackState(
                                nextCurrentMediaId = player.currentMediaItem?.mediaId,
                                nextCurrentIsPlaying = player.isPlaying,
                            )
                            adapter.updateVisiblePlaybackState(listView)
                        }
                    }
                    browser.addListener(playbackListener)
                    listView.setTag(R.id.text, playbackListener)
                } else {
                    listView.setTag(R.id.text, null)
                }
                listView.setTag(R.id.list, browser)
            }
            root.findViewById<QuickBarEx>(R.id.main_quickbar)?.apply {
                visibility = if (hasSongs && selectedSortIndex == 0) View.VISIBLE else View.GONE
                (layoutParams as? FrameLayout.LayoutParams)?.let { params ->
                    params.gravity = Gravity.END
                    layoutParams = params
                }
                setLetters(QuickBarEx.DefaultLetters)
                setLongPressEnabled(false)
                setQBListener(
                    object : QuickBarEx.QBListener {
                        override fun onLetterChanged(letter: String, action: Int): Boolean {
                            val position = adapter.positionForLetter(letter)
                            if (position < 0) {
                                return false
                            }
                            listView.setSelection(position)
                            return true
                        }
                    },
                )
            }
            listView.setOnItemClickListener { _, _, position, _ ->
                val item = adapter.itemAt(position) ?: return@setOnItemClickListener
                if (editMode) {
                    onToggleSongSelected(item.mediaId)
                    return@setOnItemClickListener
                }
                val songIndex = adapter.songIndexAt(position) ?: return@setOnItemClickListener
                adapter.setPlaybackState(item.mediaId, true)
                adapter.updateVisiblePlaybackState(listView)
                browser?.setMediaItems(adapter.items, songIndex, 0L)
                browser?.prepare()
                browser?.play()
            }
        },
    )
}

private fun ActionButtonGroup.setupLegacySongsSortHeader(
    selectedSortIndex: Int,
    onSortSelected: (Int) -> Unit,
) {
    setActionButtonGroupBackground(R.drawable.secondary_bar)
    getLeftActionButton().visibility = View.GONE
    val sidePadding = resources.getDimensionPixelSize(R.dimen.button_group_left_right_padding)
    setActionButtonGroupSidePadding(sidePadding, sidePadding)
    setShadowDrawable(R.drawable.smartisan_secondary_bar_shadow)
    setActionButtonGroupShadowVisibility(true)

    val labels = intArrayOf(
        R.string.sort_by_song_name,
        R.string.sort_by_song_score,
        R.string.sort_by_song_play_time,
        R.string.sort_by_song_update_time,
    )
    repeat(getButtonCount().coerceAtMost(labels.size)) { index ->
        getButton(index).apply {
            setButtonText(index, labels[index])
            gravity = android.view.Gravity.CENTER
            setOnClickListener {
                onSortSelected(index)
            }
        }
    }
    setButtonActivated(selectedSortIndex)
}

private fun List<MediaItem>.sortedForLegacySort(sortIndex: Int): List<MediaItem> {
    return when (sortIndex) {
        0 -> sortedWith(
            compareBy<MediaItem> { item ->
                item.legacySortBucket()
            }.thenBy { item ->
                item.legacySortKey()
            },
        )
        else -> this
    }
}

private fun MediaItem.legacySortTitle(): String {
    return mediaMetadata.displayTitle?.toString()
        ?: mediaMetadata.title?.toString()
        ?: ""
}

private fun MediaItem.legacySortKey(): String {
    return LegacyTitleNormalizer.normalize(legacySortTitle())
}

private fun MediaItem.legacySortBucket(): String {
    val letter = legacySectionLetter()
    return if (letter == "#") {
        "ZZZ"
    } else {
        letter
    }
}

private fun MediaItem.legacySectionLetter(): String {
    val firstLetter = legacySortKey().firstOrNull { char ->
        char.isLetterOrDigit()
    } ?: return "#"
    val upper = firstLetter.uppercaseChar()
    return if (upper in 'A'..'Z') {
        upper.toString()
    } else {
        "#"
    }
}

private object LegacyTitleNormalizer {
    private val hanToLatin = runCatching {
        Transliterator.getInstance("Han-Latin; Latin-ASCII")
    }.getOrNull()
    private val combiningMarks = "\\p{Mn}+".toRegex()

    fun normalize(title: String): String {
        val trimmed = title.trim()
        val transliterated = hanToLatin?.transliterate(trimmed) ?: trimmed
        return Normalizer.normalize(transliterated, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
            .trim()
    }
}

@Composable
private fun LegacyPortPlaylistList(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val playlistRepository = remember(context.applicationContext) {
        PlaylistRepository.getInstance(context.applicationContext)
    }
    val playlists by playlistRepository.playlists.collectAsState(initial = emptyList())

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            ListView(viewContext).apply {
                divider = ColorDrawable(viewContext.getColor(R.color.listview_divider_color))
                dividerHeight = resources.getDimensionPixelSize(R.dimen.listview_dividerHeight)
                selector = viewContext.getDrawable(R.drawable.listview_selector)
                cacheColorHint = Color.TRANSPARENT
                setBackgroundColor(Color.TRANSPARENT)
                layoutAnimation = AnimationUtils.loadLayoutAnimation(viewContext, R.anim.list_anim_layout)
            }
        },
        update = { listView ->
            listView.visibility = if (active) View.VISIBLE else View.INVISIBLE
            val adapter = listView.adapter as? LegacyPlaylistAdapter ?: LegacyPlaylistAdapter().also { adapter ->
                listView.adapter = adapter
            }
            if (adapter.updateItems(playlists)) {
                listView.scheduleLayoutAnimation()
            }
        },
    )
}

@Composable
private fun LegacyPortMoreList(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            LayoutInflater.from(viewContext).inflate(R.layout.more_fragment_layout, null, false).apply {
                findViewById<ListView>(R.id.list)?.apply {
                    adapter = LegacyMoreAdapter()
                    layoutAnimation = AnimationUtils.loadLayoutAnimation(viewContext, R.anim.list_anim_layout)
                    setOnItemClickListener { _, _, _, _ -> }
                }
            }
        },
        update = { root ->
            root.visibility = if (active) View.VISIBLE else View.INVISIBLE
            root.findViewById<ListView>(R.id.list)?.apply {
                if (adapter !is LegacyMoreAdapter) {
                    adapter = LegacyMoreAdapter()
                    scheduleLayoutAnimation()
                }
            }
        },
    )
}

@Composable
internal fun rememberLegacyLibraryMediaState(): LegacyLibraryMediaState {
    val context = LocalContext.current
    val browser = LocalPlaybackBrowser.current
    val hasPermission = hasAudioPermission(context)
    var state by remember(browser) { mutableStateOf(LegacyLibraryMediaState()) }

    LaunchedEffect(browser, hasPermission) {
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

private class LegacySongsAdapter : BaseAdapter() {
    var items: List<MediaItem> = emptyList()
        private set
    private var rows: List<LegacySongRow> = emptyList()
    private var currentMediaId: String? = null
    private var currentIsPlaying: Boolean = false
    private var sectioned: Boolean = false
    private var editMode: Boolean = false
    private var selectedMediaIds: Set<String> = emptySet()

    fun updateItems(
        nextItems: List<MediaItem>,
        nextCurrentMediaId: String?,
        nextCurrentIsPlaying: Boolean,
        nextSectioned: Boolean,
        nextEditMode: Boolean,
        nextSelectedMediaIds: Set<String>,
    ): Boolean {
        val contentChanged = items != nextItems ||
            sectioned != nextSectioned
        val playbackChanged = currentMediaId != nextCurrentMediaId ||
            currentIsPlaying != nextCurrentIsPlaying
        val editModeChanged = editMode != nextEditMode
        val selectionChanged = selectedMediaIds != nextSelectedMediaIds
        if (!contentChanged && !playbackChanged && !editModeChanged && !selectionChanged) {
            return false
        }

        items = nextItems
        currentMediaId = nextCurrentMediaId
        currentIsPlaying = nextCurrentIsPlaying
        sectioned = nextSectioned
        editMode = nextEditMode
        selectedMediaIds = nextSelectedMediaIds
        if (contentChanged) {
            rows = buildLegacySongRows(nextItems, nextSectioned)
            notifyDataSetChanged()
        }
        return contentChanged
    }

    fun setPlaybackState(
        nextCurrentMediaId: String?,
        nextCurrentIsPlaying: Boolean,
    ) {
        currentMediaId = nextCurrentMediaId
        currentIsPlaying = nextCurrentIsPlaying
    }

    fun updateVisiblePlaybackState(listView: ListView) {
        updateVisibleSongRows(
            listView = listView,
            animateEditMode = false,
        )
    }

    fun updateVisibleSongRows(
        listView: ListView,
        animateEditMode: Boolean,
    ) {
        for (childIndex in 0 until listView.childCount) {
            val position = listView.firstVisiblePosition + childIndex
            val mediaItem = itemAt(position)
            val child = listView.getChildAt(childIndex)
            val titleView = child
                ?.findViewById<TextView>(R.id.listview_item_line_one) as? StretchTextView
            if (mediaItem == null || child == null || titleView == null) {
                continue
            }
            if (!editMode && mediaItem.mediaId == currentMediaId) {
                titleView.c(currentIsPlaying)
            } else {
                titleView.setShowingPlayImage(false)
            }
            (child as? EditableListViewItem)?.bindLegacyEditState(
                enabled = editMode,
                checked = mediaItem.mediaId in selectedMediaIds,
                animate = animateEditMode,
            )
        }
    }

    fun itemAt(position: Int): MediaItem? = (rows.getOrNull(position) as? LegacySongRow.Song)?.mediaItem

    fun songIndexAt(position: Int): Int? = (rows.getOrNull(position) as? LegacySongRow.Song)?.songIndex

    fun positionForLetter(letter: String): Int {
        return rows.indexOfFirst { row ->
            row is LegacySongRow.Header && row.letter == letter
        }
    }

    override fun getCount(): Int = rows.size

    override fun getItem(position: Int): Any = rows[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getViewTypeCount(): Int = 2

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is LegacySongRow.Header -> 0
            is LegacySongRow.Song -> 1
        }
    }

    override fun isEnabled(position: Int): Boolean {
        return rows.getOrNull(position) is LegacySongRow.Song
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return when (val row = rows[position]) {
            is LegacySongRow.Header -> getHeaderView(row, convertView, parent)
            is LegacySongRow.Song -> getSongView(row.mediaItem, convertView, parent)
        }
    }

    private fun getHeaderView(
        row: LegacySongRow.Header,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.smartlist_header, parent, false)
        view.setBackgroundResource(R.drawable.smartlist_header_bg)
        view.findViewById<TextView>(R.id.text)?.text = row.letter
        return view
    }

    private fun getSongView(
        item: MediaItem,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.item_track_list, parent, false)
        val metadata = item.mediaMetadata
        val selected = item.mediaId == currentMediaId
        val title = metadata.displayTitle?.toString()
            ?: metadata.title?.toString()
            ?: parent.context.getString(R.string.unknown_song_title)
        val artist = metadata.artist?.toString()
            ?: metadata.subtitle?.toString()
            ?: parent.context.getString(R.string.unknown_artist)
        val album = metadata.albumTitle?.toString()
            ?.takeIf(String::isNotBlank)
        val subtitle = if (album.isNullOrBlank()) {
            artist
        } else {
            "$artist - $album"
        }

        view.isSelected = false
        view.isActivated = false
        view.findViewById<TextView>(R.id.listview_item_line_one)?.apply {
            text = title
            isSelected = true
            setTextColor(LegacyPrimaryTextColor)
            if (this is StretchTextView) {
                if (!editMode && selected) {
                    c(currentIsPlaying)
                } else {
                    setShowingPlayImage(false)
                }
            }
        }
        view.findViewById<TextView>(R.id.listview_item_line_two)?.apply {
            text = subtitle
            setTextColor(LegacySecondaryTextColor)
        }
        view.findViewById<TextView>(R.id.tv_duration)?.text = metadata.durationMs?.formatDuration().orEmpty()
        view.findViewById<ImageView>(R.id.mime_type)?.apply {
            val badgeRes = item.legacyQualityBadgeRes()
            if (badgeRes != null) {
                visibility = View.VISIBLE
                setImageResource(badgeRes)
            } else {
                visibility = View.GONE
            }
        }
        view.findViewById<CheckBox>(R.id.cb_del)?.isChecked = item.mediaId in selectedMediaIds
        view.findViewById<View>(R.id.iv_right)?.visibility = View.GONE
        view.findViewById<View>(R.id.tv_duration)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.img_action_more)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.relativeLayout1)?.apply {
            val params = layoutParams as? RelativeLayout.LayoutParams ?: return@apply
            params.removeRule(RelativeLayout.LEFT_OF)
            params.removeRule(RelativeLayout.RIGHT_OF)
            params.removeRule(RelativeLayout.ALIGN_PARENT_LEFT)
            params.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
            params.leftMargin = resources.getDimensionPixelSize(R.dimen.listview_items_margin_left)
            params.rightMargin = resources.getDimensionPixelSize(R.dimen.letters_bar_width)
            layoutParams = params
        }
        (view as? EditableListViewItem)?.bindLegacyEditState(
            enabled = editMode,
            checked = item.mediaId in selectedMediaIds,
            animate = false,
        )
        return view
    }

    private fun Long.formatDuration(): String {
        if (this <= 0L) {
            return ""
        }
        val totalSeconds = this / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }
}

private sealed class LegacySongRow {
    data class Header(val letter: String) : LegacySongRow()

    data class Song(
        val mediaItem: MediaItem,
        val songIndex: Int,
    ) : LegacySongRow()
}

private fun buildLegacySongRows(
    mediaItems: List<MediaItem>,
    sectioned: Boolean,
): List<LegacySongRow> {
    if (!sectioned) {
        return mediaItems.mapIndexed { index, mediaItem ->
            LegacySongRow.Song(mediaItem, index)
        }
    }
    val rows = mutableListOf<LegacySongRow>()
    var previousLetter: String? = null
    mediaItems.forEachIndexed { index, mediaItem ->
        val letter = mediaItem.legacySectionLetter()
        if (letter != previousLetter) {
            rows += LegacySongRow.Header(letter)
            previousLetter = letter
        }
        rows += LegacySongRow.Song(mediaItem, index)
    }
    return rows
}

private class LegacyPlaylistAdapter : BaseAdapter() {
    private var items: List<UserPlaylistSummary> = emptyList()

    fun updateItems(nextItems: List<UserPlaylistSummary>): Boolean {
        if (items == nextItems) {
            return false
        }
        items = nextItems
        notifyDataSetChanged()
        return true
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.item_listview, parent, false)
        val item = items[position]
        view.findViewById<TextView>(R.id.listview_item_line_one)?.text = item.name
        view.findViewById<TextView>(R.id.listview_item_line_two)?.text =
            parent.context.resources.getQuantityString(
                R.plurals.legacy_playlist_song_count,
                item.songCount,
                item.songCount,
            )
        view.findViewById<View>(R.id.cb_del)?.visibility = View.GONE
        view.findViewById<View>(R.id.arrow)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.iv_right_view)?.visibility = View.GONE
        return view
    }
}

private enum class LegacyMoreEntry(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
) {
    Style(
        labelRes = R.string.tab_style,
        iconRes = R.drawable.tabbar_style_selector,
    ),
    LovedSongs(
        labelRes = R.string.collect_music,
        iconRes = R.drawable.tabbar_like_selector,
    ),
    Folder(
        labelRes = R.string.tab_directory,
        iconRes = R.drawable.tabbar_folder_selector,
    ),
}

private class LegacyMoreAdapter : BaseAdapter() {
    private val entries = LegacyMoreEntry.entries

    override fun getCount(): Int = entries.size

    override fun getItem(position: Int): Any = entries[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.more_item, parent, false)
        val itemView: ListContentItemText = (view as? ListContentItemText)
            ?: view.findViewById<ListContentItemText>(R.id.list_content_item)
            ?: return view
        val entry = entries[position]
        itemView.setIcon(entry.iconRes)
        itemView.setTitle(parent.context.getString(entry.labelRes))
        itemView.setSummary(null)
        itemView.setSubtitle(null)
        itemView.setArrowVisible(true)
        return view
    }
}

private val LegacyPrimaryTextColor = Color.rgb(0x35, 0x35, 0x39)
private val LegacySecondaryTextColor = Color.rgb(0xa4, 0xa7, 0xac)
private val LegacySelectedTextColor = Color.rgb(0xe6, 0x40, 0x40)

private fun MediaItem.legacyQualityBadgeRes(): Int? {
    return when (mediaMetadata.extras?.getString(LocalAudioLibrary.AudioQualityBadgeExtraKey)) {
        "flac" -> R.drawable.audio_quality_flac
        "ape" -> R.drawable.audio_quality_ape
        "wav" -> R.drawable.audio_quality_wav
        "aiff" -> R.drawable.audio_quality_aiff
        "alac" -> R.drawable.audio_quality_alac
        "cue" -> R.drawable.audio_quality_cue
        else -> null
    }
}

private fun String.toLegacyMediaStoreDeleteUri(): Uri? {
    val mediaStoreId = trim().toLongOrNull() ?: return null
    return ContentUris.withAppendedId(
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
        mediaStoreId,
    )
}

@Composable
private fun LegacyPortTitleBar(
    destination: MusicDestination,
    songsEditMode: Boolean,
    selectedSongCount: Int,
    albumEditMode: Boolean,
    selectedAlbumCount: Int,
    albumDetailTitle: String?,
    albumViewMode: AlbumViewMode,
    artistTarget: LegacyArtistTarget?,
    artistAlbumViewMode: AlbumViewMode,
    onEnterSongsEditMode: () -> Unit,
    onExitSongsEditMode: () -> Unit,
    onRequestDeleteSelected: () -> Unit,
    onEnterAlbumEditMode: () -> Unit,
    onExitAlbumEditMode: () -> Unit,
    onToggleAlbumViewMode: () -> Unit,
    onAlbumDetailBack: () -> Unit,
    onArtistBack: () -> Unit,
    onToggleArtistAlbumViewMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LegacyPortSmartisanTitleBar(modifier = modifier) { titleBar ->
        titleBar.setupLegacyMainTitleBar(
            destination = destination,
            songsEditMode = songsEditMode,
            selectedSongCount = selectedSongCount,
            albumEditMode = albumEditMode,
            selectedAlbumCount = selectedAlbumCount,
            albumDetailTitle = albumDetailTitle,
            albumViewMode = albumViewMode,
            artistTarget = artistTarget,
            artistAlbumViewMode = artistAlbumViewMode,
            onEnterSongsEditMode = onEnterSongsEditMode,
            onExitSongsEditMode = onExitSongsEditMode,
            onRequestDeleteSelected = onRequestDeleteSelected,
            onEnterAlbumEditMode = onEnterAlbumEditMode,
            onExitAlbumEditMode = onExitAlbumEditMode,
            onToggleAlbumViewMode = onToggleAlbumViewMode,
            onAlbumDetailBack = onAlbumDetailBack,
            onArtistBack = onArtistBack,
            onToggleArtistAlbumViewMode = onToggleArtistAlbumViewMode,
        )
    }
}

private fun TitleBar.setupLegacyMainTitleBar(
    destination: MusicDestination,
    songsEditMode: Boolean,
    selectedSongCount: Int,
    albumEditMode: Boolean,
    selectedAlbumCount: Int,
    albumDetailTitle: String?,
    albumViewMode: AlbumViewMode,
    artistTarget: LegacyArtistTarget?,
    artistAlbumViewMode: AlbumViewMode,
    onEnterSongsEditMode: () -> Unit,
    onExitSongsEditMode: () -> Unit,
    onRequestDeleteSelected: () -> Unit,
    onEnterAlbumEditMode: () -> Unit,
    onExitAlbumEditMode: () -> Unit,
    onToggleAlbumViewMode: () -> Unit,
    onAlbumDetailBack: () -> Unit,
    onArtistBack: () -> Unit,
    onToggleArtistAlbumViewMode: () -> Unit,
) {
    removeAllLeftViews()
    removeAllRightViews()
    setShadowVisible(false)
    setCenterText(albumDetailTitle ?: artistTarget?.title ?: destination.label)

    if (destination == MusicDestination.Album && albumDetailTitle != null) {
        addLeftImageView(R.drawable.standard_icon_back_selector).apply {
            setOnClickListener {
                onAlbumDetailBack()
            }
        }
        return
    }

    if (destination == MusicDestination.Artist && artistTarget != null) {
        addLeftImageView(R.drawable.standard_icon_back_selector).apply {
            setOnClickListener {
                onArtistBack()
            }
        }
        if (artistTarget.showsAlbumSwitch) {
            val switchButton = CheckBox(context, null).apply {
                setButtonDrawable(R.drawable.album_switch_selector)
                background = null
                isChecked = artistAlbumViewMode == AlbumViewMode.List
                setOnClickListener {
                    onToggleArtistAlbumViewMode()
                }
            }
            addRightView(switchButton)
        }
        return
    }

    if (destination == MusicDestination.Songs && songsEditMode) {
        addLeftImageView(R.drawable.standard_icon_cancel_selector).apply {
            setOnClickListener {
                onExitSongsEditMode()
            }
        }
        addRightImageView(R.drawable.titlebar_btn_delete_selector).apply {
            isEnabled = selectedSongCount > 0
            setOnClickListener {
                if (selectedSongCount > 0) {
                    onRequestDeleteSelected()
                }
            }
        }
        return
    }

    if (destination == MusicDestination.Album && albumEditMode) {
        addLeftImageView(R.drawable.standard_icon_cancel_selector).apply {
            setOnClickListener {
                onExitAlbumEditMode()
            }
        }
        addRightImageView(R.drawable.titlebar_btn_delete_selector).apply {
            isEnabled = selectedAlbumCount > 0
        }
        return
    }

    when (destination) {
        MusicDestination.More -> {
            addLeftImageView(R.drawable.standard_icon_settings_selector)
            addRightImageView(R.drawable.search_btn_selector)
        }
        MusicDestination.Artist -> {
            addLeftImageView(R.drawable.standard_icon_multi_select_selector).visibility = View.INVISIBLE
            addRightImageView(R.drawable.search_btn_selector)
        }
        else -> {
            addLeftImageView(R.drawable.standard_icon_multi_select_selector).apply {
                setOnClickListener {
                    when (destination) {
                        MusicDestination.Songs -> {
                            onEnterSongsEditMode()
                        }
                        MusicDestination.Album -> {
                            onEnterAlbumEditMode()
                        }
                        else -> Unit
                    }
                }
            }
            addRightImageView(R.drawable.search_btn_selector)
            if (destination == MusicDestination.Album) {
                val switchButton = CheckBox(context, null).apply {
                    setButtonDrawable(R.drawable.album_switch_selector)
                    background = null
                    isChecked = albumViewMode == AlbumViewMode.List
                    setOnClickListener {
                        onToggleAlbumViewMode()
                    }
                }
                addRightView(switchButton)
            }
        }
    }
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
): Bitmap? = withContext(Dispatchers.IO) {
    loadArtworkData(mediaItem)
        ?: loadArtworkUri(context, mediaItem)
        ?: loadAlbumArtworkUri(context, mediaItem)
        ?: loadMediaThumbnail(context, mediaItem)
        ?: loadEmbeddedPicture(context, mediaItem)
}

private fun loadArtworkData(mediaItem: MediaItem): Bitmap? {
    val artworkData = mediaItem.mediaMetadata.artworkData ?: return null
    return runCatching {
        BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)
    }.getOrNull()
}

private fun loadArtworkUri(
    context: android.content.Context,
    mediaItem: MediaItem,
): Bitmap? {
    val artworkUri = mediaItem.mediaMetadata.artworkUri ?: return null
    return runCatching {
        context.contentResolver.openInputStream(artworkUri)?.use(BitmapFactory::decodeStream)
    }.getOrNull() ?: runCatching {
        context.contentResolver.loadThumbnail(artworkUri, Size(128, 128), null)
    }.getOrNull()
}

private fun loadAlbumArtworkUri(
    context: android.content.Context,
    mediaItem: MediaItem,
): Bitmap? {
    val albumId = mediaItem.mediaMetadata.extras
        ?.getLong(LocalAudioLibrary.AlbumIdExtraKey)
        ?.takeIf { it > 0L }
        ?: return null
    val artworkUri = LocalAudioLibrary.albumArtworkUri(albumId)
    return runCatching {
        context.contentResolver.loadThumbnail(artworkUri, Size(128, 128), null)
    }.getOrNull() ?: runCatching {
        context.contentResolver.openInputStream(artworkUri)?.use(BitmapFactory::decodeStream)
    }.getOrNull()
}

private fun loadMediaThumbnail(
    context: android.content.Context,
    mediaItem: MediaItem,
): Bitmap? {
    val mediaUri = mediaItem.localConfiguration?.uri ?: return null
    return runCatching {
        context.contentResolver.loadThumbnail(mediaUri, Size(128, 128), null)
    }.getOrNull()
}

private fun loadEmbeddedPicture(
    context: android.content.Context,
    mediaItem: MediaItem,
): Bitmap? {
    val mediaUri = mediaItem.localConfiguration?.uri ?: return null
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, mediaUri)
            retriever.embeddedPicture?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

private fun ExternalAudioLaunchRequest.toExternalAudioMediaItem(
    fallbackTitle: String,
    artist: String?,
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
    if (albumId != null) {
        metadataBuilder.setArtworkUri(LocalAudioLibrary.albumArtworkUri(albumId))
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
