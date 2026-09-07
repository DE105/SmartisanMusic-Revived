package com.smartisan.music.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.smartisan.music.R
import com.smartisan.music.ui.shell.PageStackTransition
import com.smartisan.music.ui.shell.titlebar.TitleBarTransition

/**
 * One stationary title surface above full-width page motion; pages supply only content and events.
 */
@Composable
internal fun <T : Any> SmartisanPageStack(
    secondaryKey: T?,
    primaryTitle: @Composable () -> Unit,
    secondaryTitle: @Composable (T) -> Unit,
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val motion = remember { SmartisanNavigationMotion(secondaryKey != null) }
    val titleHeight =
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
            dimensionResource(R.dimen.title_bar_height)
    Column(modifier.fillMaxSize()) {
        TitleBarTransition(
            secondaryKey,
            Modifier.fillMaxWidth().height(titleHeight),
            sharedMotion = motion,
            primaryContent = primaryTitle,
            secondaryContent = secondaryTitle,
        )
        CompositionLocalProvider(LocalSmartisanTitleVisible provides false) {
            PageStackTransition(
                secondaryKey,
                Modifier.fillMaxWidth().weight(1f),
                sharedMotion = motion,
                primaryContent = primaryContent,
                secondaryContent = secondaryContent,
            )
        }
    }
}
