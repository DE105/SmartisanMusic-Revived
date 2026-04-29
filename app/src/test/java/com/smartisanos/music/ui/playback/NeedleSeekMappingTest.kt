package com.smartisanos.music.ui.playback

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NeedleSeekMappingTest {

    @Test
    fun `needle playback arc maps to current media position`() {
        val durationMs = 200_000L

        assertEquals(0L, needleSeekPositionFromRotation(rotationDegrees = 14f, durationMs))
        assertEquals(100_000L, needleSeekPositionFromRotation(rotationDegrees = 19f, durationMs))
        assertEquals(200_000L, needleSeekPositionFromRotation(rotationDegrees = 24f, durationMs))
    }

    @Test
    fun `needle rest arc does not seek`() {
        assertNull(needleSeekPositionFromRotation(rotationDegrees = -12f, durationMs = 200_000L))
        assertNull(needleSeekPositionFromRotation(rotationDegrees = 13.99f, durationMs = 200_000L))
        assertNull(needleSeekPositionFromRotation(rotationDegrees = 19f, durationMs = 0L))
    }

    @Test
    fun `needle point mapping clamps to supported arc`() {
        val containerSize = IntSize(width = 360, height = 357)
        val beyondEndPoint = playbackNeedleGeometry(
            containerSize = containerSize,
            layoutScalePx = 1f,
            rotationDegrees = 40f,
        ).tip
        val beforeRestPoint = playbackNeedleGeometry(
            containerSize = containerSize,
            layoutScalePx = 1f,
            rotationDegrees = -30f,
        ).tip

        assertEquals(
            24f,
            needleSeekRotationFromPoint(
                point = beyondEndPoint,
                containerSize = containerSize,
                layoutScalePx = 1f,
            ),
            0.001f,
        )
        assertEquals(
            -12f,
            needleSeekRotationFromPoint(
                point = beforeRestPoint,
                containerSize = containerSize,
                layoutScalePx = 1f,
            ),
            0.001f,
        )
    }

    @Test
    fun `angle delta normalizes across opposite angle boundary`() {
        assertEquals(2f, normalizeAngleDelta(-179f - 179f), 0.001f)
        assertEquals(-2f, normalizeAngleDelta(179f - -179f), 0.001f)
    }

    @Test
    fun `needle hit region follows visible arm segment`() {
        val containerSize = IntSize(width = 360, height = 357)
        val geometry = playbackNeedleGeometry(
            containerSize = containerSize,
            layoutScalePx = 1f,
            rotationDegrees = 14f,
        )
        val middleOfArm = Offset(
            x = (geometry.pivot.x + geometry.tip.x) / 2f,
            y = (geometry.pivot.y + geometry.tip.y) / 2f,
        )
        val farFromArm = Offset(
            x = middleOfArm.x + 80f,
            y = middleOfArm.y,
        )

        assertTrue(
            isWithinNeedleSeekRegion(
                point = middleOfArm,
                containerSize = containerSize,
                layoutScalePx = 1f,
                rotationDegrees = 14f,
                tolerancePx = 28f,
            ),
        )
        assertFalse(
            isWithinNeedleSeekRegion(
                point = farFromArm,
                containerSize = containerSize,
                layoutScalePx = 1f,
                rotationDegrees = 14f,
                tolerancePx = 28f,
            ),
        )
    }

    @Test
    fun `needle hit region includes pickup head`() {
        val containerSize = IntSize(width = 360, height = 357)
        val geometry = playbackNeedleGeometry(
            containerSize = containerSize,
            layoutScalePx = 1f,
            rotationDegrees = 14f,
        )
        val justOutsidePickup = Offset(
            x = geometry.pickupCenter.x + 44f,
            y = geometry.pickupCenter.y,
        )

        assertTrue(
            isWithinNeedleSeekRegion(
                point = geometry.pickupCenter,
                containerSize = containerSize,
                layoutScalePx = 1f,
                rotationDegrees = 14f,
                tolerancePx = 28f,
            ),
        )
        assertFalse(
            isWithinNeedleSeekRegion(
                point = justOutsidePickup,
                containerSize = containerSize,
                layoutScalePx = 1f,
                rotationDegrees = 14f,
                tolerancePx = 28f,
            ),
        )
    }

    @Test
    fun `disc tap inside touch slop can toggle lyrics`() {
        val center = Offset(100f, 100f)
        val initialPosition = Offset(140f, 100f)
        val finalPosition = Offset(146f, 104f)

        assertTrue(
            isDiscTapWithinSlop(
                initialPosition = initialPosition,
                finalPosition = finalPosition,
                maxMoveDistance = 8f,
                center = center,
                radius = 80f,
                tapTouchSlop = 12f,
            ),
        )
    }

    @Test
    fun `disc movement past touch slop is scratch gesture`() {
        val center = Offset(100f, 100f)
        val initialPosition = Offset(140f, 100f)
        val finalPosition = Offset(160f, 120f)

        assertFalse(
            isDiscTapWithinSlop(
                initialPosition = initialPosition,
                finalPosition = finalPosition,
                maxMoveDistance = 28f,
                center = center,
                radius = 80f,
                tapTouchSlop = 12f,
            ),
        )
    }

    @Test
    fun `scratch start position uses playback position at drag start`() {
        assertEquals(42_000L, scratchStartPosition(positionMs = 42_000L, durationMs = 180_000L))
        assertEquals(180_000L, scratchStartPosition(positionMs = 181_500L, durationMs = 180_000L))
    }
}
