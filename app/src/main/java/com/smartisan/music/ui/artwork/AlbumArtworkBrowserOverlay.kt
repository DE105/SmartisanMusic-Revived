package com.smartisan.music.ui.artwork

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.smartisan.music.R
import com.smartisan.music.ui.album.AlbumSummary
import com.smartisan.music.ui.components.rememberSmartisanDrawablePainter
import com.smartisan.music.ui.library.rememberAlbumArtworkLoader
import kotlin.math.roundToInt

internal data class AlbumArtworkBrowserState(
    val album: AlbumSummary,
    val sourceBounds: Rect,
    val onSourceVisibilityChanged: (Boolean) -> Unit = {},
)

@Composable
internal fun AlbumArtworkBrowserOverlay(
    state: AlbumArtworkBrowserState?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var retained by remember { mutableStateOf<AlbumArtworkBrowserState?>(null) }
    var viewport by remember { mutableStateOf<Rect?>(null) }
    val motion = remember { GalleryArtworkMotion() }
    LaunchedEffect(state, viewport != null) {
        if (state != null) {
            retained = state
            if (viewport != null) motion.animateTo(true)
        } else if (retained != null) {
            motion.animateTo(false)
            retained = null
        }
    }
    val displayed = state ?: retained ?: return
    val placed = viewport != null
    DisposableEffect(displayed, placed) {
        if (placed) displayed.onSourceVisibilityChanged(false)
        onDispose { if (placed) displayed.onSourceVisibilityChanged(true) }
    }
    val position = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ) = IntOffset.Zero
        }
    }
    Popup(
        popupPositionProvider = position,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true, clippingEnabled = false),
    ) {
        DisposableEffect(Unit) { onDispose { viewport = null } }
        val loader = rememberAlbumArtworkLoader()
        val decodeSize = viewport?.let { minOf(it.width, it.height).roundToInt() }
        val bitmap by
            produceState(loader.cached(displayed.album), displayed.album, decodeSize) {
                if (decodeSize != null) value = loader.load(displayed.album, decodeSize)
            }
        val placeholder = rememberSmartisanDrawablePainter(R.drawable.noalbumcover_220)
        val painter =
            bitmap?.let { remember(it) { BitmapPainter(it.asImageBitmap()) } } ?: placeholder
        val dismissLabel = stringResource(R.string.back)
        val dismiss by rememberUpdatedState(onDismissRequest)
        Box(
            modifier.fillMaxSize().onGloballyPositioned {
                viewport = Rect(it.positionOnScreen(), it.size.toSize())
            }
        ) {
            Canvas(
                Modifier.fillMaxSize()
                    .pointerInput(displayed.album.id, painter) {
                        detectTapGestures { point ->
                            val bounds = viewport ?: return@detectTapGestures
                            val frame =
                                galleryArtworkFrame(
                                    displayed.sourceBounds.translate(-bounds.topLeft),
                                    size.toSize(),
                                    painter.intrinsicSize,
                                    motion.expansion.value,
                                    motion.cropReveal.value,
                                )
                            if (!frame.clip.contains(point)) dismiss()
                        }
                    }
                    .semantics {
                        contentDescription = dismissLabel
                        onClick(label = dismissLabel) {
                            dismiss()
                            true
                        }
                    }
            ) {
                val bounds = viewport ?: return@Canvas
                val frame =
                    galleryArtworkFrame(
                        displayed.sourceBounds.translate(-bounds.topLeft),
                        size,
                        painter.intrinsicSize,
                        motion.expansion.value,
                        motion.cropReveal.value,
                    )
                drawRect(Color.Black, alpha = motion.background.value)
                clipRect(frame.clip.left, frame.clip.top, frame.clip.right, frame.clip.bottom) {
                    translate(frame.image.left, frame.image.top) {
                        with(painter) { draw(frame.image.size) }
                    }
                }
            }
        }
    }
}
