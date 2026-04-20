package com.smartisanos.music.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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

private val TopBarFillTop = Color(0xFFFBFBFB)
private val TopBarFillUpper = Color(0xFFF9F9F9)
private val TopBarFillLower = Color(0xFFF5F5F5)
private val TopBarFillBottom = Color(0xFFF0F0F0)
private val TopBarBottomEdge = Color(0xFFE4E4E4)
private val TopBarTextColor = Color(0x99000000)

private val TitleBarActionText = Color(0x73000000)
private val TitleBarActionPressedText = Color(0x8A000000)
private val TitleBarToolbarBackground = Color(0xFFFDFDFD)
private val TitleBarToolbarPressedBackground = Color(0xFFF4F4F4)
private val TitleBarToolbarBorder = Color(0xFFE7E7E7)
private val TitleBarToolbarPressedBorder = Color(0xFFDCDCDC)
private val TitleBarDangerBackground = Color(0xFFEE9B98)
private val TitleBarDangerPressedBackground = Color(0xFFE48784)
private val TitleBarDangerBorder = Color(0xFFD77C79)
private val TitleBarDangerText = Color.White
private val TitleBarDangerDisabledBackground = Color(0xFFF2F2F2)
private val TitleBarDangerDisabledBorder = Color(0xFFDCDCDC)
private val TitleBarDangerDisabledText = Color(0x66000000)
private val TitleBarDangerToolbarText = Color(0xFFEB7D74)
private val TitleBarDangerToolbarPressedText = Color(0xFFE06A61)

private val TopBarHorizontalPadding = 10.dp
private val TopBarButtonHeight = 29.dp
private val TopBarToolbarButtonHeight = 29.dp
private val TopBarButtonWidth = 42.dp
private val TopBarBackButtonWidth = 52.dp
private val TopBarBackButtonHeight = 29.dp
private val TopBarToolbarButtonWidth = 46.dp
private val TopBarToolbarTextButtonMinWidth = 44.dp
private val TopBarButtonCorner = 7.dp
private val TopBarToolbarButtonCorner = 4.dp
private val TopBarToolbarTextHorizontalPadding = 4.dp
private val TopBarBackContentHorizontalPadding = 8.dp
private val TopBarBackContentSpacing = 4.dp
private val TopBarBackIconSize = 14.dp
private val TopBarBackIconStroke = 1.8.dp

private val TopBarTitleStyle = TextStyle(
    fontSize = 20.sp,
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SmartisanTopBarHeight + topInset)
            .drawWithCache {
                val fillBrush = Brush.verticalGradient(
                    colors = listOf(
                        TopBarFillTop,
                        TopBarFillUpper,
                        TopBarFillLower,
                        TopBarFillBottom,
                    ),
                )
                val strokeWidth = 1f
                val lineY = size.height - (strokeWidth / 2f)

                onDrawBehind {
                    drawRect(brush = fillBrush)
                    drawLine(
                        color = TopBarBottomEdge,
                        start = Offset(0f, lineY),
                        end = Offset(size.width, lineY),
                        strokeWidth = strokeWidth,
                    )
                }
            },
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
    val borderColor = if (pressed) TitleBarToolbarPressedBorder else TitleBarToolbarBorder
    val backgroundColor = if (pressed) TitleBarToolbarPressedBackground else TitleBarToolbarBackground

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
                    Modifier
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(TopBarToolbarButtonCorner),
                        )
                        .background(
                            color = backgroundColor,
                            shape = RoundedCornerShape(TopBarToolbarButtonCorner),
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
    val borderColor = when {
        !enabled -> TitleBarDangerDisabledBorder
        isToolbar && pressed -> TitleBarToolbarPressedBorder
        isToolbar -> TitleBarToolbarBorder
        enabled -> TitleBarDangerBorder
        else -> TitleBarDangerDisabledBorder
    }
    val backgroundColor = when {
        !enabled -> TitleBarDangerDisabledBackground
        isToolbar && pressed -> TitleBarToolbarPressedBackground
        isToolbar -> TitleBarToolbarBackground
        pressed -> TitleBarDangerPressedBackground
        else -> TitleBarDangerBackground
    }
    val textColor = when {
        !enabled -> TitleBarDangerDisabledText
        isToolbar && pressed -> TitleBarDangerToolbarPressedText
        isToolbar -> TitleBarDangerToolbarText
        else -> TitleBarDangerText
    }
    val width = if (isToolbar) TopBarToolbarTextButtonMinWidth else TopBarButtonWidth
    val height = if (isToolbar) TopBarToolbarButtonHeight else TopBarButtonHeight
    val corner = if (isToolbar) TopBarToolbarButtonCorner else TopBarButtonCorner

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(corner),
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(corner),
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
    val borderColor = if (pressed) TitleBarToolbarPressedBorder else TitleBarToolbarBorder
    val backgroundColor = if (pressed) TitleBarToolbarPressedBackground else TitleBarToolbarBackground

    Box(
        modifier = modifier
            .width(resolvedWidth)
            .height(resolvedHeight)
            .then(
                if (buttonStyle == SmartisanTopBarIconButtonStyle.Toolbar) {
                    Modifier
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(TopBarToolbarButtonCorner),
                        )
                        .background(
                            color = backgroundColor,
                            shape = RoundedCornerShape(TopBarToolbarButtonCorner),
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
private fun SmartisanDrawableBackground(
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
