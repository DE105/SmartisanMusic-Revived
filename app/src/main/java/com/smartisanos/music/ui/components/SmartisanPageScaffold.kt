package com.smartisanos.music.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.layout.ContentScale
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

private val TitleBarButtonBackground = Color(0xFFF7F7F7)
private val TitleBarButtonPressedBackground = Color(0xFFEFEFEF)
private val TitleBarButtonBorder = Color(0xFFDCDCDC)
private val TitleBarActionText = Color(0x8F000000)
private val TitleBarActionPressedText = Color(0xFF515257)
private val TitleBarDangerBackground = Color(0xFFEE9B98)
private val TitleBarDangerPressedBackground = Color(0xFFE48784)
private val TitleBarDangerBorder = Color(0xFFD77C79)
private val TitleBarDangerText = Color.White
private val TitleBarDangerDisabledBackground = Color(0xFFF2F2F2)
private val TitleBarDangerDisabledBorder = Color(0xFFDCDCDC)
private val TitleBarDangerDisabledText = Color(0x66000000)

private val TopBarHorizontalPadding = 10.dp
private val TopBarButtonHeight = 30.dp
private val TopBarButtonWidth = 42.dp
private val TopBarButtonCorner = 7.dp

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
    width: Dp = TopBarButtonWidth,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .width(width)
            .height(TopBarButtonHeight)
            .border(
                width = 1.dp,
                color = TitleBarButtonBorder,
                shape = RoundedCornerShape(TopBarButtonCorner),
            )
            .background(
                color = if (pressed) TitleBarButtonPressedBackground else TitleBarButtonBackground,
                shape = RoundedCornerShape(TopBarButtonCorner),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = TopBarButtonTextStyle,
            color = if (pressed) TitleBarActionPressedText else TitleBarActionText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SmartisanTopBarDangerButton(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val borderColor = when {
        enabled -> TitleBarDangerBorder
        else -> TitleBarDangerDisabledBorder
    }
    val backgroundColor = when {
        !enabled -> TitleBarDangerDisabledBackground
        pressed -> TitleBarDangerPressedBackground
        else -> TitleBarDangerBackground
    }
    val textColor = when {
        enabled -> TitleBarDangerText
        else -> TitleBarDangerDisabledText
    }

    Box(
        modifier = modifier
            .width(TopBarButtonWidth)
            .height(TopBarButtonHeight)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(TopBarButtonCorner),
            )
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(TopBarButtonCorner),
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
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .width(width)
            .height(TopBarButtonHeight)
            .border(
                width = 1.dp,
                color = TitleBarButtonBorder,
                shape = RoundedCornerShape(TopBarButtonCorner),
            )
            .background(
                color = if (pressed) TitleBarButtonPressedBackground else TitleBarButtonBackground,
                shape = RoundedCornerShape(TopBarButtonCorner),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(
                if (pressed) pressedIconRes else iconRes,
            ),
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
    }
}
