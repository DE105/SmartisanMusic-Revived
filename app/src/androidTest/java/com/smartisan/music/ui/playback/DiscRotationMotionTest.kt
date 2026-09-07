package com.smartisan.music.ui.playback

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiscRotationMotionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun manualRotationSurvivesAutomaticFramesAndPauseKeepsTheAngle() {
        val running = mutableStateOf(true)
        val manual = mutableFloatStateOf(0f)
        lateinit var rotation: State<Float>
        compose.mainClock.autoAdvance = false
        compose.setContent {
            rotation = rememberSmoothDiscRotation(running.value, 2000f, manual.floatValue)
        }
        compose.mainClock.advanceTimeBy(320)
        val before = compose.runOnIdle { rotation.value }
        compose.runOnIdle { manual.floatValue = 90f }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle {
            assertTrue(normalizeAngleDelta(rotation.value - before) in 90f..120f)
            running.value = false
        }
        compose.mainClock.advanceTimeBy(32)
        val paused = compose.runOnIdle { rotation.value }
        compose.mainClock.advanceTimeBy(320)
        compose.runOnIdle { assertEquals(paused, rotation.value, .001f) }
    }
}
