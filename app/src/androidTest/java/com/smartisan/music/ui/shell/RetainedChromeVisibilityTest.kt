package com.smartisan.music.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RetainedChromeVisibilityTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun overlayDismissalRestoresChromeWithoutReplayingItsEntrance() {
        val visible = mutableStateOf(true)
        var entrances = 0
        var clicks = 0
        compose.setContent {
            Box(Modifier.size(200.dp).retainedChromeVisibility(visible.value)) {
                LaunchedEffect(Unit) { entrances++ }
                Box(Modifier.size(60.dp).testTag("control").clickable { clicks++ })
            }
        }
        compose.onNodeWithTag("control").performClick()
        repeat(3) {
            compose.runOnIdle { visible.value = false }
            compose.onNodeWithTag("control").assertDoesNotExist()
            compose.runOnIdle { visible.value = true }
            compose.onNodeWithTag("control").performClick()
        }
        compose.runOnIdle {
            assertEquals(1, entrances)
            assertEquals(4, clicks)
        }
    }
}
