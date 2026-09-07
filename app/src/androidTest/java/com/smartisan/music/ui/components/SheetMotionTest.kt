package com.smartisan.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SheetMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun panelRemainsOpaqueWhileTheScrimFadesAndCanReverseItsExit() {
        val visible = mutableStateOf(false)
        compose.setContent {
            Box(Modifier.size(240.dp, 400.dp).background(Color.White)) {
                SmartisanAnimatedSheet(visible.value, {}, Modifier.fillMaxSize()) {
                    Box(
                        Modifier.fillMaxWidth()
                            .height(120.dp)
                            .background(Color.Red)
                            .testTag("panel")
                    )
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { visible.value = true }
        compose.mainClock.advanceTimeBy(160)
        val image = compose.onNodeWithTag("panel").captureToImage()
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels)
        assertTrue(
            "The scrim must not fade the panel",
            pixels.any { (it and 0x00ffffff) == 0x00ff0000 },
        )
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { visible.value = false }
        compose.mainClock.advanceTimeBy(96)
        compose.runOnIdle { visible.value = true }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("panel").assertExists()
        compose.runOnIdle { visible.value = false }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithTag("panel").assertDoesNotExist()
    }
}
