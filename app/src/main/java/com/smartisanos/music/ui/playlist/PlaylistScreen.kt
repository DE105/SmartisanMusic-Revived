package com.smartisanos.music.ui.playlist

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.data.playlist.PlaylistAddResult
import com.smartisanos.music.data.playlist.PlaylistCreateResult
import com.smartisanos.music.data.playlist.PlaylistRenameResult
import com.smartisanos.music.data.playlist.PlaylistRepository
import com.smartisanos.music.data.playlist.UserPlaylistDetail
import com.smartisanos.music.data.playlist.UserPlaylistSummary
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.await
import com.smartisanos.music.ui.components.SecondaryPageTransition
import com.smartisanos.music.ui.components.SmartisanBlankState
import com.smartisanos.music.ui.components.SmartisanConfirmDialog
import com.smartisanos.music.ui.components.SmartisanTopBar
import com.smartisanos.music.ui.components.SmartisanTopBarDangerButton
import com.smartisanos.music.ui.components.SmartisanTopBarIconButton
import com.smartisanos.music.ui.components.SmartisanTopBarShadow
import com.smartisanos.music.ui.components.SmartisanTopBarTextButton
import com.smartisanos.music.ui.components.audioPermission
import com.smartisanos.music.ui.components.hasAudioPermission
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

private val PlaylistPageBackground = Color(0xFFF7F7F7)
private val PlaylistListRowBackground = Color(0xFFFDFDFD)
private val PlaylistListDivider = Color(0xFFE7E7E7)
private val PlaylistListPressedBackground = Color(0xFFF0F0F0)
private val PlaylistTitleColor = Color(0xCC000000)
private val PlaylistSubtitleColor = Color(0x73000000)
private val PlaylistArrowColor = Color(0x59000000)
private val PlaylistActionRowBackground = Color(0xFFFBFBFB)
private val PlaylistActionDivider = Color(0xFFE4E4E4)
private val PlaylistActionTextColor = Color(0x8F000000)
private val PlaylistActionPressedBackground = Color(0xFFF0F0F0)
private val PlaylistCompactActionBorder = Color(0xFFD7D7D7)
private val PlaylistTrackSelectedColor = Color(0xFFE64040)
private val PlaylistTrackDurationColor = Color(0x66000000)
private val PlaylistSelectionCircleBorder = Color(0x2E000000)
private val PlaylistSelectionCircleFill = Color(0xFFE95A4E)
private val PlaylistSelectionCheckColor = Color.White
private val PlaylistOverlayEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}

private val PlaylistTitleStyle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.Medium,
    color = PlaylistTitleColor,
)
private val PlaylistSubtitleStyle = TextStyle(
    fontSize = 13.sp,
    color = PlaylistSubtitleColor,
)
private val PlaylistTrackTitleStyle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    color = PlaylistTitleColor,
)
private val PlaylistTrackSubtitleStyle = TextStyle(
    fontSize = 13.sp,
    color = PlaylistSubtitleColor,
)
private val PlaylistActionTextStyle = TextStyle(
    fontSize = 14.sp,
    color = PlaylistActionTextColor,
)

private val PlaylistRowHeight = 61.dp
private val PlaylistActionRowHeight = 45.dp
private val PlaylistMiniPlayerReservedHeight = 73.dp
private val PlaylistSelectionSize = 22.dp
private val PlaylistCompactActionButtonHeight = 34.dp
private val PlaylistCompactActionButtonIconSize = 18.dp
private val PlaylistCompactActionButtonShape = RoundedCornerShape(8.dp)
private val PlaylistRowStartPadding = 18.dp
private val PlaylistRowContentEndPadding = 2.dp
private val PlaylistDurationStartPadding = 6.dp
private val PlaylistDurationOuterPadding = 2.dp
private val PlaylistTrackMoreWidth = 43.dp
private const val PlaylistOverlayDurationMillis = 300
private const val PlaylistOverlayFadeMillis = 160

