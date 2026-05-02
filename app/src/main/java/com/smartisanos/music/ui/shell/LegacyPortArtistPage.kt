package com.smartisanos.music.ui.shell

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
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
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.replaceQueueAndPlay
import com.smartisanos.music.ui.album.AlbumSummary
import com.smartisanos.music.ui.album.AlbumViewMode
import com.smartisanos.music.ui.album.buildAlbumSummaries
import com.smartisanos.music.ui.artist.ArtistSummary
import com.smartisanos.music.ui.artist.buildArtistSummaries
import java.text.Collator
import java.util.Locale
import kotlin.random.Random

private const val ArtistAlbumSwitchBaseDurationMillis = 150L
private const val ArtistAlbumSwitchStaggerMillis = 10L
private const val LegacyArtistListFooterThreshold = 8
private val LegacyArtistPrimaryTextColor = Color.rgb(0x35, 0x35, 0x39)
private val LegacyArtistSecondaryTextColor = Color.rgb(0xa4, 0xa7, 0xac)
private val LegacyArtistFooterTextColor = Color.rgb(0xbc, 0xbc, 0xbc)

internal sealed interface LegacyArtistTarget {
    val artistId: String
    val artistName: String
    val title: String

    data class Albums(
        override val artistId: String,
        override val artistName: String,
    ) : LegacyArtistTarget {
        override val title: String = artistName
    }

    data class AllSongs(
        override val artistId: String,
        override val artistName: String,
        override val title: String,
    ) : LegacyArtistTarget

    data class Album(
        override val artistId: String,
        override val artistName: String,
        val albumId: String,
        override val title: String,
        val fromArtistAlbums: Boolean,
    ) : LegacyArtistTarget
}

internal fun LegacyArtistTarget.parentTarget(): LegacyArtistTarget? {
    return when (this) {
        is LegacyArtistTarget.Albums -> null
        is LegacyArtistTarget.AllSongs -> LegacyArtistTarget.Albums(
            artistId = artistId,
            artistName = artistName,
        )
        is LegacyArtistTarget.Album -> if (fromArtistAlbums) {
            LegacyArtistTarget.Albums(
                artistId = artistId,
                artistName = artistName,
            )
        } else {
            null
        }
    }
}

internal val LegacyArtistTarget.showsAlbumSwitch: Boolean
    get() = this is LegacyArtistTarget.Albums

