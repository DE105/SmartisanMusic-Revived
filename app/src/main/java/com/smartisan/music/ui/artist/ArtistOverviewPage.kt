package com.smartisan.music.ui.artist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.smartisan.music.R
import com.smartisan.music.ui.components.smartisanVerticalScrollbar
import com.smartisan.music.ui.library.LibraryDivider
import com.smartisan.music.ui.library.LibraryFooter
import com.smartisan.music.ui.library.LibrarySummaryRow
import com.smartisan.music.ui.library.libraryTexture

@Composable
internal fun ArtistOverviewPage(
    active: Boolean,
    artists: List<ArtistSummary>,
    onArtistSelected: (ArtistSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    Box(modifier.fillMaxSize().libraryTexture()) {
        if (active)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().smartisanVerticalScrollbar(listState),
            ) {
                items(artists, key = { artist -> artist.id }) { artist ->
                    LibrarySummaryRow(
                        title = artist.name,
                        subtitle =
                            stringResource(
                                R.string.library_artist_summary,
                                pluralStringResource(
                                    R.plurals.library_album_count,
                                    artist.albumCount,
                                    artist.albumCount,
                                ),
                                pluralStringResource(
                                    R.plurals.track_count,
                                    artist.trackCount,
                                    artist.trackCount,
                                ),
                            ),
                        onClick = { onArtistSelected(artist) },
                    )
                    LibraryDivider()
                }
                item(key = "artist-footer") {
                    LibraryFooter(R.plurals.library_artist_count, artists.size)
                }
            }
    }
}
