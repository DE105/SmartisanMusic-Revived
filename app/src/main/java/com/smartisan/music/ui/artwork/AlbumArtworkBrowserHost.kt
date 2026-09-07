package com.smartisan.music.ui.artwork

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics

@Stable
internal class AlbumArtworkBrowserController {
    var request by mutableStateOf<AlbumArtworkBrowserState?>(null)
        private set

    var presented by mutableStateOf(false)
        private set

    var generation by mutableIntStateOf(0)
        private set

    private var owner: Any? = null

    fun open(owner: Any, request: AlbumArtworkBrowserState) {
        if (this.owner !== owner) generation++
        this.owner = owner
        this.request = request
        presented = true
    }

    fun dismiss() {
        request = null
    }

    fun closed() {
        if (request == null) {
            presented = false
            owner = null
        }
    }

    fun removeOwner(owner: Any) {
        if (this.owner === owner) {
            request = null
            presented = false
            this.owner = null
            generation++
        }
    }
}

internal val LocalAlbumArtworkBrowser =
    staticCompositionLocalOf<AlbumArtworkBrowserController> {
        error("Album artwork preview requires AlbumArtworkBrowserHost")
    }

/** Preview and thumbnail share one window, so their final drawing changes in the same frame. */
@Composable
internal fun AlbumArtworkBrowserHost(content: @Composable () -> Unit) {
    val controller = remember { AlbumArtworkBrowserController() }
    CompositionLocalProvider(LocalAlbumArtworkBrowser provides controller) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .then(if (controller.presented) Modifier.clearAndSetSemantics {} else Modifier)
            ) {
                content()
            }
            key(controller.generation) {
                AlbumArtworkBrowserOverlay(
                    controller.request,
                    controller::dismiss,
                    modifier = Modifier.fillMaxSize(),
                    onClosed = controller::closed,
                )
            }
        }
    }
}

/** Owns the temporary hiding request and restores it exactly once, including cancellation. */
internal class ArtworkSourceVisibility(private val change: (Boolean) -> Unit) {
    private var hidden = false

    fun hide() {
        if (!hidden) {
            hidden = true
            change(false)
        }
    }

    fun restore() {
        if (hidden) {
            hidden = false
            change(true)
        }
    }
}
