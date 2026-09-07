package com.smartisan.music.ui.library

import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartisan.music.ui.album.AlbumViewMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlbumCollectionEntranceTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var scope: CoroutineScope
    private val entrance = AlbumCollectionEntrance(true)
    private val albums = listOf("first", "second", "third")

    private fun show() {
        compose.setContent { scope = rememberCoroutineScope() }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
    }

    @Test
    fun loadingDoesNotSpendTheGridEntranceAndItemsRevealInOrder() {
        show()
        compose.mainClock.advanceTimeBy(1200)
        compose.runOnIdle {
            assertEquals(AlbumEntrancePhase.Waiting, entrance.phase)
            assertEquals(0f, entrance.alpha("first"), 0f)
            scope.launch { entrance.reveal(albums, AlbumViewMode.Tile) }
        }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle {
            assertTrue(entrance.alpha("first") > entrance.alpha("second"))
            assertEquals(0f, entrance.alpha("third"), 0f)
        }
        compose.mainClock.advanceTimeBy(500)
        compose.runOnIdle {
            assertEquals(AlbumEntrancePhase.Complete, entrance.phase)
            albums.forEach { assertEquals(1f, entrance.alpha(it), 0f) }
        }
    }

    @Test
    fun listModeUsesTheListEntranceAndLaterUpdatesDoNotReplayIt() {
        show()
        compose.runOnIdle { scope.launch { entrance.reveal(albums, AlbumViewMode.List) } }
        compose.mainClock.advanceTimeBy(220)
        compose.runOnIdle {
            assertEquals(AlbumEntrancePhase.Complete, entrance.phase)
            scope.launch { entrance.reveal(albums + "new", AlbumViewMode.List) }
        }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle {
            assertEquals(AlbumEntrancePhase.Complete, entrance.phase)
            assertEquals(1f, entrance.alpha("new"), 0f)
        }
    }

    @Test
    fun leavingOrSwitchingLayoutsDuringEntranceCannotLeaveRowsHidden() {
        show()
        lateinit var job: Job
        compose.runOnIdle { job = scope.launch { entrance.reveal(albums, AlbumViewMode.Tile) } }
        compose.mainClock.advanceTimeBy(64)
        compose.runOnIdle { job.cancel() }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle {
            assertEquals(AlbumEntrancePhase.Complete, entrance.phase)
            albums.forEach { assertEquals(1f, entrance.alpha(it), 0f) }
        }
    }
}
