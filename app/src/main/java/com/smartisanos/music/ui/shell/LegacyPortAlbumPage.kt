package com.smartisanos.music.ui.shell

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.playback.LocalAudioLibrary
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.ui.album.AlbumSummary
import com.smartisanos.music.ui.album.AlbumViewMode
import com.smartisanos.music.ui.album.buildAlbumSummaries
import com.smartisanos.music.ui.widgets.EditableLayout

private const val AlbumSwitchBaseDurationMillis = 150L
private const val AlbumSwitchStaggerMillis = 10L
private const val AlbumListFadeInMillis = 180L
private val LegacyAlbumPrimaryTextColor = Color.rgb(0x35, 0x35, 0x39)
private val LegacyAlbumSecondaryTextColor = Color.rgb(0xa4, 0xa7, 0xac)
private val LegacyAlbumSelectedTextColor = Color.rgb(0xe6, 0x40, 0x40)

@Composable
internal fun LegacyPortAlbumPage(
    viewMode: AlbumViewMode,
    editMode: Boolean,
    selectedAlbumIds: Set<String>,
    hiddenMediaIds: Set<String>,
    onToggleAlbumSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val browser = LocalPlaybackBrowser.current
    val songs = rememberLegacyLibraryMediaItems()
    val visibleSongs = remember(songs, hiddenMediaIds) {
        songs.filterNot { mediaItem -> mediaItem.mediaId in hiddenMediaIds }
    }
    val albums = remember(visibleSongs, context) {
        buildAlbumSummaries(
            mediaItems = visibleSongs,
            unknownAlbumTitle = context.getString(R.string.unknown_album),
            multipleArtistsTitle = context.getString(R.string.many_artist),
        )
    }
    var currentMediaId by remember(browser) {
        mutableStateOf(browser?.currentMediaItem?.mediaId)
    }
    val switchAnimator = remember { LegacyAlbumViewSwitchAnimator() }

    DisposableEffect(browser) {
        val playbackBrowser = browser ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                currentMediaId = player.currentMediaItem?.mediaId
            }
        }
        playbackBrowser.addListener(listener)
        onDispose {
            playbackBrowser.removeListener(listener)
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            LegacyAlbumRoot(viewContext)
        },
        update = { root ->
            root.bindPlayActions(
                albums = albums,
                enabled = albums.isNotEmpty() && !editMode,
                onPlay = play@{ shuffle ->
                    val playbackBrowser = browser ?: return@play
                    val albumSongs = albums.flatMap { it.songs }
                    if (albumSongs.isEmpty()) {
                        return@play
                    }
                    playbackBrowser.shuffleModeEnabled = shuffle
                    playbackBrowser.setMediaItems(albumSongs, 0, 0L)
                    playbackBrowser.prepare()
                    playbackBrowser.play()
                },
            )

            val listAdapter = root.listView.adapter as? LegacyAlbumListAdapter
                ?: LegacyAlbumListAdapter().also { adapter ->
                    root.listView.adapter = adapter
                }
            val gridAdapter = root.gridView.adapter as? LegacyAlbumGridAdapter
                ?: LegacyAlbumGridAdapter().also { adapter ->
                    root.gridView.adapter = adapter
                }
            val previousEditMode = root.editMode
            val animateEditMode = previousEditMode != null && previousEditMode != editMode
            root.editMode = editMode
            val listContentChanged = listAdapter.updateItems(
                nextItems = albums,
                nextCurrentMediaId = currentMediaId,
                nextEditMode = editMode,
                nextSelectedAlbumIds = selectedAlbumIds,
            )
            gridAdapter.updateItems(
                nextItems = albums,
                nextCurrentMediaId = currentMediaId,
                nextEditMode = editMode,
                nextSelectedAlbumIds = selectedAlbumIds,
            )
            if (!listContentChanged) {
                listAdapter.updateVisibleRows(root.listView, animate = animateEditMode)
            }

            root.listView.setOnItemClickListener { _, _, position, _ ->
                val album = listAdapter.itemAt(position) ?: return@setOnItemClickListener
                if (editMode) {
                    onToggleAlbumSelected(album.id)
                }
            }
            root.gridView.setOnItemClickListener { _, _, position, _ ->
                val album = gridAdapter.itemAt(position) ?: return@setOnItemClickListener
                if (editMode) {
                    onToggleAlbumSelected(album.id)
                }
            }

            val previousMode = root.viewMode
            root.viewMode = viewMode
            if (previousMode == null) {
                root.showModeImmediately(viewMode)
            } else if (previousMode != viewMode) {
                switchAnimator.animate(
                    root = root,
                    from = previousMode,
                    to = viewMode,
                )
            }
        },
    )
}

