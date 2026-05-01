package com.smartisanos.music.ui.shell

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import android.util.Size
import android.widget.ImageView
import androidx.media3.common.MediaItem
import com.smartisanos.music.playback.LocalAudioLibrary
import com.smartisanos.music.ui.album.AlbumSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun ImageView.bindLegacyAlbumArtwork(
    album: AlbumSummary,
    fallbackRes: Int,
    sizePx: Int,
    artworkLoader: LegacyAlbumArtworkLoader,
) {
    scaleType = ImageView.ScaleType.CENTER_CROP
    artworkLoader.bind(
        imageView = this,
        album = album,
        fallbackRes = fallbackRes,
        sizePx = sizePx,
    )
}

internal class LegacyAlbumArtworkLoader(context: Context) {
    private val appContext = context.applicationContext
    private var scope = newScope()
    private val pendingViews = mutableMapOf<String, MutableSet<ImageView>>()
    private val jobs = mutableMapOf<String, Job>()

    fun bind(
        imageView: ImageView,
        album: AlbumSummary,
        fallbackRes: Int,
        sizePx: Int,
    ) {
        val previousKey = imageView.tag as? String
        val request = album.artworkRequest(sizePx)
        if (request == null) {
            imageView.setImageResource(fallbackRes)
            return
        }

        imageView.tag = request.viewTag
        cache.get(request.cacheKey)?.let { cached ->
            imageView.setImageBitmap(cached)
            if (cached.width >= request.sizePx && cached.height >= request.sizePx) {
                return
            }
        } ?: run {
            if (previousKey != request.viewTag || imageView.drawable == null) {
                imageView.setImageResource(fallbackRes)
            }
        }
        pendingViews.getOrPut(request.jobKey) { linkedSetOf() } += imageView
        if (jobs.containsKey(request.jobKey)) {
            return
        }

        jobs[request.jobKey] = ensureScope().launch {
            val bitmap = withContext(Dispatchers.IO) {
                loadBitmap(request)
            }
            jobs.remove(request.jobKey)
            if (bitmap != null) {
                bitmap.prepareToDraw()
                cache.put(request.cacheKey, bitmap)
            }
            val views = pendingViews.remove(request.jobKey).orEmpty()
            views.forEach { pendingImageView ->
                if (pendingImageView.tag == request.viewTag) {
                    if (bitmap != null) {
                        pendingImageView.setImageBitmap(bitmap)
                    } else {
                        pendingImageView.setImageResource(fallbackRes)
                    }
                }
            }
        }
    }

    fun clear() {
        scope.cancel()
        jobs.clear()
        pendingViews.clear()
    }

    private fun ensureScope(): CoroutineScope {
        if (scope.coroutineContext[Job]?.isActive != true) {
            scope = newScope()
        }
        return scope
    }

    private fun loadBitmap(request: LegacyAlbumArtworkRequest): Bitmap? {
        val size = Size(request.sizePx, request.sizePx)
        return request.artworkData?.let { artworkData ->
            runCatching {
                decodeByteArraySampled(artworkData, size)
            }.getOrNull()
        } ?: request.artworkUri?.let { artworkUri ->
            loadBitmapFromUri(artworkUri, size)
        } ?: request.trackArtworkUri?.let { trackArtworkUri ->
            loadBitmapFromUri(trackArtworkUri, size)
        } ?: request.mediaUri?.let { mediaUri ->
            runCatching {
                appContext.contentResolver.loadThumbnail(mediaUri, size, null)
            }.getOrNull()
        } ?: loadEmbeddedPicture(request.mediaItem, size)
    }

    private fun loadBitmapFromUri(uri: Uri, size: Size): Bitmap? {
        return runCatching {
            appContext.contentResolver.loadThumbnail(uri, size, null)
        }.getOrNull() ?: runCatching {
            decodeStreamSampled(uri, size)
        }.getOrNull()
    }

    private fun decodeStreamSampled(uri: Uri, size: Size): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        appContext.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null
        val sampleOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(boundsOptions, size)
        }
        return appContext.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, sampleOptions)
        }
    }

    private fun loadEmbeddedPicture(mediaItem: MediaItem, size: Size): Bitmap? {
        return runCatching {
            val mediaUri = mediaItem.localConfiguration?.uri ?: return@runCatching null
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(appContext, mediaUri)
                retriever.embeddedPicture?.let { bytes ->
                    decodeByteArraySampled(bytes, size)
                }
            } finally {
                retriever.release()
            }
        }.getOrNull()
    }

    private fun newScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    private companion object {
        val cache = object : LruCache<String, Bitmap>(albumArtworkCacheSizeKb()) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }
}

private data class LegacyAlbumArtworkRequest(
    val mediaItem: MediaItem,
    val artworkUri: Uri?,
    val trackArtworkUri: Uri?,
    val mediaUri: Uri?,
    val artworkData: ByteArray?,
    val viewTag: String,
    val sizePx: Int,
) {
    val cacheKey: String = viewTag
    val jobKey: String = "$viewTag@$sizePx"
}

private fun AlbumSummary.artworkRequest(sizePx: Int): LegacyAlbumArtworkRequest? {
    val mediaItem = representative
    val mediaId = mediaItem.mediaId.toLongOrNull()
    val albumId = representative.mediaMetadata.extras
        ?.getLong(LocalAudioLibrary.AlbumIdExtraKey)
        ?.takeIf { it > 0L }
    val artworkUri = representative.mediaMetadata.artworkUri
        ?: albumId?.let(LocalAudioLibrary::albumArtworkUri)
    val trackArtworkUri = mediaId?.let { id ->
        Uri.parse("content://media/external/audio/media/$id/albumart")
    }
    val mediaUri = mediaItem.localConfiguration?.uri
    val artworkData = mediaItem.mediaMetadata.artworkData
    if (artworkUri == null && trackArtworkUri == null && mediaUri == null && artworkData == null) {
        return null
    }
    return LegacyAlbumArtworkRequest(
        mediaItem = mediaItem,
        artworkUri = artworkUri,
        trackArtworkUri = trackArtworkUri,
        mediaUri = mediaUri,
        artworkData = artworkData,
        viewTag = albumId?.let { album -> "album:$album" }
            ?: mediaId?.let { id -> "track:$id" }
            ?: "${title}|${artist}",
        sizePx = sizePx,
    )
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

private fun albumArtworkCacheSizeKb(): Int {
    val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    return (maxMemoryKb / 16).coerceAtLeast(4 * 1024)
}
