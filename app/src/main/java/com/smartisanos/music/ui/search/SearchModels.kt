package com.smartisanos.music.ui.search

import androidx.media3.common.MediaItem
import com.smartisanos.music.ui.album.AlbumSummary
import com.smartisanos.music.ui.album.buildAlbumSummaries
import com.smartisanos.music.ui.artist.ArtistSummary
import com.smartisanos.music.ui.artist.buildArtistSummaries
import java.util.Locale

internal data class SearchResults(
    val query: String,
    val songs: List<MediaItem>,
    val albums: List<AlbumSummary>,
    val artists: List<ArtistSummary>,
) {
    val hasResults: Boolean = songs.isNotEmpty() || albums.isNotEmpty() || artists.isNotEmpty()
}

internal fun buildSearchResults(
    query: String,
    songs: List<MediaItem>,
    unknownAlbumTitle: String,
    unknownArtistTitle: String,
    multipleArtistsTitle: String,
): SearchResults {
    val normalizedQuery = normalizeSearchQuery(query)
    if (normalizedQuery.isEmpty()) {
        return SearchResults(
            query = normalizedQuery,
            songs = emptyList(),
            albums = emptyList(),
            artists = emptyList(),
        )
    }

    val matchedSongs = songs.filter { item ->
        item.searchableSongFields().any { field ->
            field.contains(normalizedQuery)
        }
    }
    val matchedAlbums = buildAlbumSummaries(
        mediaItems = songs,
        unknownAlbumTitle = unknownAlbumTitle,
        multipleArtistsTitle = multipleArtistsTitle,
    ).filter { album ->
        album.searchableAlbumFields().any { field ->
            field.contains(normalizedQuery)
        }
    }
    val matchedArtists = buildArtistSummaries(
        mediaItems = songs,
        unknownArtistTitle = unknownArtistTitle,
        unknownAlbumTitle = unknownAlbumTitle,
    ).filter { artist ->
        artist.searchableArtistFields().any { field ->
            field.contains(normalizedQuery)
        }
    }

    return SearchResults(
        query = normalizedQuery,
        songs = matchedSongs,
        albums = matchedAlbums,
        artists = matchedArtists,
    )
}

internal fun normalizeSearchQuery(query: String): String {
    return query.trim().lowercase(Locale.ROOT)
}

private fun MediaItem.searchableSongFields(): List<String> {
    val metadata = mediaMetadata
    return listOfNotNull(
        metadata.title?.toString(),
        metadata.displayTitle?.toString(),
        metadata.artist?.toString(),
        metadata.albumTitle?.toString(),
        metadata.albumArtist?.toString(),
    ).map(::normalizeSearchQuery)
}

private fun AlbumSummary.searchableAlbumFields(): List<String> {
    return listOf(title, artist).map(::normalizeSearchQuery)
}

private fun ArtistSummary.searchableArtistFields(): List<String> {
    return listOf(name).map(::normalizeSearchQuery)
}
