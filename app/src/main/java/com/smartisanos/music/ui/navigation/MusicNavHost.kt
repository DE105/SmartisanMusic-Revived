package com.smartisanos.music.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.common.MediaItem
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
    songsEditMode: Boolean = false,
    selectedSongIdsInEdit: Set<String> = emptySet(),
    moreSecondaryPage: MoreSecondaryPage? = null,
    folderEditMode: Boolean = false,
    selectedDirectoryKey: String? = null,
    selectedGenreId: String? = null,
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
    onGenreSelected: (String, String) -> Unit = { _, _ -> },
    onGenreBack: () -> Unit = {},
    onSongsEditSelectionChanged: (Set<String>) -> Unit = {},
    onRequestAddToPlaylist: (List<MediaItem>) -> Unit = {},
    onRequestAddToQueue: (List<MediaItem>) -> Unit = {},
    onScratchEnabledChange: (Boolean) -> Unit = {},
    onHidePlayerAxisEnabledChange: (Boolean) -> Unit = {},
    onPopcornSoundEnabledChange: (Boolean) -> Unit = {},
) {
    NavHost(
        navController = navController,
        startDestination = MusicDestination.Playlist.route,
        modifier = modifier,
        enterTransition = {
            fadeIn()
        },
        exitTransition = {
            fadeOut()
        },
        popEnterTransition = {
            fadeIn()
        },
        popExitTransition = {
            fadeOut()
        },
    ) {
        composable(MusicDestination.Playlist.route) {
            PlaylistScreen(
                onRequestAddToPlaylist = onRequestAddToPlaylist,
                onRequestAddToQueue = onRequestAddToQueue,
            )
        }
        composable(MusicDestination.Artist.route) {
            ArtistScreen(
                selectedArtistId = selectedArtistId,
                onArtistSelected = onArtistSelected,
                onArtistBack = onArtistBack,
                onAddToPlaylistRequest = onRequestAddToPlaylist,
                onAddToQueueRequest = onRequestAddToQueue,
            )
        }
        composable(MusicDestination.Album.route) {
            AlbumScreen(
                viewMode = albumViewMode,
                selectedAlbumId = selectedAlbumId,
                onAlbumSelected = onAlbumSelected,
                onAlbumBack = onAlbumBack,
                onAddToPlaylistRequest = onRequestAddToPlaylist,
                onAddToQueueRequest = onRequestAddToQueue,
            )
        }
        composable(MusicDestination.Songs.route) {
            SongsScreen(
                editMode = songsEditMode,
                selectedMediaIds = selectedSongIdsInEdit,
                onEditSelectionChanged = onSongsEditSelectionChanged,
            )
        }
        composable(MusicDestination.More.route) {
            MoreScreen(
                secondaryPage = moreSecondaryPage,
                folderEditMode = folderEditMode,
                selectedDirectoryKey = selectedDirectoryKey,
                selectedGenreId = selectedGenreId,
                playbackSettings = playbackSettings,
                onEntryClick = onMoreEntryClick,
                onSecondaryBack = onMoreSecondaryBack,
                onDirectorySelected = onDirectorySelected,
                onDirectoryBack = onDirectoryBack,
                onDirectoryEditSelectionChanged = onDirectoryEditSelectionChanged,
                onGenreSelected = onGenreSelected,
                onGenreBack = onGenreBack,
                onRequestAddToPlaylist = onRequestAddToPlaylist,
                onRequestAddToQueue = onRequestAddToQueue,
                onScratchEnabledChange = onScratchEnabledChange,
                onHidePlayerAxisEnabledChange = onHidePlayerAxisEnabledChange,
                onPopcornSoundEnabledChange = onPopcornSoundEnabledChange,
            )
        }
    }
}