@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    onRequestAddToPlaylist: (List<MediaItem>) -> Unit = {},
    onRequestAddToQueue: (List<MediaItem>) -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playbackBrowser = LocalPlaybackBrowser.current
    val exclusionsStore = remember(context.applicationContext) {
        LibraryExclusionsStore(context.applicationContext)
    }
    val playlistRepository = remember(context.applicationContext) {
        PlaylistRepository.getInstance(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    val libraryRevision by exclusionsStore.revision.collectAsState(initial = 0)
    val playlists by playlistRepository.playlists.collectAsState(initial = emptyList())
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }
    var permissionVersion by remember { mutableIntStateOf(0) }
    var songs by remember(playbackBrowser) { mutableStateOf(emptyList<MediaItem>()) }
    var currentMediaId by remember(playbackBrowser) {
        mutableStateOf(playbackBrowser?.currentMediaItem?.mediaId)
    }
    var rootEditMode by rememberSaveable { mutableStateOf(false) }
    var selectedPlaylistIdsInEdit by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var selectedPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var detailEditMode by rememberSaveable { mutableStateOf(false) }
    var selectedTrackIdsInEdit by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var pendingTrackActionMediaId by rememberSaveable { mutableStateOf<String?>(null) }
    var addSongsTargetPlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSongIdsForAdd by rememberSaveable { mutableStateOf(emptySet<String>()) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var createDialogInitialValue by rememberSaveable { mutableStateOf("") }
    var renamePlaylistId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDeletePlaylistsDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteSinglePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteTracksDialog by rememberSaveable { mutableStateOf(false) }
    val hasPermission = hasAudioPermission(context)

    val activePlaylistId = addSongsTargetPlaylistId ?: selectedPlaylistId
    val activePlaylistFlow = remember(activePlaylistId, playlistRepository) {
        activePlaylistId?.let(playlistRepository::observePlaylistDetail) ?: flowOf(null)
    }
    val activePlaylist by activePlaylistFlow.collectAsState(initial = null)
    val librarySongsById = remember(songs) { songs.associateBy(MediaItem::mediaId) }
    val detailTracks = remember(activePlaylist, librarySongsById) {
        activePlaylist?.mediaIds?.mapNotNull(librarySongsById::get).orEmpty()
    }
    val pendingTrackActionItem = remember(pendingTrackActionMediaId, detailTracks) {
        detailTracks.firstOrNull { it.mediaId == pendingTrackActionMediaId }
    }
    val addableSongs = remember(activePlaylist, songs) {
        val existingIds = activePlaylist?.mediaIds?.toSet().orEmpty()
        songs.filterNot { it.mediaId in existingIds }
    }
    val addSongsCandidates = remember(addableSongs) {
        addableSongs.sortedBy { it.playlistTitleKey() }
    }
    val openPlaylistAddSongs: (String) -> Unit = { playlistId ->
        selectedPlaylistId = playlistId
        detailEditMode = false
        selectedTrackIdsInEdit = emptySet()
        pendingTrackActionMediaId = null
        addSongsTargetPlaylistId = playlistId
        selectedSongIdsForAdd = emptySet()
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

    LaunchedEffect(playlists, selectedPlaylistId, addSongsTargetPlaylistId) {
        if (selectedPlaylistId != null && playlists.none { it.id == selectedPlaylistId }) {
            selectedPlaylistId = null
            detailEditMode = false
            selectedTrackIdsInEdit = emptySet()
            pendingTrackActionMediaId = null
            addSongsTargetPlaylistId = null
            selectedSongIdsForAdd = emptySet()
        }
        if (addSongsTargetPlaylistId != null && playlists.none { it.id == addSongsTargetPlaylistId }) {
            addSongsTargetPlaylistId = null
            selectedSongIdsForAdd = emptySet()
        }
    }

    BackHandler(
        enabled = rootEditMode || selectedPlaylistId != null || addSongsTargetPlaylistId != null,
    ) {
        when {
            addSongsTargetPlaylistId != null -> {
                addSongsTargetPlaylistId = null
                selectedSongIdsForAdd = emptySet()
            }
            detailEditMode -> {
                detailEditMode = false
                selectedTrackIdsInEdit = emptySet()
            }
            selectedPlaylistId != null -> {
                selectedPlaylistId = null
            }
            rootEditMode -> {
                rootEditMode = false
                selectedPlaylistIdsInEdit = emptySet()
            }
        }
    }

    if (pendingTrackActionItem != null && activePlaylist != null) {
        val targetItem = pendingTrackActionItem
        PlaylistTrackActionDialog(
            thirdActionText = stringResource(R.string.delete_track),
            thirdActionIconRes = R.drawable.more_select_icon_delete,
            thirdActionPressedIconRes = R.drawable.more_select_icon_delete,
            onDismiss = {
                pendingTrackActionMediaId = null
            },
            onAddToPlaylistClick = {
                pendingTrackActionMediaId = null
                onRequestAddToPlaylist(listOf(targetItem))
            },
            onAddToQueueClick = {
                pendingTrackActionMediaId = null
                onRequestAddToQueue(listOf(targetItem))
            },
            onThirdActionClick = {
                pendingTrackActionMediaId = null
                selectedTrackIdsInEdit = setOf(targetItem.mediaId)
                showDeleteTracksDialog = true
            },
        )
    }

    if (!hasPermission) {
        PlaylistPermissionState(
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

    if (showCreateDialog) {
        PlaylistNameDialog(
            title = stringResource(R.string.new_playlist),
            initialValue = createDialogInitialValue,
            selectAllOnOpen = true,
            onDismiss = { showCreateDialog = false },
            onConfirm = { input ->
                scope.launch {
                    when (val result = playlistRepository.createPlaylist(input)) {
                        PlaylistCreateResult.EmptyName -> {
                            toast(context, R.string.playlist_create_failed)
                        }
                        PlaylistCreateResult.DuplicateName -> {
                            toast(context, R.string.playlist_duplicate_name)
                        }
                        is PlaylistCreateResult.Success -> {
                            showCreateDialog = false
                            rootEditMode = false
                            selectedPlaylistIdsInEdit = emptySet()
                            openPlaylistAddSongs(result.playlistId)
                        }
                    }
                }
            },
        )
    }

    if (renamePlaylistId != null) {
        val renameTargetId = renamePlaylistId!!
        val renameInitial = playlists.firstOrNull { it.id == renameTargetId }?.name.orEmpty()
        PlaylistNameDialog(
            title = stringResource(R.string.playlist_rename_title),
            initialValue = renameInitial,
            confirmText = stringResource(R.string.done),
            onDismiss = { renamePlaylistId = null },
            onConfirm = { input ->
                scope.launch {
                    when (playlistRepository.renamePlaylist(renameTargetId, input)) {
                        PlaylistRenameResult.Success -> {
                            renamePlaylistId = null
                        }
                        PlaylistRenameResult.DuplicateName -> {
                            toast(context, R.string.playlist_duplicate_name)
                        }
                        PlaylistRenameResult.EmptyName -> {
                            toast(context, R.string.playlist_create_failed)
                        }
                        PlaylistRenameResult.MissingPlaylist -> {
                            renamePlaylistId = null
                        }
                    }
                }
            },
        )
    }

    if (showDeletePlaylistsDialog) {
        ConfirmDialog(
            title = stringResource(R.string.playlist_delete_confirm),
            onDismiss = { showDeletePlaylistsDialog = false },
            onConfirm = {
                val targets = selectedPlaylistIdsInEdit
                showDeletePlaylistsDialog = false
                scope.launch {
                    playlistRepository.deletePlaylists(targets)
                    rootEditMode = false
                    selectedPlaylistIdsInEdit = emptySet()
                }
            },
        )
    }

    if (showDeleteSinglePlaylistDialog && activePlaylist != null) {
        ConfirmDialog(
            title = stringResource(R.string.playlist_delete_single_confirm),
            onDismiss = { showDeleteSinglePlaylistDialog = false },
            onConfirm = {
                val targetId = activePlaylist?.id
                showDeleteSinglePlaylistDialog = false
                if (targetId != null) {
                    scope.launch {
                        playlistRepository.deletePlaylists(setOf(targetId))
                        selectedPlaylistId = null
                        addSongsTargetPlaylistId = null
                        detailEditMode = false
                        selectedTrackIdsInEdit = emptySet()
                        pendingTrackActionMediaId = null
                        selectedSongIdsForAdd = emptySet()
                    }
                }
            },
        )
    }

    if (showDeleteTracksDialog && activePlaylist != null) {
        ConfirmDialog(
            title = stringResource(R.string.playlist_remove_song_confirm),
            onDismiss = { showDeleteTracksDialog = false },
            onConfirm = {
                val playlistId = activePlaylist?.id
                val targets = selectedTrackIdsInEdit
                showDeleteTracksDialog = false
                if (playlistId != null) {
                    scope.launch {
                        playlistRepository.removeMediaIds(playlistId, targets)
                        detailEditMode = false
                        selectedTrackIdsInEdit = emptySet()
                    }
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PlaylistPageBackground),
    ) {
        SecondaryPageTransition(
            secondaryKey = selectedPlaylistId,
            modifier = Modifier.fillMaxWidth(),
            label = "playlist top bar",
            primaryContent = {
                PlaylistPrimaryTopBar(
                    editMode = rootEditMode,
                    deleteEnabled = selectedPlaylistIdsInEdit.isNotEmpty(),
                    onEditClick = {
                        rootEditMode = true
                        selectedPlaylistIdsInEdit = emptySet()
                    },
                    onDoneClick = {
                        rootEditMode = false
                        selectedPlaylistIdsInEdit = emptySet()
                    },
                    onDeleteClick = {
                        if (selectedPlaylistIdsInEdit.isNotEmpty()) {
                            showDeletePlaylistsDialog = true
                        }
                    },
                )
            },
            secondaryContent = {
                PlaylistOverlayPageTransition(
                    secondaryKey = if (addSongsTargetPlaylistId != null) addSongsTargetPlaylistId else null,
                    modifier = Modifier.fillMaxWidth(),
                    label = "playlist inner top bar",
                    primaryContent = {
                        PlaylistDetailTopBar(
                            title = activePlaylist?.name.orEmpty(),
                            editMode = detailEditMode,
                            deleteEnabled = selectedTrackIdsInEdit.isNotEmpty(),
                            onBackClick = {
                                selectedPlaylistId = null
                                detailEditMode = false
                                selectedTrackIdsInEdit = emptySet()
                            },
                            onDoneClick = {
                                detailEditMode = false
                                selectedTrackIdsInEdit = emptySet()
                            },
                            onDeleteTracksClick = {
                                if (selectedTrackIdsInEdit.isNotEmpty()) {
                                    showDeleteTracksDialog = true
                                }
                            },
                        )
                    },
                    secondaryContent = {
                        PlaylistAddSongsTopBar(
                            onDoneClick = {
                                val targetId = addSongsTargetPlaylistId ?: return@PlaylistAddSongsTopBar
                                val targets = selectedSongIdsForAdd.toList()
                                scope.launch {
                                    val result = playlistRepository.addMediaIds(targetId, targets)
                                    handlePlaylistAddResult(context, result)
                                    addSongsTargetPlaylistId = null
                                    selectedSongIdsForAdd = emptySet()
                                }
                            },
                        )
                    },
                )
            },
        )
        SmartisanTopBarShadow()
        SecondaryPageTransition(
            secondaryKey = selectedPlaylistId,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(PlaylistPageBackground),
            label = "playlist content",
            primaryContent = {
                PlaylistPrimaryPage(
                    playlists = playlists,
                    editMode = rootEditMode,
                    selectedIds = selectedPlaylistIdsInEdit,
                    onCreatePlaylist = {
                        scope.launch {
                            createDialogInitialValue = playlistRepository.suggestNextUntitledName()
                            showCreateDialog = true
                        }
                    },
                    onPlaylistClick = { playlist ->
                        if (rootEditMode) {
                            selectedPlaylistIdsInEdit = selectedPlaylistIdsInEdit.toggle(playlist.id)
                        } else {
                            selectedPlaylistId = playlist.id
                            detailEditMode = false
                            selectedTrackIdsInEdit = emptySet()
                            pendingTrackActionMediaId = null
                        }
                    },
                    onPlaylistLongClick = { playlist ->
                        if (!rootEditMode) {
                            renamePlaylistId = playlist.id
                        }
                    },
                )
            },
            secondaryContent = {
                PlaylistOverlayPageTransition(
                    secondaryKey = if (addSongsTargetPlaylistId != null) addSongsTargetPlaylistId else null,
                    modifier = Modifier.fillMaxSize(),
                    label = "playlist detail content",
                    primaryContent = {
                        PlaylistDetailPage(
                            playlist = activePlaylist,
                            tracks = detailTracks,
                            currentMediaId = currentMediaId,
                            editMode = detailEditMode,
                            selectedTrackIds = selectedTrackIdsInEdit,
                            onShufflePlay = {
                                val shuffledTracks = detailTracks.shuffled()
                                if (shuffledTracks.isNotEmpty()) {
                                    playbackBrowser?.setMediaItems(shuffledTracks, 0, 0L)
                                    playbackBrowser?.prepare()
                                    playbackBrowser?.play()
                                }
                            },
                            onDeletePlaylist = {
                                showDeleteSinglePlaylistDialog = true
                            },
                            onEditPlaylist = {
                                detailEditMode = true
                                selectedTrackIdsInEdit = emptySet()
                            },
                            onOpenAddSongs = {
                                activePlaylist?.id?.let(openPlaylistAddSongs)
                            },
                            onTrackClick = { item, index ->
                                if (detailEditMode) {
                                    selectedTrackIdsInEdit = selectedTrackIdsInEdit.toggle(item.mediaId)
                                } else {
                                    val targetTracks = detailTracks
                                    playbackBrowser?.setMediaItems(targetTracks, index, 0L)
                                    playbackBrowser?.prepare()
                                    playbackBrowser?.play()
                                }
                            },
                            onTrackLongClick = { item ->
                                if (!detailEditMode) {
                                    selectedTrackIdsInEdit = setOf(item.mediaId)
                                    detailEditMode = true
                                }
                            },
                            onTrackMoreClick = { item ->
                                pendingTrackActionMediaId = item.mediaId
                            },
                        )
                    },
                    secondaryContent = {
                        PlaylistAddSongsPage(
                            songs = addSongsCandidates,
                            currentMediaId = currentMediaId,
                            selectedSongIds = selectedSongIdsForAdd,
                            modifier = Modifier.fillMaxSize(),
                            onSongToggle = { item ->
                                selectedSongIdsForAdd = selectedSongIdsForAdd.toggle(item.mediaId)
                            },
                            onSongPlayPreview = { index ->
                                playbackBrowser?.setMediaItems(addSongsCandidates, index, 0L)
                                playbackBrowser?.prepare()
                                playbackBrowser?.play()
                            },
                        )
                    },
                )
            },
        )
    }
}

@Composable
private fun PlaylistPrimaryTopBar(
    editMode: Boolean,
    deleteEnabled: Boolean,
    onEditClick: () -> Unit,
    onDoneClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    SmartisanTopBar(
        title = stringResource(R.string.tab_play_list),
        leftContent = {
            SmartisanTopBarTextButton(
                text = if (editMode) stringResource(R.string.done) else stringResource(R.string.edit),
                onClick = if (editMode) onDoneClick else onEditClick,
            )
        },
        rightContent = {
            if (editMode) {
                SmartisanTopBarDangerButton(
                    text = stringResource(R.string.delete),
                    enabled = deleteEnabled,
                    onClick = onDeleteClick,
                )
            } else {
                SmartisanTopBarIconButton(
                    iconRes = R.drawable.search_icon,
                    pressedIconRes = R.drawable.search_icon_down,
                    contentDescription = stringResource(R.string.tab_local_search),
                    iconSize = 34.dp,
                )
            }
        },
    )
}

@Composable
private fun PlaylistDetailTopBar(
    title: String,
    editMode: Boolean,
    deleteEnabled: Boolean,
    onBackClick: () -> Unit,
    onDoneClick: () -> Unit,
    onDeleteTracksClick: () -> Unit,
) {
    SmartisanTopBar(
        title = title,
        leftContent = {
            SmartisanTopBarTextButton(
                text = if (editMode) stringResource(R.string.done) else stringResource(R.string.tab_play_list),
                width = if (editMode) 42.dp else 72.dp,
                onClick = if (editMode) onDoneClick else onBackClick,
            )
        },
        rightContent = if (editMode) {
            {
                SmartisanTopBarDangerButton(
                    text = stringResource(R.string.delete),
                    enabled = deleteEnabled,
                    onClick = onDeleteTracksClick,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun PlaylistAddSongsTopBar(
    onDoneClick: () -> Unit,
) {
    SmartisanTopBar(
        title = stringResource(R.string.playlist_song_title),
        rightContent = {
            SmartisanTopBarTextButton(
                text = stringResource(R.string.done),
                onClick = onDoneClick,
            )
        },
    )
}

@Composable
private fun PlaylistPrimaryPage(
    playlists: List<UserPlaylistSummary>,
    editMode: Boolean,
    selectedIds: Set<String>,
    onCreatePlaylist: () -> Unit,
    onPlaylistClick: (UserPlaylistSummary) -> Unit,
    onPlaylistLongClick: (UserPlaylistSummary) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PlaylistPageBackground),
    ) {
        if (playlists.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (!editMode) {
                    NewPlaylistRow(onClick = onCreatePlaylist)
                }
                SmartisanBlankState(
                    iconRes = R.drawable.blank_playlist,
                    title = stringResource(R.string.no_playlist),
                    subtitle = stringResource(R.string.create_playlist),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = PlaylistMiniPlayerReservedHeight),
        ) {
            if (!editMode) {
                item(key = "new_playlist") {
                    NewPlaylistRow(onClick = onCreatePlaylist)
                }
            }
            items(
                items = playlists,
                key = UserPlaylistSummary::id,
            ) { playlist ->
                PlaylistListRow(
                    playlist = playlist,
                    editMode = editMode,
                    selected = playlist.id in selectedIds,
                    onClick = { onPlaylistClick(playlist) },
                    onLongClick = { onPlaylistLongClick(playlist) },
                )
            }
        }
    }
}

@Composable
private fun NewPlaylistRow(
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .background(Color(0xFFF6F6F6))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(
                    if (pressed) R.drawable.btn_add_down else R.drawable.btn_add,
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(27.dp)
                    .alpha(if (pressed) 1f else 0.72f),
            )
            Text(
                text = stringResource(R.string.new_playlist),
                style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = if (pressed) Color(0xFF515257) else Color(0x80000000),
            )
        }
    }
}

@Composable
private fun PlaylistListRow(
    playlist: UserPlaylistSummary,
    editMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (pressed) PlaylistListPressedBackground else PlaylistListRowBackground,
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    if (!editMode) {
                        onLongClick()
                    }
                },
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlaylistRowHeight)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editMode) {
                PlaylistSelectionCircle(
                    selected = selected,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = playlist.name,
                    style = PlaylistTitleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.playlist_song_count, playlist.songCount),
                    style = PlaylistSubtitleStyle,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (!editMode) {
                Text(
                    text = "›",
                    color = PlaylistArrowColor,
                    fontSize = 20.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PlaylistListDivider),
        )
    }
}

@Composable
private fun PlaylistDetailPage(
    playlist: UserPlaylistDetail?,
    tracks: List<MediaItem>,
    currentMediaId: String?,
    editMode: Boolean,
    selectedTrackIds: Set<String>,
    modifier: Modifier = Modifier,
    onShufflePlay: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onEditPlaylist: () -> Unit,
    onOpenAddSongs: () -> Unit,
    onTrackClick: (MediaItem, Int) -> Unit,
    onTrackLongClick: (MediaItem) -> Unit,
    onTrackMoreClick: (MediaItem) -> Unit,
) {
    if (playlist == null) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(bottom = PlaylistMiniPlayerReservedHeight),
    ) {
        item(key = "${playlist.id}:header") {
            if (editMode) {
                PlaylistTrackEditHeader(
                    selectedCount = selectedTrackIds.size,
                    totalCount = tracks.size,
                    onOpenAddSongs = onOpenAddSongs,
                )
            } else {
                PlaylistDetailActionRow(
                    onShufflePlay = onShufflePlay,
                    onDeletePlaylist = onDeletePlaylist,
                    onEditPlaylist = onEditPlaylist,
                )
            }
        }
        if (tracks.isEmpty()) {
            item(key = "${playlist.id}:blank") {
                SmartisanBlankState(
                    iconRes = R.drawable.blank_song,
                    title = stringResource(R.string.no_song),
                    subtitle = stringResource(R.string.addsong_playlist),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 84.dp),
                )
            }
            return@LazyColumn
        }
        items(
            items = tracks,
            key = MediaItem::mediaId,
        ) { item ->
            PlaylistTrackRow(
                mediaItem = item,
                selected = item.mediaId == currentMediaId && !editMode,
                editMode = editMode,
                checked = item.mediaId in selectedTrackIds,
                onClick = {
                    onTrackClick(item, tracks.indexOf(item))
                },
                onLongClick = {
                    onTrackLongClick(item)
                },
                onMoreClick = {
                    onTrackMoreClick(item)
                },
            )
        }
    }
}

@Composable
private fun PlaylistDetailActionRow(
    onShufflePlay: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onEditPlaylist: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlaylistActionRowHeight)
            .background(PlaylistActionRowBackground)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PlaylistLegacyActionButton(
            text = stringResource(R.string.s_random_play),
            backgroundRes = R.drawable.btn_shuffle,
            backgroundPressedRes = R.drawable.btn_shuffle_down,
            iconRes = R.drawable.btn_shuffle2,
            iconPressedRes = R.drawable.btn_shuffle2_down,
            modifier = Modifier.weight(1f),
            onClick = onShufflePlay,
        )
        PlaylistLegacyActionButton(
            text = stringResource(R.string.s_remove_track_list),
            backgroundRes = R.drawable.btn_deletelist,
            backgroundPressedRes = R.drawable.btn_deletelist_down,
            iconRes = R.drawable.btn_deletelist2,
            iconPressedRes = R.drawable.btn_deletelist2_down,
            modifier = Modifier.weight(1f),
            onClick = onDeletePlaylist,
        )
        PlaylistLegacyActionButton(
            text = stringResource(R.string.s_edit_track_list),
            backgroundRes = R.drawable.btn_editlist,
            backgroundPressedRes = R.drawable.btn_editlist_down,
            iconRes = R.drawable.btn_editlist2,
            iconPressedRes = R.drawable.btn_editlist2_down,
            modifier = Modifier.weight(1f),
            onClick = onEditPlaylist,
        )
    }
}

