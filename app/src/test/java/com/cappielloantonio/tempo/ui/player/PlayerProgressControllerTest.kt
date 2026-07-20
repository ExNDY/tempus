package com.cappielloantonio.tempo.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerProgressControllerTest {
    @Test
    fun `unknown duration normalizes both values to zero`() {
        assertEquals(
            PlayerProgressSnapshot(positionMs = 0L, durationMs = 0L),
            normalizePlayerProgress(positionMs = 9_000L, durationMs = -1L),
        )
    }

    @Test
    fun `position is clamped into known duration`() {
        assertEquals(
            PlayerProgressSnapshot(positionMs = 0L, durationMs = 10_000L),
            normalizePlayerProgress(positionMs = -5L, durationMs = 10_000L),
        )
        assertEquals(
            PlayerProgressSnapshot(positionMs = 10_000L, durationMs = 10_000L),
            normalizePlayerProgress(positionMs = 12_000L, durationMs = 10_000L),
        )
    }
}
