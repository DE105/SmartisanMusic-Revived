package com.smartisan.music.ui.shell

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics

/** Keep playback-bar animation state while excluding covered chrome from drawing and input. */
internal fun Modifier.retainedChromeVisibility(visible: Boolean): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            if (visible) placeable.place(0, 0)
        }
    }
    .then(if (visible) Modifier else Modifier.clearAndSetSemantics {})
