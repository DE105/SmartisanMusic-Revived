package com.smartisan.music.ui.artwork

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlbumArtworkBrowserHostTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var browser: AlbumArtworkBrowserController
    private var sourceVisible by mutableStateOf(true)
    private var sourceMounted by mutableStateOf(true)

    private fun show() {
        val image = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        image.eraseColor(android.graphics.Color.BLUE)
        val bytes =
            ByteArrayOutputStream().use {
                image.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.toByteArray()
            }
        image.recycle()
        val song =
            MediaItem.Builder()
                .setMediaId("synthetic")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                        .build()
                )
                .build()
        val album =
            com.smartisan.music.ui.album.AlbumSummary(
                "synthetic",
                "Album",
                "Artist",
                listOf(song),
                null,
            )
        compose.setContent {
            AlbumArtworkBrowserHost {
                browser = LocalAlbumArtworkBrowser.current
                if (sourceMounted) {
                    val owner = remember { Any() }
                    var bounds by remember { mutableStateOf<Rect?>(null) }
                    DisposableEffect(owner) { onDispose { browser.removeOwner(owner) } }
                    Box(
                        Modifier.size(80.dp)
                            .testTag("source")
                            .onGloballyPositioned {
                                bounds = Rect(it.positionOnScreen(), it.size.toSize())
                            }
                            .graphicsLayer { alpha = if (sourceVisible) 1f else 0f }
                            .background(Color.Blue)
                            .clickable {
                                browser.open(
                                    owner,
                                    AlbumArtworkBrowserState(album, requireNotNull(bounds)) {
                                        sourceVisible = it
                                    },
                                )
                            }
                    )
                }
            }
        }
        compose.onNodeWithTag("source").performClick()
        compose.waitForIdle()
    }

    @Test
    fun returnKeepsOneWindowAndNeverLeavesBothRepresentationsAbsent() {
        show()
        compose.onRoot().assertExists()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { browser.dismiss() }
        repeat(20) {
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle { assertTrue(sourceVisible || browser.presented) }
        }
        compose.runOnIdle {
            assertTrue(sourceVisible)
            assertFalse(browser.presented)
        }
        compose.onNodeWithTag("source").assertExists()
    }

    @Test
    fun removingTheSourceAlsoRemovesItsPreview() {
        show()
        compose.runOnIdle { sourceMounted = false }
        compose.waitForIdle()
        compose.runOnIdle { assertFalse(browser.presented) }
        compose.onRoot().assertExists()
    }
}
