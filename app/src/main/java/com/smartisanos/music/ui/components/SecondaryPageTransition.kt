package com.smartisanos.music.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

private const val SecondaryPageEnterMillis = 300
private const val SecondaryPageExitMillis = 260
private const val SecondaryPageFadeMillis = 140
private const val SecondaryPageFadeDelayMillis = 30
private const val SecondaryPageParentParallaxDivisor = 3

@Composable
fun <T : Any> SecondaryPageTransition(
    secondaryKey: T?,
    modifier: Modifier = Modifier,
    label: String = "secondary page transition",
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = secondaryKey,
        modifier = modifier,
        contentAlignment = Alignment.TopStart,
        transitionSpec = {
            val transform = when {
                initialState == null && targetState != null -> {
                    val enter = slideIntoContainer(
                        towards = SlideDirection.Left,
                        animationSpec = tween(
                            durationMillis = SecondaryPageEnterMillis,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = SecondaryPageFadeMillis,
                            delayMillis = SecondaryPageFadeDelayMillis,
                        ),
                    )
                    enter togetherWith ExitTransition.KeepUntilTransitionsFinished
                }
                initialState != null && targetState == null -> {
                    val enter = slideIntoContainer(
                        towards = SlideDirection.Right,
                        animationSpec = tween(
                            durationMillis = SecondaryPageExitMillis,
                            easing = FastOutSlowInEasing,
                        ),
                        initialOffset = { fullSlideOffset ->
                            fullSlideOffset / SecondaryPageParentParallaxDivisor
                        },
                    ) + fadeIn(
                        animationSpec = tween(durationMillis = SecondaryPageFadeMillis),
                    )
                    val exit = slideOutOfContainer(
                        towards = SlideDirection.Right,
                        animationSpec = tween(
                            durationMillis = SecondaryPageExitMillis,
                            easing = FastOutSlowInEasing,
                        ),
                    ) + fadeOut(
                        animationSpec = tween(durationMillis = SecondaryPageFadeMillis),
                    )
                    enter togetherWith exit
                }
                else -> {
                    val enter = fadeIn(
                        animationSpec = tween(durationMillis = SecondaryPageFadeMillis),
                    )
                    val exit = fadeOut(
                        animationSpec = tween(durationMillis = SecondaryPageFadeMillis),
                    )
                    enter togetherWith exit
                }
            }

            transform
                .apply {
                    targetContentZIndex = if (targetState == null) 0f else 1f
                }
                .using(SizeTransform(clip = false))
        },
        label = label,
    ) { targetKey ->
        if (targetKey == null) {
            primaryContent()
        } else {
            secondaryContent(targetKey)
        }
    }
}
