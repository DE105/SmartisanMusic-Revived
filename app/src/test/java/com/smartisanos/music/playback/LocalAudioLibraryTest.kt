package com.smartisanos.music.playback

import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
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

    @Test
    fun `fixLegacyMetadataEncoding repairs utf8 text decoded as latin1`() {
        val mojibake = String("周杰伦".toByteArray(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1)

        assertEquals("周杰伦", mojibake.fixLegacyMetadataEncoding())
    }

    @Test
    fun `fixLegacyMetadataEncoding keeps normal metadata`() {
        assertEquals("Björk", "Björk".fixLegacyMetadataEncoding())
        assertEquals("São Paulo", "São Paulo".fixLegacyMetadataEncoding())
        assertEquals("naïve", "naïve".fixLegacyMetadataEncoding())
        assertEquals("周杰伦", "周杰伦".fixLegacyMetadataEncoding())
    }
}
