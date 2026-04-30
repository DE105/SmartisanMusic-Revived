package com.smartisanos.music.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.R

private val TopBarTextColor = Color(0xFF333333)

private val TitleBarActionText = Color(0xFF595959)
private val TitleBarActionPressedText = Color(0xFF333333)

private val TopBarHorizontalPadding = 10.dp
private val TopBarButtonHeight = 30.dp
private val TopBarToolbarButtonHeight = 30.dp
private val TopBarButtonWidth = 42.dp
private val TopBarBackButtonWidth = 56.dp
private val TopBarBackButtonHeight = 30.dp
private val TopBarToolbarButtonWidth = 46.dp
private val TopBarToolbarTextButtonMinWidth = 48.dp
private val TopBarButtonCorner = 5.dp
private val TopBarToolbarButtonCorner = 5.dp
private val TopBarToolbarTextHorizontalPadding = 6.dp
private val TopBarBackContentHorizontalPadding = 8.dp
private val TopBarBackContentSpacing = 3.dp
private val TopBarBackIconSize = 14.dp
private val TopBarBackIconStroke = 2.2.dp

private val TopBarTitleStyle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.Medium,
    color = TopBarTextColor,
)
private val TopBarButtonTextStyle = TextStyle(
    fontSize = 14.sp,
    fontWeight = FontWeight.Medium,
    color = TitleBarActionText,
)

val SmartisanTopBarHeight = 50.dp
val SmartisanTopBarShadowHeight = 2.dp

enum class SmartisanTopBarTextButtonStyle {
    Filled,
    Back,
    Toolbar,
}

enum class SmartisanTopBarIconButtonStyle {
    Plain,
    Filled,
    Toolbar,
}

enum class SmartisanTopBarDangerButtonStyle {
    Filled,
    Toolbar,
}

enum class SmartisanTitleBarSurfaceStyle {
    Main,
    Playback,
}

private fun Modifier.smartisanButtonBackground(
    pressed: Boolean,
    enabled: Boolean = true,
    isDanger: Boolean = false,
    cornerRadius: Dp = 5.dp
): Modifier = drawWithCache {
    val radiusPx = cornerRadius.toPx()
    val strokeWidth = 1.dp.toPx()
    val halfStroke = strokeWidth / 2f

    val normalGradient = Brush.verticalGradient(
        0.0f to if (isDanger) Color(0xFFED8380) else Color(0xFFFFFFFF),
        0.05f to if (isDanger) Color(0xFFE87A77) else Color(0xFFFDFDFD),
        1.0f to if (isDanger) Color(0xFFE2615E) else Color(0xFFF6F6F6)
    )
    val pressedGradient = Brush.verticalGradient(
        0.0f to if (isDanger) Color(0xFFC04B48) else Color(0xFFE0E0E0),
        0.1f to if (isDanger) Color(0xFFD05C59) else Color(0xFFE8E8E8),
        1.0f to if (isDanger) Color(0xFFD66A67) else Color(0xFFEBEBEB)
    )
    val disabledGradient = Brush.verticalGradient(
        listOf(if (isDanger) Color(0xFFF2F2F2) else Color(0xFFF9F9F9), if (isDanger) Color(0xFFEBEBEB) else Color(0xFFF2F2F2))
    )

    val bgBrush = when {
        !enabled -> disabledGradient
        pressed -> pressedGradient
        else -> normalGradient
    }

    val borderColor = when {
        !enabled -> Color(0xFFE0E0E0)
        isDanger -> Color(0xFFD06461)
        pressed -> Color(0xFFC4C4C4)
        else -> Color(0xFFD4D4D4)
    }

    onDrawBehind {
        // Drop shadow for lifted effect
        if (!pressed && enabled) {
            drawRoundRect(
                color = Color(0x0F000000),
                topLeft = Offset(0f, 1.5f),
                size = size,
                cornerRadius = CornerRadius(radiusPx, radiusPx)
            )
        }

        // Background
        drawRoundRect(
            brush = bgBrush,
            size = size,
            cornerRadius = CornerRadius(radiusPx, radiusPx)
        )

        // Border
        drawRoundRect(
            color = borderColor,
            topLeft = Offset(halfStroke, halfStroke),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(radiusPx - halfStroke, radiusPx - halfStroke),
            style = Stroke(strokeWidth)
        )
    }
}

