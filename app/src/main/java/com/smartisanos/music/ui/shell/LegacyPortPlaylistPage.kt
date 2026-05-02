package com.smartisanos.music.ui.shell

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.icu.text.Transliterator
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.data.playlist.PlaylistCreateResult
import com.smartisanos.music.data.playlist.PlaylistRenameResult
import com.smartisanos.music.data.playlist.PlaylistRepository
import com.smartisanos.music.data.playlist.UserPlaylistDetail
import com.smartisanos.music.data.playlist.UserPlaylistSummary
import com.smartisanos.music.playback.LocalAudioLibrary
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.replaceQueueAndPlay
import com.smartisanos.music.ui.widgets.CustomCheckBox
import com.smartisanos.music.ui.widgets.EditableLayout
import com.smartisanos.music.ui.widgets.EditableListViewItem
import com.smartisanos.music.ui.widgets.StretchTextView
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import smartisanos.app.MenuDialog
import smartisanos.widget.ActionButtonGroup
import smartisanos.widget.TitleBar
import smartisanos.widget.letters.QuickBarEx
import java.text.Normalizer
import java.util.Locale

private const val PlaylistAddModeSlideMillis = 300
private const val PlaylistRootFooterThreshold = 8
private val PlaylistAddModeEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}
private val PlaylistPrimaryTextColor = Color.rgb(0x35, 0x35, 0x39)
private val PlaylistSecondaryTextColor = Color.rgb(0xa4, 0xa7, 0xac)
private val PlaylistFooterTextColor = Color.rgb(0xbc, 0xbc, 0xbc)

private data class LegacyPlaylistTarget(
    val playlistId: String,
    val title: String,
)

private sealed interface LegacyPlaylistNameDialogRequest {
    val initialName: String

    data class Create(
        override val initialName: String,
    ) : LegacyPlaylistNameDialogRequest

    data class Rename(
        val playlistId: String,
        override val initialName: String,
    ) : LegacyPlaylistNameDialogRequest
}

private enum class LegacyPlaylistDeleteRequest {
    RootSelected,
    DetailPlaylist,
    DetailTracks,
}

