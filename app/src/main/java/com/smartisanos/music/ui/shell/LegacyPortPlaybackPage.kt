package com.smartisanos.music.ui.shell

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.data.favorite.FavoriteSongsRepository
import com.smartisanos.music.data.settings.PlaybackSettings
import com.smartisanos.music.playback.LocalPlaybackController
import com.smartisanos.music.ui.playback.PlaybackScreen
import kotlinx.coroutines.launch
import smartisanos.widget.TitleBar
import kotlin.math.abs

@Composable
internal fun LegacyPortPlaybackPage(
    playbackSettings: PlaybackSettings,
    onScratchEnabledChange: (Boolean) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val controller = LocalPlaybackController.current
    val titleState = rememberLegacyPlaybackTitleState()
    val favoriteRepository = remember(context.applicationContext) {
        FavoriteSongsRepository.getInstance(context.applicationContext)
    }
    val favoriteIds by favoriteRepository.observeFavoriteIds().collectAsState(initial = emptySet())
    var queueSnapshot by remember(controller, context, favoriteIds) {
        mutableStateOf(controller.toLegacyPlaybackQueueSnapshot(context, favoriteIds))
    }
    var queueVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val titleContentHeight = dimensionResource(R.dimen.titlebar_height)
    val density = LocalDensity.current
    val statusBarHeight = with(density) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    val titleTopPadding = statusBarHeight + titleContentHeight

    DisposableEffect(controller, context, favoriteIds) {
        val playbackController = controller ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                queueSnapshot = player.toLegacyPlaybackQueueSnapshot(context, favoriteIds)
            }
        }
        playbackController.addListener(listener)
        queueSnapshot = playbackController.toLegacyPlaybackQueueSnapshot(context, favoriteIds)
        onDispose {
            playbackController.removeListener(listener)
        }
    }

    BackHandler(enabled = queueVisible) {
        queueVisible = false
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(ComposeColor.White),
    ) {
        val screenHeightPx = with(density) { maxHeight.roundToPx() }
        val playerOffsetY by animateFloatAsState(
            targetValue = if (queueVisible) screenHeightPx.toFloat() else 0f,
            animationSpec = tween(
                durationMillis = LegacyQueueRevealDurationMillis,
                easing = { fraction ->
                    val inverse = 1f - fraction
                    1f - inverse * inverse * inverse
                },
            ),
            label = "legacy playback queue reveal",
        )

        LegacyPlaybackQueueLayer(
            snapshot = queueSnapshot,
            onItemClick = { queueIndex ->
                controller?.seekToDefaultPosition(queueIndex)
            },
            onFavoriteCurrentClick = {
                val mediaId = queueSnapshot.current?.mediaId.orEmpty()
                if (mediaId.isNotBlank()) {
                    scope.launch {
                        favoriteRepository.toggle(mediaId)
                    }
                }
            },
            onClearUpcomingClick = {
                val playbackController = controller
                if (playbackController != null) {
                    val playingIndex = playbackController.currentMediaItemIndex
                    val itemCount = playbackController.mediaItemCount
                    if (playingIndex >= 0 && playingIndex + 1 < itemCount) {
                        playbackController.removeMediaItems(playingIndex + 1, itemCount)
                    }
                }
            },
            onMoveRequest = { fromIndex, toIndex ->
                val playbackController = controller
                if (playbackController != null && fromIndex != toIndex) {
                    val itemCount = playbackController.mediaItemCount
                    if (fromIndex in 0 until itemCount && toIndex in 0 until itemCount) {
                        playbackController.moveMediaItem(fromIndex, toIndex)
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = statusBarHeight)
                .zIndex(0f),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
                .graphicsLayer {
                    translationY = playerOffsetY
                },
        ) {
            PlaybackScreen(
                playbackSettings = playbackSettings,
                onScratchEnabledChange = onScratchEnabledChange,
                onCollapse = onCollapse,
                showTopBar = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = titleTopPadding),
            )
        }
        LegacyPlaybackTitleBarShadow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = titleTopPadding)
                .height(dimensionResource(R.dimen.title_bar_shadow_height))
                .zIndex(2f),
        )
        LegacyPortPlaybackTitleBar(
            title = titleState.title,
            artist = titleState.artist,
            queueVisible = queueVisible,
            onCollapse = onCollapse,
            onQueueClick = {
                queueVisible = !queueVisible
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(3f),
        )
    }
}

