package com.smartisanos.music.playback

import androidx.media3.common.Player

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
