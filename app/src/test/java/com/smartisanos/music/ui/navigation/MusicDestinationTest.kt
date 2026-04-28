package com.smartisanos.music.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicDestinationTest {

    @Test
    fun fromRouteReturnsDestinationForKnownRoute() {
        assertEquals(MusicDestination.Album, MusicDestination.fromRoute("album"))
    }

    @Test
    fun fromRouteReturnsNullForUnknownRoute() {
        assertNull(MusicDestination.fromRoute("missing"))
    }

    @Test
    fun fromRouteOrDefaultFallsBackToSongs() {
        assertEquals(MusicDestination.Songs, MusicDestination.fromRouteOrDefault(null))
        assertEquals(MusicDestination.Songs, MusicDestination.fromRouteOrDefault(""))
        assertEquals(MusicDestination.Songs, MusicDestination.fromRouteOrDefault("missing"))
    }
}