@Composable
private fun LegacyPlaybackTitleBarShadow(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            FrameLayout(context).apply {
                clipChildren = false
                addView(
                    View(context).apply {
                        setBackgroundResource(R.drawable.title_bar_shadow)
                    },
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                    ),
                )
                addView(
                    View(context).apply {
                        setBackgroundResource(R.drawable.search_bar_background)
                    },
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        resources.getDimensionPixelSize(R.dimen.bar_divider_height),
                        Gravity.TOP,
                    ),
                )
            }
        },
    )
}

@Composable
private fun LegacyPortPlaybackTitleBar(
    title: String,
    artist: String,
    queueVisible: Boolean,
    onCollapse: () -> Unit,
    onQueueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleContentHeight = dimensionResource(R.dimen.titlebar_height)
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
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(titleContentHeight),
            factory = { context ->
                TitleBar(context).apply {
                    setTitleBarHeight(resources.getDimensionPixelSize(R.dimen.titlebar_height))
                    setShadowVisible(false)
                    setBackgroundColor(Color.WHITE)
                }
            },
            update = { titleBar ->
                titleBar.setupLegacyPlaybackTitleBar(
                    title = title,
                    artist = artist,
                    queueVisible = queueVisible,
                    onCollapse = onCollapse,
                    onQueueClick = onQueueClick,
                )
            },
        )
    }
}

private fun TitleBar.setupLegacyPlaybackTitleBar(
    title: String,
    artist: String,
    queueVisible: Boolean,
    onCollapse: () -> Unit,
    onQueueClick: () -> Unit,
) {
    setTitleBarHeight(resources.getDimensionPixelSize(R.dimen.titlebar_height))
    setShadowVisible(false)
    setBackgroundColor(Color.WHITE)
    removeAllLeftViews()
    removeAllRightViews()
    if (queueVisible) {
        setCenterText(context.getString(R.string.playlist_title))
    } else {
        setCenterView(
            PlaybackCenterTitle(context).apply {
                setTitle(title)
                setSubTitle(artist)
            },
        )
    }
    addLeftImageView(R.drawable.btn_current_playing_back_selector).apply {
        contentDescription = context.getString(R.string.playback_left_btn_content_description)
        setOnClickListener {
            onCollapse()
        }
    }
    addRightImageView(R.drawable.btn_current_playing_check_selector).apply {
        contentDescription = context.getString(
            if (queueVisible) {
                R.string.playqueue_btn_hide_content_description
            } else {
                R.string.playqueue_btn_show_content_description
            },
        )
        setOnClickListener {
            onQueueClick()
        }
    }
}

@Composable
private fun LegacyPlaybackQueueLayer(
    snapshot: LegacyPlaybackQueueSnapshot,
    onItemClick: (Int) -> Unit,
    onFavoriteCurrentClick: () -> Unit,
    onClearUpcomingClick: () -> Unit,
    onMoveRequest: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            LegacyPlaybackQueueView(context)
        },
        update = { view ->
            view.bind(
                snapshot = snapshot,
                callbacks = LegacyPlaybackQueueCallbacks(
                    onItemClick = onItemClick,
                    onFavoriteCurrentClick = onFavoriteCurrentClick,
                    onClearUpcomingClick = onClearUpcomingClick,
                    onMoveRequest = onMoveRequest,
                ),
            )
        },
    )
}

@Composable
private fun rememberLegacyPlaybackTitleState(): LegacyPlaybackTitleState {
    val context = LocalContext.current
    val controller = LocalPlaybackController.current
    var titleState by remember(controller, context) {
        mutableStateOf(controller.toLegacyPlaybackTitleState(context))
    }

    DisposableEffect(controller, context) {
        val playbackController = controller ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                titleState = player.toLegacyPlaybackTitleState(context)
            }
        }
        playbackController.addListener(listener)
        titleState = playbackController.toLegacyPlaybackTitleState(context)
        onDispose {
            playbackController.removeListener(listener)
        }
    }

    return titleState
}

private data class LegacyPlaybackTitleState(
    val title: String,
    val artist: String,
)

private data class LegacyPlaybackQueueSnapshot(
    val history: List<LegacyPlaybackQueueTrack> = emptyList(),
    val current: LegacyPlaybackQueueTrack? = null,
    val upcoming: List<LegacyPlaybackQueueTrack> = emptyList(),
    val isCurrentFavorite: Boolean = false,
)

private data class LegacyPlaybackQueueTrack(
    val queueIndex: Int,
    val mediaId: String,
    val title: String,
    val artist: String,
    val mediaItem: MediaItem,
)

private data class LegacyDragTouch(
    val track: LegacyPlaybackQueueTrack,
    val adapterPosition: Int,
    val child: View,
)

private data class LegacyDragBitmap(
    val bitmap: Bitmap,
    val paddingTop: Int,
    val paddingBottom: Int,
)