@Composable
internal fun LegacyPortPlaylistPage(
    mediaItems: List<MediaItem>,
    active: Boolean,
    hiddenMediaIds: Set<String>,
    onTrackMoreClick: (MediaItem) -> Unit,
    onAddModeActiveChanged: (Boolean) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val browser = LocalPlaybackBrowser.current
    val scope = rememberCoroutineScope()
    val playlistRepository = remember(context.applicationContext) {
        PlaylistRepository.getInstance(context.applicationContext)
    }
    val playlists by playlistRepository.playlists.collectAsState(initial = emptyList())
    val visibleSongs = remember(mediaItems, hiddenMediaIds) {
        mediaItems.filterNot { item -> item.mediaId in hiddenMediaIds }
    }
    val songsById = remember(visibleSongs) {
        visibleSongs.associateBy(MediaItem::mediaId)
    }

    var target by remember { mutableStateOf<LegacyPlaylistTarget?>(null) }
    var rootEditMode by remember { mutableStateOf(false) }
    var selectedPlaylistIds by remember { mutableStateOf(emptySet<String>()) }
    var detailEditMode by remember { mutableStateOf(false) }
    var selectedTrackIds by remember { mutableStateOf(emptySet<String>()) }
    var addMode by remember { mutableStateOf(false) }
    var selectedAddSongIds by remember { mutableStateOf(emptySet<String>()) }
    var nameDialogRequest by remember { mutableStateOf<LegacyPlaylistNameDialogRequest?>(null) }
    var deleteRequest by remember { mutableStateOf<LegacyPlaylistDeleteRequest?>(null) }

    val activePlaylistId = target?.playlistId
    val activePlaylistFlow = remember(activePlaylistId, playlistRepository) {
        activePlaylistId?.let(playlistRepository::observePlaylistDetail) ?: flowOf(null)
    }
    val activePlaylist by activePlaylistFlow.collectAsState(initial = null)
    val activeSummary = remember(playlists, activePlaylistId) {
        activePlaylistId?.let { id -> playlists.firstOrNull { playlist -> playlist.id == id } }
    }
    val detailTitle = activePlaylist?.name ?: activeSummary?.name ?: target?.title.orEmpty()
    val detailTracks = remember(activePlaylist, songsById) {
        activePlaylist?.mediaIds?.mapNotNull(songsById::get).orEmpty()
    }
    val addableSongs = remember(activePlaylist, visibleSongs) {
        val existingIds = activePlaylist?.mediaIds?.toSet().orEmpty()
        visibleSongs.filterNot { item -> item.mediaId in existingIds }
    }

    LaunchedEffect(activePlaylistId, activePlaylist, playlists) {
        if (activePlaylistId != null && activePlaylist == null && playlists.none { it.id == activePlaylistId }) {
            target = null
            detailEditMode = false
            addMode = false
            selectedTrackIds = emptySet()
            selectedAddSongIds = emptySet()
        }
    }
    LaunchedEffect(addMode, target) {
        onAddModeActiveChanged(addMode && target != null)
    }
    DisposableEffect(Unit) {
        onDispose {
            onAddModeActiveChanged(false)
        }
    }

    BackHandler(enabled = addMode) {
        addMode = false
        selectedAddSongIds = emptySet()
    }
    BackHandler(enabled = !addMode && detailEditMode) {
        detailEditMode = false
        selectedTrackIds = emptySet()
    }
    BackHandler(enabled = !addMode && !detailEditMode && target != null) {
        target = null
    }
    BackHandler(enabled = target == null && rootEditMode) {
        rootEditMode = false
        selectedPlaylistIds = emptySet()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColor.White),
    ) {
        LegacyPlaylistTitleArea(
            target = target,
            detailTitle = detailTitle,
            rootEditMode = rootEditMode,
            rootSelectedCount = selectedPlaylistIds.size,
            detailEditMode = detailEditMode,
            addMode = addMode,
            onRootEnterEdit = {
                rootEditMode = true
                selectedPlaylistIds = emptySet()
            },
            onRootExitEdit = {
                rootEditMode = false
                selectedPlaylistIds = emptySet()
            },
            onRootDeleteSelected = {
                if (selectedPlaylistIds.isNotEmpty()) {
                    deleteRequest = LegacyPlaylistDeleteRequest.RootSelected
                }
            },
            onDetailBack = {
                target = null
                detailEditMode = false
                addMode = false
                selectedTrackIds = emptySet()
                selectedAddSongIds = emptySet()
            },
            onDetailEnterEdit = {
                detailEditMode = true
                selectedTrackIds = emptySet()
            },
            onDetailExitEdit = {
                detailEditMode = false
                selectedTrackIds = emptySet()
            },
            onAddModeConfirm = {
                val playlistId = target?.playlistId ?: return@LegacyPlaylistTitleArea
                val mediaIds = selectedAddSongIds.toList()
                if (mediaIds.isEmpty()) {
                    addMode = false
                    selectedAddSongIds = emptySet()
                    return@LegacyPlaylistTitleArea
                }
                scope.launch {
                    playlistRepository.addMediaIds(playlistId, mediaIds)
                    selectedAddSongIds = emptySet()
                    addMode = false
                }
            },
            onSearchClick = onSearchClick,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            LegacyPortPageStackTransition(
                secondaryKey = target,
                modifier = Modifier.fillMaxSize(),
                label = "legacy playlist transition",
                primaryContent = {
                    LegacyPlaylistRootPage(
                        active = active && target == null,
                        playlists = playlists,
                        editMode = rootEditMode,
                        selectedPlaylistIds = selectedPlaylistIds,
                        onCreatePlaylist = {
                            scope.launch {
                                nameDialogRequest = LegacyPlaylistNameDialogRequest.Create(
                                    initialName = playlistRepository.suggestNextUntitledName(),
                                )
                            }
                        },
                        onRenamePlaylist = { playlist ->
                            nameDialogRequest = LegacyPlaylistNameDialogRequest.Rename(
                                playlistId = playlist.id,
                                initialName = playlist.name,
                            )
                        },
                        onPlaylistClick = { playlist ->
                            if (rootEditMode) {
                                selectedPlaylistIds = selectedPlaylistIds.toggle(playlist.id)
                            } else {
                                target = LegacyPlaylistTarget(
                                    playlistId = playlist.id,
                                    title = playlist.name,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                },
                secondaryContent = { playlistTarget ->
                    LegacyPlaylistDetailPage(
                        active = active && playlistTarget == target && !addMode,
                        playlist = activePlaylist,
                        title = detailTitle,
                        tracks = detailTracks,
                        editMode = detailEditMode,
                        selectedTrackIds = selectedTrackIds,
                        browser = browser,
                        onShuffle = {
                            if (detailTracks.isEmpty()) {
                                return@LegacyPlaylistDetailPage
                            }
                            val shuffled = detailTracks.shuffled()
                            browser.replaceQueueAndPlay(
                                mediaItems = shuffled,
                                shuffleModeEnabled = true,
                            )
                        },
                        onDeletePlaylist = {
                            deleteRequest = LegacyPlaylistDeleteRequest.DetailPlaylist
                        },
                        onEditModeChange = { enabled ->
                            detailEditMode = enabled
                            selectedTrackIds = emptySet()
                        },
                        onAddOrRemoveClick = {
                            if (selectedTrackIds.isEmpty()) {
                                addMode = true
                                selectedAddSongIds = emptySet()
                            } else {
                                deleteRequest = LegacyPlaylistDeleteRequest.DetailTracks
                            }
                        },
                        onToggleAll = { checked ->
                            selectedTrackIds = if (checked) {
                                detailTracks.map(MediaItem::mediaId).toSet()
                            } else {
                                emptySet()
                            }
                        },
                        onReorderTracks = { orderedMediaIds ->
                            val playlistId = target?.playlistId ?: return@LegacyPlaylistDetailPage
                            scope.launch {
                                playlistRepository.reorderVisibleMediaIds(playlistId, orderedMediaIds)
                            }
                        },
                        onTrackClick = { item, index ->
                            if (detailEditMode) {
                                selectedTrackIds = selectedTrackIds.toggle(item.mediaId)
                                return@LegacyPlaylistDetailPage
                            }
                            browser.replaceQueueAndPlay(detailTracks, index)
                        },
                        onTrackMoreClick = onTrackMoreClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                },
            )
            androidx.compose.animation.AnimatedVisibility(
                visible = addMode && target != null,
                modifier = Modifier.fillMaxSize(),
                enter = slideInVertically(
                    animationSpec = tween(
                        durationMillis = PlaylistAddModeSlideMillis,
                        easing = PlaylistAddModeEasing,
                    ),
                    initialOffsetY = { it },
                ),
                exit = slideOutVertically(
                    animationSpec = tween(
                        durationMillis = PlaylistAddModeSlideMillis,
                        easing = PlaylistAddModeEasing,
                    ),
                    targetOffsetY = { it },
                ),
            ) {
                LegacyPlaylistAddSongsPage(
                    active = active && addMode,
                    songs = addableSongs,
                    selectedSongIds = selectedAddSongIds,
                    browser = browser,
                    onToggleSong = { mediaId ->
                        selectedAddSongIds = selectedAddSongIds.toggle(mediaId)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    LegacyPlaylistNameDialogOverlay(
        request = nameDialogRequest,
        onDismiss = {
            nameDialogRequest = null
        },
        onConfirm = { request, name ->
            scope.launch {
                when (request) {
                    is LegacyPlaylistNameDialogRequest.Create -> {
                        when (val result = playlistRepository.createPlaylist(name)) {
                            is PlaylistCreateResult.Success -> {
                                nameDialogRequest = null
                                target = LegacyPlaylistTarget(
                                    playlistId = result.playlistId,
                                    title = name.trim(),
                                )
                                detailEditMode = false
                                selectedTrackIds = emptySet()
                                if (visibleSongs.isNotEmpty()) {
                                    addMode = true
                                    selectedAddSongIds = emptySet()
                                }
                            }
                            PlaylistCreateResult.DuplicateName -> {
                                Toast.makeText(context, R.string.playlist_duplicate_name, Toast.LENGTH_SHORT).show()
                            }
                            PlaylistCreateResult.EmptyName -> Unit
                        }
                    }
                    is LegacyPlaylistNameDialogRequest.Rename -> {
                        when (playlistRepository.renamePlaylist(request.playlistId, name)) {
                            PlaylistRenameResult.Success -> {
                                nameDialogRequest = null
                                if (target?.playlistId == request.playlistId) {
                                    target = target?.copy(title = name.trim())
                                }
                            }
                            PlaylistRenameResult.DuplicateName -> {
                                Toast.makeText(context, R.string.playlist_duplicate_name, Toast.LENGTH_SHORT).show()
                            }
                            PlaylistRenameResult.EmptyName,
                            PlaylistRenameResult.MissingPlaylist,
                            -> Unit
                        }
                    }
                }
            }
        },
    )

    LegacyPlaylistDeleteDialog(
        request = deleteRequest,
        onDismiss = {
            deleteRequest = null
        },
        onConfirm = { request ->
            scope.launch {
                when (request) {
                    LegacyPlaylistDeleteRequest.RootSelected -> {
                        playlistRepository.deletePlaylists(selectedPlaylistIds)
                        selectedPlaylistIds = emptySet()
                        rootEditMode = false
                    }
                    LegacyPlaylistDeleteRequest.DetailPlaylist -> {
                        val playlistId = target?.playlistId
                        if (playlistId != null) {
                            playlistRepository.deletePlaylists(setOf(playlistId))
                        }
                        target = null
                        detailEditMode = false
                        addMode = false
                        selectedTrackIds = emptySet()
                    }
                    LegacyPlaylistDeleteRequest.DetailTracks -> {
                        val playlistId = target?.playlistId
                        if (playlistId != null) {
                            playlistRepository.removeMediaIds(playlistId, selectedTrackIds)
                        }
                        selectedTrackIds = emptySet()
                        detailEditMode = false
                    }
                }
                deleteRequest = null
            }
        },
    )
}

@Composable
private fun LegacyPlaylistTitleArea(
    target: LegacyPlaylistTarget?,
    detailTitle: String,
    rootEditMode: Boolean,
    rootSelectedCount: Int,
    detailEditMode: Boolean,
    addMode: Boolean,
    onRootEnterEdit: () -> Unit,
    onRootExitEdit: () -> Unit,
    onRootDeleteSelected: () -> Unit,
    onDetailBack: () -> Unit,
    onDetailEnterEdit: () -> Unit,
    onDetailExitEdit: () -> Unit,
    onAddModeConfirm: () -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (addMode && target != null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(ComposeColor.White),
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsTopHeight(WindowInsets.statusBars),
            )
            val promptHeight = dimensionResource(R.dimen.status_bar_height)
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(promptHeight),
                factory = { context ->
                    TextView(context).apply {
                        gravity = Gravity.CENTER
                        setSingleLine(true)
                        ellipsize = TextUtils.TruncateAt.END
                        setTextColor(context.getColor(R.color.title_color))
                        setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_size_act_title))
                    }
                },
                update = { promptText ->
                    val shortTitle = detailTitle.ellipsizeMiddle(8)
                    promptText.text = promptText.context.getString(R.string.add_track_to) + " \"$shortTitle\""
                },
            )
            LegacyPortSmartisanTitleBar(includeStatusBar = false) { titleBar ->
                titleBar.setupLegacyPlaylistAddModeTitleBar(onAddModeConfirm)
            }
        }
    } else {
        LegacyPortSmartisanTitleBar(modifier = modifier) { titleBar ->
            titleBar.setupLegacyPlaylistTitleBar(
                target = target,
                detailTitle = detailTitle,
                rootEditMode = rootEditMode,
                rootSelectedCount = rootSelectedCount,
                detailEditMode = detailEditMode,
                onRootEnterEdit = onRootEnterEdit,
                onRootExitEdit = onRootExitEdit,
                onRootDeleteSelected = onRootDeleteSelected,
                onDetailBack = onDetailBack,
                onDetailEnterEdit = onDetailEnterEdit,
                onDetailExitEdit = onDetailExitEdit,
                onSearchClick = onSearchClick,
            )
        }
    }
}

private fun TitleBar.setupLegacyPlaylistTitleBar(
    target: LegacyPlaylistTarget?,
    detailTitle: String,
    rootEditMode: Boolean,
    rootSelectedCount: Int,
    detailEditMode: Boolean,
    onRootEnterEdit: () -> Unit,
    onRootExitEdit: () -> Unit,
    onRootDeleteSelected: () -> Unit,
    onDetailBack: () -> Unit,
    onDetailEnterEdit: () -> Unit,
    onDetailExitEdit: () -> Unit,
    onSearchClick: () -> Unit,
) {
    setShadowVisible(false)
    setCenterText(if (target == null) context.getString(R.string.tab_play_list) else detailTitle)

    when {
        target == null && rootEditMode -> {
            setPlaylistLeftImageView(R.drawable.standard_icon_cancel_selector) {
                onRootExitEdit()
            }
            setPlaylistRightImageView(R.drawable.titlebar_btn_delete_selector).apply {
                isEnabled = rootSelectedCount > 0
                setOnClickListener {
                    if (rootSelectedCount > 0) {
                        onRootDeleteSelected()
                    }
                }
            }
        }
        target == null -> {
            setPlaylistLeftImageView(R.drawable.standard_icon_multi_select_selector) {
                onRootEnterEdit()
            }
            setPlaylistRightImageView(R.drawable.search_btn_selector).apply {
                isEnabled = true
                setOnClickListener {
                    onSearchClick()
                }
            }
        }
        detailEditMode -> {
            setPlaylistLeftImageView(R.drawable.standard_icon_cancel_selector) {
                onDetailExitEdit()
            }
            setPlaylistRightImageView(R.drawable.standard_icon_hignlight_confirm_selector).apply {
                isEnabled = true
                setOnClickListener {
                    onDetailExitEdit()
                }
            }
        }
        else -> {
            setPlaylistLeftImageView(R.drawable.standard_icon_back_selector) {
                onDetailBack()
            }
            setPlaylistRightImageView(R.drawable.standard_icon_multi_select_selector).apply {
                isEnabled = true
                setOnClickListener {
                    onDetailEnterEdit()
                }
            }
        }
    }
}

private fun TitleBar.setPlaylistLeftImageView(
    resId: Int,
    onClick: () -> Unit,
): ImageView {
    return ((getLeftViewByIndex(0) as? ImageView) ?: addLeftImageView(resId)).apply {
        visibility = View.VISIBLE
        isEnabled = true
        setImageResource(resId)
        setOnClickListener {
            onClick()
        }
    }
}

private fun TitleBar.setPlaylistRightImageView(resId: Int): ImageView {
    return ((getRightViewByIndex(0) as? ImageView) ?: addRightImageView(resId)).apply {
        visibility = View.VISIBLE
        setImageResource(resId)
    }
}

private fun TitleBar.setupLegacyPlaylistAddModeTitleBar(onConfirm: () -> Unit) {
    removeAllLeftViews()
    removeAllRightViews()
    setShadowVisible(false)
    setCenterText(context.getString(R.string.name_tracks))
    addRightImageView(R.drawable.standard_icon_hignlight_confirm_selector).apply {
        setOnClickListener {
            onConfirm()
        }
    }
}

@Composable
private fun LegacyPlaylistRootPage(
    active: Boolean,
    playlists: List<UserPlaylistSummary>,
    editMode: Boolean,
    selectedPlaylistIds: Set<String>,
    onCreatePlaylist: () -> Unit,
    onRenamePlaylist: (UserPlaylistSummary) -> Unit,
    onPlaylistClick: (UserPlaylistSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LegacyPlaylistRootView(context)
        },
        update = { root ->
            root.visibility = if (active) View.VISIBLE else View.INVISIBLE
            root.bind(
                playlists = playlists,
                editMode = editMode,
                selectedPlaylistIds = selectedPlaylistIds,
                onCreatePlaylist = onCreatePlaylist,
                onRenamePlaylist = onRenamePlaylist,
                onPlaylistClick = onPlaylistClick,
            )
        },
    )
}

@Composable
private fun LegacyPlaylistDetailPage(
    active: Boolean,
    playlist: UserPlaylistDetail?,
    title: String,
    tracks: List<MediaItem>,
    editMode: Boolean,
    selectedTrackIds: Set<String>,
    browser: Player?,
    onShuffle: () -> Unit,
    onDeletePlaylist: () -> Unit,
    onEditModeChange: (Boolean) -> Unit,
    onAddOrRemoveClick: () -> Unit,
    onToggleAll: (Boolean) -> Unit,
    onReorderTracks: (List<String>) -> Unit,
    onTrackClick: (MediaItem, Int) -> Unit,
    onTrackMoreClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LegacyPlaylistDetailRootView(context)
        },
        update = { root ->
            root.visibility = if (active) View.VISIBLE else View.INVISIBLE
            root.bind(
                title = title,
                tracks = tracks,
                editMode = editMode,
                selectedTrackIds = selectedTrackIds,
                currentMediaId = browser?.currentMediaItem?.mediaId,
                currentIsPlaying = browser?.isPlaying == true,
                onShuffle = onShuffle,
                onDeletePlaylist = onDeletePlaylist,
                onEditModeChange = onEditModeChange,
                onAddOrRemoveClick = onAddOrRemoveClick,
                onToggleAll = onToggleAll,
                onReorderTracks = onReorderTracks,
                onTrackClick = onTrackClick,
                onTrackMoreClick = onTrackMoreClick,
            )
            root.bindPlayback(browser)
            if (playlist == null && tracks.isEmpty()) {
                root.setEmptyVisible(true)
            }
        },
    )
}

@Composable
private fun LegacyPlaylistAddSongsPage(
    active: Boolean,
    songs: List<MediaItem>,
    selectedSongIds: Set<String>,
    browser: Player?,
    onToggleSong: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSortIndex by remember { mutableStateOf(0) }
    val sortedSongs = remember(songs, selectedSortIndex) {
        songs.sortedForPlaylistAddMode(selectedSortIndex)
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LegacyPlaylistAddSongsRootView(context)
        },
        update = { root ->
            root.visibility = if (active) View.VISIBLE else View.INVISIBLE
            root.bind(
                songs = sortedSongs,
                selectedSongIds = selectedSongIds,
                currentMediaId = browser?.currentMediaItem?.mediaId,
                currentIsPlaying = browser?.isPlaying == true,
                selectedSortIndex = selectedSortIndex,
                onSortSelected = { index ->
                    selectedSortIndex = index
                },
                onToggleSong = onToggleSong,
            )
            root.bindPlayback(browser)
        },
    )
}

