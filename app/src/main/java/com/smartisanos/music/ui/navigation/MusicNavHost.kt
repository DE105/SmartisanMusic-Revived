package com.smartisanos.music.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartisanos.music.data.settings.PlaybackSettings
import com.smartisanos.music.ui.album.AlbumScreen
import com.smartisanos.music.ui.album.AlbumViewMode
import com.smartisanos.music.ui.artist.ArtistScreen
import com.smartisanos.music.ui.more.MoreScreen
import com.smartisanos.music.ui.more.MoreSecondaryPage
import com.smartisanos.music.ui.playlist.PlaylistScreen
import com.smartisanos.music.ui.songs.SongsScreen

@Composable
fun MusicNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    albumViewMode: AlbumViewMode = AlbumViewMode.Tile,
    selectedAlbumId: String? = null,
    selectedArtistId: String? = null,
    moreSecondaryPage: MoreSecondaryPage? = null,
    folderEditMode: Boolean = false,
    selectedDirectoryKey: String? = null,
    playbackSettings: PlaybackSettings = PlaybackSettings(),
    onAlbumSelected: (String, String) -> Unit = { _, _ -> },
    onAlbumBack: () -> Unit = {},
    onArtistSelected: (String, String) -> Unit = { _, _ -> },
    onArtistBack: () -> Unit = {},
    onMoreEntryClick: (String) -> Unit = {},
    onMoreSecondaryBack: () -> Unit = {},
    onDirectorySelected: (String, String) -> Unit = { _, _ -> },
    onDirectoryBack: () -> Unit = {},
    onDirectoryEditSelectionChanged: (Set<String>) -> Unit = {},
    onScratchEnabledChange: (Boolean) -> Unit = {},
    onHidePlayerAxisEnabledChange: (Boolean) -> Unit = {},
    onPopcornSoundEnabledChange: (Boolean) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = MusicDestination.Playlist.route,
        modifier = modifier,
    ) {
        composable(MusicDestination.Playlist.route) {
            PlaylistScreen()
        }
        composable(MusicDestination.Artist.route) {
            ArtistScreen(
                selectedArtistId = selectedArtistId,
                onArtistSelected = onArtistSelected,
                onArtistBack = onArtistBack,
            )
        }
        composable(MusicDestination.Album.route) {
            AlbumScreen(
                viewMode = albumViewMode,
                selectedAlbumId = selectedAlbumId,
                onAlbumSelected = onAlbumSelected,
                onAlbumBack = onAlbumBack,
            )
        }
        composable(MusicDestination.Songs.route) {
            SongsScreen()
        }
        composable(MusicDestination.More.route) {
            MoreScreen(
                secondaryPage = moreSecondaryPage,
                folderEditMode = folderEditMode,
                selectedDirectoryKey = selectedDirectoryKey,
                playbackSettings = playbackSettings,
                onEntryClick = onMoreEntryClick,
                onSecondaryBack = onMoreSecondaryBack,
                onDirectorySelected = onDirectorySelected,
                onDirectoryBack = onDirectoryBack,
                onDirectoryEditSelectionChanged = onDirectoryEditSelectionChanged,
                onScratchEnabledChange = onScratchEnabledChange,
                onHidePlayerAxisEnabledChange = onHidePlayerAxisEnabledChange,
                onPopcornSoundEnabledChange = onPopcornSoundEnabledChange,
            )
        }
    }
}
