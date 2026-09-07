package com.smartisan.music.ui.artwork

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryArtworkGeometryTest {
    private val source = Rect(20f, 80f, 80f, 140f)
    private val viewport = Size(400f, 800f)

    @Test
    fun squareCoverStartsExactlyAtTheThumbnailAndEndsCentered() {
        val start = galleryArtworkFrame(source, viewport, Size(400f, 400f), 0f, 0f)
        assertRect(source, start.image)
        assertRect(source, start.clip)
        val end = galleryArtworkFrame(source, viewport, Size(400f, 400f), 1f, 1f)
        assertRect(Rect(0f, 200f, 400f, 600f), end.image)
        assertRect(end.image, end.clip)
    }

    @Test
    fun wideArtworkUncropsWithoutStretching() {
        val start = galleryArtworkFrame(source, viewport, Size(800f, 400f), 0f, 0f)
        assertRect(source, start.clip)
        assertEquals(2f, start.image.width / start.image.height, .0001f)
        val end = galleryArtworkFrame(source, viewport, Size(800f, 400f), 1f, 1f)
        assertRect(Rect(0f, 300f, 400f, 500f), end.image)
    }

    @Test
    fun tallArtworkFitsTheAvailableHeight() {
        val end = galleryArtworkFrame(source, viewport, Size(200f, 800f), 1f, 1f)
        assertRect(Rect(100f, 0f, 300f, 800f), end.image)
    }

    @Test
    fun centerMovesOnAStraightLineWithoutThePreviousBezierDetour() {
        val middle = galleryArtworkFrame(source, viewport, Size(400f, 400f), .5f, 1f)
        assertEquals((source.center.x + 200f) / 2, middle.image.center.x, .0001f)
        assertEquals((source.center.y + 400f) / 2, middle.image.center.y, .0001f)
    }

    @Test
    fun openingSlightlyOvershootsThenSettlesAtTheExactDestination() {
        assertEquals(0f, GalleryOpenEasing.transform(0f), .0001f)
        assertEquals(1.1f, GalleryOpenEasing.transform(175f / 300f), .0001f)
        assertTrue(GalleryOpenEasing.transform(.8f) in 1f..1.1f)
        assertEquals(1f, GalleryOpenEasing.transform(1f), .0001f)
    }

    private fun assertRect(expected: Rect, actual: Rect) {
        assertEquals(expected.left, actual.left, .0001f)
        assertEquals(expected.top, actual.top, .0001f)
        assertEquals(expected.right, actual.right, .0001f)
        assertEquals(expected.bottom, actual.bottom, .0001f)
    }
}
