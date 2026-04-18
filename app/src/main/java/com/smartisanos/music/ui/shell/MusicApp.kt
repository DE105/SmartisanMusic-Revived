package com.smartisanos.music.ui.shell

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
import com.smartisanos.music.R
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.data.settings.PlaybackSettings
import com.smartisanos.music.data.settings.PlaybackSettingsStore
import com.smartisanos.music.playback.ProvidePlaybackController
import com.smartisanos.music.ui.album.AlbumViewMode
import com.smartisanos.music.ui.components.GlobalPlaybackBar
import com.smartisanos.music.ui.components.SecondaryPageTransition
import com.smartisanos.music.ui.components.SmartisanBottomBar
import com.smartisanos.music.ui.components.SmartisanTopBar
import com.smartisanos.music.ui.components.SmartisanTopBarIconButton
import com.smartisanos.music.ui.components.SmartisanTopBarShadow
import com.smartisanos.music.ui.components.SmartisanTopBarTextButton
import com.smartisanos.music.ui.components.SmartisanTopBarDangerButton
import com.smartisanos.music.ui.more.MoreSecondaryPage
import com.smartisanos.music.ui.navigation.MusicDestination
import com.smartisanos.music.ui.navigation.MusicNavHost
import com.smartisanos.music.ui.playback.PlaybackScreen
import kotlinx.coroutines.launch

private val ShellBackground = Color(0xFFF7F7F7)
private val SearchIconSize = 34.dp
private val AlbumViewModeIconSize = 18.dp
private val SettingsIconSize = 18.dp
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
        var playbackVisible by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        var albumViewMode by rememberSaveable { androidx.compose.runtime.mutableStateOf(AlbumViewMode.Tile) }
        var selectedAlbumId by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var selectedAlbumTitle by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var selectedArtistId by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var selectedArtistTitle by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var moreSecondaryPage by rememberSaveable { androidx.compose.runtime.mutableStateOf<MoreSecondaryPage?>(null) }
        var folderEditMode by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
        var selectedDirectoryKey by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var selectedDirectoryTitle by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
        var selectedDirectoryKeysInEdit by remember { androidx.compose.runtime.mutableStateOf(emptySet<String>()) }
        val exclusionsStore = remember(context.applicationContext) {
            LibraryExclusionsStore(context.applicationContext)
        }
        val playbackSettingsStore = remember(context.applicationContext) {
            PlaybackSettingsStore(context.applicationContext)
        }
        val playbackSettings by playbackSettingsStore.settings.collectAsState(
            initial = PlaybackSettings(),
        )
        val appScope = rememberCoroutineScope()
        val showFolderPage = moreSecondaryPage == MoreSecondaryPage.Folder
        val folderDetailVisible = showFolderPage && selectedDirectoryKey != null
        val folderOverviewEditing = showFolderPage && !folderDetailVisible && folderEditMode

        val closeAlbumDetail = {
            selectedAlbumId = null
            selectedAlbumTitle = null
        }
        val closeArtistDetail = {
            selectedArtistId = null
            selectedArtistTitle = null
        }
        val closeFolderPage = {
            moreSecondaryPage = null
            folderEditMode = false
            selectedDirectoryKey = null
            selectedDirectoryTitle = null
            selectedDirectoryKeysInEdit = emptySet()
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
                MoreSecondaryPage.Settings -> {
                    moreSecondaryPage = null
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
                    if (currentDestination == MusicDestination.Album) {
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
                                                        onClick = handleMoreBack,
                                                    )
                                                    showsFolderEditActions -> SmartisanTopBarTextButton(
                                                        text = stringResource(R.string.done),
                                                        onClick = handleMoreBack,
                                                    )
                                                    else -> SmartisanTopBarTextButton(
                                                        text = currentDestination.label,
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
                                                            )
                                                            SmartisanTopBarTextButton(
                                                                text = stringResource(R.string.edit),
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
                                    MoreSecondaryPage.Settings -> {
                                        SmartisanTopBar(
                                            title = stringResource(R.string.setting),
                                            leftContent = {
                                                SmartisanTopBarTextButton(
                                                    text = currentDestination.label,
                                                    onClick = handleMoreBack,
                                                )
                                            },
                                        )
                                    }
                                }
                            },
                        )
                    } else {
                        MusicShellTopBar(
                            destination = currentDestination,
                            albumViewMode = albumViewMode,
                            detailTitle = null,
                            onAlbumViewModeToggle = toggleAlbumViewMode,
                            onDetailBack = closeAlbumDetail,
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
                                if (entryName == "Folder") {
                                    moreSecondaryPage = MoreSecondaryPage.Folder
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
                        SmartisanTopBarShadow(
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
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
    onMoreSettingsClick: () -> Unit = {},
) {
    val showsDetail = detailTitle != null
    val showsSettings = !showsDetail && destination == MusicDestination.More
    val showsEdit = !showsDetail && (
        destination == MusicDestination.Playlist ||
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
                    onClick = onMoreSettingsClick,
                )
            }
        } else if (showsEdit) {
            {
                SmartisanTopBarTextButton(
                    text = stringResource(R.string.edit),
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
                            onClick = onAlbumViewModeToggle,
                        )
                    }
                    if (showsSearch) {
                        SmartisanTopBarIconButton(
                            iconRes = R.drawable.search_icon,
                            pressedIconRes = R.drawable.search_icon_down,
                            contentDescription = stringResource(R.string.tab_local_search),
                            iconSize = SearchIconSize,
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
