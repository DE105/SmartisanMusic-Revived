package com.smartisan.music.ui.shell.titlebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import com.smartisan.music.R
import com.smartisan.music.ui.navigation.LocalSmartisanTitleMotion
import com.smartisan.music.ui.navigation.ProjectableNavigationTitle
import com.smartisan.music.ui.navigation.SmartisanNavigationMotion
import com.smartisan.music.ui.navigation.SmartisanTitleMotion
import kotlinx.coroutines.flow.first

/** Keep the title background stationary; only the individual title and action slots animate. */
@Composable
internal fun <T : Any> TitleBarTransition(
    secondaryKey: T?,
    modifier: Modifier = Modifier,
    label: String = "title bar transition",
    sharedMotion: SmartisanNavigationMotion? = null,
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable (T) -> Unit,
) {
    val localMotion = remember { SmartisanNavigationMotion(secondaryKey != null) }
    val motion = sharedMotion ?: localMotion
    var retained by remember { mutableStateOf(secondaryKey) }
    LaunchedEffect(secondaryKey) {
        if (secondaryKey != null) {
            if (sharedMotion == null && retained != null && retained != secondaryKey)
                motion.snapTo(false)
            retained = secondaryKey
            if (sharedMotion == null) motion.animateTo(true)
        } else if (retained != null) {
            if (sharedMotion == null) motion.animateTo(false)
            else snapshotFlow { motion.closed }.first { it }
            retained = null
        }
    }
    val titleHeight =
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
            dimensionResource(R.dimen.title_bar_height)
    ProjectableNavigationTitle(modifier, titleHeight) { titleModifier ->
        val parentMotion = LocalSmartisanTitleMotion.current
        Box(
            titleModifier
                .clipToBounds()
                .then(
                    if (parentMotion == null)
                        Modifier.background(colorResource(R.color.title_bar_background))
                    else Modifier
                )
        ) {
            CompositionLocalProvider(
                LocalSmartisanTitleMotion provides SmartisanTitleMotion(motion, false, parentMotion)
            ) {
                primaryContent()
            }
            retained?.let { entry ->
                CompositionLocalProvider(
                    LocalSmartisanTitleMotion provides
                        SmartisanTitleMotion(motion, true, parentMotion)
                ) {
                    secondaryContent(entry)
                }
            }
        }
    }
}
