package com.smartisanos.music.ui.playback

import com.smartisanos.music.playback.EmbeddedLyrics
import com.smartisanos.music.playback.EmbeddedLyricsLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackLyricsRenderModelTest {

    private val fallbackLines = listOf(
        "歌曲名",
        "歌手名",
        "轻触“歌词遮罩”切换播放页",
        "轻触“歌词遮罩”切换歌词页",
        "更多提示",
    )

    @Test
    fun `keeps synced lyrics inactive before first timestamp`() {
        val model = buildPlaybackLyricsRenderModel(
            lyrics = EmbeddedLyrics(
                lines = listOf(
                    EmbeddedLyricsLine(text = "第一句", timestampMs = 1_000L),
                    EmbeddedLyricsLine(text = "第二句", timestampMs = 2_000L),
                ),
                isTimeSynced = true,
            ),
            fallbackLines = fallbackLines,
            currentPositionMs = 500L,
        )

        assertEquals(PlaybackLyricsMode.Timed, model.mode)
        assertEquals(0, model.focusIndex)
        assertEquals(0, model.alphaAnchorIndex)
        assertNull(model.highlightedIndex)
    }

    @Test
    fun `keeps all static lyrics lines reachable`() {
        val model = buildPlaybackLyricsRenderModel(
            lyrics = EmbeddedLyrics(
                lines = (1..20).map { index ->
                    EmbeddedLyricsLine(text = "第$index 行")
                },
                isTimeSynced = false,
            ),
            fallbackLines = fallbackLines,
            currentPositionMs = 0L,
        )

        assertEquals(PlaybackLyricsMode.Static, model.mode)
        assertEquals(20, model.lines.size)
        assertEquals("第20 行", model.lines.last())
        assertNull(model.highlightedIndex)
    }

    @Test
    fun `centers fallback lyrics on the third line`() {
        val model = buildPlaybackLyricsRenderModel(
            lyrics = null,
            fallbackLines = fallbackLines,
            currentPositionMs = 0L,
        )

        assertEquals(PlaybackLyricsMode.Fallback, model.mode)
        assertEquals(2, model.focusIndex)
        assertEquals(2, model.alphaAnchorIndex)
        assertEquals(2, model.highlightedIndex)
    }
}
