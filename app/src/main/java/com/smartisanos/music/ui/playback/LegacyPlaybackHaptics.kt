package com.smartisanos.music.ui.playback

import android.content.Context
import android.os.Vibrator
import smartisanos.api.VibratorSmt

internal object LegacyPlaybackHaptics {
    fun vibrateEffect(
        context: Context,
        effect: Int = DefaultEffect,
    ) {
        val vibrator = context.getSystemService(Vibrator::class.java)
        VibratorSmt.vibrateEffect(vibrator, effect)
    }

    private const val DefaultEffect = 2
}