@Composable
private fun PlaylistLegacyActionButton(
    text: String,
    @DrawableRes backgroundRes: Int,
    @DrawableRes backgroundPressedRes: Int,
    @DrawableRes iconRes: Int,
    @DrawableRes iconPressedRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        LegacyDrawableImage(
            drawableRes = if (pressed) backgroundPressedRes else backgroundRes,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(if (pressed) iconPressedRes else iconRes),
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = text,
                style = PlaylistActionTextStyle,
                modifier = Modifier.padding(bottom = 2.dp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PlaylistCompactLegacyActionButton(
    text: String,
    @DrawableRes iconRes: Int,
    @DrawableRes iconPressedRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .height(PlaylistCompactActionButtonHeight)
            .clip(PlaylistCompactActionButtonShape)
            .background(
                color = if (pressed) PlaylistActionPressedBackground else Color.White,
                shape = PlaylistCompactActionButtonShape,
            )
            .border(
                width = 1.dp,
                color = PlaylistCompactActionBorder,
                shape = PlaylistCompactActionButtonShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(if (pressed) iconPressedRes else iconRes),
                contentDescription = null,
                modifier = Modifier.size(PlaylistCompactActionButtonIconSize),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = PlaylistActionTextStyle,
                modifier = Modifier.padding(bottom = 2.dp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PlaylistTrackEditHeader(
    selectedCount: Int,
    totalCount: Int,
    onOpenAddSongs: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(45.dp)
            .background(PlaylistListRowBackground)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaylistSelectionCircle(selected = selectedCount > 0)
        Text(
            text = stringResource(R.string.playlist_selected_song_count, selectedCount, totalCount),
            style = PlaylistSubtitleStyle.copy(
                fontSize = 15.sp,
                color = PlaylistTitleColor,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        PlaylistCompactLegacyActionButton(
            text = stringResource(R.string.add_track),
            iconRes = R.drawable.btn_add_song2,
            iconPressedRes = R.drawable.btn_add_song2_down,
            modifier = Modifier.padding(start = 8.dp),
            onClick = onOpenAddSongs,
        )
    }
}

@Composable
private fun PlaylistTrackRow(
    mediaItem: MediaItem,
    selected: Boolean,
    editMode: Boolean,
    checked: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val metadata = mediaItem.mediaMetadata
    val title = metadata.displayTitle ?: metadata.title
    val subtitleParts = listOfNotNull(
        metadata.artist?.toString()?.takeIf(String::isNotBlank),
        metadata.albumTitle?.toString()?.takeIf(String::isNotBlank),
    )
    val duration = formatDuration(metadata.durationMs ?: 0L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlaylistRowHeight),
        )
        {
            LegacyDrawableImage(
                drawableRes = if (pressed) R.drawable.list_item_shadow else R.drawable.list_item_bg,
                modifier = Modifier.fillMaxSize(),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = PlaylistRowStartPadding, end = PlaylistRowContentEndPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (editMode) {
                    PlaylistSelectionCircle(
                        selected = checked,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = title?.toString().orEmpty(),
                        style = PlaylistTrackTitleStyle,
                        color = if (selected) PlaylistTrackSelectedColor else PlaylistTitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitleParts.joinToString(" · "),
                        style = PlaylistTrackSubtitleStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = duration,
                    style = PlaylistTrackSubtitleStyle.copy(color = PlaylistTrackDurationColor),
                    modifier = Modifier.padding(
                        start = PlaylistDurationStartPadding,
                        end = PlaylistDurationOuterPadding,
                    ),
                )
                if (!editMode) {
                    PlaylistTrackMoreButton(
                        onClick = onMoreClick,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PlaylistListDivider)
                    .align(Alignment.BottomStart),
            )
        }
    }
}

@Composable
private fun PlaylistTrackMoreButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .width(PlaylistTrackMoreWidth)
            .fillMaxHeight()
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

@Composable
private fun PlaylistAddSongsPage(
    songs: List<MediaItem>,
    currentMediaId: String?,
    selectedSongIds: Set<String>,
    modifier: Modifier = Modifier,
    onSongToggle: (MediaItem) -> Unit,
    onSongPlayPreview: (Int) -> Unit,
) {
    if (songs.isEmpty()) {
        SmartisanBlankState(
            iconRes = R.drawable.blank_song,
            title = stringResource(R.string.no_song),
            subtitle = stringResource(R.string.show_song),
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 84.dp),
        )
        return
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentPadding = PaddingValues(bottom = PlaylistMiniPlayerReservedHeight),
    ) {
        items(
            items = songs,
            key = MediaItem::mediaId,
        ) { item ->
            PlaylistAddSongRow(
                mediaItem = item,
                checked = item.mediaId in selectedSongIds,
                playing = item.mediaId == currentMediaId,
                onClick = {
                    onSongToggle(item)
                },
                onLongClick = {
                    onSongPlayPreview(songs.indexOf(item))
                },
            )
        }
    }
}

@Composable
private fun <T : Any> PlaylistOverlayPageTransition(
    secondaryKey: T?,
    modifier: Modifier = Modifier,
    label: String = "playlist overlay transition",
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = secondaryKey,
        modifier = modifier,
        contentAlignment = Alignment.TopStart,
        transitionSpec = {
            val transform = when {
                initialState == null && targetState != null -> {
                    val enter = slideIntoContainer(
                        towards = SlideDirection.Up,
                        animationSpec = tween(
                            durationMillis = PlaylistOverlayDurationMillis,
                            easing = PlaylistOverlayEasing,
                        ),
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = PlaylistOverlayFadeMillis),
                    )
                    enter togetherWith ExitTransition.KeepUntilTransitionsFinished
                }
                initialState != null && targetState == null -> {
                    val exit = slideOutOfContainer(
                        towards = SlideDirection.Down,
                        animationSpec = tween(
                            durationMillis = PlaylistOverlayDurationMillis,
                            easing = PlaylistOverlayEasing,
                        ),
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = PlaylistOverlayFadeMillis),
                    )
                    EnterTransition.None togetherWith exit
                }
                else -> {
                    fadeIn(animationSpec = tween(durationMillis = PlaylistOverlayFadeMillis)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = PlaylistOverlayFadeMillis))
                }
            }

            transform
                .apply {
                    targetContentZIndex = if (targetState == null) 0f else 1f
                }
                .using(SizeTransform(clip = false))
        },
        label = label,
    ) { targetKey ->
        if (targetKey == null) {
            primaryContent()
        } else {
            secondaryContent(targetKey)
        }
    }
}

@Composable
private fun PlaylistAddSongRow(
    mediaItem: MediaItem,
    checked: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val metadata = mediaItem.mediaMetadata
    val title = metadata.displayTitle ?: metadata.title
    val subtitleParts = listOfNotNull(
        metadata.artist?.toString()?.takeIf(String::isNotBlank),
        metadata.albumTitle?.toString()?.takeIf(String::isNotBlank),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (pressed) PlaylistListPressedBackground else Color.White)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlaylistRowHeight)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaylistSelectionCircle(
                selected = checked,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title?.toString().orEmpty(),
                    style = PlaylistTrackTitleStyle,
                    color = if (playing) PlaylistTrackSelectedColor else PlaylistTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitleParts.joinToString(" · "),
                    style = PlaylistTrackSubtitleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(PlaylistListDivider),
        )
    }
}

