package com.smartisan.music.ui.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartisan.music.ui.album.AlbumViewMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlbumLayoutTransitionTest {
    @get:Rule val compose = createComposeRule()

    private var mode by mutableStateOf(AlbumViewMode.List)
    private var active by mutableStateOf(true)
    private var entries by
        mutableStateOf(
            List(40) {
                LibraryAlbumEntry("entry-$it", "Album $it", "Artist", null, emptyList())
            }
        )
    private val clicks = mutableListOf<String>()

    private fun showCollection() {
        compose.setContent {
            Box(Modifier.size(360.dp, 560.dp)) {
                SmartisanAlbumCollection(entries, active, mode, null, { clicks += it.id })
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    private fun assertMode(expected: AlbumViewMode) {
        val first =
            compose.onNodeWithText("Album 0").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val second =
            compose.onNodeWithText("Album 1").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        if (expected == AlbumViewMode.List) assertTrue(second.top > first.bottom)
        else assertEquals(first.top, second.top, 1f)
        compose.onNodeWithText("Album 0").performClick()
        compose.runOnIdle { assertEquals("entry-0", clicks.last()) }
    }

    @Test
    fun switchingBothWaysProducesVisibleInteractiveDestination() {
        showCollection()
        compose.runOnIdle { mode = AlbumViewMode.Tile }
        compose.mainClock.advanceTimeBy(1400)
        assertMode(AlbumViewMode.Tile)
        compose.runOnIdle { mode = AlbumViewMode.List }
        compose.mainClock.advanceTimeBy(1400)
        assertMode(AlbumViewMode.List)
    }

    @Test
    fun reversingDuringGridRevealAndCoverTravelKeepsTheLatestMode() {
        showCollection()
        compose.runOnIdle { mode = AlbumViewMode.Tile }
        compose.mainClock.advanceTimeBy(48)
        compose.runOnIdle { mode = AlbumViewMode.List }
        compose.mainClock.advanceTimeBy(1400)
        assertMode(AlbumViewMode.List)
        compose.runOnIdle { mode = AlbumViewMode.Tile }
        compose.mainClock.advanceTimeBy(160)
        compose.runOnIdle { mode = AlbumViewMode.List }
        compose.mainClock.advanceTimeBy(1400)
        assertMode(AlbumViewMode.List)
    }

    @Test
    fun leavingDuringTransitionDoesNotRetainInvisibleContent() {
        showCollection()
        compose.runOnIdle { mode = AlbumViewMode.Tile }
        compose.mainClock.advanceTimeBy(160)
        compose.runOnIdle { active = false }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { active = true }
        compose.mainClock.advanceTimeBy(1400)
        assertMode(AlbumViewMode.Tile)
    }

    @Test
    fun scrolledAnchorIsPreservedAcrossBothLayouts() {
        showCollection()
        compose.onNodeWithTag("album-List").performScrollToIndex(12)
        compose.runOnIdle { mode = AlbumViewMode.Tile }
        compose.mainClock.advanceTimeBy(1400)
        compose.onNodeWithText("Album 12").assertIsDisplayed()
        compose.runOnIdle { mode = AlbumViewMode.List }
        compose.mainClock.advanceTimeBy(1400)
        compose.onNodeWithText("Album 12").assertIsDisplayed()
    }

    @Test
    fun removingEntriesDuringCoverTravelDisposesSharedContent() {
        showCollection()
        compose.runOnIdle { mode = AlbumViewMode.Tile }
        compose.mainClock.advanceTimeBy(1400)
        compose.runOnIdle { mode = AlbumViewMode.List }
        compose.mainClock.advanceTimeBy(80)
        compose.runOnIdle { entries = emptyList() }
        compose.mainClock.advanceTimeBy(1400)
        compose.onNodeWithText("Album 0").assertDoesNotExist()
        compose.onNodeWithTag("album-List").assertExists()
    }
}
