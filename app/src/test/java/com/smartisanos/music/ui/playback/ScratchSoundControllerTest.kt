package com.smartisanos.music.ui.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class ScratchSoundControllerTest {

    @Test
    fun `scratch rate follows original turntable mapping`() {
        assertEquals(6_000, scratchPlaybackRatePermille(deltaAngleDegrees = 36f, deltaTimeMs = 50L))
        assertEquals(4_861, scratchPlaybackRatePermille(deltaAngleDegrees = 18f, deltaTimeMs = 36L))
    }

    @Test
    fun `scratch rate uses safety clamps for noisy motion samples`() {
        assertEquals(6_000, scratchPlaybackRatePermille(deltaAngleDegrees = 54f, deltaTimeMs = 8L))
        assertEquals(675, scratchPlaybackRatePermille(deltaAngleDegrees = 5f, deltaTimeMs = 90L))
    }
}