private class LegacyPlaylistRootView(context: Context) : FrameLayout(context) {
    private val addRow = LinearLayout(context)
    private val listView = ListView(context)
    private var boundEditMode: Boolean? = null
    private var pendingEditAnimationMode: Boolean? = null
    private val blankView = LegacyPlaylistBlankView(
        context = context,
        iconRes = R.drawable.blank_playlist,
        primaryText = context.getString(R.string.no_playlist),
        secondaryText = context.getString(R.string.create_playlist),
    )
    private val footer = TextView(context).apply {
        gravity = Gravity.CENTER
        setTextColor(PlaylistFooterTextColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        setBackgroundColor(Color.WHITE)
        height = dp(30)
    }

    init {
        setBackgroundResource(R.drawable.account_background)
        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        addView(column, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        addRow.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.list_header_selector)
            isClickable = true
            isFocusable = true
            addView(
                ImageView(context).apply {
                    setImageResource(R.drawable.add_icon_selector)
                    scaleType = ImageView.ScaleType.CENTER
                },
                LinearLayout.LayoutParams(dp(60), LinearLayout.LayoutParams.MATCH_PARENT),
            )
            addView(
                TextView(context).apply {
                    text = context.getString(R.string.new_playlist)
                    setTextColor(context.getColor(R.color.list_item_first_line))
                    setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_size_yun))
                    gravity = Gravity.CENTER_VERTICAL
                    includeFontPadding = false
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        column.addView(addRow, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(60)))

        val listFrame = FrameLayout(context)
        column.addView(listFrame, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))
        listView.apply {
            divider = ColorDrawable(context.getColor(R.color.listview_divider_color))
            dividerHeight = resources.getDimensionPixelSize(R.dimen.listview_dividerHeight)
            selector = context.getDrawable(R.drawable.listview_selector)
            cacheColorHint = Color.TRANSPARENT
            setBackgroundColor(Color.TRANSPARENT)
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.list_anim_layout)
            isVerticalScrollBarEnabled = false
            addFooterView(footer, null, false)
        }
        listFrame.addView(listView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        listFrame.addView(blankView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun bind(
        playlists: List<UserPlaylistSummary>,
        editMode: Boolean,
        selectedPlaylistIds: Set<String>,
        onCreatePlaylist: () -> Unit,
        onRenamePlaylist: (UserPlaylistSummary) -> Unit,
        onPlaylistClick: (UserPlaylistSummary) -> Unit,
    ) {
        addRow.alpha = if (editMode) 0.35f else 1f
        addRow.isEnabled = !editMode
        addRow.setOnClickListener {
            if (!editMode) {
                onCreatePlaylist()
            }
        }
        blankView.visibility = if (playlists.isEmpty()) View.VISIBLE else View.GONE
        listView.visibility = if (playlists.isEmpty()) View.INVISIBLE else View.VISIBLE
        footer.visibility = if (playlists.size >= PlaylistRootFooterThreshold) View.VISIBLE else View.GONE
        footer.text = resources.getString(R.string.playlists_count, playlists.size)

        val adapter = listView.adapter as? LegacyPlaylistRootAdapter
            ?: LegacyPlaylistRootAdapter().also { adapter ->
                listView.adapter = adapter
            }
        adapter.onRenamePlaylist = onRenamePlaylist
        val previousEditMode = boundEditMode
        val animateEditMode = previousEditMode != null && previousEditMode != editMode
        if (animateEditMode) {
            adapter.forceStaticEditMode(previousEditMode)
        }
        boundEditMode = editMode
        val contentChanged = adapter.updateItems(
            nextItems = playlists,
            nextEditMode = editMode,
            nextSelectedIds = selectedPlaylistIds,
        )
        if (animateEditMode) {
            pendingEditAnimationMode = editMode
            animateVisibleRowsWhenReady(
                adapter = adapter,
                editMode = editMode,
            )
        } else if (contentChanged) {
            pendingEditAnimationMode = null
            listView.scheduleLayoutAnimation()
        } else if (pendingEditAnimationMode == editMode) {
            // 原版 ModeChanger 会直接驱动当前可见行；等待 post 动画执行前不要静态覆盖起点。
        } else {
            adapter.clearForcedStaticEditMode()
            adapter.updateVisibleRows(
                listView = listView,
                animateEditMode = false,
            )
        }
        listView.setOnItemClickListener { _, _, position, _ ->
            if (position >= adapter.count) {
                return@setOnItemClickListener
            }
            onPlaylistClick(adapter.itemAt(position) ?: return@setOnItemClickListener)
        }
    }

    private fun animateVisibleRowsWhenReady(
        adapter: LegacyPlaylistRootAdapter,
        editMode: Boolean,
        attempt: Int = 0,
    ) {
        listView.post {
            if (boundEditMode != editMode || pendingEditAnimationMode != editMode) {
                return@post
            }
            if (listView.childCount == 0 && attempt < 4) {
                animateVisibleRowsWhenReady(
                    adapter = adapter,
                    editMode = editMode,
                    attempt = attempt + 1,
                )
                return@post
            }
            pendingEditAnimationMode = null
            adapter.clearForcedStaticEditMode()
            adapter.updateVisibleRows(
                listView = listView,
                animateEditMode = true,
            )
        }
    }
}

private class LegacyPlaylistDetailRootView(context: Context) : LinearLayout(context) {
    private val header = LegacyPlaylistDetailHeader(context)
    private val listFrame = FrameLayout(context)
    val listView = ListView(context)
    private val trackAdapter = LegacyPlaylistTrackAdapter()
    private var onReorderTracksCallback: (List<String>) -> Unit = {}
    private val dragController = LegacyListDragController(
        context = context,
        hostView = listFrame,
        listView = listView,
        adapter = trackAdapter,
        onMoveCommitted = { _, _, _, _ ->
            onReorderTracksCallback(trackAdapter.orderedSongMediaIds())
        },
    )
    private val blankView = LegacyPlaylistBlankView(
        context = context,
        iconRes = R.drawable.blank_song,
        primaryText = context.getString(R.string.no_song),
        secondaryText = context.getString(R.string.addsong_playlist),
    )

    init {
        orientation = VERTICAL
        setBackgroundResource(R.drawable.account_background)
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        addView(listFrame, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        listView.apply {
            divider = ColorDrawable(context.getColor(R.color.listview_divider_color))
            dividerHeight = resources.getDimensionPixelSize(R.dimen.listview_dividerHeight)
            selector = context.getDrawable(R.drawable.listview_selector)
            cacheColorHint = Color.TRANSPARENT
            setBackgroundResource(R.drawable.account_background)
            isVerticalScrollBarEnabled = false
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.list_anim_layout)
            adapter = trackAdapter
            setOnTouchListener { _, event ->
                dragController.handleListTouch(event)
            }
        }
        listFrame.addView(listView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        listFrame.addView(
            View(context).apply {
                setBackgroundResource(R.drawable.title_bar_shadow_standard)
            },
            FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(1), Gravity.TOP),
        )
        listFrame.addView(blankView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun bind(
        title: String,
        tracks: List<MediaItem>,
        editMode: Boolean,
        selectedTrackIds: Set<String>,
        currentMediaId: String?,
        currentIsPlaying: Boolean,
        onShuffle: () -> Unit,
        onDeletePlaylist: () -> Unit,
        onEditModeChange: (Boolean) -> Unit,
        onAddOrRemoveClick: () -> Unit,
        onToggleAll: (Boolean) -> Unit,
        onReorderTracks: (List<String>) -> Unit,
        onTrackClick: (MediaItem, Int) -> Unit,
        onTrackMoreClick: (MediaItem) -> Unit,
    ) {
        onReorderTracksCallback = onReorderTracks
        trackAdapter.onMoreClick = { item ->
            if (!editMode) {
                onTrackMoreClick(item)
            }
        }
        header.bind(
            trackCount = tracks.size,
            selectedCount = selectedTrackIds.size,
            editMode = editMode,
            onShuffle = onShuffle,
            onDeletePlaylist = onDeletePlaylist,
            onEdit = { onEditModeChange(true) },
            onAddOrRemoveClick = onAddOrRemoveClick,
            onToggleAll = onToggleAll,
        )
        setEmptyVisible(tracks.isEmpty())
        val previousEditMode = listView.getTag(R.id.elvitem) as? Boolean
        val animateEditMode = previousEditMode != null && previousEditMode != editMode
        listView.setTag(R.id.elvitem, editMode)
        val changed = trackAdapter.updateItems(
            nextItems = tracks,
            nextCurrentMediaId = currentMediaId,
            nextCurrentIsPlaying = currentIsPlaying,
            nextEditMode = editMode,
            nextSelectedMediaIds = selectedTrackIds,
            nextSelectionOnlyMode = false,
            nextSectioned = false,
        )
        if (changed) {
            listView.scheduleLayoutAnimation()
        } else {
            trackAdapter.updateVisibleRows(listView, animateEditMode)
        }
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = trackAdapter.itemAt(position) ?: return@setOnItemClickListener
            onTrackClick(item, position)
        }
    }

    fun bindPlayback(player: Player?) {
        if (listView.getTag(R.id.list) !== player) {
            (listView.getTag(R.id.text) as? Player.Listener)?.let { oldListener ->
                (listView.getTag(R.id.list) as? Player)?.removeListener(oldListener)
            }
            if (player != null) {
                val listener = object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        trackAdapter.setPlaybackState(
                            nextCurrentMediaId = player.currentMediaItem?.mediaId,
                            nextCurrentIsPlaying = player.isPlaying,
                        )
                        trackAdapter.updateVisiblePlaybackState(listView)
                    }
                }
                player.addListener(listener)
                listView.setTag(R.id.text, listener)
            } else {
                listView.setTag(R.id.text, null)
            }
            listView.setTag(R.id.list, player)
        }
    }

    fun setEmptyVisible(visible: Boolean) {
        blankView.visibility = if (visible) View.VISIBLE else View.GONE
        listView.visibility = if (visible) View.INVISIBLE else View.VISIBLE
    }

    override fun onDetachedFromWindow() {
        (listView.getTag(R.id.text) as? Player.Listener)?.let { oldListener ->
            (listView.getTag(R.id.list) as? Player)?.removeListener(oldListener)
        }
        listView.setTag(R.id.text, null)
        listView.setTag(R.id.list, null)
        super.onDetachedFromWindow()
    }
}