private data class LegacyPlaybackQueueCallbacks(
    val onItemClick: (Int) -> Unit,
    val onFavoriteCurrentClick: () -> Unit,
    val onClearUpcomingClick: () -> Unit,
    val onMoveRequest: (Int, Int) -> Unit,
)

private fun Player?.toLegacyPlaybackTitleState(context: Context): LegacyPlaybackTitleState {
    val metadata = this?.currentMediaItem?.mediaMetadata
    val title = metadata?.displayTitle?.toString()?.takeIf(String::isNotBlank)
        ?: metadata?.title?.toString()?.takeIf(String::isNotBlank)
        ?: context.getString(R.string.unknown_song_title)
    val artist = metadata?.artist?.toString()
        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
        ?: metadata?.albumArtist?.toString()
            ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
        ?: ""
    return LegacyPlaybackTitleState(title = title, artist = artist)
}

private fun Player?.toLegacyPlaybackQueueSnapshot(
    context: Context,
    favoriteIds: Set<String>,
): LegacyPlaybackQueueSnapshot {
    val player = this ?: return LegacyPlaybackQueueSnapshot()
    val itemCount = player.mediaItemCount
    if (itemCount <= 0) {
        val current = player.currentMediaItem?.toLegacyPlaybackQueueTrack(context, 0)
        return LegacyPlaybackQueueSnapshot(
            current = current,
            isCurrentFavorite = current?.mediaId?.let(favoriteIds::contains) == true,
        )
    }
    val currentIndex = player.currentMediaItemIndex.takeIf { it in 0 until itemCount } ?: -1
    val tracks = (0 until itemCount).mapNotNull { index ->
        runCatching {
            player.getMediaItemAt(index).toLegacyPlaybackQueueTrack(context, index)
        }.getOrNull()
    }
    val current = tracks.firstOrNull { it.queueIndex == currentIndex }
        ?: player.currentMediaItem?.toLegacyPlaybackQueueTrack(context, currentIndex.coerceAtLeast(0))
    return LegacyPlaybackQueueSnapshot(
        history = tracks.filter { it.queueIndex < currentIndex }.takeLast(LegacyQueueHistoryLimit),
        current = current,
        upcoming = tracks.filter { it.queueIndex > currentIndex },
        isCurrentFavorite = current?.mediaId?.let(favoriteIds::contains) == true,
    )
}

private fun MediaItem.toLegacyPlaybackQueueTrack(
    context: Context,
    queueIndex: Int,
): LegacyPlaybackQueueTrack {
    val metadata = mediaMetadata
    val title = metadata.displayTitle?.toString()?.takeIf(String::isNotBlank)
        ?: metadata.title?.toString()?.takeIf(String::isNotBlank)
        ?: context.getString(R.string.unknown_song_title)
    val artist = metadata.artist?.toString()
        ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
        ?: metadata.albumArtist?.toString()
            ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", ignoreCase = true) }
        ?: ""
    return LegacyPlaybackQueueTrack(
        queueIndex = queueIndex,
        mediaId = mediaId,
        title = title,
        artist = artist,
        mediaItem = this,
    )
}

