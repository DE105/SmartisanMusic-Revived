package com.smartisanos.music.data.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistModelsTest {

    @Test
    fun `nextUntitledPlaylistName starts from one`() {
        assertEquals(
            "未命名播放列表 1",
            nextUntitledPlaylistName(emptyList()),
        )
    }

    @Test
    fun `nextUntitledPlaylistName skips occupied indexes`() {
        assertEquals(
            "未命名播放列表 3",
            nextUntitledPlaylistName(
                listOf(
                    "未命名播放列表 1",
                    "未命名播放列表 2",
                    "收藏夹",
                ),
            ),
        )
    }

    @Test
    fun `nextUntitledPlaylistName fills first gap`() {
        assertEquals(
            "未命名播放列表 2",
            nextUntitledPlaylistName(
                listOf(
                    "未命名播放列表 1",
                    "未命名播放列表 3",
                ),
            ),
        )
    }
}
