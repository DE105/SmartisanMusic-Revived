package com.smartisanos.music.playback

import android.content.ComponentName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken

val LocalPlaybackBrowser = staticCompositionLocalOf<MediaBrowser?> { null }
val LocalPlaybackController = staticCompositionLocalOf<MediaController?> { null }

@Composable
fun ProvidePlaybackController(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val sessionToken = remember(context) {
        SessionToken(context, ComponentName(context, PlaybackService::class.java))
    }
    val controllerFuture = remember(sessionToken) {
        MediaBrowser.Builder(context, sessionToken).buildAsync()
    }
    var browser by remember(controllerFuture) {
        mutableStateOf<MediaBrowser?>(null)
    }

    DisposableEffect(controllerFuture, context) {
        controllerFuture.addListener(
            {
                browser = runCatching { controllerFuture.get() }.getOrNull()
            },
            ContextCompat.getMainExecutor(context),
        )

        onDispose {
            browser = null
            MediaController.releaseFuture(controllerFuture)
        }
    }

    CompositionLocalProvider(
        LocalPlaybackBrowser provides browser,
        LocalPlaybackController provides browser,
        content = content,
    )
}
