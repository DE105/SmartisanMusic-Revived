package com.smartisanos.music.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.smartisanos.music.ui.album.AlbumScreen
import com.smartisanos.music.ui.album.AlbumViewMode
import com.smartisanos.music.ui.artist.ArtistScreen
import com.smartisanos.music.ui.more.MoreScreen
import com.smartisanos.music.ui.playlist.PlaylistScreen
import com.smartisanos.music.ui.songs.SongsScreen

@Composable
fun MusicNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    albumViewMode: AlbumViewMode = AlbumViewMode.Tile,
    selectedAlbumId: String? = null,
    selectedArtistId: String? = null,
    showFolderPage: Boolean = false,
    folderEditMode: Boolean = false,
    selectedDirectoryKey: String? = null,
    onAlbumSelected: (String, String) -> Unit = { _, _ -> },
    onAlbumBack: () -> Unit = {},
    onArtistSelected: (String, String) -> Unit = { _, _ -> },
    onArtistBack: () -> Unit = {},
    onMoreEntryClick: (String) -> Unit = {},
    onFolderBack: () -> Unit = {},
    onDirectorySelected: (String, String) -> Unit = { _, _ -> },
    onDirectoryBack: () -> Unit = {},
    onDirectoryEditSelectionChanged: (Set<String>) -> Unit = {},
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
                showFolderPage = showFolderPage,
                folderEditMode = folderEditMode,
                selectedDirectoryKey = selectedDirectoryKey,
                onEntryClick = onMoreEntryClick,
                onFolderBack = onFolderBack,
                onDirectorySelected = onDirectorySelected,
                onDirectoryBack = onDirectoryBack,
                onDirectoryEditSelectionChanged = onDirectoryEditSelectionChanged,
            )
        }
    }
}
