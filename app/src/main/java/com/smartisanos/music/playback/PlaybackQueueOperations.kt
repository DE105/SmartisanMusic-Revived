package com.smartisanos.music.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

internal fun Player?.replaceQueueAndPlay(
    mediaItems: List<MediaItem>,
    startIndex: Int = 0,
    shuffleModeEnabled: Boolean = false,
) {
    val player = this ?: return
    if (mediaItems.isEmpty()) {
        return
    }

    val safeStartIndex = startIndex.coerceIn(0, mediaItems.lastIndex)
    player.shuffleModeEnabled = shuffleModeEnabled
    player.setMediaItems(mediaItems, safeStartIndex, 0L)
    player.prepare()
    player.play()
}

internal fun Player?.removeMediaItemsByMediaIds(mediaIds: Set<String>) {
    val player = this ?: return
    val normalizedMediaIds = mediaIds.asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toSet()
    if (normalizedMediaIds.isEmpty()) {
        return
    }

    for (index in player.mediaItemCount - 1 downTo 0) {
        if (player.getMediaItemAt(index).mediaId in normalizedMediaIds) {
            player.removeMediaItem(index)
        }
    }
}

internal fun Player?.removeMediaItemsMatching(predicate: (MediaItem) -> Boolean) {
    val player = this ?: return
    var rangeEnd = player.mediaItemCount
    var index = rangeEnd - 1
    while (index >= 0) {
        if (!predicate(player.getMediaItemAt(index))) {
            rangeEnd = index
            index -= 1
            continue
        }

        var rangeStart = index
        while (rangeStart > 0 && predicate(player.getMediaItemAt(rangeStart - 1))) {
            rangeStart -= 1
        }
        player.removeMediaItems(rangeStart, rangeEnd)
        rangeEnd = rangeStart
        index = rangeStart - 1
    }
}
