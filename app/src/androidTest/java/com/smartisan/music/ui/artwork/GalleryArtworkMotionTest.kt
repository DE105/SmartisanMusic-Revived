package com.smartisan.music.ui.artwork

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryArtworkMotionTest {
    private val scale =
        object : MotionDurationScale {
            override var scaleFactor = 1f
        }
    @get:Rule val compose = createComposeRule(effectContext = scale)
    private lateinit var scope: CoroutineScope
    private val motion = GalleryArtworkMotion()
    private var animation: Job? = null

    private fun show() {
        compose.setContent { scope = rememberCoroutineScope() }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    private fun animate(open: Boolean) {
        compose.runOnIdle {
            animation?.cancel()
            animation = scope.launch { motion.animateTo(open) }
        }
    }

    @Test
    fun expansionOvershootsWhileTheBackgroundRemainsWithinItsOpacityRange() {
        show()
        animate(true)
        compose.mainClock.advanceTimeBy(176)
        compose.runOnIdle {
            assertTrue(motion.expansion.value > 1f)
            assertTrue(motion.background.value in 0f..1f)
            assertEquals(1f, motion.cropReveal.value, 0f)
        }
        compose.mainClock.advanceTimeBy(256)
        compose.runOnIdle {
            assertEquals(1f, motion.expansion.value, 0f)
            assertEquals(1f, motion.background.value, 0f)
        }
    }

    @Test
    fun closingDuringExpansionContinuesFromTheCurrentBounds() {
        show()
        animate(true)
        compose.mainClock.advanceTimeBy(96)
        val before = compose.runOnIdle { motion.expansion.value }
        animate(false)
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { assertTrue(motion.expansion.value in 0f..before) }
        compose.mainClock.advanceTimeBy(256)
        compose.runOnIdle {
            assertEquals(0f, motion.expansion.value, 0f)
            assertEquals(0f, motion.background.value, 0f)
            assertEquals(0f, motion.cropReveal.value, 0f)
        }
    }

    @Test
    fun disabledAnimationsReachTheFinalImageAndBackgroundTogether() {
        scale.scaleFactor = 0f
        show()
        animate(true)
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle {
            assertEquals(1f, motion.expansion.value, 0f)
            assertEquals(1f, motion.background.value, 0f)
        }
        animate(false)
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle {
            assertEquals(0f, motion.expansion.value, 0f)
            assertEquals(0f, motion.background.value, 0f)
        }
    }
}