@Composable
fun SmartisanTitleBarSurface(
    style: SmartisanTitleBarSurfaceStyle,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier) {
        SmartisanDrawableBackground(
            drawableRes = when (style) {
                SmartisanTitleBarSurfaceStyle.Main -> R.drawable.titlebar_bg
                SmartisanTitleBarSurfaceStyle.Playback -> R.drawable.titlebar_playing_bg
            },
            modifier = Modifier.matchParentSize(),
        )
        content()
    }
}

@Composable
fun SmartisanTopBar(
    title: String,
    modifier: Modifier = Modifier,
    leftContent: (@Composable () -> Unit)? = null,
    rightContent: (@Composable () -> Unit)? = null,
) {
    val density = LocalDensity.current
    val topInset = with(density) {
        WindowInsets.safeDrawing.getTop(this).toDp()
    }

    SmartisanTitleBarSurface(
        style = SmartisanTitleBarSurfaceStyle.Main,
        modifier = modifier
            .fillMaxWidth()
            .height(SmartisanTopBarHeight + topInset),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset),
        ) {
            Text(
                text = title,
                style = TopBarTitleStyle,
                modifier = Modifier.align(Alignment.Center),
            )
            if (leftContent != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = TopBarHorizontalPadding),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    leftContent()
                }
            }
            if (rightContent != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = TopBarHorizontalPadding),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    rightContent()
                }
            }
        }
    }
}

@Composable
fun SmartisanTopBarShadow(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.titlebar_bg_shadow),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = modifier
            .fillMaxWidth()
            .height(SmartisanTopBarShadowHeight),
    )
}

