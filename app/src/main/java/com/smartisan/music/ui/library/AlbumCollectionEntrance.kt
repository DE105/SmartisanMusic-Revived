package com.smartisan.music.ui.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.smartisan.music.ui.album.AlbumViewMode

internal enum class AlbumEntrancePhase {
    Waiting,
    Running,
    Complete,
}

/** Initial loading is distinct from switching layouts; only the first visible items participate. */
@Stable
internal class AlbumCollectionEntrance(enabled: Boolean) {
    var phase by
        mutableStateOf(if (enabled) AlbumEntrancePhase.Waiting else AlbumEntrancePhase.Complete)
        private set

    private val elapsed = Animatable(0f)
    private var order = emptyMap<String, Int>()
    private var itemDuration = 300
    private var stagger = 40

    suspend fun reveal(ids: List<String>, mode: AlbumViewMode) {
        if (phase != AlbumEntrancePhase.Waiting) return
        order = ids.withIndex().associate { it.value to it.index }
        itemDuration = if (mode == AlbumViewMode.Tile) 300 else 150
        stagger = if (mode == AlbumViewMode.Tile) 40 else 10
        val duration = itemDuration + (ids.size - 1).coerceIn(0, 20) * stagger
        phase = AlbumEntrancePhase.Running
        try {
            elapsed.animateTo(duration.toFloat(), tween(duration, easing = LinearEasing))
        } finally {
            finish()
        }
    }

    fun finish() {
        phase = AlbumEntrancePhase.Complete
    }

    fun alpha(id: String): Float =
        when (phase) {
            AlbumEntrancePhase.Waiting -> 0f
            AlbumEntrancePhase.Complete -> 1f
            AlbumEntrancePhase.Running -> {
                val position = order[id]
                if (position == null) 1f
                else
                    ((elapsed.value - position.coerceAtMost(20) * stagger) / itemDuration).coerceIn(
                        0f,
                        1f,
                    )
            }
        }
}

internal fun Modifier.albumCollectionEntrance(
    state: AlbumCollectionEntrance,
    id: String,
): Modifier = graphicsLayer { alpha = state.alpha(id) }