private class LegacyPlaylistAddSongsRootView(context: Context) : LinearLayout(context) {
    private val sortHeader = ActionButtonGroup(context)
    private val playContainer = LinearLayout(context)
    private val listFrame = FrameLayout(context)
    private val listView = ListView(context)
    private val quickBar = QuickBarEx(context)
    private val blankView = LegacyPlaylistBlankView(
        context = context,
        iconRes = R.drawable.blank_song,
        primaryText = context.getString(R.string.no_song),
        secondaryText = "",
    )
    private var lastSortIndex = -1

    init {
        orientation = VERTICAL
        setBackgroundResource(R.drawable.account_background)
        addView(sortHeader, LayoutParams(LayoutParams.MATCH_PARENT, dp(48)))
        playContainer.apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(dp(6), dp(10), dp(6), dp(10))
            alpha = 0.22f
            addView(playlistPlayActionButton(context, R.drawable.btn_icon_play_selector, R.string.tab_play_list), LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            addView(
                playlistPlayActionButton(context, R.drawable.btn_icon_shuffle_selector, R.string.s_random_play),
                LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                    leftMargin = dp(6)
                },
            )
        }
        addView(playContainer, LayoutParams(LayoutParams.MATCH_PARENT, dp(50)))
        addView(listFrame, LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
        listView.apply {
            divider = ColorDrawable(context.getColor(R.color.listview_divider_color))
            dividerHeight = resources.getDimensionPixelSize(R.dimen.listview_dividerHeight)
            selector = context.getDrawable(R.drawable.listview_selector)
            cacheColorHint = Color.TRANSPARENT
            setBackgroundResource(R.drawable.account_background)
            isVerticalScrollBarEnabled = false
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.list_anim_layout)
        }
        listFrame.addView(listView, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        listFrame.addView(
            quickBar,
            FrameLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.letters_bar_width),
                LayoutParams.MATCH_PARENT,
                Gravity.END,
            ),
        )
        listFrame.addView(blankView, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun bind(
        songs: List<MediaItem>,
        selectedSongIds: Set<String>,
        currentMediaId: String?,
        currentIsPlaying: Boolean,
        selectedSortIndex: Int,
        onSortSelected: (Int) -> Unit,
        onToggleSong: (String) -> Unit,
    ) {
        sortHeader.setupPlaylistAddSongsSortHeader(selectedSortIndex, onSortSelected)
        blankView.visibility = if (songs.isEmpty()) View.VISIBLE else View.GONE
        listView.visibility = if (songs.isEmpty()) View.INVISIBLE else View.VISIBLE
        quickBar.visibility = if (songs.isEmpty() || selectedSortIndex != 0) View.GONE else View.VISIBLE
        val adapter = listView.adapter as? LegacyPlaylistTrackAdapter
            ?: LegacyPlaylistTrackAdapter().also { adapter ->
                listView.adapter = adapter
            }
        val changed = adapter.updateItems(
            nextItems = songs,
            nextCurrentMediaId = currentMediaId,
            nextCurrentIsPlaying = currentIsPlaying,
            nextEditMode = true,
            nextSelectedMediaIds = selectedSongIds,
            nextSelectionOnlyMode = true,
            nextSectioned = selectedSortIndex == 0,
        )
        if (changed) {
            listView.scheduleLayoutAnimation()
        } else {
            adapter.updateVisibleRows(listView, animateEditMode = false)
        }
        if (selectedSortIndex != lastSortIndex) {
            listView.setSelection(0)
            lastSortIndex = selectedSortIndex
        }
        quickBar.setLetters(QuickBarEx.DefaultLetters)
        quickBar.setLongPressEnabled(false)
        quickBar.setQBListener(
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
        listView.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.itemAt(position) ?: return@setOnItemClickListener
            onToggleSong(item.mediaId)
        }
    }

    fun bindPlayback(player: Player?) {
        val adapter = listView.adapter as? LegacyPlaylistTrackAdapter ?: return
        if (listView.getTag(R.id.list) !== player) {
            (listView.getTag(R.id.text) as? Player.Listener)?.let { oldListener ->
                (listView.getTag(R.id.list) as? Player)?.removeListener(oldListener)
            }
            if (player != null) {
                val listener = object : Player.Listener {
                    override fun onEvents(player: Player, events: Player.Events) {
                        adapter.setPlaybackState(
                            nextCurrentMediaId = player.currentMediaItem?.mediaId,
                            nextCurrentIsPlaying = player.isPlaying,
                        )
                        adapter.updateVisiblePlaybackState(listView)
                    }
                }
                player.addListener(listener)
                listView.setTag(R.id.text, listener)
            } else {
                listView.setTag(R.id.text, null)
            }
            listView.setTag(R.id.list, player)
        }
    }

    override fun onDetachedFromWindow() {
        (listView.getTag(R.id.text) as? Player.Listener)?.let { oldListener ->
            (listView.getTag(R.id.list) as? Player)?.removeListener(oldListener)
        }
        listView.setTag(R.id.text, null)
        listView.setTag(R.id.list, null)
        super.onDetachedFromWindow()
    }
}

