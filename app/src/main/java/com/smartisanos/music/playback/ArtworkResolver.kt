package com.smartisanos.music.playback

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.util.Size
import androidx.media3.common.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class ArtworkRequestKey(
    val mediaId: String?,
    val artworkUri: String?,
    val albumId: Long?,
    val mediaUri: String?,
    val artworkDataHash: Int?,
    val artworkDataSize: Int?,
)

internal fun MediaItem.artworkRequestKey(): ArtworkRequestKey {
    val artworkData = mediaMetadata.artworkData
    return ArtworkRequestKey(
        mediaId = mediaId,
        artworkUri = mediaMetadata.artworkUri?.toString(),
        albumId = mediaMetadata.extras.albumId(),
        mediaUri = localConfiguration?.uri?.toString(),
        artworkDataHash = artworkData?.contentHashCode(),
        artworkDataSize = artworkData?.size,
    )
}

internal suspend fun loadArtworkBitmap(
    context: Context,
    mediaItem: MediaItem,
    size: Size,
): Bitmap? = withContext(Dispatchers.IO) {
    loadArtworkBitmapSync(context.applicationContext, mediaItem, size)
        ?.also { bitmap -> bitmap.prepareToDraw() }
}

internal fun loadArtworkBitmapSync(
    context: Context,
    mediaItem: MediaItem,
    size: Size,
): Bitmap? {
    val metadata = mediaItem.mediaMetadata
    return decodeArtworkData(metadata.artworkData, size)
        ?: loadArtworkUriBitmap(context, metadata.artworkUri, size)
        ?: loadTrackArtworkBitmap(context, mediaItem.mediaId, size)
        ?: loadAlbumArtworkBitmap(context, metadata.extras, size)
        ?: loadMediaThumbnail(context, mediaItem.localConfiguration?.uri, size)
        ?: loadEmbeddedPicture(context, mediaItem.localConfiguration?.uri, size)
}

internal fun loadArtworkUriBitmap(
    context: Context,
    uri: Uri?,
    size: Size,
): Bitmap? {
    uri ?: return null
    return runCatching {
        context.contentResolver.loadThumbnail(uri, size, null).scaledToFit(size)
    }.getOrNull() ?: runCatching {
        decodeStreamSampled(context, uri, size)?.scaledToFit(size)
    }.getOrNull()
}

internal fun decodeArtworkData(
    artworkData: ByteArray?,
    size: Size,
): Bitmap? {
    artworkData ?: return null
    return runCatching {
        decodeByteArraySampled(artworkData, size)?.scaledToFit(size)
    }.getOrNull()
}

private fun loadTrackArtworkBitmap(
    context: Context,
    mediaId: String,
    size: Size,
): Bitmap? {
    val numericMediaId = mediaId.toLongOrNull() ?: return null
    return loadArtworkUriBitmap(context, LocalAudioLibrary.trackArtworkUri(numericMediaId), size)
}

private fun loadAlbumArtworkBitmap(
    context: Context,
    extras: Bundle?,
    size: Size,
): Bitmap? {
    val albumId = extras.albumId() ?: return null
    return loadArtworkUriBitmap(context, LocalAudioLibrary.albumArtworkUri(albumId), size)
}

private fun loadMediaThumbnail(
    context: Context,
    mediaUri: Uri?,
    size: Size,
): Bitmap? {
    mediaUri ?: return null
    return runCatching {
        context.contentResolver.loadThumbnail(mediaUri, size, null).scaledToFit(size)
    }.getOrNull()
}

private fun loadEmbeddedPicture(
    context: Context,
    mediaUri: Uri?,
    size: Size,
): Bitmap? {
    mediaUri ?: return null
    return runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, mediaUri)
            retriever.embeddedPicture?.let { bytes ->
                decodeByteArraySampled(bytes, size)?.scaledToFit(size)
            }
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

private fun Bundle?.albumId(): Long? {
    return this
        ?.getLong(LocalAudioLibrary.AlbumIdExtraKey)
        ?.takeIf { it > 0L }
}

private fun decodeStreamSampled(
    context: Context,
    uri: Uri,
    size: Size,
): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
    } ?: return null
    val sampleOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(boundsOptions, size)
    }
    return context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, sampleOptions)
    }
}

private fun decodeByteArraySampled(
    bytes: ByteArray,
    size: Size,
): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
    val sampleOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(boundsOptions, size)
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, sampleOptions)
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    size: Size,
): Int {
    val rawHeight = options.outHeight
    val rawWidth = options.outWidth
    val requestedHeight = size.height.coerceAtLeast(1)
    val requestedWidth = size.width.coerceAtLeast(1)
    var inSampleSize = 1

    if (rawHeight > requestedHeight || rawWidth > requestedWidth) {
        val halfHeight = rawHeight / 2
        val halfWidth = rawWidth / 2
        while (
            halfHeight / inSampleSize >= requestedHeight &&
            halfWidth / inSampleSize >= requestedWidth
        ) {
            inSampleSize *= 2
        }
    }

    return inSampleSize.coerceAtLeast(1)
}

private fun Bitmap.scaledToFit(size: Size): Bitmap {
    val maxWidth = size.width.coerceAtLeast(1)
    val maxHeight = size.height.coerceAtLeast(1)
    if (width <= maxWidth && height <= maxHeight) {
        return this
    }
    val scale = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
    val scaledWidth = (width * scale).toInt().coerceAtLeast(1)
    val scaledHeight = (height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
}
