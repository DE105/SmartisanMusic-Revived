package com.smartisanos.music.playback

import android.content.ContentUris
import android.content.Context
import android.icu.text.Transliterator
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.smartisanos.music.R
import com.smartisanos.music.data.playback.PlaybackStatsRecord
import java.io.File
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class LocalAudioLibrary(
    private val context: Context,
    private val playbackStatsProvider: () -> Map<String, PlaybackStatsRecord> = { emptyMap() },
) {

    @Volatile private var audioCache = AudioCache()

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
        val currentSnapshot = currentMediaStoreSnapshot()
        val cache = audioCache
        // generation 负责日常增量变化，version 负责更大范围的媒体库重建。
        if (!forceRefresh && cache.snapshot == currentSnapshot && cache.items.isNotEmpty()) {
            return cache.items
        }

        val items = mutableListOf<MediaItem>()
        val playbackStats = runCatching(playbackStatsProvider)
            .getOrDefault(emptyMap())
        try {
            context.contentResolver.query(
                audioCollection(),
                audioItemProjection(),
                audioSelection(),
                null,
                audioSortOrder(),
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
                val generationAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.GENERATION_ADDED)
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn)
                        ?.fixLegacyMetadataEncoding()
                        ?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.unknown_song_title)
                    val artist = cursor.getString(artistColumn)
                        ?.fixLegacyMetadataEncoding()
                        ?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }
                        ?: context.getString(R.string.unknown_artist)
                    val album = cursor.getString(albumColumn)
                        ?.fixLegacyMetadataEncoding()
                        ?.takeIf { it.isNotBlank() }
                    val albumArtist = cursor.getString(albumArtistColumn)
                        ?.fixLegacyMetadataEncoding()
                        ?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }
                    val albumId = cursor.getLong(albumIdColumn).takeIf { it > 0L }
                    val durationMs = cursor.getLong(durationColumn)
                    val trackNumber = cursor.getInt(trackColumn).takeIf { it > 0 }
                    val year = cursor.getInt(yearColumn).takeIf { it > 0 }
                    val dateAddedSeconds = cursor.getLong(dateAddedColumn).takeIf { it > 0L }
                    val generationAdded = cursor.getLong(generationAddedColumn).takeIf { it > 0L }
                    val relativePath = cursor.getString(relativePathColumn)?.takeIf { it.isNotBlank() }
                    val displayName = cursor.getString(displayNameColumn)?.takeIf { it.isNotBlank() }
                    val mimeType = cursor.getString(mimeTypeColumn)?.takeIf { it.isNotBlank() }
                    val audioQualityBadge = resolveAudioQualityBadge(displayName, mimeType)
                    val titleSortKey = LegacyLibraryTitleNormalizer.normalize(title)
                    val titleSection = titleSortKey.legacyLibraryTitleSection()
                    val mediaUri = ContentUris.withAppendedId(audioCollection(), id)
                    val stats = playbackStats[id.toString()]
                    val playCount = stats?.playCount?.takeIf { it > 0L }
                    val score = stats?.score?.takeIf { it > 0 }

                    val extras = Bundle().apply {
                        putString(TitleSortKeyExtraKey, titleSortKey)
                        putString(TitleSectionExtraKey, titleSection)
                        if (!relativePath.isNullOrBlank()) {
                            putString(RelativePathExtraKey, relativePath)
                        }
                        if (albumId != null) {
                            putLong(AlbumIdExtraKey, albumId)
                        }
                        if (dateAddedSeconds != null) {
                            putLong(DateAddedExtraKey, dateAddedSeconds)
                        }
                        if (generationAdded != null) {
                            putLong(GenerationAddedExtraKey, generationAdded)
                        }
                        if (!audioQualityBadge.isNullOrBlank()) {
                            putString(AudioQualityBadgeExtraKey, audioQualityBadge)
                        }
                        if (playCount != null) {
                            putLong(PlayCountExtraKey, playCount)
                        }
                        if (score != null) {
                            putLong(RatingExtraKey, score.toLong())
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

                    metadataBuilder.setExtras(extras)

                    metadataBuilder.setArtworkUri(trackArtworkUri(id))

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

        audioCache = AudioCache(
            snapshot = currentSnapshot,
            items = items,
        )
        return items
    }

    fun invalidateAudioItems() {
        audioCache = AudioCache()
    }

    fun refreshAudioItems(): RefreshResult {
        val indexedSnapshot = queryIndexedAudioSnapshot()
        val pendingAudioPaths = discoverUnindexedAudioPaths(indexedSnapshot)
        val scanResult = scanAudioFiles(pendingAudioPaths)
        val mediaStoreRefreshSucceeded = requestMediaStoreRefresh()
        val items = getAudioItems(forceRefresh = true)
        return RefreshResult(
            items = items,
            scannedFileCount = scanResult.scannedFileCount,
            failedScanCount = scanResult.failedScanCount,
            scanTimedOut = scanResult.timedOut,
            mediaStoreRefreshSucceeded = mediaStoreRefreshSucceeded,
        )
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
        const val GenerationAddedExtraKey = "com.smartisanos.music.extra.GENERATION_ADDED"
        const val TitleSortKeyExtraKey = "com.smartisanos.music.extra.TITLE_SORT_KEY"
        const val TitleSectionExtraKey = "com.smartisanos.music.extra.TITLE_SECTION"
        const val AudioQualityBadgeExtraKey = "com.smartisanos.music.extra.AUDIO_QUALITY_BADGE"
        const val PlayCountExtraKey = "com.smartisanos.music.extra.PLAY_COUNT"
        const val RatingExtraKey = "com.smartisanos.music.extra.RATING"
        const val AudioQualityBadgeFlac = "flac"
        const val AudioQualityBadgeApe = "ape"
        const val AudioQualityBadgeWav = "wav"
        const val AudioQualityBadgeAiff = "aiff"
        const val AudioQualityBadgeAlac = "alac"
        const val AudioQualityBadgeCue = "cue"
        private const val MediaScannerWaitTimeoutSeconds = 30L
        private val AudioFileExtensions = setOf(
            "aac",
            "aif",
            "aiff",
            "alac",
            "amr",
            "ape",
            "dff",
            "dsf",
            "flac",
            "m4a",
            "m4b",
            "mid",
            "midi",
            "mka",
            "mp3",
            "oga",
            "ogg",
            "opus",
            "wav",
            "wma",
        )

        fun albumArtworkUri(albumId: Long): Uri {
            return ContentUris.withAppendedId(
                Uri.parse("content://media/external/audio/albumart"),
                albumId,
            )
        }

        fun trackArtworkUri(mediaId: Long): Uri {
            return Uri.parse("content://media/external/audio/media/$mediaId/albumart")
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

    data class RefreshResult(
        val items: List<MediaItem>,
        val scannedFileCount: Int,
        val failedScanCount: Int,
        val scanTimedOut: Boolean,
        val mediaStoreRefreshSucceeded: Boolean,
    ) {
        // ContentResolver.refresh() 是 provider best-effort 提示；MediaStore 不支持时会返回 false。
        // 手动重扫是否成功应以 MediaScanner 回调和重新查询结果为准，避免误报失败。
        val successful: Boolean
            get() = !scanTimedOut && failedScanCount == 0
    }

    private data class AudioCache(
        val snapshot: MediaStoreSnapshot? = null,
        val items: List<MediaItem> = emptyList(),
    )

    private fun currentMediaStoreSnapshot(): MediaStoreSnapshot {
        val volumes = runCatching {
            MediaStore.getExternalVolumeNames(context)
        }.getOrDefault(emptySet())

        return MediaStoreSnapshot(
            volumes
                .sorted()
                .associateWith { volume ->
                    VolumeSnapshot(
                        version = MediaStore.getVersion(context, volume),
                        generation = MediaStore.getGeneration(context, volume),
                    )
                },
        )
    }

    private data class MediaStoreSnapshot(
        val volumes: Map<String, VolumeSnapshot>,
    )

    private data class VolumeSnapshot(
        val version: String,
        val generation: Long,
    )

    private data class IndexedAudioSnapshot(
        val fileKeys: Set<String>,
    )

    private data class ExternalVolumeRoot(
        val root: File,
        val mediaStoreVolumeName: String,
    )

    private data class ScanResult(
        val scannedFileCount: Int,
        val failedScanCount: Int,
        val timedOut: Boolean,
    )

    private fun audioCollection(): Uri {
        return MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    }

    private fun audioItemProjection(): Array<String> {
        return arrayOf(
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
            MediaStore.Audio.Media.GENERATION_ADDED,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
        )
    }

    private fun audioSelection(): String {
        return buildString {
            append("${MediaStore.Audio.Media.IS_MUSIC} != 0")
            append(" AND ${MediaStore.Audio.Media.DURATION} > 0")
        }
    }

    private fun audioSortOrder(): String {
        return "${MediaStore.Audio.Media.GENERATION_ADDED} DESC"
    }

    private fun queryIndexedAudioSnapshot(): IndexedAudioSnapshot {
        val fileKeys = linkedSetOf<String>()

        runCatching {
            context.contentResolver.query(
                audioCollection(),
                arrayOf(
                    MediaStore.MediaColumns.VOLUME_NAME,
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.MediaColumns.DISPLAY_NAME,
                ),
                audioSelection(),
                null,
                null,
            )
        }.getOrNull()?.use { cursor ->
            val volumeNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.VOLUME_NAME)
            val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val volumeName = cursor.getString(volumeNameColumn)
                val relativePath = cursor.getString(relativePathColumn)
                val displayName = cursor.getString(displayNameColumn)
                indexedAudioKey(volumeName, relativePath, displayName)?.let(fileKeys::add)
            }
        }

        return IndexedAudioSnapshot(
            fileKeys = fileKeys,
        )
    }

    private fun discoverUnindexedAudioPaths(indexedSnapshot: IndexedAudioSnapshot): List<String> {
        val pendingPaths = linkedSetOf<String>()
        externalVolumeRoots().forEach { volumeRoot ->
            volumeRoot.root.walkTopDown()
                .onEnter { directory -> shouldEnterDirectory(volumeRoot.root, directory) }
                .onFail { _, _ -> }
                .forEach { candidate ->
                    val relativePathFromRoot = candidate.relativeToOrNull(volumeRoot.root)
                        ?.invariantSeparatorsPath
                        ?: return@forEach
                    if (
                        shouldSkipMediaScannerPath(relativePathFromRoot) ||
                        !candidate.isFile ||
                        !candidate.canRead() ||
                        !candidate.isAudioCandidate()
                    ) {
                        return@forEach
                    }
                    val relativePath = candidate.relativePathFromVolumeRoot(volumeRoot.root)
                    val key = indexedAudioKey(
                        volumeName = volumeRoot.mediaStoreVolumeName,
                        relativePath = relativePath,
                        displayName = candidate.name,
                    ) ?: return@forEach
                    if (key !in indexedSnapshot.fileKeys) {
                        pendingPaths += candidate.absolutePath
                    }
                }
        }
        return pendingPaths.toList()
    }

    private fun externalVolumeRoots(): Set<ExternalVolumeRoot> {
        val storageManager = context.getSystemService(StorageManager::class.java)
        return context.getExternalFilesDirs(null)
            .asSequence()
            .filterNotNull()
            .mapNotNull { appDir ->
                val root = volumeRootFromAppDir(appDir)?.takeIf { it.isDirectory }
                    ?: return@mapNotNull null
                val mediaStoreVolumeName = storageManager
                    ?.getStorageVolume(root)
                    ?.mediaStoreVolumeName
                    ?: root.absolutePath
                ExternalVolumeRoot(
                    root = root,
                    mediaStoreVolumeName = mediaStoreVolumeName,
                )
            }
            .toSet()
    }

    private fun volumeRootFromAppDir(appDir: File): File? {
        val marker = "${File.separator}Android${File.separator}data${File.separator}"
        val path = appDir.absolutePath
        val markerIndex = path.indexOf(marker)
        if (markerIndex <= 0) {
            return null
        }
        return File(path.substring(0, markerIndex))
    }

    private fun shouldEnterDirectory(scanRoot: File, directory: File): Boolean {
        if (directory == scanRoot) {
            return true
        }
        if (!directory.canRead()) {
            return false
        }
        val relativePath = directory.relativeToOrNull(scanRoot)
            ?.invariantSeparatorsPath
            .orEmpty()
        if (shouldSkipMediaScannerPath(relativePath)) {
            return false
        }
        return when {
            relativePath == "Android/data" || relativePath.startsWith("Android/data/") -> false
            relativePath == "Android/obb" || relativePath.startsWith("Android/obb/") -> false
            else -> true
        }
    }

    private fun File.relativePathFromVolumeRoot(volumeRoot: File): String? {
        val parent = parentFile ?: return null
        if (parent == volumeRoot) {
            return null
        }
        return parent.relativeToOrNull(volumeRoot)
            ?.invariantSeparatorsPath
            ?.takeIf { it.isNotBlank() }
            ?.let { "$it/" }
    }

    private fun scanAudioFiles(paths: List<String>): ScanResult {
        if (paths.isEmpty()) {
            return ScanResult(
                scannedFileCount = 0,
                failedScanCount = 0,
                timedOut = false,
            )
        }
        val scannedFileCount = AtomicInteger(0)
        val failedScanCount = AtomicInteger(0)
        var timedOut = false

        paths.chunked(128).forEach { batch ->
            val latch = CountDownLatch(batch.size)
            val acceptingCallbacks = AtomicBoolean(true)
            val completedCallbackCount = AtomicInteger(0)
            val mimeTypes = batch.map(::resolveAudioMimeType).toTypedArray()
            MediaScannerConnection.scanFile(
                context,
                batch.toTypedArray(),
                mimeTypes,
            ) { _, uri ->
                if (acceptingCallbacks.get()) {
                    completedCallbackCount.incrementAndGet()
                    if (uri == null) {
                        failedScanCount.incrementAndGet()
                    } else {
                        scannedFileCount.incrementAndGet()
                    }
                }
                latch.countDown()
            }
            if (!latch.await(MediaScannerWaitTimeoutSeconds, TimeUnit.SECONDS)) {
                acceptingCallbacks.set(false)
                timedOut = true
                failedScanCount.addAndGet(batch.size - completedCallbackCount.get())
            }
        }

        return ScanResult(
            scannedFileCount = scannedFileCount.get(),
            failedScanCount = failedScanCount.get(),
            timedOut = timedOut,
        )
    }

    private fun requestMediaStoreRefresh(): Boolean {
        return runCatching {
            context.contentResolver.refresh(
                audioCollection(),
                null,
                null,
            )
        }.getOrDefault(false)
    }

    private fun indexedAudioKey(
        volumeName: String?,
        relativePath: String?,
        displayName: String?,
    ): String? {
        val normalizedDisplayName = displayName?.trim().orEmpty()
        if (normalizedDisplayName.isEmpty()) {
            return null
        }
        val normalizedVolumeName = volumeName?.trim().orEmpty()
        val normalizedRelativePath = relativePath
            ?.replace('\\', '/')
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { if (it.endsWith('/')) it else "$it/" }
            .orEmpty()
        return "$normalizedVolumeName:$normalizedRelativePath$normalizedDisplayName"
            .lowercase(Locale.ROOT)
    }

    private fun File.isAudioCandidate(): Boolean {
        val extension = extension.lowercase(Locale.ROOT)
        return extension in AudioFileExtensions
    }

    private fun resolveAudioMimeType(path: String): String? {
        val extension = path.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
        if (extension.isEmpty()) {
            return null
        }
        return when (extension) {
            AudioQualityBadgeApe -> "audio/ape"
            AudioQualityBadgeAlac -> "audio/alac"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
    }
}

private object LegacyLibraryTitleNormalizer {
    private val hanToLatin = runCatching {
        Transliterator.getInstance("Han-Latin; Latin-ASCII")
    }.getOrNull()
    private val combiningMarks = "\\p{Mn}+".toRegex()

    @Synchronized
    fun normalize(title: String): String {
        val trimmed = title.trim()
        val transliterated = hanToLatin?.transliterate(trimmed) ?: trimmed
        return Normalizer.normalize(transliterated, Normalizer.Form.NFD)
            .replace(combiningMarks, "")
            .lowercase(Locale.ROOT)
            .trim()
    }
}

internal fun String.fixLegacyMetadataEncoding(): String {
    if (isEmpty() || !looksLikeLegacyMojibake()) {
        return this
    }
    return LegacyMetadataRepairCharsets.firstNotNullOfOrNull { charset ->
        repairLegacyMojibake(charset)
    } ?: this
}

private fun String.repairLegacyMojibake(charset: Charset): String? {
    return toByteArrayOrNull(charset)
        ?.let { bytes -> String(bytes, StandardCharsets.UTF_8) }
        ?.takeIf { repaired ->
            repaired.isNotEmpty() &&
                repaired != this &&
                ReplacementCharacter !in repaired &&
                (repaired.containsCjkOrFullWidth() || !repaired.looksLikeLegacyMojibake())
        }
}

private fun String.toByteArrayOrNull(charset: Charset): ByteArray? {
    return runCatching {
        val encoder = charset.newEncoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val buffer = encoder.encode(CharBuffer.wrap(this))
        ByteArray(buffer.remaining()).also(buffer::get)
    }.getOrNull()
}

private fun String.looksLikeLegacyMojibake(): Boolean {
    return LegacyMojibakeMarkers.any(::contains)
}

private fun String.containsCjkOrFullWidth(): Boolean {
    return any { char ->
        val code = char.code
        code in 0x3040..0x30FF ||
            code in 0x3400..0x9FFF ||
            code in 0x1100..0x11FF ||
            code in 0xAC00..0xD7AF ||
            code in 0xFF01..0xFFEF
    }
}

private val LegacyMetadataRepairCharsets = listOf(
    StandardCharsets.ISO_8859_1,
    Charset.forName("windows-1252"),
    Charset.forName("windows-1250"),
)

private val LegacyMojibakeMarkers = listOf("Ã", "Â", "ã", "ď", "ï", "æ", "å", "¤", "½", "ž")
private const val ReplacementCharacter = '\uFFFD'

private fun String.legacyLibraryTitleSection(): String {
    val firstLetter = firstOrNull { char ->
        char.isLetterOrDigit()
    } ?: return "#"
    val upper = firstLetter.uppercaseChar()
    return if (upper in 'A'..'Z') {
        upper.toString()
    } else {
        "#"
    }
}

internal fun shouldSkipMediaScannerPath(relativePath: String): Boolean {
    return relativePath
        .replace('\\', '/')
        .split('/')
        .filter(String::isNotEmpty)
        .any { segment -> segment.startsWith('.') }
}
