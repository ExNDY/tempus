package com.cappielloantonio.tempo.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueRestoreTest {
    @Test
    fun `empty queue has no restore target`() {
        assertNull(normalizeQueueRestoreTarget(3, 10_000L, 0))
    }

    @Test
    fun `restore target clamps damaged index and negative position`() {
        assertEquals(
            QueueRestoreTarget(index = 2, positionMs = 0L),
            normalizeQueueRestoreTarget(Int.MAX_VALUE, -1L, 3),
        )
        assertEquals(
            QueueRestoreTarget(index = 0, positionMs = 42L),
            normalizeQueueRestoreTarget(-7, 42L, 3),
        )
    }

    @Test
    fun `snapshot applies only to unchanged empty live player`() {
        assertTrue(shouldApplyQueueRestore(4L, 4L, false, 0))
        assertFalse(shouldApplyQueueRestore(4L, 5L, false, 0))
        assertFalse(shouldApplyQueueRestore(4L, 4L, false, 1))
        assertFalse(shouldApplyQueueRestore(4L, 4L, true, 0))
    }
}
