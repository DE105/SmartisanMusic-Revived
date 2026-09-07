package com.smartisan.music.ui.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.smartisan.music.R
import com.smartisan.music.ui.navigation.LocalProjectedNavigationTitle
import com.smartisan.music.ui.navigation.LocalSmartisanTitleMotion
import com.smartisan.music.ui.navigation.LocalSmartisanTitleVisible
import com.smartisan.music.ui.navigation.ProjectableNavigationTitle

internal data class SmartisanTitleBarAction(
    @param:DrawableRes val iconRes: Int,
    val contentDescription: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val checked: Boolean? = null,
    val modifier: Modifier = Modifier,
)

/** Resource-based title bar with physical left/right action groups and a symmetric center. */
@Composable
internal fun SmartisanTitleBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: SmartisanTitleBarAction? = null,
    action: SmartisanTitleBarAction? = null,
    includeStatusBar: Boolean = true,
    showShadow: Boolean = true,
    navigationActions: List<SmartisanTitleBarAction> = emptyList(),
    actions: List<SmartisanTitleBarAction> = emptyList(),
    centerContent: (@Composable () -> Unit)? = null,
    contentHeight: androidx.compose.ui.unit.Dp = dimensionResource(R.dimen.title_bar_height),
) {
    if (!LocalSmartisanTitleVisible.current) return
    if (LocalProjectedNavigationTitle.current != null) {
        val height =
            contentHeight +
                if (includeStatusBar)
                    WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                else 0.dp
        ProjectableNavigationTitle(modifier, height) { titleModifier ->
            SmartisanTitleBar(
                title = title,
                modifier = titleModifier,
                navigationIcon = navigationIcon,
                action = action,
                includeStatusBar = includeStatusBar,
                showShadow = showShadow,
                navigationActions = navigationActions,
                actions = actions,
                centerContent = centerContent,
                contentHeight = contentHeight,
            )
        }
        return
    }
    val titleMotion = LocalSmartisanTitleMotion.current
    val shadowHeight = dimensionResource(R.dimen.title_bar_shadow_height)
    val iconSize = dimensionResource(R.dimen.standard_icon_size)
    val edgeMargin = dimensionResource(R.dimen.bar_margin_edge)
    val leftActions = listOfNotNull(navigationIcon) + navigationActions
    val rightActions = listOfNotNull(action) + actions
    val gap = dimensionResource(R.dimen.title_bar_margin_view)
    val widestCount = maxOf(leftActions.size, rightActions.size)
    val titleInset =
        if (widestCount > 0) edgeMargin + iconSize * widestCount + gap * (widestCount - 1) else 0.dp
    val centerVisible =
        widestCount == 0 || titleInset + gap <= dimensionResource(R.dimen.title_bar_center_limite)
    val contentAlpha = titleMotion?.contentAlpha ?: 1f
    val leftAlpha = titleMotion?.leftAlpha ?: 1f
    val leftSlide = with(LocalDensity.current) { (iconSize + edgeMargin * 2).toPx() }
    Column(
        modifier
            .then(if (showShadow) Modifier.zIndex(1f) else Modifier)
            .fillMaxWidth()
            .then(
                if (titleMotion == null)
                    Modifier.background(colorResource(R.color.title_bar_background))
                else Modifier
            )
    ) {
        if (includeStatusBar) {
            Spacer(Modifier.fillMaxWidth().windowInsetsTopHeight(WindowInsets.statusBars))
        }
        Box(Modifier.fillMaxWidth().height(contentHeight)) {
            if (centerVisible && contentAlpha > 0f) {
                if (centerContent == null) {
                    SmartisanTitleText(
                        title,
                        titleInset,
                        Modifier.matchParentSize().graphicsLayer { alpha = contentAlpha },
                    )
                } else {
                    Box(
                        Modifier.matchParentSize().padding(horizontal = titleInset).graphicsLayer {
                            alpha = contentAlpha
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        centerContent()
                    }
                }
            }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                if (leftAlpha > 0f)
                    Row(
                        Modifier.align(AbsoluteAlignment.CenterLeft)
                            .absolutePadding(left = edgeMargin)
                            .graphicsLayer {
                                alpha = leftAlpha
                                translationX = (titleMotion?.leftOffset ?: 0f) * leftSlide
                            },
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        leftActions.forEach { TitleBarIcon(it) }
                    }
                if (contentAlpha > 0f)
                    Row(
                        Modifier.align(AbsoluteAlignment.CenterRight)
                            .absolutePadding(right = edgeMargin)
                            .graphicsLayer { alpha = contentAlpha },
                        horizontalArrangement = Arrangement.spacedBy(gap),
                    ) {
                        rightActions.asReversed().forEach { TitleBarIcon(it) }
                    }
            }
            if (showShadow && (titleMotion == null || !titleMotion.detail)) {
                SmartisanDrawableBackground(
                    R.drawable.title_bar_shadow,
                    Modifier.align(Alignment.BottomCenter)
                        .offset(y = shadowHeight)
                        .fillMaxWidth()
                        .height(shadowHeight),
                )
            }
        }
    }
}

@Composable
private fun TitleBarIcon(action: SmartisanTitleBarAction, modifier: Modifier = Modifier) {
    val motionInteractive = LocalSmartisanTitleMotion.current?.interactive ?: true
    val iconSize = dimensionResource(R.dimen.standard_icon_size)
    val viewConfiguration = LocalViewConfiguration.current
    val iconViewConfiguration =
        remember(viewConfiguration, iconSize) {
            object : ViewConfiguration by viewConfiguration {
                override val minimumTouchTargetSize = DpSize(iconSize, iconSize)
            }
        }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val scale by
        animateFloatAsState(
            targetValue = if (pressed) TitleBarPressedScale else 1f,
            animationSpec =
                spring(
                    dampingRatio = TitleBarDampingRatio,
                    stiffness = TitleBarStiffness,
                    visibilityThreshold = TitleBarVisibilityThreshold,
                ),
            label = "Smartisan title icon press",
        )
    // Preserve this shim button's 36dp hit bounds and transform input with the whole icon.
    CompositionLocalProvider(LocalViewConfiguration provides iconViewConfiguration) {
        Box(
            modifier
                .then(action.modifier)
                .size(iconSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    role = Role.Button,
                    enabled = action.enabled && motionInteractive,
                    onClick = smartisanClick(action.onClick),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter =
                    rememberSmartisanDrawablePainter(
                        action.iconRes,
                        enabled = action.enabled,
                        pressed = pressed,
                        focused = focused,
                        checked = action.checked == true,
                    ),
                contentDescription = action.contentDescription,
                contentScale = ContentScale.None,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

// TitleBar.TitleBarIconScaleTouchListener, retained from the calibrated 8.1.0 shim.
private const val TitleBarPressedScale = 1.33f
private const val TitleBarDampingRatio = 0.55f
private const val TitleBarStiffness = 800f
// DynamicAnimation 1.1.0: MIN_VISIBLE_CHANGE_SCALE (1/500) * THRESHOLD_MULTIPLIER (0.75).
private const val TitleBarVisibilityThreshold = 0.0015f

@Preview(name = "Title / light", widthDp = 360)
@Preview(name = "Title / dark", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Title / large text", widthDp = 320, fontScale = 1.5f)
@Composable
private fun SmartisanTitleBarPreview() {
    SmartisanTitleBar(
        title = stringResource(R.string.app_icon),
        navigationIcon =
            SmartisanTitleBarAction(
                R.drawable.standard_icon_back_selector,
                stringResource(R.string.back),
                onClick = {},
            ),
        includeStatusBar = false,
    )
}