private class LegacyAlbumRoot(context: Context) : LinearLayout(context) {
    val listView: ListView
    val gridView: GridView
    var viewMode: AlbumViewMode? = null
    var editMode: Boolean? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.TRANSPARENT)

        val playContainer = LayoutInflater.from(context)
            .inflate(R.layout.layout_play_container, this, false)
            .apply {
                id = R.id.play_container
            }
        addView(
            playContainer,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            ),
        )
        addView(
            View(context).apply {
                setBackgroundColor(context.getColor(R.color.listview_divider_color))
            },
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(R.dimen.listview_dividerHeight),
            ),
        )

        val content = FrameLayout(context).apply {
            id = R.id.fl_list_tile
            setBackgroundResource(R.drawable.account_background)
        }
        addView(
            content,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                0,
                1f,
            ),
        )

        listView = ListView(context).apply {
            id = R.id.list
            divider = ColorDrawable(context.getColor(R.color.listview_divider_color))
            dividerHeight = resources.getDimensionPixelSize(R.dimen.listview_dividerHeight)
            selector = context.getDrawable(R.drawable.listview_selector)
            cacheColorHint = Color.TRANSPARENT
            setBackgroundColor(Color.TRANSPARENT)
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.list_anim_layout)
        }
        content.addView(
            listView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        gridView = GridView(context).apply {
            id = R.id.listview_grid
            numColumns = resources.getInteger(R.integer.gridview_columns)
            gravity = Gravity.CENTER_HORIZONTAL
            selector = ColorDrawable(Color.TRANSPARENT)
            cacheColorHint = Color.TRANSPARENT
            setBackgroundColor(Color.WHITE)
            verticalSpacing = resources.getDimensionPixelSize(R.dimen.gridview_verticalSpacing)
            horizontalSpacing = resources.getDimensionPixelSize(R.dimen.gridview_horizontalSpacing)
            setPadding(
                resources.getDimensionPixelSize(R.dimen.gridview_margin),
                0,
                resources.getDimensionPixelSize(R.dimen.gridview_margin),
                0,
            )
            clipToPadding = true
            visibility = View.GONE
        }
        content.addView(
            gridView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    fun bindPlayActions(
        albums: List<AlbumSummary>,
        enabled: Boolean,
        onPlay: (shuffle: Boolean) -> Unit,
    ) {
        findViewById<View>(R.id.play_container)?.apply {
            visibility = if (albums.isEmpty()) View.GONE else View.VISIBLE
            alpha = if (enabled) 1f else 0.22f
        }
        findViewById<View>(R.id.bt_play)?.apply {
            isEnabled = enabled
            setOnClickListener {
                if (enabled) {
                    onPlay(false)
                }
            }
        }
        findViewById<View>(R.id.bt_shuffle)?.apply {
            isEnabled = enabled
            setOnClickListener {
                if (enabled) {
                    onPlay(true)
                }
            }
        }
    }

    fun showModeImmediately(mode: AlbumViewMode) {
        listView.visibility = if (mode == AlbumViewMode.List) View.VISIBLE else View.INVISIBLE
        listView.alpha = 1f
        gridView.visibility = if (mode == AlbumViewMode.Tile) View.VISIBLE else View.GONE
    }
}

private class LegacyAlbumListAdapter : BaseAdapter() {
    private var items: List<AlbumSummary> = emptyList()
    private var currentMediaId: String? = null
    private var editMode: Boolean = false
    private var selectedAlbumIds: Set<String> = emptySet()

    fun updateItems(
        nextItems: List<AlbumSummary>,
        nextCurrentMediaId: String?,
        nextEditMode: Boolean,
        nextSelectedAlbumIds: Set<String>,
    ): Boolean {
        val contentChanged = items != nextItems
        val stateChanged = currentMediaId != nextCurrentMediaId ||
            editMode != nextEditMode ||
            selectedAlbumIds != nextSelectedAlbumIds
        if (!contentChanged && !stateChanged) {
            return false
        }
        items = nextItems
        currentMediaId = nextCurrentMediaId
        editMode = nextEditMode
        selectedAlbumIds = nextSelectedAlbumIds
        if (contentChanged) {
            notifyDataSetChanged()
        }
        return contentChanged
    }

    fun itemAt(position: Int): AlbumSummary? = items.getOrNull(position)

    fun updateVisibleRows(listView: ListView, animate: Boolean) {
        for (childIndex in 0 until listView.childCount) {
            val position = listView.firstVisiblePosition + childIndex
            val album = itemAt(position) ?: continue
            val child = listView.getChildAt(childIndex) ?: continue
            val selected = album.songs.any { it.mediaId == currentMediaId }
            child.findViewById<TextView>(R.id.listview_item_line_one)?.setTextColor(
                if (selected) LegacyAlbumSelectedTextColor else LegacyAlbumPrimaryTextColor,
            )
            (child as? EditableLayout)?.bindLegacyEditState(
                enabled = editMode,
                checked = album.id in selectedAlbumIds,
                animate = animate,
            )
        }
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_album_layout, parent, false)
        val album = items[position]
        view.tag = album.id
        view.findViewById<ImageView>(R.id.listview_item_image)?.apply {
            tag = album.id
            bindLegacyAlbumArtwork(album, R.drawable.noalbumcover_120)
        }
        view.findViewById<View>(R.id.iv_mask_albumcover)?.setBackgroundResource(R.drawable.mask_albumcover_list)
        val selected = album.songs.any { it.mediaId == currentMediaId }
        view.findViewById<TextView>(R.id.listview_item_line_one)?.apply {
            text = album.title
            setTextColor(if (selected) LegacyAlbumSelectedTextColor else LegacyAlbumPrimaryTextColor)
        }
        view.findViewById<TextView>(R.id.listview_item_line_two)?.apply {
            text = album.artist
            setTextColor(LegacyAlbumSecondaryTextColor)
        }
        (view as? EditableLayout)?.bindLegacyEditState(
            enabled = editMode,
            checked = album.id in selectedAlbumIds,
            animate = false,
        )
        return view
    }
}

