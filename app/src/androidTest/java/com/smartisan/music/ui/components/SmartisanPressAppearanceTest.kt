package com.smartisan.music.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartisan.music.R
import com.smartisan.music.ui.songs.SmartisanSongRow
import com.smartisan.music.ui.playback.PressedDrawableButton
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmartisanPressAppearanceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dayQuickTapDrawsBlueBackgroundAndWhiteText() = checkRow(false)

    @Test fun nightQuickTapDrawsBlueBackgroundAndWhiteText() = checkRow(true)

    @Test fun playbackButtonUsesItsPressedArtworkAndRestoresIt() {
        var clicks = 0
        compose.setContent {
            PressedDrawableButton(
                R.drawable.more_btn,
                R.drawable.more_btn_down,
                "More actions",
                Modifier.size(40.dp).testTag("button"),
                onClick = { clicks++ },
            )
        }
        fun pixels(): IntArray {
            val image = compose.onNodeWithTag("button").captureToImage()
            return IntArray(image.width * image.height).also { image.readPixels(it) }
        }
        val normal = pixels()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("button").performTouchInput {
            down(center)
            up()
        }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { assertEquals(1, clicks) }
        assertFalse("Button must use its own pressed artwork", normal.contentEquals(pixels()))
        compose.mainClock.advanceTimeBy(200)
        assertArrayEquals(normal, pixels())
    }

    private fun checkRow(night: Boolean) {
        var clicks = 0
        var normalColor = 0
        val item = MediaItem.Builder().setMediaId("sample")
            .setMediaMetadata(MediaMetadata.Builder().setTitle("Sample title").setArtist("Artist").build())
            .build()
        compose.setContent {
            val context = LocalContext.current
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val themed = remember(context, night) { context.createConfigurationContext(configuration) }
            normalColor = themed.getColor(R.color.surface_card)
            CompositionLocalProvider(LocalContext provides themed, LocalConfiguration provides configuration) {
                LazyColumn(Modifier.width(360.dp)) {
                    item {
                        SmartisanSongRow(item, { clicks++ }, {}, Modifier.testTag("song"))
                    }
                }
            }
        }
        assertEquals(normalColor, backgroundPixel())
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("song").performTouchInput {
            down(center)
            up()
        }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { assertEquals("Click callback must not wait for feedback", 1, clicks) }
        val background = backgroundPixel()
        assertTrue((background and 255) > ((background ushr 16) and 255))
        val title = compose.onNodeWithText("Sample title", useUnmergedTree = true).captureToImage()
        val pixels = IntArray(title.width * title.height)
        title.readPixels(pixels)
        assertTrue("Title must turn white over blue", pixels.count { it == -1 } > 5)
        compose.mainClock.advanceTimeBy(200)
        assertEquals(normalColor, backgroundPixel())
    }

    private fun backgroundPixel(): Int {
        val image = compose.onNodeWithTag("song").captureToImage()
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels)
        return pixels[(image.height - 4) * image.width + image.width / 2]
    }
}
