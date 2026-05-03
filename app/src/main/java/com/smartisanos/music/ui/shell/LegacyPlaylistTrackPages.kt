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

@Composable
internal fun LegacyPlaylistDetailPage(
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
internal fun LegacyPlaylistAddSongsPage(
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
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, dpPx(48)))
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
            FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, dpPx(1), Gravity.TOP),
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
        addView(sortHeader, LayoutParams(LayoutParams.MATCH_PARENT, dpPx(48)))
        playContainer.apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(dpPx(6), dpPx(10), dpPx(6), dpPx(10))
            alpha = 0.22f
            addView(playlistPlayActionButton(context, R.drawable.btn_icon_play_selector, R.string.tab_play_list), LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
            addView(
                playlistPlayActionButton(context, R.drawable.btn_icon_shuffle_selector, R.string.s_random_play),
                LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                    leftMargin = dpPx(6)
                },
            )
        }
        addView(playContainer, LayoutParams(LayoutParams.MATCH_PARENT, dpPx(50)))
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
                leftMargin = -dpPx(6)
            })
            addView(detailActionButton(context, R.drawable.btn_editlist2_selector, R.string.s_edit_track_list), LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1f).apply {
                leftMargin = -dpPx(6)
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
                addView(addOrRemoveIcon, LinearLayout.LayoutParams(dpPx(14), dpPx(14)).apply {
                    leftMargin = dpPx(10)
                    rightMargin = dpPx(10)
                })
                addView(addOrRemoveText, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            }
            addView(addOrRemoveButton, RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, dpPx(30)).apply {
                rightMargin = dpPx(6)
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
                leftMargin = dpPx(18)
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
        addOrRemoveText.setPadding(0, 0, dpPx(10), 0)
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
                rightMargin = dpPx(10)
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
                rightMargin = dpPx(10)
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
            item.playlistExtraLong(
                LocalAudioLibrary.RatingExtraKey,
                "score",
                "rating",
                "play_score",
            )
        }
        2 -> sortedByPlaylistMetricDescending { item ->
            item.playlistExtraLong(
                LocalAudioLibrary.PlayCountExtraKey,
                "play_count",
                "playCount",
                "play_count_all",
            )
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
        // 缺少历史指标时保留可见切换并保证排序稳定。
        sortedWith(
            compareBy<MediaItem> { item ->
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

internal fun <T> Set<T>.togglePlaylistSelection(value: T): Set<T> {
    return if (value in this) this - value else this + value
}