@Composable
internal fun LegacyPortArtistPage(
    mediaItems: List<MediaItem>,
    active: Boolean,
    selectedTarget: LegacyArtistTarget?,
    albumViewMode: AlbumViewMode,
    hiddenMediaIds: Set<String>,
    onTargetChanged: (LegacyArtistTarget?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val browser = LocalPlaybackBrowser.current
    val visibleSongs = remember(mediaItems, hiddenMediaIds) {
        mediaItems.filterNot { mediaItem -> mediaItem.mediaId in hiddenMediaIds }
    }
    val artists = remember(visibleSongs, context) {
        buildArtistSummaries(
            mediaItems = visibleSongs,
            unknownArtistTitle = context.getString(R.string.unknown_artist),
            unknownAlbumTitle = context.getString(R.string.unknown_album),
        )
    }
    val selectedArtist = remember(artists, selectedTarget) {
        selectedTarget?.artistId?.let { artistId ->
            artists.firstOrNull { artist -> artist.id == artistId }
        }
    }
    val selectedArtistAlbums = remember(selectedArtist, context) {
        selectedArtist?.albumSummaries(context).orEmpty()
    }
    val selectedArtistState = remember(selectedArtist, selectedArtistAlbums, selectedTarget) {
        if (selectedArtist != null && selectedTarget != null) {
            LegacySelectedArtistState(
                artist = selectedArtist,
                target = selectedTarget,
                albums = selectedArtistAlbums,
            )
        } else {
            null
        }
    }
    val switchAnimator = remember { LegacyArtistAlbumViewSwitchAnimator() }

    LegacyPortPageStackTransition(
        secondaryKey = selectedArtistState,
        modifier = modifier,
        label = "legacy artist transition",
        primaryContent = {
            LegacyPortArtistOverviewPage(
                active = active,
                artists = artists,
                onArtistSelected = { artist ->
                    val albums = artist.albumSummaries(context)
                    val target = if (albums.size > 1) {
                        LegacyArtistTarget.Albums(
                            artistId = artist.id,
                            artistName = artist.name,
                        )
                    } else {
                        val album = albums.firstOrNull()
                        if (album == null) {
                            LegacyArtistTarget.AllSongs(
                                artistId = artist.id,
                                artistName = artist.name,
                                title = context.getString(R.string.artist_all_songs),
                            )
                        } else {
                            LegacyArtistTarget.Album(
                                artistId = artist.id,
                                artistName = artist.name,
                                albumId = album.id,
                                title = album.title,
                                fromArtistAlbums = false,
                            )
                        }
                    }
                    onTargetChanged(target)
                },
                modifier = Modifier.fillMaxSize(),
            )
        },
        secondaryContent = { state ->
            LegacyPortSelectedArtistPage(
                artist = state.artist,
                albums = state.albums.ifEmpty { state.artist.albumSummaries(context) },
                target = selectedTarget?.takeIf { target -> target.artistId == state.artist.id } ?: state.target,
                browser = browser,
                albumViewMode = albumViewMode,
                onTargetChanged = onTargetChanged,
                switchAnimator = switchAnimator,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

private data class LegacySelectedArtistState(
    val artist: ArtistSummary,
    val target: LegacyArtistTarget,
    val albums: List<AlbumSummary>,
)

@Composable
internal fun LegacyPortArtistTitleStack(
    selectedTarget: LegacyArtistTarget?,
    modifier: Modifier = Modifier,
    content: @Composable (LegacyArtistTarget?, Modifier) -> Unit,
) {
    val titleEntry = selectedTarget?.toTitleStackEntry()
    LegacyPortPageStackTransition(
        secondaryKey = titleEntry,
        modifier = modifier,
        label = "legacy artist title transition",
        primaryContent = {
            content(null, Modifier.fillMaxSize())
        },
        secondaryContent = { entry ->
            when (entry) {
                is LegacyArtistTitleStackEntry.Direct -> {
                    content(entry.target, Modifier.fillMaxSize())
                }
                is LegacyArtistTitleStackEntry.ArtistRoot -> {
                    val nestedTarget = selectedTarget?.takeIf { target ->
                        target.artistId == entry.artistId && target !is LegacyArtistTarget.Albums
                    }
                    LegacyPortPageStackTransition(
                        secondaryKey = nestedTarget,
                        modifier = Modifier.fillMaxSize(),
                        label = "legacy artist nested title transition",
                        primaryContent = {
                            content(
                                LegacyArtistTarget.Albums(
                                    artistId = entry.artistId,
                                    artistName = entry.artistName,
                                ),
                                Modifier.fillMaxSize(),
                            )
                        },
                        secondaryContent = { target ->
                            content(target, Modifier.fillMaxSize())
                        },
                    )
                }
            }
        },
    )
}

private sealed interface LegacyArtistTitleStackEntry {
    data class ArtistRoot(
        val artistId: String,
        val artistName: String,
    ) : LegacyArtistTitleStackEntry

    data class Direct(
        val target: LegacyArtistTarget.Album,
    ) : LegacyArtistTitleStackEntry
}

private fun LegacyArtistTarget.toTitleStackEntry(): LegacyArtistTitleStackEntry {
    return when (this) {
        is LegacyArtistTarget.Album -> if (fromArtistAlbums) {
            LegacyArtistTitleStackEntry.ArtistRoot(
                artistId = artistId,
                artistName = artistName,
            )
        } else {
            LegacyArtistTitleStackEntry.Direct(this)
        }
        else -> LegacyArtistTitleStackEntry.ArtistRoot(
            artistId = artistId,
            artistName = artistName,
        )
    }
}

@Composable
private fun LegacyPortArtistOverviewPage(
    active: Boolean,
    artists: List<ArtistSummary>,
    onArtistSelected: (ArtistSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            LegacyArtistOverviewRoot(viewContext)
        },
        update = { root ->
            root.visibility = if (active) View.VISIBLE else View.INVISIBLE
            val adapter = root.listView.adapter as? LegacyArtistOverviewAdapter
                ?: LegacyArtistOverviewAdapter().also { adapter ->
                    root.listView.adapter = adapter
                }
            if (adapter.updateItems(artists)) {
                root.listView.scheduleLayoutAnimation()
            }
            root.bindFooter(artists.size)
            root.listView.setOnItemClickListener { _, _, position, _ ->
                adapter.itemAt(position)?.let(onArtistSelected)
            }
        },
    )
}

private class LegacyArtistOverviewRoot(context: Context) : FrameLayout(context) {
    val listView: ListView
    private val footer = LegacyArtistFooterView(context)

    init {
        setBackgroundResource(R.drawable.account_background)
        listView = ListView(context).apply {
            id = R.id.list
            divider = ColorDrawable(context.getColor(R.color.listview_divider_color))
            dividerHeight = resources.getDimensionPixelSize(R.dimen.listview_dividerHeight)
            selector = context.getDrawable(R.drawable.listview_selector)
            cacheColorHint = Color.TRANSPARENT
            setBackgroundResource(R.drawable.account_background)
            setPadding(0, 0, 0, resources.legacyArtistScrollBottomPadding())
            clipToPadding = false
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.list_anim_layout)
            addFooterView(footer, null, false)
        }
        addView(
            listView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
    }

    fun bindFooter(artistCount: Int) {
        footer.bind(
            text = context.getString(R.string.legacy_artist_count, artistCount),
            visible = artistCount >= LegacyArtistListFooterThreshold,
        )
    }
}

private class LegacyArtistOverviewAdapter : BaseAdapter() {
    private var items: List<ArtistSummary> = emptyList()

    fun updateItems(nextItems: List<ArtistSummary>): Boolean {
        if (items == nextItems) {
            return false
        }
        items = nextItems
        notifyDataSetChanged()
        return true
    }

    fun itemAt(position: Int): ArtistSummary? = items.getOrNull(position)

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.artist_listview_items_text, parent, false)
        val artist = items[position]
        view.setBackgroundColor(Color.WHITE)
        view.findViewById<TextView>(R.id.listview_item_line_one)?.apply {
            text = artist.name
            setTextColor(LegacyArtistPrimaryTextColor)
        }
        view.findViewById<TextView>(R.id.listview_item_line_two)?.apply {
            text = parent.context.getString(
                R.string.legacy_artist_summary,
                artist.albumCount,
                artist.trackCount,
            )
            setTextColor(LegacyArtistSecondaryTextColor)
        }
        return view
    }
}

@Composable
private fun LegacyPortSelectedArtistPage(
    artist: ArtistSummary,
    albums: List<AlbumSummary>,
    target: LegacyArtistTarget?,
    browser: Player?,
    albumViewMode: AlbumViewMode,
    onTargetChanged: (LegacyArtistTarget?) -> Unit,
    switchAnimator: LegacyArtistAlbumViewSwitchAnimator,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val directAlbumTarget = target as? LegacyArtistTarget.Album
    if (directAlbumTarget != null && !directAlbumTarget.fromArtistAlbums) {
        albums.firstOrNull { album -> album.id == directAlbumTarget.albumId }?.let { album ->
            LegacyPortAlbumDetailPage(
                album = album,
                modifier = modifier,
            )
            return
        }
    }

    val nestedTarget = target?.takeIf { currentTarget ->
        currentTarget.artistId == artist.id && currentTarget !is LegacyArtistTarget.Albums
    }
    val entries = remember(artist, albums, context) {
        buildArtistAlbumEntries(
            artist = artist,
            albums = albums,
            allSongsTitle = context.getString(R.string.artist_all_songs),
        )
    }
    LegacyPortPageStackTransition(
        secondaryKey = nestedTarget,
        modifier = modifier,
        label = "legacy selected artist transition",
        primaryContent = {
            LegacyPortArtistAlbumsPage(
                artist = artist,
                entries = entries,
                browser = browser,
                viewMode = albumViewMode,
                onTargetChanged = onTargetChanged,
                switchAnimator = switchAnimator,
                modifier = Modifier.fillMaxSize(),
            )
        },
        secondaryContent = { detailTarget ->
            when (detailTarget) {
                is LegacyArtistTarget.AllSongs -> {
                    LegacyPortArtistAllSongsPage(
                        artistName = artist.name,
                        songs = artist.songs,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                is LegacyArtistTarget.Album -> {
                    albums.firstOrNull { album -> album.id == detailTarget.albumId }?.let { album ->
                        LegacyPortAlbumDetailPage(
                            album = album,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                is LegacyArtistTarget.Albums -> Unit
            }
        },
    )
}

@Composable
private fun LegacyPortArtistAlbumsPage(
    artist: ArtistSummary,
    entries: List<LegacyArtistAlbumEntry>,
    browser: Player?,
    viewMode: AlbumViewMode,
    onTargetChanged: (LegacyArtistTarget?) -> Unit,
    switchAnimator: LegacyArtistAlbumViewSwitchAnimator,
    modifier: Modifier = Modifier,
) {
    var currentMediaId by remember(browser) {
        mutableStateOf(browser?.currentMediaItem?.mediaId)
    }

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
            LegacyArtistAlbumsRoot(viewContext)
        },
        update = { root ->
            val listAdapter = root.listView.adapter as? LegacyArtistAlbumListAdapter
                ?: LegacyArtistAlbumListAdapter(root.artworkLoader).also { adapter ->
                    root.listView.adapter = adapter
                }
            val gridAdapter = root.gridView.adapter as? LegacyArtistAlbumGridAdapter
                ?: LegacyArtistAlbumGridAdapter(root.artworkLoader).also { adapter ->
                    root.gridView.adapter = adapter
                }
            val listContentChanged = listAdapter.updateItems(
                nextItems = entries,
                nextCurrentMediaId = currentMediaId,
            )
            val gridContentChanged = gridAdapter.updateItems(
                nextItems = entries,
                nextCurrentMediaId = currentMediaId,
            )
            if (!listContentChanged) {
                listAdapter.updateVisibleRows(root.listView)
            }
            if (!gridContentChanged) {
                gridAdapter.updateVisibleRows(root.gridView)
            }

            root.listView.setOnItemClickListener { _, _, position, _ ->
                listAdapter.itemAt(position)?.toTarget(
                    artist = artist,
                    allSongsTitle = root.context.getString(R.string.artist_all_songs),
                )?.let(onTargetChanged)
            }
            root.gridView.setOnItemClickListener { _, _, position, _ ->
                gridAdapter.itemAt(position)?.toTarget(
                    artist = artist,
                    allSongsTitle = root.context.getString(R.string.artist_all_songs),
                )?.let(onTargetChanged)
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

private class LegacyArtistAlbumsRoot(context: Context) : FrameLayout(context) {
    val listHost: FrameLayout
    val listView: ListView
    val gridView: GridView
    val artworkLoader = LegacyAlbumArtworkLoader(context)
    var viewMode: AlbumViewMode? = null

    init {
        setBackgroundResource(R.drawable.account_background)
        listHost = FrameLayout(context).apply {
            setBackgroundResource(R.drawable.account_background)
            visibility = View.VISIBLE
        }
        addView(
            listHost,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )

        listView = ListView(context).apply {
            id = R.id.list
            divider = ColorDrawable(context.getColor(R.color.listview_divider_color))
            dividerHeight = resources.getDimensionPixelSize(R.dimen.listview_dividerHeight)
            selector = context.getDrawable(R.drawable.listview_selector)
            cacheColorHint = Color.TRANSPARENT
            setBackgroundResource(R.drawable.account_background)
            setPadding(0, 0, 0, resources.legacyArtistScrollBottomPadding())
            clipToPadding = false
            layoutAnimation = AnimationUtils.loadLayoutAnimation(context, R.anim.list_anim_layout)
        }
        listHost.addView(
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
                resources.legacyArtistScrollBottomPadding(),
            )
            clipToPadding = false
            visibility = View.GONE
        }
        addView(
            gridView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
    }

    fun showModeImmediately(mode: AlbumViewMode) {
        listHost.visibility = if (mode == AlbumViewMode.List) View.VISIBLE else View.GONE
        listHost.alpha = 1f
        listView.visibility = View.VISIBLE
        listView.alpha = 1f
        gridView.visibility = if (mode == AlbumViewMode.Tile) View.VISIBLE else View.GONE
        gridView.alpha = 1f
    }

    override fun onDetachedFromWindow() {
        artworkLoader.clear()
        super.onDetachedFromWindow()
    }
}

private class LegacyArtistAlbumListAdapter(
    private val artworkLoader: LegacyAlbumArtworkLoader,
) : BaseAdapter() {
    private var items: List<LegacyArtistAlbumEntry> = emptyList()
    private var currentMediaId: String? = null

    fun updateItems(
        nextItems: List<LegacyArtistAlbumEntry>,
        nextCurrentMediaId: String?,
    ): Boolean {
        val contentChanged = items != nextItems
        val stateChanged = currentMediaId != nextCurrentMediaId
        if (!contentChanged && !stateChanged) {
            return false
        }
        items = nextItems
        currentMediaId = nextCurrentMediaId
        if (contentChanged) {
            notifyDataSetChanged()
        }
        return contentChanged
    }

    fun itemAt(position: Int): LegacyArtistAlbumEntry? = items.getOrNull(position)

    fun updateVisibleRows(listView: ListView) {
        for (childIndex in 0 until listView.childCount) {
            val position = listView.firstVisiblePosition + childIndex
            val item = itemAt(position) ?: continue
            val child = listView.getChildAt(childIndex) ?: continue
            bindState(child, item)
        }
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(parent.context)
            .inflate(R.layout.artist_listview_items, parent, false)
        val item = items[position]
        view.setBackgroundColor(Color.WHITE)
        view.tag = item.stableId
        view.findViewById<ImageView>(R.id.listview_item_image)?.apply {
            setTag(R.string.add_track, position)
            when (item) {
                is LegacyArtistAlbumEntry.AllSongs -> {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setTag(R.id.legacy_album_artwork_request, null)
                    setImageResource(R.drawable.noalbumcover_all_songs)
                }
                is LegacyArtistAlbumEntry.Album -> bindLegacyAlbumArtwork(
                    album = item.album,
                    fallbackRes = R.drawable.noalbumcover_120,
                    sizePx = parent.resources.getDimensionPixelSize(R.dimen.album_list_item_image_width),
                    artworkLoader = artworkLoader,
                )
            }
        }
        view.findViewById<View>(R.id.iv_mask_albumcover)?.visibility =
            if (item is LegacyArtistAlbumEntry.AllSongs) View.GONE else View.VISIBLE
        view.findViewById<View>(R.id.iv_mask_albumcover)?.setBackgroundResource(R.drawable.mask_albumcover_list)
        bindState(view, item)
        return view
    }

    private fun bindState(view: View, item: LegacyArtistAlbumEntry) {
        val selected = item.songs.any { song -> song.mediaId == currentMediaId }
        view.findViewById<TextView>(R.id.listview_item_line_one)?.apply {
            text = item.title
            setTextColor(if (selected) Color.rgb(0xe6, 0x40, 0x40) else LegacyArtistPrimaryTextColor)
        }
        view.findViewById<TextView>(R.id.listview_item_line_two)?.apply {
            text = context.getString(R.string.album_track_count, item.trackCount)
            setTextColor(LegacyArtistSecondaryTextColor)
        }
    }
}

private class LegacyArtistAlbumGridAdapter(
    private val artworkLoader: LegacyAlbumArtworkLoader,
) : BaseAdapter() {
    private var items: List<LegacyArtistAlbumEntry> = emptyList()
    private var currentMediaId: String? = null

    fun updateItems(
        nextItems: List<LegacyArtistAlbumEntry>,
        nextCurrentMediaId: String?,
    ): Boolean {
        val contentChanged = items != nextItems
        val stateChanged = currentMediaId != nextCurrentMediaId
        if (!contentChanged && !stateChanged) {
            return false
        }
        items = nextItems
        currentMediaId = nextCurrentMediaId
        if (contentChanged) {
            notifyDataSetChanged()
        }
        return contentChanged
    }

    fun itemAt(position: Int): LegacyArtistAlbumEntry? = items.getOrNull(position)

    fun updateVisibleRows(gridView: GridView) {
        for (childIndex in 0 until gridView.childCount) {
            val position = gridView.firstVisiblePosition + childIndex
            val item = itemAt(position) ?: continue
            val child = gridView.getChildAt(childIndex) ?: continue
            bindState(child, item)
        }
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: createGridItem(parent)
        val item = items[position]
        view.tag = item.stableId
        view.findViewById<ImageView>(R.id.gridview_image)?.apply {
            setTag(R.string.add_track, position)
            when (item) {
                is LegacyArtistAlbumEntry.AllSongs -> {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setTag(R.id.legacy_album_artwork_request, null)
                    setImageResource(R.drawable.noalbumcover_all_songs2)
                }
                is LegacyArtistAlbumEntry.Album -> bindLegacyAlbumArtwork(
                    album = item.album,
                    fallbackRes = R.drawable.noalbumcover_220,
                    sizePx = parent.resources.getDimensionPixelSize(R.dimen.gridview_item_ccontainer_height),
                    artworkLoader = artworkLoader,
                )
            }
        }
        bindState(view, item)
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationX = 0f
        view.translationY = 0f
        return view
    }

    private fun bindState(view: View, item: LegacyArtistAlbumEntry) {
        val selected = item.songs.any { song -> song.mediaId == currentMediaId }
        view.findViewById<TextView>(R.id.tv_album_name)?.apply {
            text = item.title
            setTextColor(if (selected) Color.rgb(0xe6, 0x40, 0x40) else Color.BLACK)
            visibility = View.VISIBLE
        }
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
                },
                LinearLayout.LayoutParams(coverSize, coverSize),
            )
            addView(
                TextView(context).apply {
                    id = R.id.tv_album_name
                    gravity = Gravity.CENTER
                    ellipsize = TextUtils.TruncateAt.END
                    setSingleLine(true)
                    textSize = 13f
                    setTextColor(Color.BLACK)
                },
                LinearLayout.LayoutParams(coverSize, LinearLayout.LayoutParams.WRAP_CONTENT),
            )
        }
    }
}

@Composable
private fun LegacyPortArtistAllSongsPage(
    artistName: String,
    songs: List<MediaItem>,
    modifier: Modifier = Modifier,
) {
    val browser = LocalPlaybackBrowser.current
    val sortedSongs = remember(songs) {
        songs.sortedWith(artistAllSongsComparator())
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            LegacyArtistAllSongsRoot(context)
        },
        update = { root ->
            root.bindHeader(
                title = root.context.getString(R.string.artist_all_songs),
                artistName = artistName,
                enabled = sortedSongs.isNotEmpty(),
                onShuffle = {
                    if (sortedSongs.isNotEmpty()) {
                        val startIndex = Random.nextInt(sortedSongs.size)
                        browser.replaceQueueAndPlay(
                            mediaItems = sortedSongs,
                            startIndex = startIndex,
                            shuffleModeEnabled = true,
                        )
                    }
                },
            )
            val adapter = root.listView.adapter as? LegacyAlbumTrackAdapter
                ?: LegacyAlbumTrackAdapter().also { adapter ->
                    root.listView.adapter = adapter
                }
            val contentChanged = adapter.updateItems(
                nextItems = sortedSongs,
                nextCurrentMediaId = browser?.currentMediaItem?.mediaId,
                nextCurrentIsPlaying = browser?.isPlaying == true,
                nextShowTrackArtists = true,
                nextForceSequentialTrackNumbers = true,
            )
            root.bindPlayback(browser, adapter)
            if (!contentChanged) {
                adapter.updateVisibleRows(root.listView)
            }
            root.listView.setOnItemClickListener { _, _, position, _ ->
                val trackIndex = position - root.listView.headerViewsCount
                if (trackIndex < 0 || trackIndex >= sortedSongs.size) {
                    return@setOnItemClickListener
                }
                browser.replaceQueueAndPlay(sortedSongs, trackIndex)
            }
        },
    )
}

private class LegacyArtistAllSongsRoot(context: Context) : FrameLayout(context) {
    val listView: ListView
    private val header = LegacyArtistAllSongsHeader(context)
    private var playbackPlayer: Player? = null
    private var playbackListener: Player.Listener? = null

    init {
        setBackgroundResource(R.drawable.account_background)
        listView = ListView(context).apply {
            id = R.id.list
            divider = ColorDrawable(context.getColor(R.color.listview_divider_color))
            dividerHeight = resources.getDimensionPixelSize(R.dimen.listview_dividerHeight)
            selector = context.getDrawable(R.drawable.listview_selector)
            cacheColorHint = Color.TRANSPARENT
            setBackgroundResource(R.drawable.account_background)
            isVerticalScrollBarEnabled = false
            setHeaderDividersEnabled(false)
            addHeaderView(header, null, false)
        }
        addView(
            listView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
    }

    fun bindPlayback(
        player: Player?,
        adapter: LegacyAlbumTrackAdapter,
    ) {
        if (playbackPlayer === player) {
            return
        }
        playbackListener?.let { listener ->
            playbackPlayer?.removeListener(listener)
        }
        playbackPlayer = player
        playbackListener = null
        if (player == null) {
            return
        }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (adapter.setPlaybackState(player.currentMediaItem?.mediaId, player.isPlaying)) {
                    adapter.updateVisibleRows(listView)
                }
            }
        }
        playbackListener = listener
        player.addListener(listener)
    }

    fun bindHeader(
        title: String,
        artistName: String,
        enabled: Boolean,
        onShuffle: () -> Unit,
    ) {
        header.bind(
            title = title,
            artistName = artistName,
            enabled = enabled,
            onShuffle = onShuffle,
        )
    }

    override fun onDetachedFromWindow() {
        playbackListener?.let { listener ->
            playbackPlayer?.removeListener(listener)
        }
        playbackListener = null
        playbackPlayer = null
        super.onDetachedFromWindow()
    }
}

private class LegacyArtistAllSongsHeader(context: Context) : RelativeLayout(context) {
    private val albumImage = ImageView(context)
    private val albumName = TextView(context)
    private val albumArtist = TextView(context)
    private val shuffle = ImageButton(context)

    init {
        setBackgroundResource(R.drawable.ablum_crosstexture_bg)
        layoutParams = AbsListView.LayoutParams(
            AbsListView.LayoutParams.MATCH_PARENT,
            dp(150),
        )

        val coverSize = resources.getDimensionPixelSize(R.dimen.gridview_item_ccontainer_height)
        val coverContainer = FrameLayout(context).apply {
            id = R.id.fl_album_image
            albumImage.apply {
                id = R.id.album_image
                scaleType = ImageView.ScaleType.CENTER_CROP
                cropToPadding = true
                setImageResource(R.drawable.noalbumcover_all_songs2)
            }
            addView(
                albumImage,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
        }
        addView(
            coverContainer,
            LayoutParams(coverSize, coverSize).apply {
                leftMargin = resources.getDimensionPixelSize(R.dimen.gridview_margin)
                addRule(ALIGN_PARENT_LEFT)
                addRule(CENTER_VERTICAL)
            },
        )

        addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(resources.getDimensionPixelSize(R.dimen.alum_tile_paddingleft), 0, 0, 0)
                albumName.legacyArtistHeaderText(
                    id = R.id.album_name,
                    textSizePx = resources.getDimension(R.dimen.text_size_medium),
                    color = LegacyArtistPrimaryTextColor,
                )
                addView(
                    albumName,
                    LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.listview_items_header_width),
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
                albumArtist.legacyArtistHeaderText(
                    id = R.id.album_artist_name,
                    textSizePx = resources.getDimension(R.dimen.text_size_small),
                    color = LegacyArtistPrimaryTextColor,
                )
                addView(
                    albumArtist,
                    LinearLayout.LayoutParams(
                        resources.getDimensionPixelSize(R.dimen.listview_items_header_width),
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ),
                )
            },
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                addRule(RIGHT_OF, R.id.fl_album_image)
                addRule(CENTER_VERTICAL)
            },
        )

        shuffle.apply {
            setBackgroundResource(R.drawable.btn_album_shuffle3_selector)
            contentDescription = context.getString(R.string.play_shuffle)
        }
        addView(
            shuffle,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                rightMargin = dp(12)
                bottomMargin = dp(6)
                addRule(ALIGN_PARENT_RIGHT)
                addRule(ALIGN_PARENT_BOTTOM)
            },
        )

        addView(
            View(context).apply {
                setBackgroundResource(R.drawable.ablum_crosstexture_bg_shadow)
            },
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(6),
            ).apply {
                addRule(ALIGN_PARENT_BOTTOM)
            },
        )
    }

    fun bind(
        title: String,
        artistName: String,
        enabled: Boolean,
        onShuffle: () -> Unit,
    ) {
        albumName.text = title
        albumArtist.text = artistName
        shuffle.isEnabled = enabled
        shuffle.alpha = if (enabled) 1f else 0.3f
        shuffle.setOnClickListener {
            if (enabled) {
                onShuffle()
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

private fun TextView.legacyArtistHeaderText(
    id: Int,
    textSizePx: Float,
    color: Int,
) {
    this.id = id
    gravity = Gravity.CENTER_VERTICAL
    ellipsize = TextUtils.TruncateAt.END
    isSingleLine = true
    setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx)
    setTextColor(color)
}

private sealed class LegacyArtistAlbumEntry {
    abstract val stableId: String
    abstract val title: String
    abstract val songs: List<MediaItem>

    val trackCount: Int
        get() = songs.size

    data class AllSongs(
        override val stableId: String,
        override val title: String,
        override val songs: List<MediaItem>,
    ) : LegacyArtistAlbumEntry()

    data class Album(
        val album: AlbumSummary,
    ) : LegacyArtistAlbumEntry() {
        override val stableId: String = album.id
        override val title: String = album.title
        override val songs: List<MediaItem> = album.songs
    }
}

private fun buildArtistAlbumEntries(
    artist: ArtistSummary,
    albums: List<AlbumSummary>,
    allSongsTitle: String,
): List<LegacyArtistAlbumEntry> {
    return listOf(
        LegacyArtistAlbumEntry.AllSongs(
            stableId = "${artist.id}:all",
            title = allSongsTitle,
            songs = artist.songs,
        ),
    ) + albums.map { album -> LegacyArtistAlbumEntry.Album(album) }
}

private fun LegacyArtistAlbumEntry.toTarget(
    artist: ArtistSummary,
    allSongsTitle: String,
): LegacyArtistTarget {
    return when (this) {
        is LegacyArtistAlbumEntry.AllSongs -> LegacyArtistTarget.AllSongs(
            artistId = artist.id,
            artistName = artist.name,
            title = allSongsTitle,
        )
        is LegacyArtistAlbumEntry.Album -> LegacyArtistTarget.Album(
            artistId = artist.id,
            artistName = artist.name,
            albumId = album.id,
            title = album.title,
            fromArtistAlbums = true,
        )
    }
}

private fun ArtistSummary.albumSummaries(context: Context): List<AlbumSummary> {
    return buildAlbumSummaries(
        mediaItems = songs,
        unknownAlbumTitle = context.getString(R.string.unknown_album),
        multipleArtistsTitle = context.getString(R.string.many_artist),
    )
}

private fun artistAllSongsComparator(): Comparator<MediaItem> {
    val collator = Collator.getInstance(Locale.CHINA).apply {
        strength = Collator.PRIMARY
    }
    return Comparator { left, right ->
        val leftTitle = left.artistAllSongsSortTitle()
        val rightTitle = right.artistAllSongsSortTitle()
        val localized = collator.compare(leftTitle, rightTitle)
        if (localized != 0) {
            localized
        } else {
            leftTitle.lowercase(Locale.ROOT).compareTo(rightTitle.lowercase(Locale.ROOT))
        }
    }
}

private fun MediaItem.artistAllSongsSortTitle(): String {
    return (mediaMetadata.displayTitle ?: mediaMetadata.title)
        ?.toString()
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: mediaId
}

private class LegacyArtistFooterView(context: Context) : LinearLayout(context) {
    private val content = TextView(context).apply {
        gravity = Gravity.CENTER
        setTextColor(LegacyArtistFooterTextColor)
        textSize = 15f
        typeface = Typeface.DEFAULT
        setBackgroundResource(R.drawable.account_background)
        val verticalPadding = (8f * resources.displayMetrics.density).toInt()
        setPadding(0, verticalPadding, 0, verticalPadding)
    }

    init {
        orientation = VERTICAL
        setBackgroundResource(R.drawable.account_background)
        addView(
            content,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            ),
        )
        bind(text = "", visible = false)
    }

    fun bind(text: String, visible: Boolean) {
        content.text = text
        content.visibility = if (visible) View.VISIBLE else View.GONE
    }
}

private class LegacyArtistAlbumViewSwitchAnimator {
    private var animator: Animator? = null
    private val interpolator = DecelerateInterpolator()
    private var generation: Int = 0

    fun animate(
        root: LegacyArtistAlbumsRoot,
        from: AlbumViewMode?,
        to: AlbumViewMode,
    ) {
        generation += 1
        val currentGen = generation
        animator?.cancel()
        animator = null
        if (from == AlbumViewMode.List && to == AlbumViewMode.Tile) {
            animateListToGrid(root, currentGen)
        } else if (from == AlbumViewMode.Tile && to == AlbumViewMode.List) {
            animateGridToList(root, currentGen)
        } else {
            root.showModeImmediately(to)
        }
    }

    private fun animateListToGrid(root: LegacyArtistAlbumsRoot, gen: Int) {
        val listHost = root.listHost
        val listView = root.listView
        val gridView = root.gridView
        val firstVisiblePosition = listView.firstVisiblePosition
        val lastVisiblePosition = listView.lastVisiblePosition
        listHost.animate().cancel()
        listHost.visibility = View.VISIBLE
        listHost.alpha = 1f
        gridView.alpha = 0f
        afterNextLayout(gridView, gen) {
            if (gen != generation) return@afterNextLayout
            val animators = mutableListOf<Animator>()
            val gridFirstVisiblePosition = gridView.firstVisiblePosition
            for (index in 0 until gridView.childCount) {
                val gridChild = gridView.getChildAt(index) ?: continue
                val gridCover = gridChild.findViewById<View>(R.id.gridview_image) ?: continue
                val position = gridCover.getTag(R.string.add_track) as? Int ?: continue
                val animationOrder = artistGridAnimationOrder(
                    position = position,
                    firstVisiblePosition = gridFirstVisiblePosition,
                    fallbackIndex = index,
                )
                val listCover = listView.findArtistCoverByPosition(position)
                gridChild.prepareForArtistAlbumSwitch()
                gridChild.applyArtistListToGridStart(
                    listView = listView,
                    listCover = listCover,
                    gridCover = gridCover,
                    targetPosition = position,
                    listFirstPosition = firstVisiblePosition,
                    listLastPosition = lastVisiblePosition,
                )
                animators += ObjectAnimator.ofPropertyValuesHolder(
                    gridChild,
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_X, gridChild.translationX, 0f),
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, gridChild.translationY, 0f),
                    PropertyValuesHolder.ofFloat(View.SCALE_X, gridChild.scaleX, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, gridChild.scaleY, 1f),
                ).apply {
                    duration = ArtistAlbumSwitchBaseDurationMillis
                    startDelay = animationOrder * ArtistAlbumSwitchStaggerMillis
                    interpolator = this@LegacyArtistAlbumViewSwitchAnimator.interpolator
                }
            }
            if (animators.isEmpty()) {
                gridView.alpha = 1f
                listHost.visibility = View.GONE
                resetGridChildren(gridView)
                return@afterNextLayout
            }
            val nextAnimator = AnimatorSet().apply {
                playTogether(animators)
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationStart(animation: Animator) {
                            gridView.alpha = 1f
                            listHost.visibility = View.GONE
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            resetGridChildren(gridView)
                            (gridView.adapter as? BaseAdapter)?.notifyDataSetChanged()
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            resetGridChildren(gridView)
                            (gridView.adapter as? BaseAdapter)?.notifyDataSetChanged()
                        }
                    },
                )
            }
            animator = nextAnimator
            nextAnimator.start()
        }
        gridView.setSelection(firstVisiblePosition)
        gridView.visibility = View.VISIBLE
        gridView.requestLayout()
    }

    private fun animateGridToList(root: LegacyArtistAlbumsRoot, gen: Int) {
        val listHost = root.listHost
        val listView = root.listView
        val gridView = root.gridView
        val firstVisiblePosition = gridView.firstVisiblePosition
        listHost.animate().cancel()
        listHost.alpha = 0f
        listHost.visibility = View.VISIBLE
        listView.alpha = 1f
        listView.visibility = View.VISIBLE
        afterNextLayout(listView, gen) {
            if (gen != generation) return@afterNextLayout
            val animators = mutableListOf<Animator>()
            val hiddenListTargets = mutableSetOf<View>()
            val gridFirstVisiblePosition = gridView.firstVisiblePosition
            val listFirstPosition = listView.firstVisiblePosition
            val listLastPosition = listView.lastVisiblePosition
            for (index in 0 until gridView.childCount) {
                val gridChild = gridView.getChildAt(index) ?: continue
                val gridCover = gridChild.findViewById<View>(R.id.gridview_image) ?: continue
                val position = gridCover.getTag(R.string.add_track) as? Int ?: continue
                val animationOrder = artistGridAnimationOrder(
                    position = position,
                    firstVisiblePosition = gridFirstVisiblePosition,
                    fallbackIndex = index,
                )
                val listCover = listView.findArtistCoverByPosition(position)
                gridChild.prepareForArtistAlbumSwitch()
                val target = gridChild.artistGridToListTarget(
                    listView = listView,
                    listCover = listCover,
                    gridView = gridView,
                    gridCover = gridCover,
                    targetPosition = position,
                    listFirstPosition = listFirstPosition,
                    listLastPosition = listLastPosition,
                )
                animators += ObjectAnimator.ofPropertyValuesHolder(
                    gridChild,
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_X, 0f, target.translationX),
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, target.translationY),
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, target.scaleX),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, target.scaleY),
                ).apply {
                    duration = ArtistAlbumSwitchBaseDurationMillis
                    startDelay = animationOrder * ArtistAlbumSwitchStaggerMillis
                    interpolator = this@LegacyArtistAlbumViewSwitchAnimator.interpolator
                    addListener(
                        object : android.animation.AnimatorListenerAdapter() {
                            override fun onAnimationStart(animation: Animator) {
                                listCover?.artistParentView()?.let { listTarget ->
                                    listTarget.visibility = View.INVISIBLE
                                    hiddenListTargets += listTarget
                                }
                            }

                            override fun onAnimationEnd(animation: Animator) {
                                listCover?.artistParentView()?.visibility = View.VISIBLE
                            }

                            override fun onAnimationCancel(animation: Animator) {
                                listCover?.artistParentView()?.visibility = View.VISIBLE
                            }
                        },
                    )
                }
            }
            if (animators.isEmpty()) {
                listHost.alpha = 1f
                gridView.visibility = View.GONE
                resetGridChildren(gridView)
                return@afterNextLayout
            }
            val nextAnimator = AnimatorSet().apply {
                playTogether(animators)
                addListener(
                    object : android.animation.AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            finishGridToList(listHost, gridView, hiddenListTargets)
                        }

                        override fun onAnimationCancel(animation: Animator) {
                            finishGridToList(listHost, gridView, hiddenListTargets)
                        }
                    },
                )
            }
            animator = nextAnimator
            nextAnimator.start()
        }
        listView.setSelectionFromTop(firstVisiblePosition, 0)
        listView.requestLayout()
    }

    private fun afterNextLayout(
        view: View,
        gen: Int,
        block: () -> Unit,
    ) {
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (view.viewTreeObserver.isAlive) {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
                if (gen == generation) {
                    block()
                }
            }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
    }

    private fun finishGridToList(
        listHost: View,
        gridView: GridView,
        hiddenListTargets: Set<View>,
    ) {
        hiddenListTargets.forEach { target ->
            target.visibility = View.VISIBLE
        }
        listHost.animate().cancel()
        listHost.alpha = 1f
        gridView.visibility = View.GONE
        resetGridChildren(gridView)
    }

    private fun resetGridChildren(gridView: GridView) {
        for (index in 0 until gridView.childCount) {
            gridView.getChildAt(index)?.apply {
                translationX = 0f
                translationY = 0f
                scaleX = 1f
                scaleY = 1f
                alpha = 1f
                setLayerType(View.LAYER_TYPE_NONE, null)
                findViewById<View>(R.id.tv_album_name)?.visibility = View.VISIBLE
            }
        }
    }
}

