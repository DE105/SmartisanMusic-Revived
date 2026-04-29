package com.smartisanos.music.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistRecognitionSettingsStoreTest {

    @Test
    fun normalizeArtistRecognitionValueKeepsCommaAsValue() {
        val value = normalizeArtistRecognitionValue(
            " , ",
        )

        assertEquals(",", value)
    }

    @Test
    fun normalizeArtistRecognitionValueTrimsMatchingQuotes() {
        val value = normalizeArtistRecognitionValue(
            " \"feat.\" ",
        )

        assertEquals("feat.", value)
    }

    @Test
    fun normalizeArtistRecognitionValuesDropsBlankAndDuplicateValues() {
        val values = setOf(" / ", "", " /", " ; ").normalizedArtistRecognitionValues()

        assertEquals(setOf("/", ";"), values)
    }

    @Test
    fun normalizeArtistRecognitionValuesDropsCaseInsensitiveDuplicates() {
        val values = setOf(" Band/name ", "band/name", "Singer").normalizedArtistRecognitionValues()

        assertEquals(setOf("Band/name", "Singer"), values)
    }
}
