package com.smartisanos.music.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.media3.common.MediaItem
import com.smartisanos.music.playback.LocalAudioLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun loadEmbeddedArtwork(
    context: Context,
    mediaItem: MediaItem,
): ImageBitmap? = loadArtwork(context, mediaItem)

suspend fun loadArtwork(
    context: Context,
    mediaItem: MediaItem,
): ImageBitmap? = withContext(Dispatchers.IO) {
    loadArtworkData(mediaItem)
        ?: loadUiArtworkUri(context, mediaItem)
        ?: loadArtworkUri(context, mediaItem)
        ?: loadMediaThumbnail(context, mediaItem)
        ?: loadEmbeddedPicture(context, mediaItem)
}

suspend fun loadArtworkThumbnail(
    context: Context,
    mediaItem: MediaItem,
    size: Size,
): ImageBitmap? = withContext(Dispatchers.IO) {
    loadUiArtworkUriThumbnail(context, mediaItem, size)
        ?: loadArtworkUriThumbnail(context, mediaItem, size)
        ?: loadMediaThumbnail(context, mediaItem, size)
        ?: loadArtworkData(mediaItem, size)
        ?: loadEmbeddedPicture(context, mediaItem, size)
}

private fun loadArtworkData(mediaItem: MediaItem): ImageBitmap? {
    val artworkData = mediaItem.mediaMetadata.artworkData ?: return null
    return runCatching {
        BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)?.asImageBitmap()
    }.getOrNull()
}

private fun loadArtworkData(
    mediaItem: MediaItem,
    size: Size,
): ImageBitmap? {
    val artworkData = mediaItem.mediaMetadata.artworkData ?: return null
    return runCatching {
        decodeByteArraySampled(artworkData, size)?.toPreparedImageBitmap()
    }.getOrNull()
}

private fun loadUiArtworkUri(
    context: Context,
    mediaItem: MediaItem,
): ImageBitmap? {
    val artworkUri = mediaItem.uiArtworkUri() ?: return null
    return loadArtworkUri(context, artworkUri)
}

private fun loadArtworkUri(
    context: Context,
    mediaItem: MediaItem,
): ImageBitmap? {
    val artworkUri = mediaItem.mediaMetadata.artworkUri ?: return null
    return loadArtworkUri(context, artworkUri)
}

private fun loadArtworkUri(
    context: Context,
    artworkUri: Uri,
): ImageBitmap? {
    return runCatching {
        context.contentResolver.openInputStream(artworkUri)?.use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull() ?: runCatching {
        context.contentResolver.loadThumbnail(artworkUri, ArtworkThumbnailSize, null).asImageBitmap()
    }.getOrNull()
}

private fun loadUiArtworkUriThumbnail(
    context: Context,
    mediaItem: MediaItem,
    size: Size,
): ImageBitmap? {
    val artworkUri = mediaItem.uiArtworkUri() ?: return null
    return loadArtworkUriThumbnail(context, artworkUri, size)
}

private fun loadArtworkUriThumbnail(
    context: Context,
    mediaItem: MediaItem,
    size: Size,
): ImageBitmap? {
    val artworkUri = mediaItem.mediaMetadata.artworkUri ?: return null
    return loadArtworkUriThumbnail(context, artworkUri, size)
}

private fun loadArtworkUriThumbnail(
    context: Context,
    artworkUri: Uri,
    size: Size,
): ImageBitmap? {
    return runCatching {
        context.contentResolver.loadThumbnail(artworkUri, size, null).toPreparedImageBitmap()
    }.getOrNull() ?: runCatching {
        decodeStreamSampled(context, artworkUri, size)?.toPreparedImageBitmap()
    }.getOrNull()
}

private fun MediaItem.uiArtworkUri(): Uri? {
    val albumId = mediaMetadata.extras
        ?.getLong(LocalAudioLibrary.AlbumIdExtraKey)
        ?.takeIf { it > 0L }
        ?: return null
    return LocalAudioLibrary.albumArtworkUri(albumId)
}

private fun loadMediaThumbnail(
    context: Context,
    mediaItem: MediaItem,
): ImageBitmap? {
    val mediaUri = mediaItem.localConfiguration?.uri ?: return null
    return runCatching {
        context.contentResolver.loadThumbnail(mediaUri, ArtworkThumbnailSize, null).asImageBitmap()
    }.getOrNull()
}

private fun loadMediaThumbnail(
    context: Context,
    mediaItem: MediaItem,
    size: Size,
): ImageBitmap? {
    val mediaUri = mediaItem.localConfiguration?.uri ?: return null
    return runCatching {
        context.contentResolver.loadThumbnail(mediaUri, size, null).toPreparedImageBitmap()
    }.getOrNull()
}

private fun loadEmbeddedPicture(
    context: Context,
    mediaItem: MediaItem,
): ImageBitmap? {
    return runCatching {
        val uri = mediaItem.localConfiguration?.uri ?: return@runCatching null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.let { bytes ->
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

private fun loadEmbeddedPicture(
    context: Context,
    mediaItem: MediaItem,
    size: Size,
): ImageBitmap? {
    return runCatching {
        val uri = mediaItem.localConfiguration?.uri ?: return@runCatching null
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.embeddedPicture?.let { bytes ->
                decodeByteArraySampled(bytes, size)?.toPreparedImageBitmap()
            }
        } finally {
            retriever.release()
        }
    }.getOrNull()
}

private fun decodeStreamSampled(
    context: Context,
    uri: android.net.Uri,
    size: Size,
): Bitmap? {
    val boundsOptions = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, boundsOptions)
    }
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

private fun Bitmap.toPreparedImageBitmap(): ImageBitmap {
    return asImageBitmap().also { image ->
        image.prepareToDraw()
    }
}

private val ArtworkThumbnailSize = Size(512, 512)
