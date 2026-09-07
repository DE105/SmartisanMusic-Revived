package com.smartisan.music.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartisan.music.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SmartisanPressFeedbackTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sameFrameTapRemainsVisibleThenClears() {
        val source = MutableInteractionSource()
        lateinit var pressed: State<Boolean>
        compose.setContent {
            pressed = source.collectSmartisanPressedAsState()
            Box(
                Modifier.size(100.dp).testTag("row")
                    .smartisanPainterBackground(
                        rememberSmartisanDrawablePainter(
                            R.drawable.listview_selector,
                            pressed = pressed.value,
                        )
                    )
            )
        }
        compose.mainClock.autoAdvance = false
        val press = PressInteraction.Press(Offset.Zero)
        compose.runOnIdle {
            source.tryEmit(press)
            source.tryEmit(PressInteraction.Release(press))
        }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { assertTrue(pressed.value) }
        val image = compose.onNodeWithTag("row").captureToImage()
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels)
        val center = pixels[(image.height / 2) * image.width + image.width / 2]
        assertTrue("Quick tap must draw the original blue background", (center and 255) > ((center ushr 16) and 255))
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle { assertFalse(pressed.value) }
    }

    @Test fun newPressSurvivesPreviousReleaseAndScrollCancellationClearsIt() {
        val source = MutableInteractionSource()
        lateinit var pressed: State<Boolean>
        compose.setContent { pressed = source.collectSmartisanPressedAsState() }
        compose.mainClock.autoAdvance = false
        val first = PressInteraction.Press(Offset.Zero)
        val second = PressInteraction.Press(Offset.Zero)
        compose.runOnIdle {
            source.tryEmit(first)
            source.tryEmit(PressInteraction.Release(first))
        }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { source.tryEmit(second) }
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle {
            assertTrue(pressed.value)
            source.tryEmit(PressInteraction.Cancel(second))
        }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { assertFalse(pressed.value) }
    }
}
