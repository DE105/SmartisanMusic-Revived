package com.smartisanos.music.ui.shell

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import com.smartisanos.music.R
import com.smartisanos.music.ui.album.AlbumViewMode

internal class LegacyArtistFooterView(context: Context) : LinearLayout(context) {
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

internal class LegacyArtistAlbumViewSwitchAnimator {
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

internal fun android.content.res.Resources.legacyArtistScrollBottomPadding(): Int {
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