private class LegacyPlaylistDetailHeader(context: Context) : FrameLayout(context) {
    private val normalHeader = LinearLayout(context)
    private val editHeader = RelativeLayout(context)
    private val selectAllCheckBox = CustomCheckBox(context)
    private val selectedText = TextView(context)
    private val addOrRemoveButton = LinearLayout(context)
    private val addOrRemoveIcon = ImageView(context)
    private val addOrRemoveText = TextView(context)
    private var lastEditMode: Boolean? = null

    init {
        setBackgroundColor(Color.WHITE)
        normalHeader.apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            isBaselineAligned = false
            addView(detailActionButton(context, R.drawable.btn_shuffle2_selector, R.string.s_random_play), LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            addView(detailActionButton(context, R.drawable.btn_deletelist2_selector, R.string.s_remove_track_list), LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                leftMargin = -dp(6)
            })
            addView(detailActionButton(context, R.drawable.btn_editlist2_selector, R.string.s_edit_track_list), LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                leftMargin = -dp(6)
            })
        }
        addView(normalHeader, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

        editHeader.apply {
            setBackgroundColor(Color.WHITE)
            selectAllCheckBox.id = View.generateViewId()
            selectAllCheckBox.buttonDrawable = context.getDrawable(R.drawable.check_box_selector)
            selectAllCheckBox.setPadding(0, 0, 0, 0)
            addView(selectAllCheckBox, RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = resources.getDimensionPixelSize(R.dimen.check_box_margin_left)
                addRule(RelativeLayout.CENTER_VERTICAL)
            })
            addOrRemoveButton.id = View.generateViewId()
            addOrRemoveButton.apply {
                gravity = Gravity.CENTER
                isClickable = true
                isFocusable = true
                addView(addOrRemoveIcon, LinearLayout.LayoutParams(dp(14), dp(14)).apply {
                    leftMargin = dp(10)
                    rightMargin = dp(10)
                })
                addView(addOrRemoveText, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            }
            addView(addOrRemoveButton, RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dp(30)).apply {
                rightMargin = dp(6)
                addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                addRule(RelativeLayout.CENTER_VERTICAL)
            })
            selectedText.apply {
                setTextColor(PlaylistSecondaryTextColor)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_size_better))
                gravity = Gravity.CENTER_VERTICAL
                setSingleLine(true)
            }
            addView(selectedText, RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(18)
                addRule(RelativeLayout.RIGHT_OF, selectAllCheckBox.id)
                addRule(RelativeLayout.LEFT_OF, addOrRemoveButton.id)
                addRule(RelativeLayout.CENTER_VERTICAL)
            })
        }
        addView(editHeader, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun bind(
        trackCount: Int,
        selectedCount: Int,
        editMode: Boolean,
        onShuffle: () -> Unit,
        onDeletePlaylist: () -> Unit,
        onEdit: () -> Unit,
        onAddOrRemoveClick: () -> Unit,
        onToggleAll: (Boolean) -> Unit,
    ) {
        val animateMode = lastEditMode != null && lastEditMode != editMode
        lastEditMode = editMode
        if (animateMode) {
            animateHeaderMode(editMode)
        } else {
            setHeaderMode(editMode)
        }
        if (!editMode) {
            normalHeader.getChildAt(0).setOnClickListener { onShuffle() }
            normalHeader.getChildAt(1).setOnClickListener { onDeletePlaylist() }
            normalHeader.getChildAt(2).setOnClickListener { onEdit() }
            return
        }
        selectAllCheckBox.setOnCheckedChangeListener(null)
        selectAllCheckBox.isChecked = trackCount > 0 && selectedCount == trackCount
        selectAllCheckBox.isEnabled = trackCount > 0
        selectAllCheckBox.setOnCheckedChangeListener { _, checked ->
            onToggleAll(checked)
        }
        selectedText.text = context.getString(R.string.selected_item_format, selectedCount, trackCount)
        val removing = selectedCount > 0
        addOrRemoveButton.setBackgroundResource(if (removing) R.drawable.btn_red_bg_selector else R.drawable.btn_add_song_selector)
        addOrRemoveIcon.setImageResource(if (removing) R.drawable.btn_delete_song2_selector else R.drawable.btn_add_song2_selector)
        addOrRemoveText.text = context.getString(if (removing) R.string.delete_track else R.string.add_track)
        addOrRemoveText.setTextColor(context.getColor(if (removing) R.color.btn_text_color_red else R.color.btn_text_color_blue))
        addOrRemoveText.setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.button_text_size))
        addOrRemoveText.typeface = Typeface.DEFAULT_BOLD
        addOrRemoveText.setPadding(0, 0, dp(10), 0)
        addOrRemoveButton.setOnClickListener { onAddOrRemoveClick() }
    }

    private fun setHeaderMode(editMode: Boolean) {
        normalHeader.animate().cancel()
        editHeader.animate().cancel()
        normalHeader.visibility = if (editMode) View.GONE else View.VISIBLE
        normalHeader.alpha = if (editMode) 0f else 1f
        editHeader.visibility = if (editMode) View.VISIBLE else View.GONE
        editHeader.alpha = if (editMode) 1f else 0f
    }

    private fun animateHeaderMode(editMode: Boolean) {
        normalHeader.animate().cancel()
        editHeader.animate().cancel()
        if (editMode) {
            normalHeader.visibility = View.VISIBLE
            normalHeader.alpha = 1f
            editHeader.visibility = View.VISIBLE
            editHeader.alpha = 0f
            normalHeader.animate()
                .alpha(0f)
                .setDuration(140L)
                .withEndAction {
                    normalHeader.visibility = View.GONE
                }
                .start()
            editHeader.animate()
                .alpha(1f)
                .setDuration(200L)
                .start()
        } else {
            normalHeader.visibility = View.VISIBLE
            normalHeader.alpha = 0f
            editHeader.visibility = View.VISIBLE
            editHeader.alpha = 1f
            editHeader.animate()
                .alpha(0f)
                .setDuration(140L)
                .withEndAction {
                    editHeader.visibility = View.GONE
                }
                .start()
            normalHeader.animate()
                .alpha(1f)
                .setDuration(200L)
                .start()
        }
    }
}

