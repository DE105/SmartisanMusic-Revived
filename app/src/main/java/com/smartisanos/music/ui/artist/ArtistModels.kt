package com.smartisanos.music.ui.artist

import androidx.media3.common.MediaItem
import com.smartisanos.music.playback.LocalAudioLibrary
import java.text.Collator
import java.util.Locale

internal data class ArtistSummary(
    val id: String,
    val name: String,
    val songs: List<MediaItem>,
    val albumCount: Int,
) {
    val representative: MediaItem = songs.first()
    val trackCount: Int = songs.size
}

internal fun buildArtistSummaries(
    mediaItems: List<MediaItem>,
    unknownArtistTitle: String,
    unknownAlbumTitle: String,
): List<ArtistSummary> {
    val groups = linkedMapOf<String, MutableArtistGroup>()

    mediaItems.forEach { item ->
        val metadata = item.mediaMetadata
        val artistName = metadata.artist.toDisplayText() ?: unknownArtistTitle
        val artistKey = artistName.normalizedKey()
        val group = groups.getOrPut(artistKey) {
            MutableArtistGroup(
                id = "artist:$artistKey",
                name = artistName,
            )
        }
        group.songs += item
        group.albumKeys += item.albumGroupingKey(unknownAlbumTitle)
    }

    return groups.values
        .map { group ->
            ArtistSummary(
                id = group.id,
                name = group.name,
                songs = group.songs.sortedWith(
                    compareBy<MediaItem> {
                        it.mediaMetadata.albumTitle.toDisplayText()?.normalizedKey().orEmpty()
                    }
                        .thenBy { it.mediaMetadata.trackNumber ?: Int.MAX_VALUE }
                        .thenBy { it.mediaMetadata.title?.toString().orEmpty().normalizedKey() },
                ),
                albumCount = group.albumKeys.size,
            )
        }
        .sortedWith(artistNameComparator())
}

private data class MutableArtistGroup(
    val id: String,
    val name: String,
    val songs: MutableList<MediaItem> = mutableListOf(),
    val albumKeys: MutableSet<String> = linkedSetOf(),
)

private fun MediaItem.albumGroupingKey(unknownAlbumTitle: String): String {
    val albumId = mediaMetadata.extras
        ?.getLong(LocalAudioLibrary.AlbumIdExtraKey)
        ?.takeIf { it > 0L }
    return albumId?.let { "album:$it" }
        ?: (mediaMetadata.albumTitle.toDisplayText() ?: unknownAlbumTitle).normalizedKey()
}

private fun artistNameComparator(): Comparator<ArtistSummary> {
    val collator = Collator.getInstance(Locale.CHINA).apply {
        strength = Collator.PRIMARY
    }
    return Comparator { left, right ->
        val localizedOrder = collator.compare(left.name, right.name)
        if (localizedOrder != 0) {
            localizedOrder
        } else {
            left.name.normalizedKey().compareTo(right.name.normalizedKey())
        }
    }
}

private fun CharSequence?.toDisplayText(): String? {
    return this?.toString()?.trim()?.takeIf { it.isNotEmpty() }
}

private fun String.normalizedKey(): String {
    return lowercase(Locale.ROOT)
}