@Composable
private fun PlaylistSelectionCircle(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(PlaylistSelectionSize)
            .border(
                width = 1.dp,
                color = if (selected) PlaylistSelectionCircleFill else PlaylistSelectionCircleBorder,
                shape = CircleShape,
            )
            .background(
                color = if (selected) PlaylistSelectionCircleFill else Color.Transparent,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Text(
                text = "✓",
                color = PlaylistSelectionCheckColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun PlaylistPermissionState(
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
private fun ConfirmDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    SmartisanConfirmDialog(
        title = title,
        confirmText = stringResource(R.string.done),
        dismissText = stringResource(R.string.cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

@Composable
private fun LegacyDrawableImage(
    @DrawableRes drawableRes: Int,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_XY
            }
        },
        modifier = modifier,
        update = { imageView ->
            imageView.setImageResource(drawableRes)
        },
    )
}

private fun MediaItem.playlistTitleKey(): String {
    return mediaMetadata.displayTitle?.toString()
        ?: mediaMetadata.title?.toString()
        ?: ""
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

private fun handlePlaylistAddResult(
    context: android.content.Context,
    result: PlaylistAddResult,
) {
    when {
        result.addedCount > 0 -> toast(context, R.string.playlist_added)
        result.duplicateCount > 0 -> toast(context, R.string.playlist_song_exists)
    }
}

private fun toast(
    context: android.content.Context,
    stringRes: Int,
) {
    Toast.makeText(context, context.getString(stringRes), Toast.LENGTH_SHORT).show()
}
