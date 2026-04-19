package com.smartisanos.music.data.genre

import android.content.Context
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GenreTagRepository(
    private val mediaStoreVersionProvider: () -> String,
    private val mediaStoreGenreReader: () -> Map<String, String?>,
    private val embeddedGenreReader: (MediaItem) -> String?,
) {

    constructor(context: Context) : this(
        mediaStoreVersionProvider = {
            MediaStore.getVersion(context)
        },
        mediaStoreGenreReader = {
            queryMediaStoreGenres(context)
        },
        embeddedGenreReader = { mediaItem ->
            readEmbeddedGenre(context, mediaItem)
        },
    )

    suspend fun loadGenres(mediaItems: List<MediaItem>): Map<String, String?> = withContext(Dispatchers.IO) {
        val mediaStoreVersion = mediaStoreVersionProvider()
        ensureCacheVersion(mediaStoreVersion)
        primeCacheFromMediaStore(mediaStoreVersion)

        buildMap(mediaItems.size) {
            mediaItems.forEach { item ->
                put(item.mediaId, loadGenre(mediaStoreVersion, item))
            }
        }
    }

    private fun loadGenre(
        mediaStoreVersion: String,
        mediaItem: MediaItem,
    ): String? {
        synchronized(CacheLock) {
            if (genreCache[mediaItem.mediaId] != null || mediaItem.mediaId in retrieverResolvedIds) {
                return genreCache[mediaItem.mediaId]
            }
        }

        val genre = embeddedGenreReader(mediaItem)

        synchronized(CacheLock) {
            ensureCacheVersion(mediaStoreVersion)
            genreCache[mediaItem.mediaId] = genre
            retrieverResolvedIds += mediaItem.mediaId
        }
        return genre
    }

    private fun primeCacheFromMediaStore(mediaStoreVersion: String) {
        synchronized(CacheLock) {
            if (mediaStoreCachePrimed) {
                return
            }
        }

        val resolvedGenres = runCatching {
            mediaStoreGenreReader()
        }.getOrNull() ?: return

        synchronized(CacheLock) {
            ensureCacheVersion(mediaStoreVersion)
            genreCache.putAll(resolvedGenres)
            mediaStoreCachePrimed = true
        }
    }

    private fun ensureCacheVersion(mediaStoreVersion: String) {
        synchronized(CacheLock) {
            if (cachedMediaStoreVersion == mediaStoreVersion) {
                return
            }
            cachedMediaStoreVersion = mediaStoreVersion
            genreCache.clear()
            retrieverResolvedIds.clear()
            mediaStoreCachePrimed = false
        }
    }

    companion object {
        private val CacheLock = Any()
        private var cachedMediaStoreVersion: String? = null
        private val genreCache = mutableMapOf<String, String?>()
        private val retrieverResolvedIds = mutableSetOf<String>()
        private var mediaStoreCachePrimed = false

        internal fun resetCacheForTest() {
            synchronized(CacheLock) {
                cachedMediaStoreVersion = null
                genreCache.clear()
                retrieverResolvedIds.clear()
                mediaStoreCachePrimed = false
            }
        }
    }
}

private fun queryMediaStoreGenres(context: Context): Map<String, String?> {
    val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.AudioColumns.GENRE,
    )
    val selection = buildString {
        append("${MediaStore.Audio.Media.IS_MUSIC} != 0")
        append(" AND ${MediaStore.Audio.Media.DURATION} > 0")
    }

    val cursor = context.contentResolver.query(
        collection,
        projection,
        selection,
        null,
        null,
    ) ?: throw IllegalStateException("MediaStore genre query returned null cursor")

    cursor.use {
        val idColumn = cursor.getColumnIndex(MediaStore.Audio.Media._ID)
        val genreColumn = cursor.getColumnIndex(MediaStore.Audio.AudioColumns.GENRE)
        if (idColumn == -1 || genreColumn == -1) {
            return emptyMap()
        }

        return buildMap {
            while (cursor.moveToNext()) {
                val mediaId = cursor.getLong(idColumn).toString()
                val genre = extractPrimaryGenre(cursor.getString(genreColumn))
                put(mediaId, genre)
            }
        }
    }
}

private fun readEmbeddedGenre(
    context: Context,
    mediaItem: MediaItem,
): String? {
    return mediaItem.localConfiguration?.uri?.let { mediaUri ->
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, mediaUri)
                extractPrimaryGenre(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE),
                )
            }
        }.getOrNull()
    }
}

internal fun resetGenreTagRepositoryCacheForTest() {
    GenreTagRepository.resetCacheForTest()
}

internal fun extractPrimaryGenre(rawGenre: String?): String? {
    if (rawGenre.isNullOrBlank()) {
        return null
    }

    return rawGenre
        .split(';', '；', ',', '，', '|')
        .asSequence()
        .map(String::trim)
        .firstOrNull(String::isNotEmpty)
}
