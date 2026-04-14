package com.smartisanos.music.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BlankTitleColor = Color(0x32000000)
private val BlankSubtitleColor = Color(0x2A000000)
private val BlankTitleHighlightColor = Color(0x80FFFFFF)
private val BlankSubtitleHighlightColor = Color(0x73FFFFFF)

private val BlankTitleStyle = TextStyle(
    fontSize = 23.sp,
    fontWeight = FontWeight.SemiBold,
    color = BlankTitleColor,
)
private val BlankSubtitleStyle = TextStyle(
    fontSize = 15.sp,
    color = BlankSubtitleColor,
)

private val BlankIconSize = 132.dp
private val BlankIconAlpha = 0.34f
private val BlankContentOffsetY = 0.dp
private val BlankTextHighlightOffset = 1.dp

@Composable
fun SmartisanBlankState(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = BlankIconSize,
    iconAlpha: Float = BlankIconAlpha,
    contentOffsetY: Dp = BlankContentOffsetY,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.offset(y = contentOffsetY),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(iconSize)
                    .alpha(iconAlpha),
            )
            EngravedHintText(
                text = title,
                style = BlankTitleStyle,
                color = BlankTitleColor,
                highlightColor = BlankTitleHighlightColor,
                modifier = Modifier.padding(top = 8.dp),
            )
            EngravedHintText(
                text = subtitle,
                style = BlankSubtitleStyle,
                color = BlankSubtitleColor,
                highlightColor = BlankSubtitleHighlightColor,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun EngravedHintText(
    text: String,
    style: TextStyle,
    color: Color,
    highlightColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = style,
            color = highlightColor,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = BlankTextHighlightOffset),
        )
        Text(
            text = text,
            style = style,
            color = color,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}