@Composable
fun SmartisanTopBarTextButton(
    text: String,
    width: Dp? = null,
    modifier: Modifier = Modifier,
    buttonStyle: SmartisanTopBarTextButtonStyle = SmartisanTopBarTextButtonStyle.Filled,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val resolvedWidth = width ?: when (buttonStyle) {
        SmartisanTopBarTextButtonStyle.Filled -> TopBarButtonWidth
        SmartisanTopBarTextButtonStyle.Back -> TopBarBackButtonWidth
        SmartisanTopBarTextButtonStyle.Toolbar -> TopBarToolbarTextButtonMinWidth
    }
    val resolvedHeight = if (buttonStyle == SmartisanTopBarTextButtonStyle.Toolbar) {
        TopBarToolbarButtonHeight
    } else if (buttonStyle == SmartisanTopBarTextButtonStyle.Back) {
        TopBarBackButtonHeight
    } else {
        TopBarButtonHeight
    }
    val usesToolbarContainer = buttonStyle == SmartisanTopBarTextButtonStyle.Toolbar ||
        buttonStyle == SmartisanTopBarTextButtonStyle.Back

    Box(
        modifier = modifier
            .then(
                if (buttonStyle == SmartisanTopBarTextButtonStyle.Back) {
                    Modifier.defaultMinSize(minWidth = resolvedWidth)
                } else {
                    Modifier.width(resolvedWidth)
                },
            )
            .height(resolvedHeight)
            .then(
                if (usesToolbarContainer) {
                    Modifier.smartisanButtonBackground(
                        pressed = pressed,
                        cornerRadius = TopBarToolbarButtonCorner
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (buttonStyle == SmartisanTopBarTextButtonStyle.Back) {
            val contentColor = if (pressed) TitleBarActionPressedText else TitleBarActionText
            Row(
                modifier = Modifier.padding(horizontal = TopBarBackContentHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(TopBarBackContentSpacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmartisanBackChevron(
                    color = contentColor,
                    modifier = Modifier.size(TopBarBackIconSize),
                )
                Text(
                    text = text,
                    style = TopBarButtonTextStyle,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        } else {
            Text(
                text = text,
                style = TopBarButtonTextStyle,
                color = if (pressed) TitleBarActionPressedText else TitleBarActionText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = TopBarToolbarTextHorizontalPadding),
            )
        }
    }
}

@Composable
fun SmartisanTopBarDangerButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    buttonStyle: SmartisanTopBarDangerButtonStyle = SmartisanTopBarDangerButtonStyle.Filled,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val isToolbar = buttonStyle == SmartisanTopBarDangerButtonStyle.Toolbar
    
    val textColor = when {
        !enabled -> Color(0x80FFFFFF)
        else -> Color.White
    }
    val width = if (isToolbar) TopBarToolbarTextButtonMinWidth else TopBarButtonWidth
    val height = if (isToolbar) TopBarToolbarButtonHeight else TopBarButtonHeight
    val corner = if (isToolbar) TopBarToolbarButtonCorner else TopBarButtonCorner

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .smartisanButtonBackground(
                pressed = pressed,
                enabled = enabled,
                isDanger = true,
                cornerRadius = corner
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TopBarButtonTextStyle,
            color = textColor,
        )
    }
}

@Composable
fun SmartisanTopBarIconButton(
    @DrawableRes iconRes: Int,
    @DrawableRes pressedIconRes: Int,
    contentDescription: String,
    iconSize: Dp,
    modifier: Modifier = Modifier,
    width: Dp = TopBarButtonWidth,
    buttonStyle: SmartisanTopBarIconButtonStyle = SmartisanTopBarIconButtonStyle.Plain,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val iconAlpha = if (enabled) 1f else 0.45f
    val resolvedWidth = if (buttonStyle == SmartisanTopBarIconButtonStyle.Toolbar) {
        TopBarToolbarButtonWidth
    } else {
        width
    }
    val resolvedHeight = if (buttonStyle == SmartisanTopBarIconButtonStyle.Toolbar) {
        TopBarToolbarButtonHeight
    } else {
        TopBarButtonHeight
    }

    Box(
        modifier = modifier
            .width(resolvedWidth)
            .height(resolvedHeight)
            .then(
                if (buttonStyle == SmartisanTopBarIconButtonStyle.Toolbar) {
                    Modifier.smartisanButtonBackground(
                        pressed = pressed,
                        enabled = enabled,
                        cornerRadius = TopBarToolbarButtonCorner
                    )
                } else {
                    Modifier
                },
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (buttonStyle == SmartisanTopBarIconButtonStyle.Filled) {
            SmartisanDrawableBackground(
                drawableRes = if (pressed) R.drawable.btn_cancel_down else R.drawable.btn_cancel,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Image(
            painter = painterResource(
                if (pressed) pressedIconRes else iconRes,
            ),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            alpha = iconAlpha,
        )
    }
}

@Composable
fun SmartisanDrawableBackground(
    @DrawableRes drawableRes: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Canvas(modifier = modifier) {
        val drawable = context.getDrawable(drawableRes) ?: return@Canvas
        drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
        drawIntoCanvas { canvas ->
            drawable.draw(canvas.nativeCanvas)
        }
    }
}

@Composable
private fun SmartisanBackChevron(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { TopBarBackIconStroke.toPx() }

    Canvas(modifier = modifier) {
        val centerY = size.height / 2f
        val leftX = size.width * 0.28f
        val midX = size.width * 0.58f
        val topY = size.height * 0.24f
        val bottomY = size.height * 0.76f

        drawLine(
            color = color,
            start = Offset(midX, topY),
            end = Offset(leftX, centerY),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(leftX, centerY),
            end = Offset(midX, bottomY),
            strokeWidth = strokeWidthPx,
            cap = StrokeCap.Round,
        )
    }
}
