package com.smartisanos.music.playback

import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand

internal const val ScratchSeekModeAction = "com.smartisanos.music.action.SET_SCRATCH_SEEK_MODE"
internal const val ScratchSeekModeEnabledKey = "scratch_seek_mode_enabled"

internal val ScratchSeekModeCommand = SessionCommand(ScratchSeekModeAction, Bundle.EMPTY)

internal fun MediaController.setScratchSeekModeEnabled(enabled: Boolean) {
    val args = Bundle().apply {
        putBoolean(ScratchSeekModeEnabledKey, enabled)
    }
    sendCustomCommand(ScratchSeekModeCommand, args)
}
