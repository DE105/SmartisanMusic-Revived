package com.smartisan.music.ui.artwork

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkSourceVisibilityTest {
    @Test
    fun explicitRestoreAndDisposalDoNotRestoreTwice() {
        val changes = mutableListOf<Boolean>()
        val visibility = ArtworkSourceVisibility(changes::add)
        visibility.hide()
        visibility.restore()
        visibility.restore()
        assertEquals(listOf(false, true), changes)
    }

    @Test
    fun cancellationBeforeHidingDoesNotChangeTheSource() {
        val changes = mutableListOf<Boolean>()
        val visibility = ArtworkSourceVisibility(changes::add)
        visibility.restore()
        assertEquals(emptyList<Boolean>(), changes)
    }
}
