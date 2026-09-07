package com.smartisan.music.ui.playback

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SleepTimerMotionTest {
    private val scale =
        object : MotionDurationScale {
            override var scaleFactor = 1f
        }
    @get:Rule val compose = createComposeRule(effectContext = scale)
    private lateinit var state: SleepTimerWheelState
    private lateinit var scope: CoroutineScope
    private val changes = mutableListOf<Int>()

    private fun show() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val context = LocalContext.current
            val decay = rememberSplineBasedDecay<Float>()
            state =
                androidx.compose.runtime.remember(context, decay) {
                    SleepTimerWheelState(context, 8, decay).also { it.rowHeight = 100f }
                }
            scope = rememberCoroutineScope()
        }
        compose.mainClock.advanceTimeBy(32)
    }

    @Test
    fun stepAndFlingFinishOnAnInBoundsSelectionUsingTheComposeClock() {
        show()
        compose.runOnIdle { state.motion = scope.launch { state.step(1, changes::add) } }
        compose.mainClock.advanceTimeBy(600)
        compose.runOnIdle {
            assertEquals(1, state.index)
            assertEquals(0f, state.offset, .001f)
            assertEquals(listOf(1), changes)
            state.motion = scope.launch { state.flingOrSnap(-1200, changes::add) }
        }
        compose.mainClock.advanceTimeBy(5000)
        compose.runOnIdle {
            assertTrue(state.index in 1..7)
            assertEquals(0f, state.offset, .001f)
            assertTrue(state.motion?.isCompleted == true)
        }
    }

    @Test
    fun cancelledStepCannotKeepMovingAfterANewGesture() {
        show()
        compose.runOnIdle { state.motion = scope.launch { state.step(1, changes::add) } }
        compose.mainClock.advanceTimeBy(64)
        val offset = compose.runOnIdle {
            state.stop()
            state.offset
        }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle {
            assertEquals(offset, state.offset, .001f)
            state.motion = scope.launch { state.snap(changes::add) }
        }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { assertEquals(0f, state.offset, .001f) }
    }

    @Test
    fun disablingAnimationsStillSnapsToTheSelectedRow() {
        scale.scaleFactor = 0f
        show()
        compose.runOnIdle {
            state.scrollBy(-35f, changes::add)
            state.motion = scope.launch { state.snap(changes::add) }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle {
            assertEquals(0, state.index)
            assertEquals(0f, state.offset, .001f)
        }
    }
}