private class LegacyPlaybackQueueView(context: Context) : FrameLayout(context) {
    private val queueAdapter = LegacyPlaybackQueueAdapter(context)
    private val listView: ListView
    private val dragInterpolator = android.animation.TimeInterpolator { fraction ->
        if (fraction < LegacyQueueDragInterpolatorPivot) {
            2f * fraction * fraction
        } else {
            val inverse = fraction - 1f
            1f - 2f * inverse * inverse
        }
    }
    private var callbacks = LegacyPlaybackQueueCallbacks(
        onItemClick = {},
        onFavoriteCurrentClick = {},
        onClearUpcomingClick = {},
        onMoveRequest = { _, _ -> },
    )
    private var dragSource: LegacyPlaybackQueueTrack? = null
    private var dragSourceAdapterPosition = ListView.INVALID_POSITION
    private var dragTargetAdapterPosition = ListView.INVALID_POSITION
    private var dragStartRawY = 0f
    private var dragTouchOffsetY = 0
    private var dragFloatPaddingTop = 0
    private var dragRowHeight = 0
    private var dragFloatView: ImageView? = null
    private var dragging = false
    private var finishingDrag = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        clipChildren = false
        clipToPadding = false
        LayoutInflater.from(context).inflate(R.layout.legacy_now_playing_layout, this, true)
        listView = findViewById<ListView>(R.id.list).apply {
            clipChildren = false
            clipToPadding = false
            adapter = queueAdapter
            setOnTouchListener { _, event ->
                handleListTouch(event)
            }
            setOnItemClickListener { _, _, position, _ ->
                queueAdapter.trackAt(position)?.let { track ->
                    this@LegacyPlaybackQueueView.callbacks.onItemClick(track.queueIndex)
                }
            }
        }
    }

    fun bind(
        snapshot: LegacyPlaybackQueueSnapshot,
        callbacks: LegacyPlaybackQueueCallbacks,
    ) {
        this.callbacks = callbacks
        queueAdapter.bind(snapshot, callbacks)
    }

    override fun onDetachedFromWindow() {
        queueAdapter.clear()
        super.onDetachedFromWindow()
    }

    private fun handleListTouch(event: MotionEvent): Boolean {
        if (finishingDrag) {
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val touchedItem = findTouchedDragHandleTrack(event) ?: return false
                dragSource = touchedItem.track
                dragSourceAdapterPosition = touchedItem.adapterPosition
                dragTargetAdapterPosition = touchedItem.adapterPosition
                dragStartRawY = event.rawY
                dragging = false
                beginDragVisual(touchedItem.child, event)
                listView.parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (dragSource == null) {
                    return false
                }
                updateDragFloatPosition(event, animate = false)
                if (abs(event.rawY - dragStartRawY) > touchSlop) {
                    dragging = true
                    updateDragTarget(event)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (dragSource == null) {
                    return false
                }
                updateDragFloatPosition(event, animate = false)
                if (dragging) {
                    updateDragTarget(event)
                }
                finishDrag(commitMove = dragging)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                if (dragSource == null) {
                    return false
                }
                finishDrag(commitMove = false)
                return true
            }
        }
        return false
    }

    private fun findTouchedDragHandleTrack(event: MotionEvent): LegacyDragTouch? {
        val position = listView.pointToPosition(event.x.toInt(), event.y.toInt())
        if (position == ListView.INVALID_POSITION) {
            return null
        }
        val track = queueAdapter.reorderableTrackAt(position) ?: return null
        val child = listView.getChildAt(position - listView.firstVisiblePosition) ?: return null
        val dragHandle = child.findViewById<View>(R.id.iv_right) ?: return null
        val hitRect = Rect()
        dragHandle.getHitRect(hitRect)
        hitRect.offset(child.left, child.top)
        hitRect.inset(-touchSlop, -touchSlop)
        return if (hitRect.contains(event.x.toInt(), event.y.toInt())) {
            LegacyDragTouch(track, position, child)
        } else {
            null
        }
    }

    private fun findDropTargetAdapterPosition(event: MotionEvent): Int {
        val y = event.y.toInt()
        val adapterPosition = listView.pointToPosition(listView.width / 2, y)
        if (adapterPosition != ListView.INVALID_POSITION) {
            queueAdapter.reorderableTrackAt(adapterPosition)?.let { return adapterPosition }
        }
        return when {
            y < firstReorderableChildTop() -> queueAdapter.firstReorderableAdapterPosition()
            y > lastReorderableChildBottom() -> queueAdapter.lastReorderableAdapterPosition()
            else -> nearestVisibleReorderableAdapterPosition(y)
        }
    }

    private fun beginDragVisual(child: View, event: MotionEvent) {
        clearDragFloatView()
        child.isPressed = false
        val dragBitmap = child.createDragBitmap()
        val listOffset = listViewOffsetInSelf()
        dragFloatPaddingTop = dragBitmap.paddingTop
        dragRowHeight = child.height
        dragTouchOffsetY = event.y.toInt() - child.top + dragFloatPaddingTop
        dragFloatView = ImageView(context).apply {
            setImageBitmap(dragBitmap.bitmap)
            alpha = LegacyQueueDragFloatAlpha
            layoutParams = LayoutParams(child.width, dragBitmap.bitmap.height).apply {
                leftMargin = listOffset.first + child.left
                topMargin = listOffset.second + child.top - dragFloatPaddingTop
            }
        }
        addView(dragFloatView)
        child.visibility = View.INVISIBLE
        updateDragFloatPosition(event, animate = false)
    }

    private fun updateDragFloatPosition(event: MotionEvent, animate: Boolean) {
        val floatView = dragFloatView ?: return
        val listOffset = listViewOffsetInSelf()
        val minY = listOffset.second - dragFloatPaddingTop
        val maxY = listOffset.second + listView.height - floatView.height
        val targetY = (listOffset.second + event.y.toInt() - dragTouchOffsetY).coerceIn(minY, maxY)
        if (animate) {
            floatView.animate()
                .y(targetY.toFloat())
                .setDuration(LegacyQueueDragSettleDurationMillis)
                .setInterpolator(dragInterpolator)
                .start()
        } else {
            floatView.y = targetY.toFloat()
        }
    }

    private fun updateDragTarget(event: MotionEvent) {
        val targetPosition = findDropTargetAdapterPosition(event)
        if (
            targetPosition == ListView.INVALID_POSITION ||
            targetPosition == dragTargetAdapterPosition ||
            queueAdapter.reorderableTrackAt(targetPosition) == null
        ) {
            return
        }
        dragTargetAdapterPosition = targetPosition
        animateVisibleRowsForDrag()
    }

    private fun animateVisibleRowsForDrag() {
        val sourcePosition = dragSourceAdapterPosition
        val targetPosition = dragTargetAdapterPosition
        if (sourcePosition == ListView.INVALID_POSITION || targetPosition == ListView.INVALID_POSITION) {
            return
        }
        val rowHeight = dragRowHeight.takeIf { it > 0 }
            ?: listView.getChildAt(sourcePosition - listView.firstVisiblePosition)?.height
            ?: resources.getDimensionPixelSize(R.dimen.listview_item_height)
        val firstVisible = listView.firstVisiblePosition
        repeat(listView.childCount) { childIndex ->
            val child = listView.getChildAt(childIndex)
            val adapterPosition = firstVisible + childIndex
            val targetTranslation = when {
                adapterPosition == sourcePosition -> 0f
                targetPosition > sourcePosition && adapterPosition in (sourcePosition + 1)..targetPosition -> -rowHeight.toFloat()
                targetPosition < sourcePosition && adapterPosition in targetPosition until sourcePosition -> rowHeight.toFloat()
                else -> 0f
            }
            if (adapterPosition == sourcePosition) {
                child.visibility = View.INVISIBLE
            } else {
                child.visibility = View.VISIBLE
            }
            child.animate()
                .translationY(targetTranslation)
                .setDuration(LegacyQueueDragShuffleDurationMillis)
                .setInterpolator(dragInterpolator)
                .start()
        }
    }

    private fun finishDrag(commitMove: Boolean) {
        val source = dragSource
        val targetPosition = dragTargetAdapterPosition
        val target = queueAdapter.reorderableTrackAt(targetPosition)
        val shouldMove = commitMove &&
            source != null &&
            target != null &&
            dragSourceAdapterPosition != ListView.INVALID_POSITION &&
            targetPosition != ListView.INVALID_POSITION &&
            source.queueIndex != target.queueIndex
        val settleY = adapterPositionTopInSelf(
            if (shouldMove) targetPosition else dragSourceAdapterPosition,
        ) ?: dragFloatView?.y?.toInt()
        finishingDrag = true
        val floatView = dragFloatView
        if (floatView == null) {
            completeDrag(shouldMove, source, target)
            return
        }
        val animator = floatView.animate()
            .setDuration(LegacyQueueDragSettleDurationMillis)
            .setInterpolator(dragInterpolator)
            .withEndAction {
                completeDrag(shouldMove, source, target)
            }
        if (settleY != null) {
            animator.y(settleY.toFloat())
        }
        animator.start()
    }

    private fun completeDrag(
        shouldMove: Boolean,
        source: LegacyPlaybackQueueTrack?,
        target: LegacyPlaybackQueueTrack?,
    ) {
        clearDragFloatView()
        resetVisibleRows()
        val sourcePosition = dragSourceAdapterPosition
        val targetPosition = dragTargetAdapterPosition
        resetDrag()
        if (
            shouldMove &&
            source != null &&
            target != null &&
            sourcePosition != ListView.INVALID_POSITION &&
            targetPosition != ListView.INVALID_POSITION
        ) {
            queueAdapter.movePreviewRow(sourcePosition, targetPosition)
            callbacks.onMoveRequest(source.queueIndex, target.queueIndex)
        }
    }

    private fun resetVisibleRows() {
        repeat(listView.childCount) { index ->
            listView.getChildAt(index).apply {
                animate().cancel()
                translationY = 0f
                visibility = View.VISIBLE
            }
        }
    }

    private fun clearDragFloatView() {
        dragFloatView?.let { floatView ->
            floatView.animate().cancel()
            floatView.setImageDrawable(null)
            removeView(floatView)
        }
        dragFloatView = null
    }

    private fun adapterPositionTopInSelf(adapterPosition: Int): Int? {
        if (adapterPosition == ListView.INVALID_POSITION) {
            return null
        }
        val child = listView.getChildAt(adapterPosition - listView.firstVisiblePosition) ?: return null
        return listViewOffsetInSelf().second + child.top - dragFloatPaddingTop
    }

    private fun firstReorderableChildTop(): Int {
        val firstPosition = queueAdapter.firstReorderableAdapterPosition()
        val child = listView.getChildAt(firstPosition - listView.firstVisiblePosition)
        return child?.top ?: 0
    }

    private fun lastReorderableChildBottom(): Int {
        val lastPosition = queueAdapter.lastReorderableAdapterPosition()
        val child = listView.getChildAt(lastPosition - listView.firstVisiblePosition)
        return child?.bottom ?: listView.height
    }

    private fun nearestVisibleReorderableAdapterPosition(y: Int): Int {
        var nearestPosition = ListView.INVALID_POSITION
        var nearestDistance = Int.MAX_VALUE
        val firstVisible = listView.firstVisiblePosition
        repeat(listView.childCount) { childIndex ->
            val adapterPosition = firstVisible + childIndex
            if (queueAdapter.reorderableTrackAt(adapterPosition) != null) {
                val child = listView.getChildAt(childIndex)
                val distance = abs(y - (child.top + child.height / 2))
                if (distance < nearestDistance) {
                    nearestDistance = distance
                    nearestPosition = adapterPosition
                }
            }
        }
        return nearestPosition
    }

    private fun listViewOffsetInSelf(): Pair<Int, Int> {
        val rootLocation = IntArray(2)
        val listLocation = IntArray(2)
        getLocationOnScreen(rootLocation)
        listView.getLocationOnScreen(listLocation)
        return (listLocation[0] - rootLocation[0]) to (listLocation[1] - rootLocation[1])
    }

    private fun resetDrag() {
        dragSource = null
        dragSourceAdapterPosition = ListView.INVALID_POSITION
        dragTargetAdapterPosition = ListView.INVALID_POSITION
        dragging = false
        dragTouchOffsetY = 0
        dragFloatPaddingTop = 0
        dragRowHeight = 0
        finishingDrag = false
        listView.parent?.requestDisallowInterceptTouchEvent(false)
    }

    private fun View.createDragBitmap(): LegacyDragBitmap {
        val shadowTop = resources.getDrawable(R.drawable.shadow_top, null)
        val shadowBottom = resources.getDrawable(R.drawable.shadow_bottom, null)
        val topHeight = shadowTop.intrinsicHeight.coerceAtLeast(0)
        val bottomHeight = shadowBottom.intrinsicHeight.coerceAtLeast(0)
        val bitmap = Bitmap.createBitmap(width, topHeight + height + bottomHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        shadowTop.setBounds(0, 0, width, topHeight)
        shadowTop.draw(canvas)
        canvas.save()
        canvas.translate(0f, topHeight.toFloat())
        draw(canvas)
        canvas.restore()
        shadowBottom.setBounds(0, topHeight + height, width, topHeight + height + bottomHeight)
        shadowBottom.draw(canvas)
        return LegacyDragBitmap(
            bitmap = bitmap,
            paddingTop = topHeight,
            paddingBottom = bottomHeight,
        )
    }
}

