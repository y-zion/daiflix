package com.yzion.daiflix.tv.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the loading overlay label (audit P2-9): the label must always name
 * the episode, and the title only when the state actually has one.
 */
class PlayerLoadingLabelTest {

    @Test
    fun `label contains title and episode name`() {
        assertEquals(
            "正在加载 · 庆余年 第01集",
            playerLoadingLabel(vodTitle = "庆余年", episodeName = "第01集", episodeIndex = 0)
        )
    }

    @Test
    fun `label omits a blank title`() {
        assertEquals(
            "正在加载 · 第5集",
            playerLoadingLabel(vodTitle = "   ", episodeName = null, episodeIndex = 4)
        )
    }

    @Test
    fun `label falls back to the episode index`() {
        assertEquals(
            "正在加载 · 第1集",
            playerLoadingLabel(vodTitle = "", episodeName = null, episodeIndex = 0)
        )
    }
}
