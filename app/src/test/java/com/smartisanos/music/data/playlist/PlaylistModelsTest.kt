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

    @Test
    fun `compactPlaylistEntriesAfterRemoval removes deleted media ids and compacts sort order`() {
        val entries = listOf(
            PlaylistEntryEntity(
                playlistId = "playlist-1",
                mediaId = "10",
                sortOrder = 0,
                addedAt = 100L,
            ),
            PlaylistEntryEntity(
                playlistId = "playlist-1",
                mediaId = "20",
                sortOrder = 1,
                addedAt = 200L,
            ),
            PlaylistEntryEntity(
                playlistId = "playlist-1",
                mediaId = "30",
                sortOrder = 2,
                addedAt = 300L,
            ),
        )

        val result = compactPlaylistEntriesAfterRemoval(
            playlistId = "playlist-1",
            entries = entries,
            mediaIds = setOf("20"),
        )

        assertEquals(listOf("10", "30"), result.map(PlaylistEntryEntity::mediaId))
        assertEquals(listOf(0, 1), result.map(PlaylistEntryEntity::sortOrder))
        assertEquals(listOf(100L, 300L), result.map(PlaylistEntryEntity::addedAt))
    }
}
