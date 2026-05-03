package com.smartisanos.music.ui.shell.playback

import android.graphics.Bitmap
import android.graphics.Color
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import com.smartisanos.music.R
import com.smartisanos.music.isExternalAudioLaunchItem
import com.smartisanos.music.playback.loadArtworkBitmap

@Composable
internal fun LegacyPortPlaybackBar(
    snapshot: LegacyPlaybackBarSnapshot,
    favoriteIds: Set<String>,
    artworkBitmap: Bitmap?,
    onOpenPlayback: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            val root = LayoutInflater.from(context).inflate(R.layout.playback_bar, null, false)

            root.setBackgroundColor(Color.TRANSPARENT)
            root.findViewById<ImageView>(R.id.album_art)?.setImageResource(R.drawable.noalbumcover_220)
            root.findViewById<View>(R.id.playback_bar_shadow)?.setBackgroundResource(R.drawable.now_playing_bar_shadow)

            root
        },
        update = { root ->
            val mediaItem = snapshot.mediaItem
            val mediaId = mediaItem?.mediaId.orEmpty()
            val isExternalAudio = mediaItem?.isExternalAudioLaunchItem() == true
            val isFavorite = !isExternalAudio && mediaId in favoriteIds
            val title = mediaItem?.mediaMetadata?.displayTitle?.toString()
                ?: mediaItem?.mediaMetadata?.title?.toString()
                ?: context.getString(R.string.unknown_song_title)
            val artist = mediaItem?.mediaMetadata?.subtitle?.toString()
                ?: mediaItem?.mediaMetadata?.artist?.toString()
                ?: context.getString(R.string.unknown_artist)

            root.findViewById<TextView>(R.id.track_name)?.text = title
            root.findViewById<TextView>(R.id.artist_name)?.text = artist
            root.findViewById<ImageButton>(R.id.left_btn)?.apply {
                setImageResource(
                    if (isFavorite) {
                        R.drawable.float_favor_cancel_selector
                    } else {
                        R.drawable.float_favor_add_selector
                    },
                )
                isEnabled = mediaItem != null && !isExternalAudio
                setOnClickListener {
                    if (mediaItem != null) {
                        onToggleFavorite(mediaItem)
                    }
                }
            }
            root.findViewById<ImageButton>(R.id.prev_btn)?.apply {
                setImageResource(R.drawable.float_btn_prev_selector)
                setOnClickListener { onPrevious() }
            }
            root.findViewById<ImageButton>(R.id.play_btn)?.apply {
                setImageResource(
                    if (snapshot.isPlaying) {
                        R.drawable.float_btn_pause_selector
                    } else {
                        R.drawable.float_btn_play_selector
                    },
                )
                setOnClickListener { onPlayPause() }
            }
            root.findViewById<ImageButton>(R.id.next_btn)?.apply {
                setImageResource(R.drawable.float_btn_next_selector)
                setOnClickListener { onNext() }
            }
            root.findViewById<View>(R.id.song_info_zone)?.setOnClickListener {
                onOpenPlayback()
            }
            root.findViewById<ImageView>(R.id.album_art)?.apply {
                if (artworkBitmap != null) {
                    setImageBitmap(artworkBitmap)
                } else {
                    setImageResource(R.drawable.noalbumcover_220)
                }
                setOnClickListener {
                    onOpenPlayback()
                }
            }
        },
    )
}

internal data class LegacyPlaybackBarSnapshot(
    val mediaItem: MediaItem? = null,
    val isPlaying: Boolean = false,
)

internal suspend fun loadLegacyArtworkBitmap(
    context: android.content.Context,
    mediaItem: MediaItem,
): Bitmap? = loadArtworkBitmap(context, mediaItem, LegacyPlaybackBarArtworkDecodeSize)

private val LegacyPlaybackBarArtworkDecodeSize = Size(128, 128)
