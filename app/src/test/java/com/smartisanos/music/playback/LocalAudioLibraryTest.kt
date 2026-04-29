package com.smartisanos.music.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAudioLibraryTest {

    @Test
    fun `shouldSkipMediaScannerPath skips hidden directories`() {
        assertTrue(shouldSkipMediaScannerPath(".MediaTrash/song.mp3"))
        assertTrue(shouldSkipMediaScannerPath("DCIM/.mediaTrash/song.mp3"))
    }

    @Test
    fun `shouldSkipMediaScannerPath skips hidden files`() {
        assertTrue(shouldSkipMediaScannerPath("Download/.trashed-1778146976-song.mp3"))
    }

    @Test
    fun `shouldSkipMediaScannerPath keeps normal audio paths`() {
        assertFalse(shouldSkipMediaScannerPath("Music/Album/song.mp3"))
    }
}
