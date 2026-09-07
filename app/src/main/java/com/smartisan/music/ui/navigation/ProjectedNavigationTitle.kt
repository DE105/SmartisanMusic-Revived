package com.smartisan.music.ui.navigation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/** A page supplies one live title slot; the surrounding stack owns where it is rendered. */
@Stable
internal class ProjectedNavigationTitle {
    private var owner: Any? = null
    var content by mutableStateOf<(@Composable () -> Unit)?>(null)
        private set

    fun attach(token: Any, content: @Composable () -> Unit) {
        owner = token
        this.content = content
    }

    fun detach(token: Any) {
        if (owner === token) {
            owner = null
            content = null
        }
    }
}

internal val LocalProjectedNavigationTitle =
    staticCompositionLocalOf<ProjectedNavigationTitle?> { null }
internal val LocalNavigationTitleInline = staticCompositionLocalOf { false }

@Composable
internal fun RenderProjectedNavigationTitle(
    title: ProjectedNavigationTitle,
    motion: SmartisanTitleMotion,
) {
    CompositionLocalProvider(LocalSmartisanTitleMotion provides motion) {
        title.content?.invoke()
    }
}

/** Reserve the original title height without creating a second page or state owner. */
@Composable
internal fun ProjectableNavigationTitle(
    modifier: Modifier,
    height: Dp,
    content: @Composable (Modifier) -> Unit,
) {
    val outlet = LocalProjectedNavigationTitle.current
    if (outlet == null) {
        content(modifier)
        return
    }
    val currentContent by rememberUpdatedState(content)
    val currentModifier by rememberUpdatedState(modifier)
    val token = remember { Any() }
    DisposableEffect(outlet, token) {
        outlet.attach(token) {
            CompositionLocalProvider(LocalProjectedNavigationTitle provides null) {
                currentContent(currentModifier)
            }
        }
        onDispose { outlet.detach(token) }
    }
    // Keep the registration alive while a full-screen overlay uses an inline title.
    // Restoring the projected layer must not wait for another DisposableEffect to attach it.
    if (LocalNavigationTitleInline.current) {
        CompositionLocalProvider(LocalProjectedNavigationTitle provides null) {
            content(modifier)
        }
    } else {
        Spacer(modifier.height(height))
    }
}
