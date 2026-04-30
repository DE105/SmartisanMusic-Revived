package com.smartisanos.music

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.MediaItem

internal const val ExternalAudioMediaIdPrefix = "external-audio-"
internal const val ExternalAudioExtraKey = "com.smartisanos.music.extra.EXTERNAL_AUDIO"

data class ExternalAudioLaunchRequest(
    val requestId: Int,
    val uri: Uri,
    val mimeType: String?,
    val displayName: String?,
)

internal fun MediaItem.isExternalAudioLaunchItem(): Boolean {
    return mediaMetadata.extras?.getBoolean(ExternalAudioExtraKey, false) == true ||
        mediaId.startsWith(ExternalAudioMediaIdPrefix)
}

internal fun ExternalAudioLaunchRequest.resolveExternalAudioArtist(context: Context): String? {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            when (uri.scheme) {
                ContentScheme -> retriever.setDataSource(context, uri)
                FileScheme -> retriever.setDataSource(uri.path ?: return@runCatching null)
                else -> return@runCatching null
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
        } finally {
            runCatching {
                retriever.release()
            }
        }
    }.getOrNull()?.takeIf(String::isNotBlank)
}

internal fun ExternalAudioLaunchRequest.resolveExternalAudioAlbumId(context: Context): Long? {
    if (uri.scheme != ContentScheme) {
        return null
    }
    return runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Audio.Media.ALBUM_ID),
            null,
            null,
            null,
        )?.use { cursor ->
            val albumIdColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            if (albumIdColumn == -1 || !cursor.moveToFirst()) {
                return@use null
            }
            cursor.getLong(albumIdColumn).takeIf { it > 0L }
        }
    }.getOrNull()
}

private const val ContentScheme = "content"
private const val FileScheme = "file"
