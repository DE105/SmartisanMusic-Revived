package com.smartisanos.music.ui.navigation

import androidx.annotation.DrawableRes
import com.smartisanos.music.R

const val PlaybackRoute = "playback"

enum class MusicDestination(
    val route: String,
    val label: String,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val selectedIconRes: Int,
) {
    Playlist(
        route = "playlist",
        label = "播放列表",
        iconRes = R.drawable.tabbar_playlist,
        selectedIconRes = R.drawable.tabbar_playlist_down,
    ),
    Artist(
        route = "artist",
        label = "艺术家",
        iconRes = R.drawable.tabbar_artist,
        selectedIconRes = R.drawable.tabbar_artist_down,
    ),
    Album(
        route = "album",
        label = "专辑",
        iconRes = R.drawable.tabbar_album,
        selectedIconRes = R.drawable.tabbar_album_down,
    ),
    Songs(
        route = "songs",
        label = "歌曲",
        iconRes = R.drawable.tabbar_song,
        selectedIconRes = R.drawable.tabbar_song_down,
    ),
    More(
        route = "more",
        label = "更多",
        iconRes = R.drawable.tabbar_more,
        selectedIconRes = R.drawable.tabbar_more_down,
    );
}
