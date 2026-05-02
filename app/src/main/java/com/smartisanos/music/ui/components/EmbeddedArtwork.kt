package com.smartisanos.music.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.media3.common.MediaItem
import com.smartisanos.music.playback.loadArtworkBitmap

suspend fun loadEmbeddedArtwork(
    context: Context,
    mediaItem: MediaItem,
): ImageBitmap? = loadArtworkThumbnail(context, mediaItem, ArtworkThumbnailSize)

suspend fun loadArtwork(
    context: Context,
    mediaItem: MediaItem,
): ImageBitmap? = loadArtworkBitmap(context, mediaItem, ArtworkFullSize)
    ?.toPreparedImageBitmap()

suspend fun loadArtworkThumbnail(
    context: Context,
    mediaItem: MediaItem,
    size: Size,
): ImageBitmap? = loadArtworkBitmap(context, mediaItem, size)
    ?.toPreparedImageBitmap()

private fun Bitmap.toPreparedImageBitmap(): ImageBitmap {
    return asImageBitmap().also { image ->
        image.prepareToDraw()
    }
}

private val ArtworkThumbnailSize = Size(512, 512)
private val ArtworkFullSize = Size(1024, 1024)
