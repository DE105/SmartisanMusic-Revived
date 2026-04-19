package com.smartisanos.music.playback

import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackSleepTimerState(
    val durationMs: Long = 0L,
    val remainingMs: Long = 0L,
) {
    val isActive: Boolean
        get() = remainingMs > 0L
}

internal object PlaybackSleepTimer {
    private val mutableState = MutableStateFlow(PlaybackSleepTimerState())

    val state: StateFlow<PlaybackSleepTimerState> = mutableState.asStateFlow()

    private var countDownTimer: CountDownTimer? = null
    private var onFinishAction: (() -> Unit)? = null

    fun start(
        durationMs: Long,
        onFinish: () -> Unit,
    ) {
        if (durationMs <= 0L) {
            cancel()
            return
        }
        countDownTimer?.cancel()
        onFinishAction = onFinish
        mutableState.value = PlaybackSleepTimerState(
            durationMs = durationMs,
            remainingMs = durationMs,
        )
        countDownTimer = object : CountDownTimer(durationMs, SleepTimerTickIntervalMs) {
            override fun onTick(millisUntilFinished: Long) {
                mutableState.value = PlaybackSleepTimerState(
                    durationMs = durationMs,
                    remainingMs = millisUntilFinished.coerceAtLeast(0L),
                )
            }

            override fun onFinish() {
                countDownTimer = null
                mutableState.value = PlaybackSleepTimerState()
                onFinishAction?.invoke()
                onFinishAction = null
            }
        }.start()
    }

    fun cancel() {
        countDownTimer?.cancel()
        countDownTimer = null
        onFinishAction = null
        mutableState.value = PlaybackSleepTimerState()
    }

    private const val SleepTimerTickIntervalMs = 1_000L
}
