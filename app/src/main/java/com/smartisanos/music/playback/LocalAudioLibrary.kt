package com.smartisanos.music.playback

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.smartisanos.music.R
import java.util.Locale

class LocalAudioLibrary(
    private val context: Context,
) {

    private var cachedVersion: String? = null
    private var cachedItems: List<MediaItem> = emptyList()

    fun getRootItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(context.getString(R.string.library_root))
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()

        return MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(metadata)
            .build()
    }

    fun getAudioItems(forceRefresh: Boolean = false): List<MediaItem> {
        val mediaStoreVersion = MediaStore.getVersion(context)
        if (!forceRefresh && cachedVersion == mediaStoreVersion && cachedItems.isNotEmpty()) {
            return cachedItems
        }

        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
        )
        val selection = buildString {
            append("${MediaStore.Audio.Media.IS_MUSIC} != 0")
            append(" AND ${MediaStore.Audio.Media.DURATION} > 0")
        }
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val items = mutableListOf<MediaItem>()
        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumArtistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)
                        ?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.unknown_song_title)
                    val artist = cursor.getString(artistColumn)
                        ?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }
                        ?: context.getString(R.string.unknown_artist)
                    val album = cursor.getString(albumColumn)?.takeIf { it.isNotBlank() }
                    val albumArtist = cursor.getString(albumArtistColumn)
                        ?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }
                    val albumId = cursor.getLong(albumIdColumn).takeIf { it > 0L }
                    val durationMs = cursor.getLong(durationColumn)
                    val trackNumber = cursor.getInt(trackColumn).takeIf { it > 0 }
                    val year = cursor.getInt(yearColumn).takeIf { it > 0 }
                    val dateAddedSeconds = cursor.getLong(dateAddedColumn).takeIf { it > 0L }
                    val relativePath = cursor.getString(relativePathColumn)?.takeIf { it.isNotBlank() }
                    val displayName = cursor.getString(displayNameColumn)?.takeIf { it.isNotBlank() }
                    val mimeType = cursor.getString(mimeTypeColumn)?.takeIf { it.isNotBlank() }
                    val audioQualityBadge = resolveAudioQualityBadge(displayName, mimeType)
                    val mediaUri = ContentUris.withAppendedId(collection, id)

                    val extras = Bundle().apply {
                        if (!relativePath.isNullOrBlank()) {
                            putString(RelativePathExtraKey, relativePath)
                        }
                        if (albumId != null) {
                            putLong(AlbumIdExtraKey, albumId)
                        }
                        if (dateAddedSeconds != null) {
                            putLong(DateAddedExtraKey, dateAddedSeconds)
                        }
                        if (!audioQualityBadge.isNullOrBlank()) {
                            putString(AudioQualityBadgeExtraKey, audioQualityBadge)
                        }
                    }

                    val metadataBuilder = MediaMetadata.Builder()
                        .setTitle(title)
                        .setDisplayTitle(title)
                        .setArtist(artist)
                        .setSubtitle(artist)
                        .setDurationMs(durationMs)
                        .setIsPlayable(true)
                        .setIsBrowsable(false)

                    if (!album.isNullOrBlank()) {
                        metadataBuilder.setAlbumTitle(album)
                    }

                    if (!albumArtist.isNullOrBlank()) {
                        metadataBuilder.setAlbumArtist(albumArtist)
                    }

                    if (albumId != null) {
                        metadataBuilder.setArtworkUri(albumArtworkUri(albumId))
                    }

                    metadataBuilder.setExtras(extras)

                    if (trackNumber != null) {
                        metadataBuilder.setTrackNumber(trackNumber)
                    }

                    if (year != null) {
                        metadataBuilder.setReleaseYear(year)
                    }

                    items += MediaItem.Builder()
                        .setMediaId(id.toString())
                        .setUri(mediaUri)
                        .setMediaMetadata(metadataBuilder.build())
                        .build()
                }
            }
        } catch (_: SecurityException) {
            return emptyList()
        }

        cachedVersion = mediaStoreVersion
        cachedItems = items
        return items
    }

    fun getItem(mediaId: String): MediaItem? {
        if (mediaId == ROOT_ID) {
            return getRootItem()
        }
        return getAudioItems().firstOrNull { it.mediaId == mediaId }
    }

    companion object {
        const val ROOT_ID = "root"
        const val AlbumIdExtraKey = "com.smartisanos.music.extra.ALBUM_ID"
        const val RelativePathExtraKey = "com.smartisanos.music.extra.RELATIVE_PATH"
        const val DateAddedExtraKey = "com.smartisanos.music.extra.DATE_ADDED"
        const val AudioQualityBadgeExtraKey = "com.smartisanos.music.extra.AUDIO_QUALITY_BADGE"
        const val AudioQualityBadgeFlac = "flac"
        const val AudioQualityBadgeApe = "ape"
        const val AudioQualityBadgeWav = "wav"
        const val AudioQualityBadgeAiff = "aiff"
        const val AudioQualityBadgeAlac = "alac"
        const val AudioQualityBadgeCue = "cue"

        fun albumArtworkUri(albumId: Long): Uri {
            return ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId,
            )
        }

        private fun resolveAudioQualityBadge(
            displayName: String?,
            mimeType: String?,
        ): String? {
            val extension = displayName
                ?.substringAfterLast('.', missingDelimiterValue = "")
                ?.lowercase(Locale.ROOT)
                .orEmpty()
            val normalizedMimeType = mimeType?.lowercase(Locale.ROOT).orEmpty()

            return when {
                extension == AudioQualityBadgeFlac || normalizedMimeType.contains(AudioQualityBadgeFlac) -> {
                    AudioQualityBadgeFlac
                }
                extension == AudioQualityBadgeApe || normalizedMimeType.contains(AudioQualityBadgeApe) || normalizedMimeType.contains("monkeys-audio") -> {
                    AudioQualityBadgeApe
                }
                extension == AudioQualityBadgeWav || normalizedMimeType.contains(AudioQualityBadgeWav) || normalizedMimeType.contains("wave") -> {
                    AudioQualityBadgeWav
                }
                extension == AudioQualityBadgeAiff || extension == "aif" || normalizedMimeType.contains(AudioQualityBadgeAiff) -> {
                    AudioQualityBadgeAiff
                }
                extension == AudioQualityBadgeAlac || normalizedMimeType.contains(AudioQualityBadgeAlac) -> {
                    AudioQualityBadgeAlac
                }
                extension == AudioQualityBadgeCue || normalizedMimeType.contains(AudioQualityBadgeCue) -> {
                    AudioQualityBadgeCue
                }
                else -> null
            }
        }
    }
}
