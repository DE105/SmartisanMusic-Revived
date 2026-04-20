package com.smartisanos.music.ui.shell

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.data.playlist.PlaylistCreateResult
import com.smartisanos.music.data.playlist.PlaylistRepository
import com.smartisanos.music.data.settings.PlaybackSettings
import com.smartisanos.music.data.settings.PlaybackSettingsStore
import com.smartisanos.music.playback.LocalPlaybackController
import com.smartisanos.music.playback.ProvidePlaybackController
import com.smartisanos.music.ui.album.AlbumViewMode
import com.smartisanos.music.ui.components.GlobalPlaybackBar
import com.smartisanos.music.ui.components.SecondaryPageTransition
import com.smartisanos.music.ui.components.SmartisanBottomBar
import com.smartisanos.music.ui.components.SmartisanTopBar
import com.smartisanos.music.ui.components.SmartisanTopBarDangerButton
import com.smartisanos.music.ui.components.SmartisanTopBarDangerButtonStyle
import com.smartisanos.music.ui.components.SmartisanTopBarIconButton
import com.smartisanos.music.ui.components.SmartisanTopBarIconButtonStyle
import com.smartisanos.music.ui.components.SmartisanTopBarShadow
import com.smartisanos.music.ui.components.SmartisanTopBarTextButton
import com.smartisanos.music.ui.components.SmartisanTopBarTextButtonStyle
import com.smartisanos.music.ui.more.MoreSecondaryPage
import com.smartisanos.music.ui.navigation.MusicDestination
import com.smartisanos.music.ui.navigation.MusicNavHost
import com.smartisanos.music.ui.playback.PlaybackScreen
import com.smartisanos.music.ui.playlist.PlaylistNameDialog
import com.smartisanos.music.ui.playlist.PlaylistPickerDialog
import kotlinx.coroutines.launch

private val ShellBackground = Color(0xFFF7F7F7)
private val SearchIconSize = 24.dp
private val AlbumViewModeIconSize = 22.dp
private val SettingsIconSize = 24.dp
private val PlaybackOverlayEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}