private class LegacyAlbumGridAdapter : BaseAdapter() {
    private var items: List<AlbumSummary> = emptyList()
    private var currentMediaId: String? = null
    private var editMode: Boolean = false
    private var selectedAlbumIds: Set<String> = emptySet()

    fun updateItems(
        nextItems: List<AlbumSummary>,
        nextCurrentMediaId: String?,
        nextEditMode: Boolean,
        nextSelectedAlbumIds: Set<String>,
    ) {
        if (
            items == nextItems &&
            currentMediaId == nextCurrentMediaId &&
            editMode == nextEditMode &&
            selectedAlbumIds == nextSelectedAlbumIds
        ) {
            return
        }
        items = nextItems
        currentMediaId = nextCurrentMediaId
        editMode = nextEditMode
        selectedAlbumIds = nextSelectedAlbumIds
        notifyDataSetChanged()
    }

    fun itemAt(position: Int): AlbumSummary? = items.getOrNull(position)

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: createGridItem(parent)
        val album = items[position]
        view.tag = album.id
        view.findViewById<ImageView>(R.id.gridview_image)?.apply {
            tag = album.id
            bindLegacyAlbumArtwork(album, R.drawable.noalbumcover_220)
        }
        view.findViewById<TextView>(R.id.tv_album_name)?.apply {
            text = album.title
            setTextColor(if (album.songs.any { it.mediaId == currentMediaId }) LegacyAlbumSelectedTextColor else Color.BLACK)
            visibility = View.VISIBLE
        }
        view.findViewById<ImageView>(R.id.empty_selected_view)?.apply {
            setImageResource(R.drawable.albums_selected_large_empty)
            visibility = if (editMode) View.VISIBLE else View.GONE
            alpha = if (editMode) 1f else 0f
        }
        view.findViewById<ImageView>(R.id.check_view)?.apply {
            setImageResource(R.drawable.albums_selected_large)
            visibility = if (editMode) View.VISIBLE else View.GONE
            alpha = if (album.id in selectedAlbumIds) 1f else 0f
        }
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationX = 0f
        view.translationY = 0f
        return view
    }

    private fun createGridItem(parent: ViewGroup): View {
        val context = parent.context
        val coverSize = parent.resources.getDimensionPixelSize(R.dimen.gridview_item_ccontainer_height)
        return LinearLayout(context).apply {
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL
            setDuplicateParentStateEnabled(true)
            setPadding(0, parent.resources.getDimensionPixelSize(R.dimen.gridview_padding_top2), 0, 0)
            layoutParams = AbsListView.LayoutParams(
                AbsListView.LayoutParams.MATCH_PARENT,
                AbsListView.LayoutParams.WRAP_CONTENT,
            )
            addView(
                FrameLayout(context).apply {
                    id = R.id.edit_zone
                    addView(
                        ImageView(context).apply {
                            id = R.id.gridview_image
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            cropToPadding = true
                        },
                        FrameLayout.LayoutParams(coverSize, coverSize),
                    )
                    addView(
                        ImageView(context).apply {
                            id = R.id.empty_selected_view
                            scaleType = ImageView.ScaleType.FIT_XY
                        },
                        FrameLayout.LayoutParams(coverSize, coverSize),
                    )
                    addView(
                        ImageView(context).apply {
                            id = R.id.check_view
                            scaleType = ImageView.ScaleType.FIT_XY
                        },
                        FrameLayout.LayoutParams(coverSize, coverSize),
                    )
                },
                LinearLayout.LayoutParams(coverSize, coverSize),
            )
            addView(
                TextView(context).apply {
                    id = R.id.tv_album_name
                    gravity = Gravity.CENTER
                    maxLines = 1
                    setSingleLine(true)
                    textSize = 13f
                    setTextColor(Color.BLACK)
                },
                LinearLayout.LayoutParams(coverSize, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
        }
    }
}

private class LegacyAlbumViewSwitchAnimator {
    private var animator: Animator? = null
    private val interpolator = DecelerateInterpolator()

    fun animate(
        root: LegacyAlbumRoot,
        from: AlbumViewMode,
        to: AlbumViewMode,
    ) {
        animator?.cancel()
        if (from == AlbumViewMode.List && to == AlbumViewMode.Tile) {
            animateListToGrid(root)
        } else if (from == AlbumViewMode.Tile && to == AlbumViewMode.List) {
            animateGridToList(root)
        } else {
            root.showModeImmediately(to)
        }
    }

    private fun animateListToGrid(root: LegacyAlbumRoot) {
        val listView = root.listView
        val gridView = root.gridView
        gridView.setSelection(listView.firstVisiblePosition)
        gridView.visibility = View.VISIBLE
        gridView.post {
            val animators = mutableListOf<Animator>()
            for (index in 0 until gridView.childCount) {
                val gridChild = gridView.getChildAt(index) ?: continue
                val albumId = gridChild.tag as? String ?: continue
                val listCover = listView.findCoverByAlbumId(albumId)
                val gridCover = gridChild.findViewById<View>(R.id.gridview_image) ?: continue
                val start = listCover?.boundsOnScreen()
                val target = gridCover.boundsOnScreen()
                gridChild.findViewById<View>(R.id.tv_album_name)?.visibility = View.INVISIBLE
                if (start != null) {
                    gridChild.pivotX = 0f
                    gridChild.pivotY = 0f
                    gridChild.translationX = (start.left - target.left).toFloat()
                    gridChild.translationY = (start.top - target.top).toFloat()
                    gridChild.scaleX = start.width().toFloat() / target.width().coerceAtLeast(1)
                    gridChild.scaleY = start.height().toFloat() / target.height().coerceAtLeast(1)
                }
                animators += ObjectAnimator.ofPropertyValuesHolder(
                    gridChild,
                    android.animation.PropertyValuesHolder.ofFloat(View.TRANSLATION_X, gridChild.translationX, 0f),
                    android.animation.PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, gridChild.translationY, 0f),
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, gridChild.scaleX, 1f),
                    android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, gridChild.scaleY, 1f),
                ).apply {
                    duration = AlbumSwitchBaseDurationMillis + index * AlbumSwitchStaggerMillis
                    interpolator = this@LegacyAlbumViewSwitchAnimator.interpolator
                }
            }
            listView.visibility = View.INVISIBLE
            animator = AnimatorSet().apply {
                playTogether(animators)
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            resetGridChildren(gridView)
                        }
                    },
                )
                start()
            }
        }
    }

    private fun animateGridToList(root: LegacyAlbumRoot) {
        val listView = root.listView
        val gridView = root.gridView
        listView.setSelection(gridView.firstVisiblePosition)
        listView.alpha = 0f
        listView.visibility = View.VISIBLE
        listView.post {
            val animators = mutableListOf<Animator>()
            for (index in 0 until gridView.childCount) {
                val gridChild = gridView.getChildAt(index) ?: continue
                val albumId = gridChild.tag as? String ?: continue
                val listCover = listView.findCoverByAlbumId(albumId)
                val gridCover = gridChild.findViewById<View>(R.id.gridview_image) ?: continue
                val start = gridCover.boundsOnScreen()
                val target = listCover?.boundsOnScreen()
                if (target != null) {
                    gridChild.pivotX = 0f
                    gridChild.pivotY = 0f
                    animators += ObjectAnimator.ofPropertyValuesHolder(
                        gridChild,
                        android.animation.PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, (target.left - start.left).toFloat()),
                        android.animation.PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, (target.top - start.top).toFloat()),
                        android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, target.width().toFloat() / start.width().coerceAtLeast(1)),
                        android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, target.height().toFloat() / start.height().coerceAtLeast(1)),
                    ).apply {
                        duration = AlbumSwitchBaseDurationMillis + index * AlbumSwitchStaggerMillis
                        interpolator = this@LegacyAlbumViewSwitchAnimator.interpolator
                    }
                }
            }
            animator = AnimatorSet().apply {
                playTogether(animators)
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            resetGridChildren(gridView)
                            gridView.visibility = View.GONE
                            listView.animate().alpha(1f).setDuration(AlbumListFadeInMillis).start()
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            resetGridChildren(gridView)
                        }
                    },
                )
                start()
            }
        }
    }

    private fun resetGridChildren(gridView: GridView) {
        for (index in 0 until gridView.childCount) {
            gridView.getChildAt(index)?.apply {
                translationX = 0f
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
                findViewById<View>(R.id.tv_album_name)?.visibility = View.VISIBLE
            }
        }
    }
}

