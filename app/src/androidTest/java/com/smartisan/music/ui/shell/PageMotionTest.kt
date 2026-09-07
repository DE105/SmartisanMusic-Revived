package com.smartisan.music.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PageMotionTest {
    private val scale =
        object : MotionDurationScale {
            override var scaleFactor = 1f
        }
    @get:Rule val compose = createComposeRule(effectContext = scale)
    private val secondary = mutableStateOf<String?>(null)
    private var mounted = false

    private fun show() {
        compose.setContent {
            PageStackTransition(
                secondaryKey = secondary.value,
                modifier = Modifier.size(240.dp, 400.dp),
                primaryContent = { Box(Modifier.fillMaxSize().background(Color.White)) },
                secondaryContent = {
                    DisposableEffect(Unit) {
                        mounted = true
                        onDispose { mounted = false }
                    }
                    Box(Modifier.fillMaxSize().background(Color.Red).testTag("detail"))
                },
            )
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    @Test
    fun reopeningDuringPopContinuesFromTheVisiblePosition() {
        show()
        compose.runOnIdle { secondary.value = "detail" }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { secondary.value = null }
        compose.mainClock.advanceTimeBy(96)
        val before = compose.onNodeWithTag("detail").fetchSemanticsNode().positionInRoot.x
        compose.runOnIdle { secondary.value = "detail" }
        compose.mainClock.advanceTimeBy(32)
        val after = compose.onNodeWithTag("detail").fetchSemanticsNode().positionInRoot.x
        assertTrue("Reopening must not jump back to the off-screen start", after <= before + 1f)
        compose.mainClock.advanceTimeBy(400)
        assertEquals(0f, compose.onNodeWithTag("detail").fetchSemanticsNode().positionInRoot.x, 1f)
    }

    @Test
    fun outgoingContentLivesUntilTheScaledExitFinishes() {
        scale.scaleFactor = 3f
        show()
        compose.runOnIdle { secondary.value = "detail" }
        compose.mainClock.advanceTimeBy(1100)
        compose.runOnIdle { secondary.value = null }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { assertTrue("The scaled animation is still running", mounted) }
        compose.mainClock.advanceTimeBy(800)
        compose.onNodeWithTag("detail").assertDoesNotExist()
        compose.runOnIdle { assertTrue(!mounted) }
    }

    @Test
    fun disabledAnimationsDisposeTheOutgoingPageWithoutARealTimeDelay() {
        scale.scaleFactor = 0f
        show()
        compose.runOnIdle { secondary.value = "detail" }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { secondary.value = null }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithTag("detail").assertDoesNotExist()
    }
}