private class LegacyPlaylistRootAdapter : BaseAdapter() {
    private var items: List<UserPlaylistSummary> = emptyList()
    private var editMode = false
    private var forcedStaticEditMode: Boolean? = null
    private var selectedIds: Set<String> = emptySet()
    var onRenamePlaylist: (UserPlaylistSummary) -> Unit = {}

    fun forceStaticEditMode(mode: Boolean) {
        forcedStaticEditMode = mode
    }

    fun clearForcedStaticEditMode() {
        forcedStaticEditMode = null
    }

    fun updateItems(
        nextItems: List<UserPlaylistSummary>,
        nextEditMode: Boolean,
        nextSelectedIds: Set<String>,
    ): Boolean {
        val contentChanged = items != nextItems
        val editModeChanged = editMode != nextEditMode
        val selectionChanged = selectedIds != nextSelectedIds
        if (!contentChanged && !editModeChanged && !selectionChanged) {
            return false
        }
        items = nextItems
        editMode = nextEditMode
        selectedIds = nextSelectedIds
        if (contentChanged) {
            notifyDataSetChanged()
        }
        return contentChanged
    }

    fun updateVisibleRows(
        listView: ListView,
        animateEditMode: Boolean,
    ) {
        for (childIndex in 0 until listView.childCount) {
            val position = listView.firstVisiblePosition + childIndex
            val item = itemAt(position) ?: continue
            val child = listView.getChildAt(childIndex) ?: continue
            bindRow(
                view = child,
                item = item,
                visualEditMode = editMode,
                animateEditMode = animateEditMode,
            )
        }
    }

    fun itemAt(position: Int): UserPlaylistSummary? = items.getOrNull(position)

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.item_listview, parent, false)
        val item = items[position]
        bindRow(
            view = view,
            item = item,
            visualEditMode = forcedStaticEditMode ?: editMode,
            animateEditMode = false,
        )
        return view
    }

    private fun bindRow(
        view: View,
        item: UserPlaylistSummary,
        visualEditMode: Boolean,
        animateEditMode: Boolean,
    ) {
        view.findViewById<TextView>(R.id.listview_item_line_one)?.apply {
            text = item.name
            setTextColor(PlaylistPrimaryTextColor)
        }
        val context = view.context
        view.findViewById<TextView>(R.id.listview_item_line_two)?.apply {
            text = context.resources.getQuantityString(
                R.plurals.legacy_playlist_song_count,
                item.songCount,
                item.songCount,
            )
            setTextColor(PlaylistSecondaryTextColor)
        }
        view.findViewById<View>(R.id.iv_right_view)?.setOnClickListener {
            if (editMode) {
                onRenamePlaylist(item)
            }
        }
        view.findViewById<CheckBox>(R.id.cb_del)?.isChecked = item.id in selectedIds
        (view as? EditableLayout)?.bindLegacyEditState(
            enabled = visualEditMode,
            checked = item.id in selectedIds,
            animate = animateEditMode,
        )
    }
}

private sealed class LegacyPlaylistSongRow {
    data class Header(val letter: String) : LegacyPlaylistSongRow()

    data class Song(
        val item: MediaItem,
        val songIndex: Int,
    ) : LegacyPlaylistSongRow()
}

private class LegacyPlaylistTrackAdapter : BaseAdapter(), LegacyListDragAdapter<MediaItem> {
    var onMoreClick: (MediaItem) -> Unit = {}
    private var items: List<MediaItem> = emptyList()
    private var rows: List<LegacyPlaylistSongRow> = emptyList()
    private var currentMediaId: String? = null
    private var currentIsPlaying: Boolean = false
    private var editMode: Boolean = false
    private var selectedMediaIds: Set<String> = emptySet()
    private var selectionOnlyMode = false
    private var sectioned = false

    fun updateItems(
        nextItems: List<MediaItem>,
        nextCurrentMediaId: String?,
        nextCurrentIsPlaying: Boolean,
        nextEditMode: Boolean,
        nextSelectedMediaIds: Set<String>,
        nextSelectionOnlyMode: Boolean,
        nextSectioned: Boolean,
    ): Boolean {
        val contentChanged = items != nextItems || sectioned != nextSectioned
        val stateChanged = currentMediaId != nextCurrentMediaId ||
            currentIsPlaying != nextCurrentIsPlaying ||
            editMode != nextEditMode ||
            selectedMediaIds != nextSelectedMediaIds ||
            selectionOnlyMode != nextSelectionOnlyMode
        if (!contentChanged && !stateChanged) {
            return false
        }
        items = nextItems
        currentMediaId = nextCurrentMediaId
        currentIsPlaying = nextCurrentIsPlaying
        editMode = nextEditMode
        selectedMediaIds = nextSelectedMediaIds
        selectionOnlyMode = nextSelectionOnlyMode
        sectioned = nextSectioned
        if (contentChanged) {
            rows = buildPlaylistSongRows(nextItems, nextSectioned)
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
        updateVisibleRows(listView, animateEditMode = false)
    }

    fun updateVisibleRows(
        listView: ListView,
        animateEditMode: Boolean,
    ) {
        for (childIndex in 0 until listView.childCount) {
            val position = listView.firstVisiblePosition + childIndex
            val item = itemAt(position)
            val child = listView.getChildAt(childIndex)
            val titleView = child?.findViewById<TextView>(R.id.listview_item_line_one) as? StretchTextView
            if (item == null || child == null || titleView == null) {
                continue
            }
            bindPlaybackState(titleView, item)
            bindPlaylistTrackEditState(
                view = child,
                enabled = editMode,
                checked = item.mediaId in selectedMediaIds,
                selectionOnly = selectionOnlyMode,
                animate = animateEditMode,
            )
        }
    }

    fun itemAt(position: Int): MediaItem? = (rows.getOrNull(position) as? LegacyPlaylistSongRow.Song)?.item

    fun positionForLetter(letter: String): Int {
        return rows.indexOfFirst { row ->
            row is LegacyPlaylistSongRow.Header && row.letter == letter
        }
    }

    fun mediaIdAt(position: Int): String? = itemAt(position)?.mediaId

    override fun reorderableItemAt(position: Int): MediaItem? {
        if (!editMode || selectionOnlyMode) {
            return null
        }
        return itemAt(position)
    }

    override fun firstReorderableAdapterPosition(): Int {
        return rows.indexOfFirst { row ->
            row is LegacyPlaylistSongRow.Song
        }.takeIf { it >= 0 } ?: ListView.INVALID_POSITION
    }

    override fun lastReorderableAdapterPosition(): Int {
        return rows.indexOfLast { row ->
            row is LegacyPlaylistSongRow.Song
        }.takeIf { it >= 0 } ?: ListView.INVALID_POSITION
    }

    override fun movePreviewRow(
        fromPosition: Int,
        toPosition: Int,
    ) {
        if (fromPosition == toPosition) {
            return
        }
        val mutableRows = rows.toMutableList()
        val fromRow = mutableRows.getOrNull(fromPosition) as? LegacyPlaylistSongRow.Song
            ?: return
        if (mutableRows.getOrNull(toPosition) !is LegacyPlaylistSongRow.Song) {
            return
        }
        mutableRows.removeAt(fromPosition)
        mutableRows.add(toPosition.coerceIn(0, mutableRows.size), fromRow)
        rows = mutableRows
        items = mutableRows.mapNotNull { row ->
            (row as? LegacyPlaylistSongRow.Song)?.item
        }
        notifyDataSetChanged()
    }

    fun orderedSongMediaIds(): List<String> {
        return rows.mapNotNull { row ->
            (row as? LegacyPlaylistSongRow.Song)?.item?.mediaId
        }
    }

    override fun getCount(): Int = rows.size

    override fun getItem(position: Int): Any = rows[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getViewTypeCount(): Int = 2

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is LegacyPlaylistSongRow.Header -> 0
            is LegacyPlaylistSongRow.Song -> 1
        }
    }