private fun ListView.findCoverByAlbumId(albumId: String): View? {
    for (index in 0 until childCount) {
        val child = getChildAt(index) ?: continue
        if (child.tag == albumId) {
            return child.findViewById(R.id.listview_item_image)
        }
    }
    return null
}

private fun View.boundsOnScreen(): Rect {
    val location = IntArray(2)
    getLocationOnScreen(location)
    return Rect(
        location[0],
        location[1],
        location[0] + width,
        location[1] + height,
    )
}

private fun ImageView.bindLegacyAlbumArtwork(
    album: AlbumSummary,
    fallbackRes: Int,
) {
    scaleType = ImageView.ScaleType.CENTER_CROP
    setImageResource(fallbackRes)
    val artworkUri = album.representative.mediaMetadata.artworkUri
        ?: album.representative.mediaMetadata.extras
            ?.getLong(LocalAudioLibrary.AlbumIdExtraKey)
            ?.takeIf { it > 0L }
            ?.let(LocalAudioLibrary::albumArtworkUri)
    setImageUriOrFallback(artworkUri, fallbackRes)
}

private fun ImageView.setImageUriOrFallback(
    uri: Uri?,
    fallbackRes: Int,
) {
    if (uri == null) {
        setImageResource(fallbackRes)
        return
    }
    runCatching {
        setImageURI(uri)
    }.onFailure {
        setImageResource(fallbackRes)
    }
    if (drawable == null) {
        setImageResource(fallbackRes)
    }
}
