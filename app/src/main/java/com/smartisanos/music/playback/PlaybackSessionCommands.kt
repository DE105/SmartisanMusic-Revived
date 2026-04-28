package com.smartisanos.music.playback

import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand

internal const val ScratchSeekModeAction = "com.smartisanos.music.action.SET_SCRATCH_SEEK_MODE"
internal const val ScratchSeekModeEnabledKey = "scratch_seek_mode_enabled"
internal const val StartSleepTimerAction = "com.smartisanos.music.action.START_SLEEP_TIMER"
internal const val CancelSleepTimerAction = "com.smartisanos.music.action.CANCEL_SLEEP_TIMER"
internal const val SleepTimerDurationMsKey = "sleep_timer_duration_ms"
internal const val RefreshLibraryAction = "com.smartisanos.music.action.REFRESH_LIBRARY"
internal const val InvalidateLibraryAction = "com.smartisanos.music.action.INVALIDATE_LIBRARY"

internal val ScratchSeekModeCommand = SessionCommand(ScratchSeekModeAction, Bundle.EMPTY)
internal val StartSleepTimerCommand = SessionCommand(StartSleepTimerAction, Bundle.EMPTY)
internal val CancelSleepTimerCommand = SessionCommand(CancelSleepTimerAction, Bundle.EMPTY)
internal val RefreshLibraryCommand = SessionCommand(RefreshLibraryAction, Bundle.EMPTY)
internal val InvalidateLibraryCommand = SessionCommand(InvalidateLibraryAction, Bundle.EMPTY)

internal fun MediaController.setScratchSeekModeEnabled(enabled: Boolean) {
    val args = Bundle().apply {
        putBoolean(ScratchSeekModeEnabledKey, enabled)
    }
    sendCustomCommand(ScratchSeekModeCommand, args)
}

internal fun MediaController.startSleepTimer(durationMs: Long) {
    val args = Bundle().apply {
        putLong(SleepTimerDurationMsKey, durationMs)
    }
    sendCustomCommand(StartSleepTimerCommand, args)
}

internal fun MediaController.cancelSleepTimer() {
    sendCustomCommand(CancelSleepTimerCommand, Bundle.EMPTY)
}

internal fun MediaController.refreshLibrary() =
    sendCustomCommand(RefreshLibraryCommand, Bundle.EMPTY)

internal fun MediaController.invalidateLibrary() =
    sendCustomCommand(InvalidateLibraryCommand, Bundle.EMPTY)