    override fun isEnabled(position: Int): Boolean {
        return rows.getOrNull(position) is LegacyPlaylistSongRow.Song
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return when (val row = rows[position]) {
            is LegacyPlaylistSongRow.Header -> getHeaderView(row, convertView, parent)
            is LegacyPlaylistSongRow.Song -> getSongView(row.item, convertView, parent)
        }
    }

    private fun getHeaderView(
        row: LegacyPlaylistSongRow.Header,
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
        view.alpha = 1f
        view.translationY = 0f
        val title = item.mediaMetadata.displayTitle?.toString()
            ?: item.mediaMetadata.title?.toString()
            ?: parent.context.getString(R.string.unknown_song_title)
        val artist = item.mediaMetadata.artist?.toString()
            ?: item.mediaMetadata.subtitle?.toString()
            ?: parent.context.getString(R.string.unknown_artist)

        view.findViewById<TextView>(R.id.listview_item_line_one)?.apply {
            text = title
            isSelected = true
            setTextColor(PlaylistPrimaryTextColor)
            if (this is StretchTextView) {
                bindPlaybackState(this, item)
            }
        }
        view.findViewById<TextView>(R.id.listview_item_line_two)?.apply {
            text = artist
            setTextColor(PlaylistSecondaryTextColor)
        }
        view.findViewById<TextView>(R.id.tv_duration)?.text = item.mediaMetadata.durationMs?.formatPlaylistDuration().orEmpty()
        view.findViewById<View>(R.id.img_action_more)?.apply {
            isClickable = true
            isFocusable = false
            setOnClickListener {
                onMoreClick(item)
            }
        }
        view.findViewById<ImageView>(R.id.mime_type)?.apply {
            val badge = item.playlistQualityBadgeRes()
            if (badge != null) {
                visibility = View.VISIBLE
                setImageResource(badge)
            } else {
                visibility = View.GONE
            }
        }
        bindPlaylistTrackEditState(
            view = view,
            enabled = editMode,
            checked = item.mediaId in selectedMediaIds,
            selectionOnly = selectionOnlyMode,
            animate = false,
        )
        return view
    }

    private fun bindPlaybackState(titleView: StretchTextView, item: MediaItem) {
        if (!editMode && item.mediaId == currentMediaId) {
            titleView.c(currentIsPlaying)
        } else {
            titleView.setShowingPlayImage(false)
        }
    }
}

private fun bindPlaylistTrackEditState(
    view: View,
    enabled: Boolean,
    checked: Boolean,
    selectionOnly: Boolean,
    animate: Boolean,
) {
    (view as? EditableListViewItem)?.let { itemView ->
        itemView.bindLegacyPlaylistEditState(
            enabled = enabled,
            checked = checked,
            selectionOnly = selectionOnly,
            animate = animate,
        )
        return
    }
    val checkbox = view.findViewById<CheckBox>(R.id.cb_del) ?: return
    val content = view.findViewById<View>(R.id.relativeLayout1) ?: return
    val drag = view.findViewById<View>(R.id.iv_right)
    val duration = view.findViewById<View>(R.id.tv_duration)
    val more = view.findViewById<View>(R.id.img_action_more)
    checkbox.isChecked = checked
    checkbox.isClickable = false
    checkbox.isFocusable = false
    val offset = checkbox.measuredWidth.takeIf { it > 0 } ?: run {
        checkbox.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        checkbox.measuredWidth
    }
    val leftMargin = (checkbox.layoutParams as? ViewGroup.MarginLayoutParams)?.leftMargin ?: 0
    val totalOffset = (leftMargin + offset).toFloat()
    checkbox.visibility = if (enabled) View.VISIBLE else View.GONE
    checkbox.alpha = if (enabled) 1f else 0f
    checkbox.translationX = if (enabled) 0f else -totalOffset
    content.translationX = 0f
    drag?.visibility = if (enabled && !selectionOnly) View.VISIBLE else View.GONE
    duration?.visibility = if (selectionOnly) View.GONE else View.VISIBLE
    more?.visibility = if (!enabled) View.VISIBLE else View.GONE
    if (animate) {
        view.animate().setDuration(200L).start()
    }
}

@Composable
private fun LegacyPlaylistNameDialogOverlay(
    request: LegacyPlaylistNameDialogRequest?,
    onDismiss: () -> Unit,
    onConfirm: (LegacyPlaylistNameDialogRequest, String) -> Unit,
) {
    val context = LocalContext.current
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestOnConfirm by rememberUpdatedState(onConfirm)
    DisposableEffect(request) {
        val activeRequest = request ?: return@DisposableEffect onDispose { }
        val dialog = LegacyPlaylistNameDialog(
            context = context,
            title = context.getString(
                when (activeRequest) {
                    is LegacyPlaylistNameDialogRequest.Create -> R.string.new_playlist
                    is LegacyPlaylistNameDialogRequest.Rename -> R.string.playlist_rename_title
                },
            ),
            initialName = activeRequest.initialName,
            confirmText = context.getString(
                when (activeRequest) {
                    is LegacyPlaylistNameDialogRequest.Create -> R.string.rename_continue
                    is LegacyPlaylistNameDialogRequest.Rename -> R.string.save
                },
            ),
            onDismiss = latestOnDismiss,
            onConfirm = { name ->
                latestOnConfirm(activeRequest, name)
            },
        )
        dialog.show()
        onDispose {
            dialog.dismissIfShowing()
        }
    }
}

@Composable
private fun LegacyPlaylistDeleteDialog(
    request: LegacyPlaylistDeleteRequest?,
    onDismiss: () -> Unit,
    onConfirm: (LegacyPlaylistDeleteRequest) -> Unit,
) {
    val context = LocalContext.current
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestOnConfirm by rememberUpdatedState(onConfirm)
    DisposableEffect(request) {
        val activeRequest = request ?: return@DisposableEffect onDispose { }
        val dialog = MenuDialog(context).apply {
            setTitle(
                when (activeRequest) {
                    LegacyPlaylistDeleteRequest.RootSelected -> R.string.playlist_delete_confirm
                    LegacyPlaylistDeleteRequest.DetailPlaylist -> R.string.playlist_delete_single_confirm
                    LegacyPlaylistDeleteRequest.DetailTracks -> R.string.playlist_remove_song_confirm
                },
            )
            setPositiveButton(R.string.dialog_delete_conform) {
                latestOnConfirm(activeRequest)
            }
            setNegativeButton(
                View.OnClickListener {
                    latestOnDismiss()
                },
            )
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

private class LegacyPlaylistNameDialog(
    private val context: Context,
    title: String,
    initialName: String,
    confirmText: String,
    private val onDismiss: () -> Unit,
    private val onConfirm: (String) -> Unit,
) {
    private val dialog = Dialog(context)
    private val editText: EditText
    private val confirmButton: TextView

    init {
        val density = context.resources.displayMetrics.density
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 10f * density
            }
        }
        dialog.requestWindowFeature(1)
        dialog.setContentView(root)
        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnCancelListener {
            onDismiss()
        }

        root.addView(
            TextView(context).apply {
                text = title
                gravity = Gravity.CENTER
                setTextColor(context.getColor(R.color.title_color))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                typeface = Typeface.DEFAULT_BOLD
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 53)),
        )
        root.addView(divider(context), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1)))

        val editFrame = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 12), 0, dp(context, 8), 0)
            background = GradientDrawable().apply {
                setColor(Color.rgb(0xf7, 0xf8, 0xf9))
                setStroke(dp(context, 1), Color.rgb(0xe2, 0xe2, 0xe2))
                cornerRadius = 6f * density
            }
        }
        editText = EditText(context).apply {
            setSingleLine(true)
            setText(initialName)
            selectAll()
            setTextColor(PlaylistPrimaryTextColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            background = null
        }
        editFrame.addView(editText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        editFrame.addView(
            ImageView(context).apply {
                setImageResource(R.drawable.clear_text)
                setOnClickListener {
                    editText.text = null
                }
            },
            LinearLayout.LayoutParams(dp(context, 32), LinearLayout.LayoutParams.MATCH_PARENT),
        )
        root.addView(
            editFrame,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 44)).apply {
                leftMargin = dp(context, 18)
                rightMargin = dp(context, 18)
                topMargin = dp(context, 18)
                bottomMargin = dp(context, 18)
            },
        )
        root.addView(divider(context), LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 1)))

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val cancelButton = dialogButton(context, context.getString(android.R.string.cancel), Color.rgb(0x99, 0x99, 0x99)).apply {
            setOnClickListener {
                dialog.dismiss()
                onDismiss()
            }
        }
        confirmButton = dialogButton(context, confirmText, context.getColor(R.color.btn_text_color_blue)).apply {
            setOnClickListener {
                onConfirm(editText.text.toString())
            }
        }
        buttons.addView(cancelButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        buttons.addView(divider(context), LinearLayout.LayoutParams(dp(context, 1), LinearLayout.LayoutParams.MATCH_PARENT))
        buttons.addView(confirmButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
        root.addView(buttons, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 53)))

        editText.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    confirmButton.isEnabled = !s.isNullOrBlank()
                    confirmButton.alpha = if (confirmButton.isEnabled) 1f else 0.35f
                }
                override fun afterTextChanged(s: Editable?) = Unit
            },
        )
    }

    fun show() {
        dialog.show()
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDimAmount(0.54f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setLayout(dp(context, 308), WindowManager.LayoutParams.WRAP_CONTENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
        editText.postDelayed(
            {
                editText.requestFocus()
                (dialog.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            },
            300L,
        )
    }

    fun dismissIfShowing() {
        if (dialog.isShowing) {
            dialog.dismiss()
        }
    }

    private fun divider(context: Context): View {
        return View(context).apply {
            setBackgroundColor(Color.rgb(0xe8, 0xe8, 0xe8))
        }
    }

    private fun dialogButton(context: Context, text: String, color: Int): TextView {
        return TextView(context).apply {
            gravity = Gravity.CENTER
            this.text = text
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            typeface = Typeface.DEFAULT_BOLD
            isClickable = true
            isFocusable = true
        }
    }
}

private class LegacyPlaylistBlankView(
    context: Context,
    iconRes: Int,
    primaryText: String,
    secondaryText: String,
) : LinearLayout(context) {
    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setBackgroundResource(R.drawable.account_background)
        addView(
            ImageView(context).apply {
                setImageResource(iconRes)
                alpha = 0.42f
            },
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(16)
            },
        )
        addView(
            TextView(context).apply {
                text = primaryText
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(0xc8, 0xc8, 0xc8))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
                includeFontPadding = false
            },
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
        )
        if (secondaryText.isNotBlank()) {
            addView(
                TextView(context).apply {
                    text = secondaryText
                    gravity = Gravity.CENTER
                    setTextColor(Color.rgb(0xc8, 0xc8, 0xc8))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    includeFontPadding = false
                },
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    topMargin = dp(10)
                },
            )
        }
    }
}

