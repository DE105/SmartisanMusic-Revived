package com.smartisan.music.ui.artwork

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal val GalleryCubicOut = Easing {
    val remaining = 1f - it
    1f - remaining * remaining * remaining
}
internal val GalleryOpenEasing = Easing { fraction ->
    if (fraction <= 175f / 300f) 1.1f * GalleryCubicOut.transform(fraction * 300f / 175f)
    else 1.1f - .1f * ((fraction * 300f - 175f) / 125f)
}

@Stable
internal class GalleryArtworkMotion {
    val expansion = Animatable(0f)
    val background = Animatable(0f)
    val cropReveal = Animatable(0f)

    suspend fun animateTo(open: Boolean) = coroutineScope {
        launch {
            expansion.animateTo(
                if (open) 1f else 0f,
                tween(
                    if (open) 300 else 200,
                    easing = if (open) GalleryOpenEasing else GalleryCubicOut,
                ),
            )
        }
        launch {
            background.animateTo(
                if (open) 1f else 0f,
                tween(if (open) 300 else 200, easing = if (open) GalleryCubicOut else LinearEasing),
            )
        }
        launch {
            cropReveal.animateTo(
                if (open) 1f else 0f,
                tween(if (open) 50 else 200, easing = if (open) LinearEasing else GalleryCubicOut),
            )
        }
    }
}

internal data class GalleryArtworkFrame(val image: Rect, val clip: Rect)

/** Interpolate real image bounds; a cropped thumbnail gradually reveals the uncropped photo. */
internal fun galleryArtworkFrame(
    source: Rect,
    viewport: Size,
    imageSize: Size,
    expansion: Float,
    cropReveal: Float,
): GalleryArtworkFrame {
    val startScale = max(source.width / imageSize.width, source.height / imageSize.height)
    val endScale = min(viewport.width / imageSize.width, viewport.height / imageSize.height)
    val startSize = Size(imageSize.width * startScale, imageSize.height * startScale)
    val endSize = Size(imageSize.width * endScale, imageSize.height * endScale)
    val start =
        Rect(
            source.center.x - startSize.width / 2,
            source.center.y - startSize.height / 2,
            source.center.x + startSize.width / 2,
            source.center.y + startSize.height / 2,
        )
    val end =
        Rect(
            (viewport.width - endSize.width) / 2,
            (viewport.height - endSize.height) / 2,
            (viewport.width + endSize.width) / 2,
            (viewport.height + endSize.height) / 2,
        )
    val image = lerp(start, end, expansion)
    val remainingCrop = 1f - cropReveal
    val insetX = (start.width - source.width) / (2 * start.width) * image.width * remainingCrop
    val insetY = (start.height - source.height) / (2 * start.height) * image.height * remainingCrop
    return GalleryArtworkFrame(
        image,
        Rect(
            image.left + insetX,
            image.top + insetY,
            image.right - insetX,
            image.bottom - insetY,
        ),
    )
}
