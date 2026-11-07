package com.yzion.daiflix.tv.ui.screen

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the hold-to-accelerate seek decision (audit P3-1).
 *
 * The step list itself and [nextSeekStepIndex] are the whole policy: 10s -> 30s ->
 * 60s -> 300s while the gesture continues, restart at 10s otherwise.
 */
class PlayerSeekGestureTest {

    @Test
    fun `seek steps accelerate from 10s to 300s`() {
        assertArrayEquals(
            longArrayOf(10_000L, 30_000L, 60_000L, 300_000L),
            PLAYER_SEEK_STEPS_MS
        )
    }

    @Test
    fun `new gesture starts at 10s`() {
        assertEquals(0, nextSeekStepIndex(current = 3, continuing = false))
        assertEquals(0, nextSeekStepIndex(current = 0, continuing = false))
    }

    @Test
    fun `held gesture advances one step per repeat`() {
        var step = 0
        step = nextSeekStepIndex(step, continuing = true)
        assertEquals(1, step)
        step = nextSeekStepIndex(step, continuing = true)
        assertEquals(2, step)
        step = nextSeekStepIndex(step, continuing = true)
        assertEquals(3, step)
    }

    @Test
    fun `held gesture clamps at the last step`() {
        assertEquals(
            PLAYER_SEEK_STEPS_MS.lastIndex,
            nextSeekStepIndex(PLAYER_SEEK_STEPS_MS.lastIndex, continuing = true)
        )
    }
}
