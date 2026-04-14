package com.smartisanos.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.ui.navigation.MusicDestination

private val SmartisanRed = Color(0xFFE64040)
private val SmartisanText = Color(0x66000000)
private val ShellBackground = Color(0xFFF7F7F7)
private val BottomBarContentHeight = 54.dp
private val BottomBarTopPadding = 4.dp
private val BottomBarIconSize = 26.dp
private val BottomBarHorizontalPadding = 12.dp
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
    val density = LocalDensity.current
    val bottomInset = with(density) {
        WindowInsets.navigationBars.getBottom(this).toDp()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BottomBarContentHeight + bottomInset)
            .background(ShellBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarContentHeight)
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = BottomBarHorizontalPadding)
                .align(Alignment.TopCenter)
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
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = BottomBarTopPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(
                    if (selected || pressed) destination.selectedIconRes else destination.iconRes
                ),
                contentDescription = destination.label,
                modifier = Modifier.size(BottomBarIconSize),
            )
            Text(
                text = destination.label,
                style = BottomBarLabelStyle,
                color = if (selected || pressed) SmartisanRed else SmartisanText,
            )
        }
    }
}
