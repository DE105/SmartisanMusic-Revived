package com.smartisan.music.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartisan.music.ui.components.SmartisanTitleBar
import com.smartisan.music.ui.shell.PageStackTransition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransitionBoundaryTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun fullScreenMovesTheTitleAndBodyTogetherAndDoesNotProjectTheTitle() {
        val visible = mutableStateOf(false)
        val outlet = ProjectedNavigationTitle()
        compose.setContent {
            CompositionLocalProvider(LocalProjectedNavigationTitle provides outlet) {
                Box(Modifier.size(240.dp, 400.dp).background(Color.Gray)) {
                    SmartisanFullScreenTransition(visible.value) {
                        Column(Modifier.fillMaxSize().background(Color.White)) {
                            SmartisanTitleBar(
                                "Settings",
                                includeStatusBar = false,
                                contentHeight = 40.dp,
                                modifier = Modifier.testTag("moving-title"),
                            )
                            Box(Modifier.fillMaxWidth().height(200.dp).testTag("moving-body"))
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { visible.value = true }
        compose.mainClock.advanceTimeBy(96)
        val title = compose.onNodeWithTag("moving-title").fetchSemanticsNode().boundsInRoot
        val body = compose.onNodeWithTag("moving-body").fetchSemanticsNode().boundsInRoot
        assertTrue("The whole page must start below the viewport top", title.top > 0f)
        assertEquals(0f, title.left, 1f)
        assertEquals(title.bottom, body.top, 1f)
        compose.runOnIdle { assertNull(outlet.content) }
        compose.mainClock.advanceTimeBy(400)
        assertEquals(
            0f,
            compose.onNodeWithTag("moving-title").fetchSemanticsNode().boundsInRoot.top,
            1f,
        )
        compose.runOnIdle { visible.value = false }
        compose.mainClock.advanceTimeBy(96)
        assertTrue(compose.onNodeWithTag("moving-title").fetchSemanticsNode().boundsInRoot.top > 0f)
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("Settings").assertDoesNotExist()
    }

    @Test
    fun projectedTitleStaysCenteredWhileTheDestinationBodySlides() {
        val destination = mutableStateOf<String?>(null)
        compose.setContent {
            PageStackTransition(
                destination.value,
                Modifier.size(240.dp, 400.dp),
                projectTitles = true,
                primaryContent = {
                    Column {
                        SmartisanTitleBar("More")
                        Box(Modifier.fillMaxSize())
                    }
                },
                secondaryContent = {
                    Column {
                        SmartisanTitleBar("Genre")
                        Box(Modifier.fillMaxSize().testTag("genre-body"))
                    }
                },
            )
        }
        compose.waitForIdle()
        val rootCenter = compose.onNodeWithText("More").fetchSemanticsNode().boundsInRoot.center.x
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { destination.value = "genre" }
        compose.mainClock.advanceTimeBy(208)
        val incoming = compose.onNodeWithText("Genre").fetchSemanticsNode().boundsInRoot
        val body = compose.onNodeWithTag("genre-body").fetchSemanticsNode().positionInRoot
        assertEquals(rootCenter, incoming.center.x, 1f)
        assertTrue("Only the content area translates", body.x > 0f)
        compose.mainClock.advanceTimeBy(400)
        compose.onAllNodesWithText("Genre").assertCountEquals(1)
        compose.runOnIdle { destination.value = null }
        compose.mainClock.advanceTimeBy(400)
        compose.onNodeWithText("Genre").assertDoesNotExist()
        compose.onAllNodesWithText("More").assertCountEquals(1)
    }
}
