package com.smartisan.music.ui.artwork

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.unit.toSize
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
    onClosed: () -> Unit,
) {
    var retained by remember { mutableStateOf<AlbumArtworkBrowserState?>(null) }
    var viewport by remember { mutableStateOf<Rect?>(null) }
    val motion = remember { GalleryArtworkMotion() }
    val displayed = state ?: retained
    val source =
        remember(displayed) {
            displayed?.let { ArtworkSourceVisibility(it.onSourceVisibilityChanged) }
        }
    val closed by rememberUpdatedState(onClosed)
    LaunchedEffect(state, viewport != null) {
        if (state != null) {
            retained = state
            if (viewport != null) motion.animateTo(true)
        } else if (retained != null) {
            motion.animateTo(false)
            // Restore before removing the overlay, in the same window's frame transaction.
            source?.restore()
            retained = null
            closed()
        }
    }
    if (displayed == null) return
    BackHandler(onBack = onDismissRequest)
    val placed = viewport != null
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(placed) { if (placed) focusRequester.requestFocus() }
    DisposableEffect(source, placed) {
        if (placed) source?.hide()
        onDispose { source?.restore() }
    }
    val loader = rememberAlbumArtworkLoader()
    val decodeSize = viewport?.let { minOf(it.width, it.height).roundToInt() }
    val bitmap by
        produceState(loader.cached(displayed.album), displayed.album, decodeSize) {
            if (decodeSize != null) value = loader.load(displayed.album, decodeSize)
        }
    val placeholder = rememberSmartisanDrawablePainter(R.drawable.noalbumcover_220)
    val painter = bitmap?.let { remember(it) { BitmapPainter(it.asImageBitmap()) } } ?: placeholder
    val dismissLabel = stringResource(R.string.back)
    val dismiss by rememberUpdatedState(onDismissRequest)
    Box(
        modifier.fillMaxSize().onGloballyPositioned {
            viewport = Rect(it.positionOnScreen(), it.size.toSize())
        }
    ) {
        Canvas(
            Modifier.fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
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