@Composable
fun MusicApp(playbackLaunchRequest: Int = 0) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: MusicDestination.Playlist.route
    val currentDestination = MusicDestination.entries.firstOrNull { it.route == currentRoute }
        ?: MusicDestination.Playlist
    val crossTextureBrush = rememberCrossTextureBrush()

    ProvidePlaybackController {
        val playbackController = LocalPlaybackController.current
        var playbackVisible by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        var albumViewMode by rememberSaveable { androidx.compose.runtime.mutableStateOf(AlbumViewMode.Tile) }
        var selectedAlbumId by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var selectedAlbumTitle by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var selectedArtistId by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var selectedArtistTitle by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var songsEditMode by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        var selectedSongIdsInEdit by rememberSaveable { androidx.compose.runtime.mutableStateOf(emptySet<String>()) }
        var moreSecondaryPage by rememberSaveable { androidx.compose.runtime.mutableStateOf<MoreSecondaryPage?>(null) }
        var folderEditMode by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        var selectedDirectoryKey by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var selectedDirectoryTitle by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var selectedGenreId by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var selectedGenreTitle by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var selectedDirectoryKeysInEdit by remember { androidx.compose.runtime.mutableStateOf(emptySet<String>()) }
        val exclusionsStore = remember(context.applicationContext) {
            LibraryExclusionsStore(context.applicationContext)
        }
        val playbackSettingsStore = remember(context.applicationContext) {
            PlaybackSettingsStore(context.applicationContext)
        }
        val playlistRepository = remember(context.applicationContext) {
            PlaylistRepository.getInstance(context.applicationContext)
        }
        val playbackSettings by playbackSettingsStore.settings.collectAsState(
            initial = PlaybackSettings(),
        )
        val playlists by playlistRepository.playlists.collectAsState(initial = emptyList())
        val appScope = rememberCoroutineScope()
        val showFolderPage = moreSecondaryPage == MoreSecondaryPage.Folder
        val showGenrePage = moreSecondaryPage == MoreSecondaryPage.Style
        val folderDetailVisible = showFolderPage && selectedDirectoryKey != null
        val folderOverviewEditing = showFolderPage && !folderDetailVisible && folderEditMode
        val genreDetailVisible = showGenrePage && selectedGenreId != null
        var pendingPlaylistPickerMediaItems by remember { androidx.compose.runtime.mutableStateOf<List<MediaItem>?>(null) }
        var showExternalPlaylistCreateDialog by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        var externalPlaylistCreateInitialValue by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }

        val closeAlbumDetail = {
            selectedAlbumId = null
            selectedAlbumTitle = null
        }
        val closeArtistDetail = {
            selectedArtistId = null
            selectedArtistTitle = null
        }
        val closeSongsEdit = {
            songsEditMode = false
            selectedSongIdsInEdit = emptySet()
        }
        val closeFolderPage = {
            moreSecondaryPage = null
            folderEditMode = false
            selectedDirectoryKey = null
            selectedDirectoryTitle = null
            selectedDirectoryKeysInEdit = emptySet()
        }
        val closeGenreDetail = {
            selectedGenreId = null
            selectedGenreTitle = null
        }
        val closeGenrePage = {
            closeGenreDetail()
            moreSecondaryPage = null
        }
        val showToast = { textRes: Int ->
            Toast.makeText(context, context.getString(textRes), Toast.LENGTH_SHORT).show()
        }
        val enqueueMediaItems = { items: List<MediaItem> ->
            if (items.isEmpty()) {
                Unit
            }
            if (playbackController?.repeatMode == Player.REPEAT_MODE_ONE) {
                showToast(R.string.can_not_add_to_queue_single_repeat)
            } else {
                playbackController?.addMediaItems(items)
                showToast(R.string.add_to_queue_success)
            }
        }
        val requestAddToPlaylist = { items: List<MediaItem> ->
            val candidates = items.filter { it.mediaId.isNotBlank() }
            if (candidates.isNotEmpty()) {
                pendingPlaylistPickerMediaItems = candidates
            }
        }
        val handleFolderBack = {
            when {
                folderDetailVisible -> {
                    folderEditMode = false
                    selectedDirectoryKeysInEdit = emptySet()
                    selectedDirectoryKey = null
                    selectedDirectoryTitle = null
                }
                folderOverviewEditing -> {
                    folderEditMode = false
                    selectedDirectoryKeysInEdit = emptySet()
                }
                else -> closeFolderPage()
            }
        }
        val handleMoreBack = {
            when (moreSecondaryPage) {
                MoreSecondaryPage.Folder -> handleFolderBack()
                MoreSecondaryPage.LovedSongs -> {
                    moreSecondaryPage = null
                }
                MoreSecondaryPage.Settings -> {
                    moreSecondaryPage = null
                }
                MoreSecondaryPage.Style -> {
                    if (genreDetailVisible) {
                        closeGenreDetail()
                    } else {
                        closeGenrePage()
                    }
                }
                null -> Unit
            }
        }

        LaunchedEffect(playbackLaunchRequest) {
            if (playbackLaunchRequest > 0) {
                playbackVisible = true
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = ShellBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    SmartisanBottomBar(
                        currentRoute = currentRoute,
                        onDestinationSelected = { destination ->
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                ) {
                    val toggleAlbumViewMode = {
                        albumViewMode = when (albumViewMode) {
                            AlbumViewMode.List -> AlbumViewMode.Tile
                            AlbumViewMode.Tile -> AlbumViewMode.List
                        }
                    }
                    if (currentDestination == MusicDestination.Playlist) {
                        Unit
                    } else if (currentDestination == MusicDestination.Album) {
                        SecondaryPageTransition(
                            secondaryKey = selectedAlbumId,
                            modifier = Modifier.fillMaxWidth(),
                            label = "album top bar",
                            primaryContent = {
                                MusicShellTopBar(
                                    destination = currentDestination,
                                    albumViewMode = albumViewMode,
                                    detailTitle = null,
                                    onAlbumViewModeToggle = toggleAlbumViewMode,
                                    onDetailBack = closeAlbumDetail,
                                )
                            },
                            secondaryContent = {
                                MusicShellTopBar(
                                    destination = currentDestination,
                                    albumViewMode = albumViewMode,
                                    detailTitle = selectedAlbumTitle,
                                    onAlbumViewModeToggle = toggleAlbumViewMode,
                                    onDetailBack = closeAlbumDetail,
                                )
                            },
                        )
                    } else if (currentDestination == MusicDestination.Artist) {
                        SecondaryPageTransition(
                            secondaryKey = selectedArtistId,
                            modifier = Modifier.fillMaxWidth(),
                            label = "artist top bar",
                            primaryContent = {
                                MusicShellTopBar(
                                    destination = currentDestination,
                                    albumViewMode = albumViewMode,
                                    detailTitle = null,
                                    onAlbumViewModeToggle = toggleAlbumViewMode,
                                    onDetailBack = closeArtistDetail,
                                )
                            },
                            secondaryContent = {
                                MusicShellTopBar(
                                    destination = currentDestination,
                                    albumViewMode = albumViewMode,
                                    detailTitle = selectedArtistTitle,
                                    onAlbumViewModeToggle = toggleAlbumViewMode,
                                    onDetailBack = closeArtistDetail,
                                )
                            },
                        )
                    } else if (currentDestination == MusicDestination.More) {
                        SecondaryPageTransition(
                            secondaryKey = moreSecondaryPage,
                            modifier = Modifier.fillMaxWidth(),
                            label = "more top bar",
                            primaryContent = {
                                MusicShellTopBar(
                                    destination = currentDestination,
                                    albumViewMode = albumViewMode,
                                    detailTitle = null,
                                    onAlbumViewModeToggle = toggleAlbumViewMode,
                                    onDetailBack = closeAlbumDetail,
                                    onMoreSettingsClick = {
                                        moreSecondaryPage = MoreSecondaryPage.Settings
                                    },
                                )
                            },
                            secondaryContent = { page ->
                                when (page) {
                                    MoreSecondaryPage.Folder -> {
                                        val title = selectedDirectoryTitle ?: stringResource(R.string.tab_directory)
                                        val showsFolderDetail = selectedDirectoryKey != null
                                        val showsFolderEditActions = !showsFolderDetail && folderEditMode
                                        SmartisanTopBar(
                                            title = title,
                                            leftContent = {
                                                when {
                                                    showsFolderDetail -> SmartisanTopBarTextButton(
                                                        text = stringResource(R.string.tab_directory),
                                                        buttonStyle = SmartisanTopBarTextButtonStyle.Back,
                                                        onClick = handleMoreBack,
                                                    )
                                                    showsFolderEditActions -> SmartisanTopBarTextButton(
                                                        text = stringResource(R.string.done),
                                                        buttonStyle = SmartisanTopBarTextButtonStyle.Toolbar,
                                                        onClick = handleMoreBack,
                                                    )
                                                    else -> SmartisanTopBarTextButton(
                                                        text = currentDestination.label,
                                                        buttonStyle = SmartisanTopBarTextButtonStyle.Back,
                                                        onClick = handleMoreBack,
                                                    )
                                                }
                                            },
                                            rightContent = if (showsFolderDetail) {
                                                null
                                            } else {
                                                {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        if (showsFolderEditActions) {
                                                            SmartisanTopBarDangerButton(
                                                                text = stringResource(R.string.delete),
                                                                enabled = selectedDirectoryKeysInEdit.isNotEmpty(),
                                                                buttonStyle = SmartisanTopBarDangerButtonStyle.Toolbar,
                                                                onClick = {
                                                                    val targets = selectedDirectoryKeysInEdit
                                                                    if (targets.isEmpty()) {
                                                                        return@SmartisanTopBarDangerButton
                                                                    }
                                                                    appScope.launch {
                                                                        exclusionsStore.hideDirectoryKeys(targets)
                                                                    }
                                                                    folderEditMode = false
                                                                    selectedDirectoryKeysInEdit = emptySet()
                                                                },
                                                            )
                                                        } else {
                                                            SmartisanTopBarIconButton(
                                                                iconRes = R.drawable.search_icon,
                                                                pressedIconRes = R.drawable.search_icon_down,
                                                                contentDescription = stringResource(R.string.tab_local_search),
                                                                iconSize = SearchIconSize,
                                                                buttonStyle = SmartisanTopBarIconButtonStyle.Toolbar,
                                                            )
                                                            SmartisanTopBarTextButton(
                                                                text = stringResource(R.string.edit),
                                                                buttonStyle = SmartisanTopBarTextButtonStyle.Toolbar,
                                                                onClick = {
                                                                    selectedDirectoryKeysInEdit = emptySet()
                                                                    folderEditMode = true
                                                                },
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                        )
                                    }
                                    MoreSecondaryPage.LovedSongs -> {
                                        SmartisanTopBar(
                                            title = stringResource(R.string.collect_music),
                                            leftContent = {
                                                SmartisanTopBarTextButton(
                                                    text = stringResource(R.string.back),
                                                    buttonStyle = SmartisanTopBarTextButtonStyle.Back,
                                                    onClick = handleMoreBack,
                                                )
                                            },
                                        )
                                    }
                                    MoreSecondaryPage.Settings -> {
                                        SmartisanTopBar(
                                            title = stringResource(R.string.setting),
                                            leftContent = {
                                                SmartisanTopBarTextButton(
                                                    text = currentDestination.label,
                                                    buttonStyle = SmartisanTopBarTextButtonStyle.Back,
                                                    onClick = handleMoreBack,
                                                )
                                            },
                                        )
                                    }
                                    MoreSecondaryPage.Style -> {
                                        SecondaryPageTransition(
                                            secondaryKey = selectedGenreId,
                                            modifier = Modifier.fillMaxWidth(),
                                            label = "genre top bar",
                                            primaryContent = {
                                                SmartisanTopBar(
                                                    title = stringResource(R.string.tab_style),
                                                    leftContent = {
                                                        SmartisanTopBarTextButton(
                                                            text = currentDestination.label,
                                                            buttonStyle = SmartisanTopBarTextButtonStyle.Back,
                                                            onClick = handleMoreBack,
                                                        )
                                                    },
                                                    rightContent = {
                                                        SmartisanTopBarIconButton(
                                                            iconRes = R.drawable.search_icon,
                                                            pressedIconRes = R.drawable.search_icon_down,
                                                            contentDescription = stringResource(R.string.tab_local_search),
                                                            iconSize = SearchIconSize,
                                                            buttonStyle = SmartisanTopBarIconButtonStyle.Toolbar,
                                                        )
                                                    },
                                                )
                                            },
                                            secondaryContent = {
                                                SmartisanTopBar(
                                                    title = selectedGenreTitle ?: stringResource(R.string.tab_style),
                                                    leftContent = {
                                                        SmartisanTopBarTextButton(
                                                            text = stringResource(R.string.tab_style),
                                                            buttonStyle = SmartisanTopBarTextButtonStyle.Back,
                                                            onClick = handleMoreBack,
                                                        )
                                                    },
                                                )
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    } else if (currentDestination == MusicDestination.Songs && songsEditMode) {
                        SmartisanTopBar(
                            title = currentDestination.label,
                            leftContent = {
                                SmartisanTopBarTextButton(
                                    text = stringResource(R.string.done),
                                    buttonStyle = SmartisanTopBarTextButtonStyle.Toolbar,
                                    onClick = closeSongsEdit,
                                )
                            },
                            rightContent = {
                                SmartisanTopBarDangerButton(
                                    text = stringResource(R.string.delete),
                                    enabled = selectedSongIdsInEdit.isNotEmpty(),
                                    buttonStyle = SmartisanTopBarDangerButtonStyle.Toolbar,
                                    onClick = {
                                        val targets = selectedSongIdsInEdit
                                        if (targets.isEmpty()) {
                                            return@SmartisanTopBarDangerButton
                                        }
                                        appScope.launch {
                                            exclusionsStore.hideMediaIds(targets)
                                        }
                                        closeSongsEdit()
                                    },
                                )
                            },
                        )
                    } else {
                        MusicShellTopBar(
                            destination = currentDestination,
                            albumViewMode = albumViewMode,
                            detailTitle = null,
                            onAlbumViewModeToggle = toggleAlbumViewMode,
                            onDetailBack = closeAlbumDetail,
                            onEditClick = {
                                if (currentDestination == MusicDestination.Songs) {
                                    songsEditMode = true
                                    selectedSongIdsInEdit = emptySet()
                                }
                            },
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .background(brush = crossTextureBrush),
                    ) {
                        MusicNavHost(
                            navController = navController,
                            albumViewMode = albumViewMode,
                            selectedAlbumId = selectedAlbumId,
                            selectedArtistId = selectedArtistId,
                            moreSecondaryPage = moreSecondaryPage,
                            folderEditMode = folderEditMode,
                            selectedDirectoryKey = selectedDirectoryKey,
                            selectedGenreId = selectedGenreId,
                            playbackSettings = playbackSettings,
                            onAlbumSelected = { id, title ->
                                selectedAlbumId = id
                                selectedAlbumTitle = title
                            },
                            onAlbumBack = closeAlbumDetail,
                            onArtistSelected = { id, title ->
                                selectedArtistId = id
                                selectedArtistTitle = title
                            },
                            onArtistBack = closeArtistDetail,
                            onMoreEntryClick = { entryName ->
                                when (entryName) {
                                    "Folder" -> {
                                        closeGenreDetail()
                                        moreSecondaryPage = MoreSecondaryPage.Folder
                                    }
                                    "LovedSongs" -> {
                                        closeGenreDetail()
                                        moreSecondaryPage = MoreSecondaryPage.LovedSongs
                                    }
                                    "Style" -> {
                                        closeGenreDetail()
                                        moreSecondaryPage = MoreSecondaryPage.Style
                                    }
                                }
                            },
                            onMoreSecondaryBack = handleMoreBack,
                            onDirectorySelected = { key, title ->
                                folderEditMode = false
                                selectedDirectoryKeysInEdit = emptySet()
                                selectedDirectoryKey = key
                                selectedDirectoryTitle = title
                            },
                            onDirectoryBack = {
                                selectedDirectoryKey = null
                                selectedDirectoryTitle = null
                            },
                            onDirectoryEditSelectionChanged = { selection ->
                                selectedDirectoryKeysInEdit = selection
                            },
                            onGenreSelected = { id, title ->
                                selectedGenreId = id
                                selectedGenreTitle = title
                            },
                            onGenreBack = closeGenreDetail,
                            onSongsEditSelectionChanged = { selection ->
                                selectedSongIdsInEdit = selection
                            },
                            songsEditMode = songsEditMode,
                            selectedSongIdsInEdit = selectedSongIdsInEdit,
                            onRequestAddToPlaylist = requestAddToPlaylist,
                            onRequestAddToQueue = enqueueMediaItems,
                            onScratchEnabledChange = { enabled ->
                                appScope.launch {
                                    playbackSettingsStore.setScratchEnabled(enabled)
                                }
                            },
                            onHidePlayerAxisEnabledChange = { enabled ->
                                appScope.launch {
                                    playbackSettingsStore.setHidePlayerAxisEnabled(enabled)
                                }
                            },
                            onPopcornSoundEnabledChange = { enabled ->
                                appScope.launch {
                                    playbackSettingsStore.setPopcornSoundEnabled(enabled)
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                        GlobalPlaybackBar(
                            modifier = Modifier.align(Alignment.BottomCenter),
                            onOpenPlayback = {
                                playbackVisible = true
                            },
                        )
                        if (currentDestination != MusicDestination.Playlist) {
                            SmartisanTopBarShadow(
                                modifier = Modifier.align(Alignment.TopCenter),
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = playbackVisible,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                enter = slideInVertically(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = PlaybackOverlayEasing,
                    ),
                    initialOffsetY = { fullHeight -> fullHeight },
                ),
                exit = slideOutVertically(
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = PlaybackOverlayEasing,
                    ),
                    targetOffsetY = { fullHeight -> fullHeight },
                ),
            ) {
                PlaybackScreen(
                    playbackSettings = playbackSettings,
                    modifier = Modifier.fillMaxSize(),
                    onRequestAddToPlaylist = requestAddToPlaylist,
                    onRequestAddToQueue = enqueueMediaItems,
                    onScratchEnabledChange = { enabled ->
                        appScope.launch {
                            playbackSettingsStore.setScratchEnabled(enabled)
                        }
                    },
                    onCollapse = {
                        playbackVisible = false
                    },
                )
            }
            if (pendingPlaylistPickerMediaItems != null && !showExternalPlaylistCreateDialog) {
                PlaylistPickerDialog(
                    playlists = playlists,
                    onDismiss = {
                        pendingPlaylistPickerMediaItems = null
                    },
                    onCreateNewPlaylist = {
                        appScope.launch {
                            externalPlaylistCreateInitialValue = playlistRepository.suggestNextUntitledName()
                            showExternalPlaylistCreateDialog = true
                        }
                    },
                    onPlaylistSelected = { playlistId ->
                        val mediaIds = pendingPlaylistPickerMediaItems?.map(MediaItem::mediaId).orEmpty()
                        appScope.launch {
                            val result = playlistRepository.addMediaIds(playlistId, mediaIds)
                            when {
                                result.addedCount > 0 -> showToast(R.string.playlist_added)
                                result.duplicateCount > 0 -> showToast(R.string.playlist_song_exists)
                            }
                            pendingPlaylistPickerMediaItems = null
                        }
                    },
                )
            }
            if (showExternalPlaylistCreateDialog) {
                PlaylistNameDialog(
                    title = stringResource(R.string.new_playlist),
                    initialValue = externalPlaylistCreateInitialValue,
                    selectAllOnOpen = true,
                    onDismiss = {
                        showExternalPlaylistCreateDialog = false
                    },
                    onConfirm = { input ->
                        val mediaIds = pendingPlaylistPickerMediaItems?.map(MediaItem::mediaId).orEmpty()
                        appScope.launch {
                            when (val result = playlistRepository.createPlaylist(input, mediaIds)) {
                                PlaylistCreateResult.EmptyName -> showToast(R.string.playlist_create_failed)
                                PlaylistCreateResult.DuplicateName -> showToast(R.string.playlist_duplicate_name)
                                is PlaylistCreateResult.Success -> {
                                    showExternalPlaylistCreateDialog = false
                                    pendingPlaylistPickerMediaItems = null
                                    showToast(R.string.playlist_added)
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MusicShellTopBar(
    destination: MusicDestination,
    albumViewMode: AlbumViewMode,
    detailTitle: String?,
    onAlbumViewModeToggle: () -> Unit,
    onDetailBack: () -> Unit,
    onEditClick: () -> Unit = {},
    onMoreSettingsClick: () -> Unit = {},
) {
    val showsDetail = detailTitle != null
    val showsSettings = !showsDetail && destination == MusicDestination.More
    val showsEdit = !showsDetail && (
            destination == MusicDestination.Album ||
            destination == MusicDestination.Songs
        )
    val showsSearch = !showsDetail && (
        destination == MusicDestination.Playlist ||
            destination == MusicDestination.Artist ||
            destination == MusicDestination.Album ||
            destination == MusicDestination.Songs ||
            destination == MusicDestination.More
        )
    val showsAlbumViewModeToggle = !showsDetail && destination == MusicDestination.Album

    SmartisanTopBar(
        title = detailTitle ?: destination.label,
        leftContent = if (showsDetail) {
            {
                SmartisanTopBarTextButton(
                    text = stringResource(R.string.back),
                    buttonStyle = SmartisanTopBarTextButtonStyle.Back,
                    onClick = onDetailBack,
                )
            }
        } else if (showsSettings) {
            {
                SmartisanTopBarIconButton(
                    iconRes = R.drawable.tabbar_setting,
                    pressedIconRes = R.drawable.tabbar_setting_white,
                    contentDescription = stringResource(R.string.setting),
                    iconSize = SettingsIconSize,
                    buttonStyle = SmartisanTopBarIconButtonStyle.Toolbar,
                    onClick = onMoreSettingsClick,
                )
            }
        } else if (showsEdit) {
            {
                SmartisanTopBarTextButton(
                    text = stringResource(R.string.edit),
                    buttonStyle = SmartisanTopBarTextButtonStyle.Toolbar,
                    onClick = onEditClick,
                )
            }
        } else {
            null
        },
        rightContent = if (showsSearch || showsAlbumViewModeToggle) {
            {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (showsAlbumViewModeToggle) {
                        SmartisanTopBarIconButton(
                            iconRes = if (albumViewMode == AlbumViewMode.Tile) {
                                R.drawable.btn_display_list2
                            } else {
                                R.drawable.btn_display_tile2
                            },
                            pressedIconRes = if (albumViewMode == AlbumViewMode.Tile) {
                                R.drawable.btn_display_list2_down
                            } else {
                                R.drawable.btn_display_tile2_down
                            },
                            contentDescription = if (albumViewMode == AlbumViewMode.Tile) {
                                stringResource(R.string.listview_header_list)
                            } else {
                                stringResource(R.string.listview_header_tile)
                            },
                            iconSize = AlbumViewModeIconSize,
                            buttonStyle = SmartisanTopBarIconButtonStyle.Toolbar,
                            onClick = onAlbumViewModeToggle,
                        )
                    }
                    if (showsSearch) {
                        SmartisanTopBarIconButton(
                            iconRes = R.drawable.search_icon,
                            pressedIconRes = R.drawable.search_icon_down,
                            contentDescription = stringResource(R.string.tab_local_search),
                            iconSize = SearchIconSize,
                            buttonStyle = SmartisanTopBarIconButtonStyle.Toolbar,
                        )
                    }
                }
            }
        } else {
            null
        },
    )
}

@Composable
private fun rememberCrossTextureBrush(): ShaderBrush {
    val texture = ImageBitmap.imageResource(id = R.drawable.ablum_crosstexture_bg)
    return remember(texture) {
        ShaderBrush(
            ImageShader(
                image = texture,
                tileModeX = TileMode.Repeated,
                tileModeY = TileMode.Repeated,
            )
        )
    }
}
