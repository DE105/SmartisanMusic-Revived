package com.smartisan.music.ui.components

import android.view.ViewConfiguration
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.smartisan.music.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Keeps a quick tap visible even when Press and Release arrive in the same frame. */
@Composable
internal fun InteractionSource.collectSmartisanPressedAsState(): State<Boolean> {
    val pressed = remember(this) { mutableStateOf(false) }
    LaunchedEffect(this) {
        val active = mutableSetOf<PressInteraction.Press>()
        var release: Job? = null
        interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    release?.cancel()
                    active.add(interaction)
                    pressed.value = true
                }
                is PressInteraction.Release -> {
                    if (active.remove(interaction.press) && active.isEmpty()) {
                        release = launch {
                            // Match the platform's pressed-state feedback interval, starting
                            // on a frame so a same-frame tap cannot skip the drawable entirely.
                            val start = withFrameNanos { it }
                            val duration = ViewConfiguration.getPressedStateDuration() * 1_000_000L
                            while (withFrameNanos { it } - start < duration) Unit
                            pressed.value = false
                        }
                    }
                }
                is PressInteraction.Cancel -> {
                    if (active.remove(interaction.press) && active.isEmpty()) {
                        release?.cancel()
                        pressed.value = false
                    }
                }
            }
        }
    }
    return pressed
}

/** Original blue list selectors pair the pressed background with white text. */
@Composable
internal fun smartisanPressedTextColor(normalColor: Color, pressed: Boolean): Color =
    if (pressed) colorResource(R.color.setting_item_text_color_highlight) else normalColor
