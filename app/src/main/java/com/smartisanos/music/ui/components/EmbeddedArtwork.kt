package com.smartisanos.music.ui.components

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.media3.common.MediaItem
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
        ?: loadArtworkUri(context, mediaItem)
        ?: loadMediaThumbnail(context, mediaItem)
        ?: loadEmbeddedPicture(context, mediaItem)
}

private fun loadArtworkData(mediaItem: MediaItem): ImageBitmap? {
    val artworkData = mediaItem.mediaMetadata.artworkData ?: return null
    return runCatching {
        BitmapFactory.decodeByteArray(artworkData, 0, artworkData.size)?.asImageBitmap()
    }.getOrNull()
}

private fun loadArtworkUri(
    context: Context,
    mediaItem: MediaItem,
): ImageBitmap? {
    val artworkUri = mediaItem.mediaMetadata.artworkUri ?: return null
    return runCatching {
        context.contentResolver.openInputStream(artworkUri)?.use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    }.getOrNull() ?: runCatching {
        context.contentResolver.loadThumbnail(artworkUri, ArtworkThumbnailSize, null).asImageBitmap()
    }.getOrNull()
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

private val ArtworkThumbnailSize = Size(512, 512)
