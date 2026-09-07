package com.smartisan.music.ui.navigation

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
class SmartisanNavigationMotionTest {
    private val scale =
        object : MotionDurationScale {
            override var scaleFactor = 1f
        }
    @get:Rule val compose = createComposeRule(effectContext = scale)
    private lateinit var scope: CoroutineScope
    private val motion = SmartisanNavigationMotion(false)
    private var job: Job? = null

    private fun show(open: Boolean = false) {
        compose.setContent { scope = rememberCoroutineScope() }
        compose.waitForIdle()
        compose.runOnIdle { scope.launch { motion.snapTo(open) } }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    private fun move(open: Boolean) {
        compose.runOnIdle {
            job?.cancel()
            job = scope.launch { motion.animateTo(open) }
        }
    }

    @Test
    fun enteringFadesOldActionsBeforeTheBackArrowAndDetailTitle() {
        show()
        move(true)
        compose.mainClock.advanceTimeBy(80)
        compose.runOnIdle {
            assertTrue(motion.rootAlpha.value in 0f..0.99f)
            assertEquals(0f, motion.detailAlpha.value, 0f)
            assertEquals(0f, motion.backAlpha.value, 0f)
            assertEquals(1f, motion.backOffset.value, 0f)
        }
        compose.mainClock.advanceTimeBy(128)
        compose.runOnIdle {
            assertTrue(motion.backAlpha.value > 0f)
            assertTrue(motion.backOffset.value > 0f && motion.backOffset.value < 1f)
        }
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle {
            assertEquals(1f, motion.position.value, 0f)
            assertEquals(1f, motion.detailAlpha.value, 0f)
            assertEquals(0f, motion.rootAlpha.value, 0f)
        }
    }

    @Test
    fun returnMovesTheArrowRightAndDelaysTheRootTitle() {
        show(open = true)
        move(false)
        compose.mainClock.advanceTimeBy(80)
        compose.runOnIdle {
            assertTrue(motion.backOffset.value > 0f)
            assertTrue(motion.backAlpha.value < 1f)
            assertEquals(0f, motion.rootAlpha.value, 0f)
        }
        compose.mainClock.advanceTimeBy(320)
        compose.runOnIdle {
            assertEquals(1f, motion.rootAlpha.value, 0f)
            assertEquals(0f, motion.backAlpha.value, 0f)
            assertEquals(1f, motion.backOffset.value, 0f)
        }
    }

    @Test
    fun interruptedReturnDoesNotResetThePagePosition() {
        show(open = true)
        move(false)
        compose.mainClock.advanceTimeBy(80)
        val before = compose.runOnIdle { motion.position.value }
        move(true)
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { assertTrue(motion.position.value >= before) }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { assertEquals(1f, motion.position.value, 0f) }
    }

    @Test
    fun disabledAnimationsReachAllFinalSlots() {
        scale.scaleFactor = 0f
        show(open = true)
        move(false)
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle {
            assertEquals(0f, motion.position.value, 0f)
            assertEquals(1f, motion.rootAlpha.value, 0f)
            assertEquals(0f, motion.detailAlpha.value, 0f)
            assertEquals(0f, motion.backAlpha.value, 0f)
        }
    }
}
