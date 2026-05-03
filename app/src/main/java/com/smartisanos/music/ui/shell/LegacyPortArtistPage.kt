package com.smartisanos.music.ui.shell

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.GridView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.replaceQueueAndPlay
import com.smartisanos.music.ui.album.AlbumSummary
import com.smartisanos.music.ui.album.AlbumViewMode
import com.smartisanos.music.ui.album.buildAlbumSummaries
import com.smartisanos.music.ui.artist.ArtistSummary
import com.smartisanos.music.ui.artist.buildArtistSummaries
import java.text.Collator
import java.util.Locale
import kotlin.random.Random

internal const val ArtistAlbumSwitchBaseDurationMillis = 150L
internal const val ArtistAlbumSwitchStaggerMillis = 10L
internal const val LegacyArtistListFooterThreshold = 8
internal val LegacyArtistPrimaryTextColor = Color.rgb(0x35, 0x35, 0x39)
internal val LegacyArtistSecondaryTextColor = Color.rgb(0xa4, 0xa7, 0xac)
internal val LegacyArtistFooterTextColor = Color.rgb(0xbc, 0xbc, 0xbc)

internal sealed interface LegacyArtistTarget {
    val artistId: String
    val artistName: String
    val title: String

    data class Albums(
        override val artistId: String,
        override val artistName: String,
    ) : LegacyArtistTarget {
        override val title: String = artistName
    }

    data class AllSongs(
        override val artistId: String,
        override val artistName: String,
        override val title: String,
    ) : LegacyArtistTarget

    data class Album(
        override val artistId: String,
        override val artistName: String,
        val albumId: String,
        override val title: String,
        val fromArtistAlbums: Boolean,
    ) : LegacyArtistTarget
}

internal fun LegacyArtistTarget.parentTarget(): LegacyArtistTarget? {
    return when (this) {
        is LegacyArtistTarget.Albums -> null
        is LegacyArtistTarget.AllSongs -> LegacyArtistTarget.Albums(
            artistId = artistId,
            artistName = artistName,
        )
        is LegacyArtistTarget.Album -> if (fromArtistAlbums) {
            LegacyArtistTarget.Albums(
                artistId = artistId,
                artistName = artistName,
            )
        } else {
            null
        }
    }
}

internal val LegacyArtistTarget.showsAlbumSwitch: Boolean
    get() = this is LegacyArtistTarget.Albums

@Composable
internal fun LegacyPortArtistPage(
    mediaItems: List<MediaItem>,
    active: Boolean,
    selectedTarget: LegacyArtistTarget?,
    albumViewMode: AlbumViewMode,
    hiddenMediaIds: Set<String>,
    onTargetChanged: (LegacyArtistTarget?) -> Unit,
    onRequestAddToPlaylist: (List<MediaItem>) -> Unit,
    onRequestAddToQueue: (List<MediaItem>) -> Unit,
    onTrackMoreClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val browser = LocalPlaybackBrowser.current
    val visibleSongs = remember(mediaItems, hiddenMediaIds) {
        mediaItems.filterNot { mediaItem -> mediaItem.mediaId in hiddenMediaIds }
    }
    val artists = remember(visibleSongs, context) {
        buildArtistSummaries(
            mediaItems = visibleSongs,
            unknownArtistTitle = context.getString(R.string.unknown_artist),
            unknownAlbumTitle = context.getString(R.string.unknown_album),
        )
    }
    val selectedArtist = remember(artists, selectedTarget) {
        selectedTarget?.artistId?.let { artistId ->
            artists.firstOrNull { artist -> artist.id == artistId }
        }
    }
    val selectedArtistAlbums = remember(selectedArtist, context) {
        selectedArtist?.albumSummaries(context).orEmpty()
    }
    val selectedArtistState = remember(selectedArtist, selectedArtistAlbums, selectedTarget) {
        if (selectedArtist != null && selectedTarget != null) {
            LegacySelectedArtistState(
                artist = selectedArtist,
                target = selectedTarget,
                albums = selectedArtistAlbums,
            )
        } else {
            null
        }
    }
    val switchAnimator = remember { LegacyArtistAlbumViewSwitchAnimator() }

    LegacyPortPageStackTransition(
        secondaryKey = selectedArtistState,
        modifier = modifier,
        label = "legacy artist transition",
        primaryContent = {
            LegacyPortArtistOverviewPage(
                active = active,
                artists = artists,
                onArtistSelected = { artist ->
                    val albums = artist.albumSummaries(context)
                    val target = if (albums.size > 1) {
                        LegacyArtistTarget.Albums(
                            artistId = artist.id,
                            artistName = artist.name,
                        )
                    } else {
                        val album = albums.firstOrNull()
                        if (album == null) {
                            LegacyArtistTarget.AllSongs(
                                artistId = artist.id,
                                artistName = artist.name,
                                title = context.getString(R.string.artist_all_songs),
                            )
                        } else {
                            LegacyArtistTarget.Album(
                                artistId = artist.id,
                                artistName = artist.name,
                                albumId = album.id,
                                title = album.title,
                                fromArtistAlbums = false,
                            )
                        }
                    }
                    onTargetChanged(target)
                },
                modifier = Modifier.fillMaxSize(),
            )
        },
        secondaryContent = { state ->
            LegacyPortSelectedArtistPage(
                artist = state.artist,
                albums = state.albums.ifEmpty { state.artist.albumSummaries(context) },
                target = selectedTarget?.takeIf { target -> target.artistId == state.artist.id } ?: state.target,
                browser = browser,
                albumViewMode = albumViewMode,
                onTargetChanged = onTargetChanged,
                onRequestAddToPlaylist = onRequestAddToPlaylist,
                onRequestAddToQueue = onRequestAddToQueue,
                onTrackMoreClick = onTrackMoreClick,
                switchAnimator = switchAnimator,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}
