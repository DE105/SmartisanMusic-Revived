package com.smartisanos.music.ui.playback

import android.content.ContextWrapper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackScreenUtilsTest {

    @Test
    fun `queue track prefers display title and subtitle`() {
        val mediaItem = MediaItem.Builder()
            .setMediaId("42")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setDisplayTitle("Displayed title")
                    .setTitle("Fallback title")
                    .setSubtitle("Displayed artist")
                    .setArtist("Fallback artist")
                    .build(),
            )
            .build()

        val track = mediaItem.toPlaybackQueueTrack(ContextWrapper(null), queueIndex = 7)

        assertEquals("42", track.id)
        assertEquals("Displayed title", track.title)
        assertEquals("Displayed artist", track.artist)
        assertEquals(7, track.queueIndex)
    }

    @Test
    fun `format playback time pads minutes and seconds`() {
        assertEquals("00:00", formatPlaybackTime(-1L))
        assertEquals("01:05", formatPlaybackTime(65_000L))
    }

    @Test
    fun `fraction from position clamps to valid range`() {
        assertEquals(0f, fractionFromPosition(positionX = -12f, trackWidthPx = 120), 0.0001f)
        assertEquals(0.5f, fractionFromPosition(positionX = 60f, trackWidthPx = 120), 0.0001f)
        assertEquals(1f, fractionFromPosition(positionX = 132f, trackWidthPx = 120), 0.0001f)
    }
}
