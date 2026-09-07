package com.smartisan.music.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryEntranceMotionTest {
    @get:Rule val compose = createComposeRule()
    private val content = mutableStateOf(listOf("one"))
    private val active = mutableStateOf(true)
    private lateinit var entrance: LibraryListEntrance

    private fun show() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            entrance = rememberLibraryListEntrance(content.value, active.value) { 200 }
            Box(Modifier.size(100.dp).libraryListEntrance(entrance) { 0 })
        }
        compose.mainClock.advanceTimeBy(48)
    }

    @Test
    fun firstCompositionIsAlreadyHiddenBeforeTheEntranceEffectStarts() {
        var firstComplete: Boolean? = null
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val initial = rememberLibraryListEntrance(Unit, true) { 8 }
            val completeDuringComposition = initial.complete
            SideEffect { if (firstComplete == null) firstComplete = completeDuringComposition }
            Box(Modifier.size(100.dp).libraryListEntrance(initial) { 0 })
        }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { assertTrue(firstComplete == false) }
    }

    @Test
    fun firstActivationOfAMountedListDoesNotExposeAnOpaqueFrame() {
        val enabled = mutableStateOf(false)
        var firstActiveComplete: Boolean? = null
        compose.setContent {
            val initial = rememberLibraryListEntrance(Unit, enabled.value) { 8 }
            val completeDuringComposition = initial.complete
            if (enabled.value) {
                SideEffect {
                    if (firstActiveComplete == null) firstActiveComplete = completeDuringComposition
                }
                Box(Modifier.size(100.dp).libraryListEntrance(initial) { 0 })
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { enabled.value = true }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { assertTrue(firstActiveComplete == false) }
    }

    @Test
    fun liveContentReplacementDoesNotRestartTheEntrance() {
        show()
        compose.runOnIdle {
            assertTrue(!entrance.complete)
            content.value = listOf("one", "two")
        }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { assertTrue(entrance.complete) }
    }

    @Test
    fun leavingDuringEntranceRevealsTheRowsOnReturn() {
        show()
        compose.runOnIdle { active.value = false }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle {
            assertTrue(entrance.complete)
            active.value = true
        }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { assertTrue(entrance.complete) }
    }
}
