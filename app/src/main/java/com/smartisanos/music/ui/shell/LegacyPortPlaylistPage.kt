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
import com.smartisanos.music.ui.shell.titlebar.LegacyPortSmartisanTitleBar
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
internal const val PlaylistRootFooterThreshold = 8
private val PlaylistAddModeEasing = Easing { fraction ->
    1f - (1f - fraction) * (1f - fraction)
}
internal val PlaylistPrimaryTextColor = Color.rgb(0x35, 0x35, 0x39)
internal val PlaylistSecondaryTextColor = Color.rgb(0xa4, 0xa7, 0xac)
internal val PlaylistFooterTextColor = Color.rgb(0xbc, 0xbc, 0xbc)

internal data class LegacyPlaylistTarget(
    val playlistId: String,
    val title: String,
)

internal sealed interface LegacyPlaylistNameDialogRequest {
    val initialName: String

    data class Create(
        override val initialName: String,
    ) : LegacyPlaylistNameDialogRequest

    data class Rename(
        val playlistId: String,
        override val initialName: String,
    ) : LegacyPlaylistNameDialogRequest
}

internal enum class LegacyPlaylistDeleteRequest {
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
                                selectedPlaylistIds = selectedPlaylistIds.togglePlaylistSelection(playlist.id)
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
                        onTrackSelectionChange = { mediaId, selected ->
                            if ((mediaId in selectedTrackIds) != selected) {
                                selectedTrackIds = selectedTrackIds.togglePlaylistSelection(mediaId)
                            }
                        },
                        onTrackClick = { item, index ->
                            if (detailEditMode) {
                                selectedTrackIds = selectedTrackIds.togglePlaylistSelection(item.mediaId)
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
                        selectedAddSongIds = selectedAddSongIds.togglePlaylistSelection(mediaId)
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

internal class LegacyPlaylistBlankView(
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

internal fun String.ellipsizeMiddle(maxChars: Int): String {
    if (length <= maxChars) {
        return this
    }
    return take((maxChars - 3).coerceAtLeast(1)) + "..."
}

private fun View.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
