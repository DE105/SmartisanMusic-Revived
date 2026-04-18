package com.smartisanos.music.playback

import androidx.media3.extractor.metadata.id3.BinaryFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddedLyricsTest {

    @Test
    fun `extracts timed lyrics from custom lyrics frame`() {
        val lyrics = extractEmbeddedLyrics(
            entries = listOf(
                TextInformationFrame(
                    "TXXX",
                    "LYRICS",
                    listOf(
                        """
                        [offset:500]
                        [00:01.00]第一句
                        [00:02.00]
                        [00:02.50]第二句
                        """.trimIndent(),
                    ),
                ),
            ),
        )

        requireNotNull(lyrics)
        assertTrue(lyrics.isTimeSynced)
        assertEquals(3, lyrics.lines.size)
        assertEquals("第一句", lyrics.lines[0].text)
        assertEquals(1_500L, lyrics.lines[0].timestampMs)
        assertEquals("", lyrics.lines[1].text)
        assertEquals(2_500L, lyrics.lines[1].timestampMs)
        assertEquals("第二句", lyrics.lines[2].text)
        assertEquals(3_000L, lyrics.lines[2].timestampMs)
    }

    @Test
    fun `extracts unsynced lyrics from uslt frame`() {
        val lyricsText = """
            春天该很好

            你若尚在场
            秋风即使带凉
        """.trimIndent()
        val lyrics = extractEmbeddedLyrics(
            entries = listOf(
                BinaryFrame(
                    "USLT",
                    buildUsltFrameData(lyricsText),
                ),
            ),
        )

        requireNotNull(lyrics)
        assertFalse(lyrics.isTimeSynced)
        assertEquals(
            listOf("春天该很好", "", "你若尚在场", "秋风即使带凉"),
            lyrics.lines.map { it.text },
        )
    }

    @Test
    fun `ignores lyricist metadata`() {
        val lyrics = extractEmbeddedLyrics(
            entries = listOf(
                VorbisComment("LYRICIST", "林夕"),
            ),
        )

        assertNull(lyrics)
    }

    private fun buildUsltFrameData(text: String): ByteArray {
        val description = byteArrayOf(0)
        return byteArrayOf(3) +
            "eng".toByteArray(StandardCharsets.US_ASCII) +
            description +
            text.toByteArray(StandardCharsets.UTF_8)
    }
}
