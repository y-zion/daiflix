package com.yzion.daiflix.tv.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure-logic coverage for the single BACK decision of the player screen (audit P1-4).
 *
 * The regression being locked in: one BACK press must map to exactly one action, and
 * only [PlayerBackAction.EXIT_PLAYER] is allowed to pop the back stack (one level).
 */
class PlayerBackActionTest {

    @Test
    fun `open panel consumes back and only closes the panel`() {
        assertEquals(
            PlayerBackAction.CLOSE_PANEL,
            playerBackAction(panelOpen = true, hasError = false, hudVisible = true)
        )
        assertEquals(
            PlayerBackAction.CLOSE_PANEL,
            playerBackAction(panelOpen = true, hasError = false, hudVisible = false)
        )
    }

    @Test
    fun `panel wins over an error state so it can still be dismissed`() {
        assertEquals(
            PlayerBackAction.CLOSE_PANEL,
            playerBackAction(panelOpen = true, hasError = true, hudVisible = true)
        )
        assertEquals(
            PlayerBackAction.CLOSE_PANEL,
            playerBackAction(panelOpen = true, hasError = true, hudVisible = false)
        )
    }

    @Test
    fun `error state leaves the player exactly one level`() {
        assertEquals(
            PlayerBackAction.EXIT_PLAYER,
            playerBackAction(panelOpen = false, hasError = true, hudVisible = true)
        )
        assertEquals(
            PlayerBackAction.EXIT_PLAYER,
            playerBackAction(panelOpen = false, hasError = true, hudVisible = false)
        )
    }

    @Test
    fun `normal playback hides the hud before leaving`() {
        assertEquals(
            PlayerBackAction.HIDE_HUD,
            playerBackAction(panelOpen = false, hasError = false, hudVisible = true)
        )
    }

    @Test
    fun `normal playback with the hud hidden leaves exactly one level`() {
        assertEquals(
            PlayerBackAction.EXIT_PLAYER,
            playerBackAction(panelOpen = false, hasError = false, hudVisible = false)
        )
    }
}
