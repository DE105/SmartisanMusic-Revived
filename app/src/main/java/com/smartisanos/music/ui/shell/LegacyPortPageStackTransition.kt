package com.smartisanos.music.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateInt
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.cos

private const val LegacyPageStackSlideMillis = 300
private val LegacyPageStackEasing = Easing { fraction ->
    ((cos((fraction + 1f) * Math.PI) / 2.0) + 0.5).toFloat()
}
private val LegacyPageStackDecelerateEasing = Easing { fraction ->
    1f - ((1f - fraction) * (1f - fraction))
}

internal enum class LegacyPortPageStackAxis {
    Horizontal,
    VerticalPush,
}

@Composable
internal fun <T : Any> LegacyPortPageStackTransition(
    secondaryKey: T?,
    modifier: Modifier = Modifier,
    label: String = "legacy page stack transition",
    axis: LegacyPortPageStackAxis = LegacyPortPageStackAxis.Horizontal,
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable (T) -> Unit,
) {
    val visibleState = remember {
        MutableTransitionState(false)
    }
    val hasSecondary = secondaryKey != null
    visibleState.targetState = hasSecondary

    var retainedSecondaryKey by remember {
        mutableStateOf<T?>(secondaryKey)
    }
    LaunchedEffect(secondaryKey) {
        if (secondaryKey != null) {
            retainedSecondaryKey = secondaryKey
        }
    }
    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) {
            retainedSecondaryKey = null
        }
    }

    BoxWithConstraints(modifier = modifier.clipToBounds()) {
        val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
        val transition = updateTransition(
            targetState = hasSecondary,
            label = label,
        )
        val primaryOffsetX by transition.animateInt(
            transitionSpec = {
                tween(
                    durationMillis = LegacyPageStackSlideMillis,
                    easing = LegacyPageStackEasing,
                )
            },
            label = "$label primary offset",
        ) { showingSecondary ->
            if (axis == LegacyPortPageStackAxis.Horizontal && showingSecondary) -widthPx else 0
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(primaryOffsetX, 0) },
        ) {
            primaryContent()
        }

        val contentKey = secondaryKey ?: retainedSecondaryKey
        if (contentKey != null) {
            AnimatedVisibility(
                visibleState = visibleState,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f),
                enter = if (axis == LegacyPortPageStackAxis.VerticalPush) {
                    slideInVertically(
                        animationSpec = tween(
                            durationMillis = LegacyPageStackSlideMillis,
                            easing = LegacyPageStackDecelerateEasing,
                        ),
                        initialOffsetY = { it },
                    )
                } else {
                    slideInHorizontally(
                        animationSpec = tween(
                            durationMillis = LegacyPageStackSlideMillis,
                            easing = LegacyPageStackEasing,
                        ),
                        initialOffsetX = { it },
                    )
                },
                exit = if (axis == LegacyPortPageStackAxis.VerticalPush) {
                    slideOutVertically(
                        animationSpec = tween(
                            durationMillis = LegacyPageStackSlideMillis,
                            easing = LegacyPageStackDecelerateEasing,
                        ),
                        targetOffsetY = { it },
                    )
                } else {
                    slideOutHorizontally(
                        animationSpec = tween(
                            durationMillis = LegacyPageStackSlideMillis,
                            easing = LegacyPageStackEasing,
                        ),
                        targetOffsetX = { it },
                    )
                },
            ) {
                secondaryContent(contentKey)
            }
        }
    }
}
