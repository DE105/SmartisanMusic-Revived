package com.smartisanos.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.ui.navigation.MusicDestination

private val SmartisanRed = Color(0xFFE64040)
private val SmartisanText = Color(0x70000000)
private val SmartisanIconTint = Color(0xFF979797)
private val ShellBackground = Color(0xFFF7F7F7)
private val BottomBarBackground = Color(0xFFF0F0F0)
private val BottomBarVisibleHeight = 54.dp
private val BottomBarItemSpacing = 1.dp
private val BottomBarIconSize = 26.dp
private val BottomBarHorizontalPadding = 12.dp
private val MoreDotSize = 5.dp
private val MoreDotSpacing = 3.dp
private val BottomBarLabelStyle = TextStyle(
    fontSize = 9.sp,
    lineHeight = 10.sp,
)

@Composable
fun SmartisanBottomBar(
    currentRoute: String,
    onDestinationSelected: (MusicDestination) -> Unit,
) {
    val currentDestination = MusicDestination.entries.firstOrNull { it.route == currentRoute }
        ?: MusicDestination.Playlist

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarVisibleHeight)
                .background(BottomBarBackground),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BottomBarVisibleHeight)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                    )
                    .padding(horizontal = BottomBarHorizontalPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MusicDestination.entries.forEach { destination ->
                    SmartisanBottomBarItem(
                        destination = destination,
                        selected = destination == currentDestination,
                        onClick = { onDestinationSelected(destination) },
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(BottomBarBackground)
        )
    }
}

@Composable
private fun RowScope.SmartisanBottomBarItem(
    destination: MusicDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BottomBarItemSpacing),
        ) {
            if (destination == MusicDestination.More) {
                MoreDotsIcon(
                    selected = selected || pressed,
                    modifier = Modifier.size(BottomBarIconSize),
                )
            } else {
                Image(
                    painter = painterResource(
                        if (selected || pressed) destination.selectedIconRes else destination.iconRes
                    ),
                    contentDescription = destination.label,
                    modifier = Modifier.size(BottomBarIconSize),
                    colorFilter = if (selected || pressed) {
                        null
                    } else {
                        ColorFilter.tint(SmartisanIconTint, BlendMode.SrcIn)
                    },
                )
            }
            Text(
                text = destination.label,
                style = BottomBarLabelStyle,
                color = if (selected || pressed) SmartisanRed else SmartisanText,
            )
        }
    }
}

@Composable
private fun MoreDotsIcon(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val dotColor = if (selected) SmartisanRed else SmartisanIconTint

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(
                MoreDotSpacing
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(MoreDotSize)
                        .background(
                            color = dotColor,
                            shape = CircleShape,
                        )
                )
            }
        }
    }
}
