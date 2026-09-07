package com.smartisan.music.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.zIndex
import com.smartisan.music.R
import com.smartisan.music.ui.navigation.LocalNavigationTitleInline
import com.smartisan.music.ui.navigation.LocalProjectedNavigationTitle
import com.smartisan.music.ui.navigation.ProjectedNavigationTitle
import com.smartisan.music.ui.navigation.RenderProjectedNavigationTitle
import com.smartisan.music.ui.navigation.SmartisanNavigationMotion
import com.smartisan.music.ui.navigation.SmartisanTitleMotion

/** Navigation state belongs to the caller; this host retains only the outgoing drawing. */
@Composable
internal fun <T : Any> PageStackTransition(
    secondaryKey: T?,
    modifier: Modifier = Modifier,
    label: String = "page stack transition",
    sharedMotion: SmartisanNavigationMotion? = null,
    projectTitles: Boolean = false,
    titleProjectionEnabled: Boolean = true,
    primaryContent: @Composable () -> Unit,
    secondaryContent: @Composable (T) -> Unit,
) {
    val localMotion = remember { SmartisanNavigationMotion(secondaryKey != null) }
    val motion = sharedMotion ?: localMotion
    val primaryTitle = remember { ProjectedNavigationTitle() }
    val secondaryTitle = remember { ProjectedNavigationTitle() }
    val parentTitle = LocalProjectedNavigationTitle.current
    val parentInline = LocalNavigationTitleInline.current
    var retained by remember { mutableStateOf(secondaryKey) }
    LaunchedEffect(secondaryKey) {
        if (secondaryKey != null) {
            if (retained != null && retained != secondaryKey) motion.snapTo(false)
            retained = secondaryKey
            motion.animateTo(true)
        } else if (retained != null) {
            motion.animateTo(false)
            retained = null
        }
    }
    BoxWithConstraints(modifier.clipToBounds()) {
        val width = with(LocalDensity.current) { maxWidth.toPx() }
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                translationX = -width * motion.position.value
            }
        ) {
            CompositionLocalProvider(
                LocalProjectedNavigationTitle provides
                    if (projectTitles) primaryTitle else parentTitle,
                LocalNavigationTitleInline provides
                    if (projectTitles) !titleProjectionEnabled else parentInline,
            ) {
                primaryContent()
            }
        }
        retained?.let { entry ->
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer {
                        translationX = width * (1f - motion.position.value)
                    }
                    .zIndex(1f)
            ) {
                CompositionLocalProvider(
                    LocalProjectedNavigationTitle provides
                        if (projectTitles) secondaryTitle else parentTitle,
                    LocalNavigationTitleInline provides
                        if (projectTitles) !titleProjectionEnabled else parentInline,
                ) {
                    secondaryContent(entry)
                }
            }
        }
        if (projectTitles && titleProjectionEnabled) {
            val titleHeight =
                WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                    dimensionResource(R.dimen.title_bar_height)
            Box(
                Modifier.fillMaxWidth()
                    .height(titleHeight)
                    .background(colorResource(R.color.title_bar_background))
                    .zIndex(2f)
            ) {
                RenderProjectedNavigationTitle(primaryTitle, SmartisanTitleMotion(motion, false))
                RenderProjectedNavigationTitle(secondaryTitle, SmartisanTitleMotion(motion, true))
            }
        }
    }
}
