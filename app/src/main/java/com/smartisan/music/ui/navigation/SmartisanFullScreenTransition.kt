package com.smartisan.music.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.smartisan.music.ui.components.SmartisanTouchShield

/** Full-page presentation moves the title and body together, unlike a nested page stack. */
@Composable
internal fun SmartisanFullScreenTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible,
        modifier.fillMaxSize().clipToBounds(),
        enter = slideInVertically(tween(300, easing = { 1f - (1f - it) * (1f - it) })) { it },
        exit = slideOutVertically(tween(300, easing = { 1f - (1f - it) * (1f - it) })) { it },
    ) {
        Box(Modifier.fillMaxSize()) {
            SmartisanTouchShield()
            CompositionLocalProvider(
                LocalProjectedNavigationTitle provides null,
                LocalSmartisanTitleMotion provides null,
                LocalSmartisanTitleVisible provides true,
                content = content,
            )
        }
    }
}
