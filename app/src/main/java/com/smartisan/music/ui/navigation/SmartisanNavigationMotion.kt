package com.smartisan.music.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.math.cos
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal const val SmartisanNavigationDuration = 300
private val Smooth = Easing { ((1.0 - cos(it * Math.PI)) / 2.0).toFloat() }
private val Decelerate = Easing { 1f - (1f - it) * (1f - it) }

/** Independent Compose implementation of the observed Notes title choreography. */
@Stable
internal class SmartisanNavigationMotion(open: Boolean) {
    val position = Animatable(if (open) 1f else 0f)
    val rootAlpha = Animatable(if (open) 0f else 1f)
    val detailAlpha = Animatable(if (open) 1f else 0f)
    val backAlpha = Animatable(if (open) 1f else 0f)
    val backOffset = Animatable(if (open) 0f else 1f)

    val closed: Boolean
        get() =
            position.value == 0f &&
                rootAlpha.value == 1f &&
                !position.isRunning &&
                !rootAlpha.isRunning &&
                !backOffset.isRunning

    suspend fun snapTo(open: Boolean) {
        position.snapTo(if (open) 1f else 0f)
        rootAlpha.snapTo(if (open) 0f else 1f)
        detailAlpha.snapTo(if (open) 1f else 0f)
        backAlpha.snapTo(if (open) 1f else 0f)
        backOffset.snapTo(if (open) 0f else 1f)
    }

    suspend fun animateTo(open: Boolean) = coroutineScope {
        launch {
            position.animateTo(
                if (open) 1f else 0f,
                tween(SmartisanNavigationDuration, easing = if (open) Smooth else Decelerate),
            )
        }
        launch {
            rootAlpha.animateTo(
                if (open) 0f else 1f,
                tween(150, delayMillis = if (open) 0 else 150, easing = Smooth),
            )
        }
        launch {
            detailAlpha.animateTo(
                if (open) 1f else 0f,
                tween(
                    150,
                    delayMillis = if (open) 150 else 0,
                    easing = if (open) Smooth else Decelerate,
                ),
            )
        }
        launch {
            backAlpha.animateTo(
                if (open) 1f else 0f,
                tween(
                    150,
                    delayMillis = if (open) 100 else 0,
                    easing = if (open) Smooth else Decelerate,
                ),
            )
        }
        launch {
            backOffset.animateTo(
                if (open) 0f else 1f,
                tween(
                    if (open) 200 else 300,
                    delayMillis = if (open) 100 else 0,
                    easing = if (open) Smooth else Decelerate,
                ),
            )
        }
    }
}

internal data class SmartisanTitleMotion(
    val motion: SmartisanNavigationMotion,
    val detail: Boolean,
    val parent: SmartisanTitleMotion? = null,
) {
    val contentAlpha: Float
        get() =
            (if (detail) motion.detailAlpha.value else motion.rootAlpha.value) *
                (parent?.contentAlpha ?: 1f)

    val leftAlpha: Float
        get() =
            (if (detail) motion.backAlpha.value else motion.rootAlpha.value) *
                (parent?.leftAlpha ?: 1f)

    val leftOffset: Float
        get() = (if (detail) motion.backOffset.value else 0f) + (parent?.leftOffset ?: 0f)

    val interactive: Boolean
        get() =
            !motion.position.isRunning &&
                motion.position.value == (if (detail) 1f else 0f) &&
                (parent?.interactive ?: true)
}

internal val LocalSmartisanTitleMotion = staticCompositionLocalOf<SmartisanTitleMotion?> { null }
internal val LocalSmartisanTitleVisible = staticCompositionLocalOf { true }