private data class ArtistAlbumSwitchTarget(
    val translationX: Float,
    val translationY: Float,
    val scaleX: Float,
    val scaleY: Float,
)

private fun artistGridAnimationOrder(
    position: Int,
    firstVisiblePosition: Int,
    fallbackIndex: Int,
): Long {
    val order = position - firstVisiblePosition
    return if (order >= 0) order.toLong() else fallbackIndex.toLong()
}

private fun ListView.findArtistCoverByPosition(position: Int): View? {
    for (index in 0 until childCount) {
        val child = getChildAt(index) ?: continue
        val cover = child.findViewById<View>(R.id.listview_item_image) ?: continue
        if (cover.getTag(R.string.add_track) == position) {
            return cover
        }
    }
    return null
}

private fun View.prepareForArtistAlbumSwitch() {
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    pivotX = 0f
    pivotY = 0f
    findViewById<View>(R.id.tv_album_name)?.visibility = View.INVISIBLE
}

private fun View.applyArtistListToGridStart(
    listView: ListView,
    listCover: View?,
    gridCover: View,
    targetPosition: Int,
    listFirstPosition: Int,
    listLastPosition: Int,
) {
    val sampleListCover = listView.getChildAt(0)?.findViewById<View>(R.id.listview_item_image)
    val sourceWidth = sampleListCover?.width?.takeIf { it > 0 } ?: gridCover.width.coerceAtLeast(1)
    val sourceHeight = sampleListCover?.height?.takeIf { it > 0 } ?: gridCover.height.coerceAtLeast(1)
    val gridInnerWidth = (gridCover.width - gridCover.paddingTop * 2).coerceAtLeast(1)
    val gridInnerHeight = (gridCover.height - gridCover.paddingTop * 2).coerceAtLeast(1)
    scaleX = sourceWidth.toFloat() / gridInnerWidth
    scaleY = sourceHeight.toFloat() / gridInnerHeight
    if (listCover != null && targetPosition in listFirstPosition..listLastPosition) {
        val gridCoverOffsetX = gridCover.leftRelativeToArtist(this) + gridCover.paddingTop
        val gridCoverOffsetY = gridCover.topRelativeToArtist(this) + gridCover.paddingTop
        translationX = listCover.leftRelativeToArtist(listView) - left - gridCoverOffsetX * scaleX
        translationY = listCover.topRelativeToArtist(listView) - top - gridCoverOffsetY * scaleY
        return
    }
    translationX = -left.toFloat()
    translationY = when {
        targetPosition > listLastPosition -> top.toFloat()
        targetPosition < listFirstPosition -> -listView.lastArtistChildTop() / 2f
        else -> -top.toFloat()
    }
}

