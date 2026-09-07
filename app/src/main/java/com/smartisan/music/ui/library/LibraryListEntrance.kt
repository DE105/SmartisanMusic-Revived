package com.smartisan.music.ui.library

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.integerResource
import com.smartisan.music.R

@Stable
internal class LibraryListEntrance(val duration: Int, initiallyComplete: Boolean = false) {
    val elapsed = Animatable(0f)
    var complete by mutableStateOf(initiallyComplete)
}

internal fun Modifier.libraryListEntrance(state: LibraryListEntrance, order: () -> Int): Modifier =
    graphicsLayer {
        val fraction =
            if (state.complete) 1f
            else
                ((state.elapsed.value - order().coerceAtLeast(0) * state.duration * .2f) /
                        state.duration)
                    .coerceIn(0f, 1f)
        alpha = fraction * fraction
    }

/** Enter once per mounted page; live metadata and ordering updates must not flash every row. */
@Composable
internal fun rememberLibraryListEntrance(
    contentKey: Any?,
    active: Boolean,
    visibleCount: () -> Int,
): LibraryListEntrance {
    val duration = integerResource(R.integer.item_flip)
    val preview = LocalInspectionMode.current
    // Establish opacity before the first draw; LaunchedEffect is too late to hide an opaque frame.
    val state =
        remember(duration, preview) { LibraryListEntrance(duration, initiallyComplete = preview) }
    var initialized by remember(state) { mutableStateOf(false) }
    LaunchedEffect(state, contentKey, active, preview) {
        if (preview || initialized) {
            state.complete = true
            return@LaunchedEffect
        }
        if (!active) return@LaunchedEffect
        initialized = true
        withFrameNanos {}
        val total = duration + (visibleCount() - 1).coerceAtLeast(0) * (duration * .2f).toInt()
        state.elapsed.animateTo(total.toFloat(), tween(total, easing = LinearEasing))
        state.complete = true
    }
    return state
}