private fun detailActionButton(
    context: Context,
    iconRes: Int,
    textRes: Int,
): LinearLayout {
    return LinearLayout(context).apply {
        gravity = Gravity.CENTER
        orientation = LinearLayout.HORIZONTAL
        setBackgroundResource(R.drawable.title_button_bg_selector)
        isClickable = true
        isFocusable = true
        addView(
            ImageView(context).apply {
                setBackgroundResource(iconRes)
                isDuplicateParentStateEnabled = true
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = dp(10)
            },
        )
        addView(
            TextView(context).apply {
                text = context.getString(textRes)
                setTextColor(context.getColor(R.color.transparent_black))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.button_text_size))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                includeFontPadding = false
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
    }
}

private fun playlistPlayActionButton(
    context: Context,
    iconRes: Int,
    textRes: Int,
): LinearLayout {
    return LinearLayout(context).apply {
        gravity = Gravity.CENTER
        orientation = LinearLayout.HORIZONTAL
        setBackgroundResource(R.drawable.btn_red_bg_selector)
        isEnabled = false
        addView(
            ImageView(context).apply {
                setImageResource(iconRes)
                isDuplicateParentStateEnabled = true
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                rightMargin = dp(10)
            },
        )
        addView(
            TextView(context).apply {
                text = context.getString(textRes)
                setTextColor(context.getColorStateList(R.drawable.red_btn_text_color_selector))
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.settings_item_tips_text_size))
                typeface = Typeface.DEFAULT_BOLD
            },
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT),
        )
    }
}

private fun ActionButtonGroup.setupPlaylistAddSongsSortHeader(
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
        R.string.playlist_sort_song_name,
        R.string.playlist_sort_score,
        R.string.playlist_sort_play_count,
        R.string.playlist_sort_added_time,
    )
    repeat(getButtonCount().coerceAtMost(labels.size)) { index ->
        getButton(index).apply {
            setButtonText(index, labels[index])
            gravity = Gravity.CENTER
            setOnClickListener {
                onSortSelected(index)
            }
        }
    }
    setButtonActivated(selectedSortIndex)
}

private fun List<MediaItem>.sortedForPlaylistAddMode(sortIndex: Int): List<MediaItem> {
    return when (sortIndex) {
        0 -> sortedWith(
            compareBy<MediaItem> { item ->
                item.playlistSortBucket()
            }.thenBy { item ->
                item.playlistSortKey()
            },
        )
        1 -> sortedByPlaylistMetricDescending { item ->
            item.playlistExtraLong("score", "rating", "play_score")
        }
        2 -> sortedByPlaylistMetricDescending { item ->
            item.playlistExtraLong("play_count", "playCount", "play_count_all")
        }
        3 -> sortedWith(
            compareByDescending<MediaItem> { item ->
                item.playlistExtraLong(LocalAudioLibrary.DateAddedExtraKey, "date_added")
            }.thenBy { item ->
                item.playlistSortKey()
            },
        )
        else -> this
    }
}

private fun List<MediaItem>.sortedByPlaylistMetricDescending(
    metric: (MediaItem) -> Long,
): List<MediaItem> {
    val hasMetric = any { item -> metric(item) > 0L }
    return if (hasMetric) {
        sortedWith(
            compareByDescending<MediaItem> { item ->
                metric(item)
            }.thenBy { item ->
                item.playlistSortKey()
            },
        )
    } else {
        // 现代媒体库暂未持久化旧版 score/play_count，保留可见切换并保证排序稳定。
        sortedWith(
            compareByDescending<MediaItem> { item ->
                item.playlistSortKey()
            }.thenBy { item ->
                item.mediaId
            },
        )
    }
}

private fun buildPlaylistSongRows(
    mediaItems: List<MediaItem>,
    sectioned: Boolean,
): List<LegacyPlaylistSongRow> {
    if (!sectioned) {
        return mediaItems.mapIndexed { index, mediaItem ->
            LegacyPlaylistSongRow.Song(mediaItem, index)
        }
    }
    val rows = mutableListOf<LegacyPlaylistSongRow>()
    var previousLetter: String? = null
    mediaItems.forEachIndexed { index, mediaItem ->
        val letter = mediaItem.playlistSectionLetter()
        if (letter != previousLetter) {
            rows += LegacyPlaylistSongRow.Header(letter)
            previousLetter = letter
        }
        rows += LegacyPlaylistSongRow.Song(mediaItem, index)
    }
    return rows
}

private fun MediaItem.playlistSortTitle(): String {
    return mediaMetadata.displayTitle?.toString()
        ?: mediaMetadata.title?.toString()
        ?: ""
}

private fun MediaItem.playlistSortKey(): String {
    return PlaylistTitleNormalizer.normalize(playlistSortTitle())
}

private fun MediaItem.playlistExtraLong(vararg keys: String): Long {
    val extras = mediaMetadata.extras ?: return 0L
    keys.forEach { key ->
        if (!extras.containsKey(key)) {
            return@forEach
        }
        val longValue = extras.getLong(key, Long.MIN_VALUE)
        if (longValue != Long.MIN_VALUE) {
            return longValue
        }
        val intValue = extras.getInt(key, Int.MIN_VALUE)
        if (intValue != Int.MIN_VALUE) {
            return intValue.toLong()
        }
        val doubleValue = extras.getDouble(key, Double.NaN)
        if (!doubleValue.isNaN()) {
            return doubleValue.toLong()
        }
    }
    return 0L
}

private fun MediaItem.playlistSortBucket(): String {
    val letter = playlistSectionLetter()
    return if (letter == "#") "ZZZ" else letter
}

private fun MediaItem.playlistSectionLetter(): String {
    val firstLetter = playlistSortKey().firstOrNull { char ->
        char.isLetterOrDigit()
    } ?: return "#"
    val upper = firstLetter.uppercaseChar()
    return if (upper in 'A'..'Z') upper.toString() else "#"
}

private object PlaylistTitleNormalizer {
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

private fun MediaItem.playlistQualityBadgeRes(): Int? {
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

private fun Long.formatPlaylistDuration(): String {
    if (this <= 0L) {
        return ""
    }
    val totalSeconds = this / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

private fun <T> Set<T>.toggle(value: T): Set<T> {
    return if (value in this) this - value else this + value
}

private fun String.ellipsizeMiddle(maxChars: Int): String {
    if (length <= maxChars) {
        return this
    }
    return take((maxChars - 3).coerceAtLeast(1)) + "..."
}

private fun View.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