private fun View.artistGridToListTarget(
    listView: ListView,
    listCover: View?,
    gridView: GridView,
    gridCover: View,
    targetPosition: Int,
    listFirstPosition: Int,
    listLastPosition: Int,
): ArtistAlbumSwitchTarget {
    val gridInnerWidth = (gridCover.width - gridCover.paddingTop * 2).coerceAtLeast(1)
    val gridInnerHeight = (gridCover.height - gridCover.paddingTop * 2).coerceAtLeast(1)
    val sampleListCover = listCover ?: listView.getChildAt(0)?.findViewById<View>(R.id.listview_item_image)
    val scaleX = (sampleListCover?.width ?: gridInnerWidth).toFloat() / gridInnerWidth
    val scaleY = (sampleListCover?.height ?: gridInnerHeight).toFloat() / gridInnerHeight
    if (listCover != null && targetPosition in listFirstPosition..listLastPosition) {
        val gridChildX = leftRelativeToArtist(gridView)
        val gridChildY = topRelativeToArtist(gridView)
        val gridCoverX = gridCover.leftRelativeToArtist(gridView) + gridCover.paddingTop
        val gridCoverY = gridCover.topRelativeToArtist(gridView) + gridCover.paddingTop
        val listCoverX = listCover.leftRelativeToArtist(listView)
        val listCoverY = listCover.topRelativeToArtist(listView)
        return ArtistAlbumSwitchTarget(
            translationX = listCoverX - (gridChildX + (gridCoverX - gridChildX) * scaleX),
            translationY = listCoverY - (gridChildY + (gridCoverY - gridChildY) * scaleY),
            scaleX = scaleX,
            scaleY = scaleY,
        )
    }
    return ArtistAlbumSwitchTarget(
        translationX = -left.toFloat(),
        translationY = when {
            targetPosition > listLastPosition -> listView.lastArtistChildTop().toFloat()
            targetPosition < listFirstPosition -> -listView.lastArtistChildTop() / 2f
            else -> -top.toFloat()
        },
        scaleX = scaleX,
        scaleY = scaleY,
    )
}

private fun android.content.res.Resources.legacyArtistScrollBottomPadding(): Int {
    val playbackBarHeight = getDimensionPixelSize(R.dimen.play_back_content_height)
    val playbackShadowHeight = (6f * displayMetrics.density).toInt()
    val contentBottomCompensation = (6f * displayMetrics.density).toInt()
    return playbackBarHeight + playbackShadowHeight + contentBottomCompensation
}

private fun ListView.lastArtistChildTop(): Int {
    return getChildAt(childCount - 1)?.top ?: 0
}

private fun View.artistParentView(): View? = parent as? View

private fun View.leftRelativeToArtist(ancestor: View): Float {
    var result = left.toFloat()
    var current = parent as? View
    while (current != null && current !== ancestor) {
        result += current.left - current.scrollX
        current = current.parent as? View
    }
    return result
}

private fun View.topRelativeToArtist(ancestor: View): Float {
    var result = top.toFloat()
    var current = parent as? View
    while (current != null && current !== ancestor) {
        result += current.top - current.scrollY
        current = current.parent as? View
    }
    return result
}