private class LegacyPlaybackQueueAdapter(
    context: Context,
) : BaseAdapter() {
    private val inflater = LayoutInflater.from(context)
    private val appContext = context.applicationContext
    private val artworkLoader = LegacyAlbumArtworkLoader(context)
    private var rows: List<LegacyPlaybackQueueRow> = emptyList()
    private var callbacks = LegacyPlaybackQueueCallbacks(
        onItemClick = {},
        onFavoriteCurrentClick = {},
        onClearUpcomingClick = {},
        onMoveRequest = { _, _ -> },
    )
    private var currentFavorite = false

    fun bind(
        snapshot: LegacyPlaybackQueueSnapshot,
        callbacks: LegacyPlaybackQueueCallbacks,
    ) {
        this.callbacks = callbacks
        currentFavorite = snapshot.isCurrentFavorite
        rows = buildList {
            if (snapshot.history.isNotEmpty()) {
                add(LegacyPlaybackQueueRow.Header(appContext.getString(R.string.history_title), false))
                snapshot.history.forEach { track ->
                    add(LegacyPlaybackQueueRow.Track(track, LegacyPlaybackQueueSection.History))
                }
            }
            add(LegacyPlaybackQueueRow.Header(appContext.getString(R.string.playing_title), false))
            snapshot.current?.let { current ->
                add(LegacyPlaybackQueueRow.Track(current, LegacyPlaybackQueueSection.Current))
            }
            add(LegacyPlaybackQueueRow.Header(appContext.getString(R.string.orginal_title), snapshot.upcoming.isNotEmpty()))
            snapshot.upcoming.forEach { track ->
                add(LegacyPlaybackQueueRow.Track(track, LegacyPlaybackQueueSection.Upcoming))
            }
        }
        notifyDataSetChanged()
    }

    override fun getCount(): Int = rows.size

    override fun getItem(position: Int): Any = rows[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getViewTypeCount(): Int = 3

    override fun getItemViewType(position: Int): Int {
        return when (val row = rows[position]) {
            is LegacyPlaybackQueueRow.Header -> 0
            is LegacyPlaybackQueueRow.Track -> if (row.section == LegacyPlaybackQueueSection.Current) 1 else 2
        }
    }

    override fun isEnabled(position: Int): Boolean {
        return rows[position] is LegacyPlaybackQueueRow.Track
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return when (val row = rows[position]) {
            is LegacyPlaybackQueueRow.Header -> getHeaderView(row, convertView, parent)
            is LegacyPlaybackQueueRow.Track -> {
                if (row.section == LegacyPlaybackQueueSection.Current) {
                    getCurrentTrackView(row.track, convertView, parent)
                } else {
                    getNormalTrackView(row, convertView, parent)
                }
            }
        }
    }

    fun clear() {
        artworkLoader.clear()
    }

    fun reorderableTrackAt(position: Int): LegacyPlaybackQueueTrack? {
        return (rows.getOrNull(position) as? LegacyPlaybackQueueRow.Track)
            ?.takeIf { it.section == LegacyPlaybackQueueSection.Upcoming }
            ?.track
    }

    fun trackAt(position: Int): LegacyPlaybackQueueTrack? {
        return (rows.getOrNull(position) as? LegacyPlaybackQueueRow.Track)?.track
    }

    fun firstReorderableTrack(): LegacyPlaybackQueueTrack? {
        return rows.asSequence()
            .filterIsInstance<LegacyPlaybackQueueRow.Track>()
            .firstOrNull { it.section == LegacyPlaybackQueueSection.Upcoming }
            ?.track
    }

    fun lastReorderableTrack(): LegacyPlaybackQueueTrack? {
        return rows.asReversed().asSequence()
            .filterIsInstance<LegacyPlaybackQueueRow.Track>()
            .firstOrNull { it.section == LegacyPlaybackQueueSection.Upcoming }
            ?.track
    }

    fun firstReorderableAdapterPosition(): Int {
        return rows.indexOfFirst { row ->
            row is LegacyPlaybackQueueRow.Track && row.section == LegacyPlaybackQueueSection.Upcoming
        }.takeIf { it >= 0 } ?: ListView.INVALID_POSITION
    }

    fun lastReorderableAdapterPosition(): Int {
        return rows.indexOfLast { row ->
            row is LegacyPlaybackQueueRow.Track && row.section == LegacyPlaybackQueueSection.Upcoming
        }.takeIf { it >= 0 } ?: ListView.INVALID_POSITION
    }

    fun movePreviewRow(fromPosition: Int, toPosition: Int) {
        if (
            fromPosition == toPosition ||
            reorderableTrackAt(fromPosition) == null ||
            reorderableTrackAt(toPosition) == null
        ) {
            return
        }
        rows = rows.toMutableList().apply {
            val row = removeAt(fromPosition)
            add(toPosition, row)
        }
        notifyDataSetChanged()
    }

    private fun getHeaderView(
        row: LegacyPlaybackQueueRow.Header,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val view = convertView ?: inflater.inflate(R.layout.legacy_now_playing_header_layout, parent, false)
        view.layoutParams = AbsListView.LayoutParams(
            AbsListView.LayoutParams.MATCH_PARENT,
            view.resources.getDimensionPixelSize(R.dimen.now_playing_header_height),
        )
        view.setBackgroundResource(R.drawable.list_title_bg)
        view.findViewById<TextView>(R.id.header_name).text = row.title
        view.findViewById<ImageButton>(R.id.clear_btn).apply {
            visibility = if (row.clearable) View.VISIBLE else View.GONE
            setOnClickListener {
                callbacks.onClearUpcomingClick()
            }
        }
        return view
    }

    private fun getCurrentTrackView(
        track: LegacyPlaybackQueueTrack,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val view = convertView ?: inflater.inflate(R.layout.legacy_playing_now_item, parent, false)
        view.setBackgroundResource(R.drawable.playing_queue_item_selector)
        bindTrackText(view, track)
        bindArtwork(
            imageView = view.findViewById(R.id.album_cover),
            track = track,
            fallbackRes = R.drawable.playing_cover_lp,
            sizePx = view.resources.getDimensionPixelSize(R.dimen.album_cover_zone_width),
        )
        view.findViewById<RatingBar>(R.id.rb_score).rating = 0f
        view.findViewById<CheckBox>(R.id.favorite).apply {
            isChecked = currentFavorite
            setOnClickListener {
                callbacks.onFavoriteCurrentClick()
            }
        }
        return view
    }

    private fun getNormalTrackView(
        row: LegacyPlaybackQueueRow.Track,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val track = row.track
        val view = convertView ?: inflater.inflate(R.layout.legacy_playlist_normal_item, parent, false)
        view.setBackgroundResource(R.drawable.playing_queue_item_selector)
        bindTrackText(view, track)
        bindArtwork(
            imageView = view.findViewById(R.id.album_cover),
            track = track,
            fallbackRes = R.drawable.noalbumcover_120,
            sizePx = view.resources.getDimensionPixelSize(R.dimen.listview_item_image_width),
        )
        view.isClickable = false
        view.isFocusable = false
        view.findViewById<View>(R.id.iv_right).apply {
            if (row.section == LegacyPlaybackQueueSection.Upcoming) {
                visibility = View.VISIBLE
            } else {
                visibility = View.INVISIBLE
            }
            isClickable = false
            isFocusable = false
        }
        return view
    }

    private fun bindTrackText(view: View, track: LegacyPlaybackQueueTrack) {
        view.findViewById<TextView>(R.id.first_line_view).text = track.title
        view.findViewById<TextView>(R.id.second_line_view).text = track.artist
    }

    private fun bindArtwork(
        imageView: ImageView,
        track: LegacyPlaybackQueueTrack,
        fallbackRes: Int,
        sizePx: Int,
    ) {
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP
        artworkLoader.bind(
            imageView = imageView,
            mediaItem = track.mediaItem,
            fallbackRes = fallbackRes,
            sizePx = sizePx,
        )
    }
}

private sealed interface LegacyPlaybackQueueRow {
    data class Header(
        val title: String,
        val clearable: Boolean,
    ) : LegacyPlaybackQueueRow

    data class Track(
        val track: LegacyPlaybackQueueTrack,
        val section: LegacyPlaybackQueueSection,
    ) : LegacyPlaybackQueueRow
}

private enum class LegacyPlaybackQueueSection {
    History,
    Current,
    Upcoming,
}

private class PlaybackCenterTitle(context: Context) : RelativeLayout(context) {
    private val titleView: TextView
    private val subTitleView: TextView

    init {
        gravity = Gravity.CENTER
        titleView = TextView(context).apply {
            id = R.id.title_view
            ellipsize = TextUtils.TruncateAt.MARQUEE
            gravity = Gravity.CENTER
            isFocusable = true
            isFocusableInTouchMode = true
            isSelected = true
            marqueeRepeatLimit = -1
            maxLines = 1
            setSingleLine(true)
            setTextColor(context.getColor(R.color.title_color))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            typeface = Typeface.create(paint.typeface, Typeface.BOLD)
        }
        addView(
            titleView,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                addRule(CENTER_IN_PARENT)
            },
        )

        subTitleView = TextView(context).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            setSingleLine(true)
            setTextColor(context.getColor(R.color.sub_title_text_color))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            visibility = View.GONE
        }
        addView(
            subTitleView,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                addRule(BELOW, R.id.title_view)
                addRule(CENTER_HORIZONTAL)
            },
        )
    }

    fun setTitle(title: CharSequence?) {
        titleView.text = title ?: ""
    }

    fun setSubTitle(subTitle: CharSequence?) {
        val hasSubTitle = !subTitle.isNullOrBlank()
        subTitleView.text = subTitle ?: ""
        subTitleView.visibility = if (hasSubTitle) View.VISIBLE else View.GONE
        titleView.maxWidth = if (hasSubTitle) {
            resources.getDimensionPixelSize(R.dimen.max_title_view_width)
        } else {
            resources.displayMetrics.widthPixels
        }
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
        val params = titleView.layoutParams as LayoutParams
        params.removeRule(CENTER_IN_PARENT)
        params.removeRule(CENTER_VERTICAL)
        params.addRule(CENTER_HORIZONTAL)
        if (!hasSubTitle) {
            params.addRule(CENTER_VERTICAL)
        }
        titleView.layoutParams = params
    }
}

private const val LegacyQueueRevealDurationMillis = 300
private const val LegacyQueueDragShuffleDurationMillis = 150L
private const val LegacyQueueDragSettleDurationMillis = 150L
private const val LegacyQueueDragFloatAlpha = 0.66f
private const val LegacyQueueDragInterpolatorPivot = 0.5f
private const val LegacyQueueHistoryLimit = 2
