package com.smartisanos.music.ui.artist

import androidx.media3.common.MediaItem
import com.smartisanos.music.data.settings.ArtistRecognitionSettings
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
    recognitionSettings: ArtistRecognitionSettings = ArtistRecognitionSettings(),
): List<ArtistSummary> {
    val groups = linkedMapOf<String, MutableArtistGroup>()
    val recognitionRule = recognitionSettings.toRecognitionRule()

    mediaItems.forEach { item ->
        val metadata = item.mediaMetadata
        val artistNames = metadata.artist.toArtistNames(
            unknownArtistTitle = unknownArtistTitle,
            recognitionRule = recognitionRule,
        )
        artistNames.forEach { artistName ->
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

private data class ArtistRecognitionRule(
    val separators: List<String>,
    val excludedArtistNames: List<String>,
)

private fun ArtistRecognitionSettings.toRecognitionRule(): ArtistRecognitionRule {
    return ArtistRecognitionRule(
        separators = separators.asSequence()
            .mapNotNull { it.toDisplayText() }
            .distinct()
            .sortedByDescending { it.length }
            .toList(),
        excludedArtistNames = excludedArtistNames.asSequence()
            .mapNotNull { it.toDisplayText() }
            .distinctBy { it.normalizedKey() }
            .sortedByDescending { it.length }
            .toList(),
    )
}

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

private fun CharSequence?.toArtistNames(
    unknownArtistTitle: String,
    recognitionRule: ArtistRecognitionRule,
): List<String> {
    val artistName = toDisplayText() ?: unknownArtistTitle
    if (recognitionRule.separators.isEmpty()) {
        return listOf(artistName)
    }
    val protectedArtistName = artistName.protectExcludedArtistNames(recognitionRule.excludedArtistNames)
    val names = mutableListOf(protectedArtistName.value)
    recognitionRule.separators.forEach { separator ->
        val splitNames = names.flatMap { name ->
            name.split(separator)
                .mapNotNull { it.restoreProtectedArtistNames(protectedArtistName.tokens).toDisplayText() }
        }
        names.clear()
        names += splitNames
    }
    return names.distinctBy { it.normalizedKey() }.ifEmpty { listOf(unknownArtistTitle) }
}

private data class ProtectedArtistName(
    val value: String,
    val tokens: List<String>,
)

private fun String.protectExcludedArtistNames(excludedArtistNames: List<String>): ProtectedArtistName {
    var protectedValue = this
    val tokens = mutableListOf<String>()
    excludedArtistNames.forEach { excludedArtistName ->
        var searchStart = 0
        while (searchStart < protectedValue.length) {
            val index = protectedValue.indexOf(
                string = excludedArtistName,
                startIndex = searchStart,
                ignoreCase = true,
            )
            if (index < 0) {
                break
            }
            val matchedName = protectedValue.substring(index, index + excludedArtistName.length)
            val token = "\u0000${tokens.size}\u0000"
            tokens += matchedName
            protectedValue = protectedValue.replaceRange(
                startIndex = index,
                endIndex = index + excludedArtistName.length,
                replacement = token,
            )
            searchStart = index + token.length
        }
    }
    return ProtectedArtistName(
        value = protectedValue,
        tokens = tokens,
    )
}

private fun String.restoreProtectedArtistNames(tokens: List<String>): String {
    var restored = this
    tokens.forEachIndexed { index, artistName ->
        restored = restored.replace("\u0000$index\u0000", artistName)
    }
    return restored
}

private fun CharSequence?.toDisplayText(): String? {
    return this?.toString()?.trim()?.takeIf { it.isNotEmpty() }
}

private fun String.normalizedKey(): String {
    return lowercase(Locale.ROOT)
}
