package com.smartisanos.music.ui.more

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.R
import com.smartisanos.music.data.settings.PlaybackSettings
import com.smartisanos.music.ui.components.SecondaryPageTransition
import com.smartisanos.music.ui.folder.FolderScreen
import com.smartisanos.music.ui.genre.GenreScreen

private val MoreRowBackground = Color(0xFFFDFDFD)
private val MoreRowPressedBackground = Color(0xFFCACACA)
private val MoreRowDivider = Color(0xFFE8E8E8)
private val MoreRowTitleColor = Color(0xCC000000)
private val MoreRowPressedTitleColor = Color.White

private val MoreRowTitleStyle = TextStyle(
    fontSize = 17.sp,
    fontWeight = FontWeight.Medium,
)

private val MoreRowHeight = 61.dp
private val MoreRowLeadingPadding = 11.6.dp
private val MoreRowTrailingPadding = 0.dp
private val MoreRowIconTextSpacing = 10.dp

enum class MoreSecondaryPage {
    Folder,
    Settings,
    Style,
}

private enum class MorePrimaryEntry(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    @param:DrawableRes val pressedIconRes: Int,
) {
    Style(
        labelRes = R.string.tab_style,
        iconRes = R.drawable.tabbar_style,
        pressedIconRes = R.drawable.tabbar_style_white,
    ),
    LovedSongs(
        labelRes = R.string.collect_music,
        iconRes = R.drawable.tabbar_like,
        pressedIconRes = R.drawable.tabbar_like_white,
    ),
    Folder(
        labelRes = R.string.tab_directory,
        iconRes = R.drawable.tabbar_folder,
        pressedIconRes = R.drawable.tabbar_folder_white,
    ),
}

@Composable
fun MoreScreen(
    secondaryPage: MoreSecondaryPage?,
    folderEditMode: Boolean,
    selectedDirectoryKey: String?,
    selectedGenreId: String?,
    playbackSettings: PlaybackSettings,
    modifier: Modifier = Modifier,
    onEntryClick: (String) -> Unit = {},
    onSecondaryBack: () -> Unit = {},
    onDirectorySelected: (String, String) -> Unit = { _, _ -> },
    onDirectoryBack: () -> Unit = {},
    onDirectoryEditSelectionChanged: (Set<String>) -> Unit = {},
    onGenreSelected: (String, String) -> Unit = { _, _ -> },
    onGenreBack: () -> Unit = {},
    onScratchEnabledChange: (Boolean) -> Unit = {},
    onHidePlayerAxisEnabledChange: (Boolean) -> Unit = {},
    onPopcornSoundEnabledChange: (Boolean) -> Unit = {},
) {
    BackHandler(enabled = secondaryPage != null, onBack = onSecondaryBack)
    SecondaryPageTransition(
        secondaryKey = secondaryPage,
        modifier = modifier.fillMaxSize(),
        label = "more secondary page",
        primaryContent = {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                MorePrimaryEntry.entries.forEach { entry ->
                    MoreMenuRow(
                        entry = entry,
                        onClick = { onEntryClick(entry.name) },
                    )
                }
            }
        },
        secondaryContent = { page ->
            when (page) {
                MoreSecondaryPage.Folder -> FolderScreen(
                    editMode = folderEditMode,
                    selectedDirectoryKey = selectedDirectoryKey,
                    onDirectorySelected = onDirectorySelected,
                    onDirectoryBack = onDirectoryBack,
                    onEditSelectionChanged = onDirectoryEditSelectionChanged,
                    modifier = Modifier.fillMaxSize(),
                )
                MoreSecondaryPage.Settings -> SettingsScreen(
                    playbackSettings = playbackSettings,
                    modifier = Modifier.fillMaxSize(),
                    onScratchEnabledChange = onScratchEnabledChange,
                    onHidePlayerAxisEnabledChange = onHidePlayerAxisEnabledChange,
                    onPopcornSoundEnabledChange = onPopcornSoundEnabledChange,
                )
                MoreSecondaryPage.Style -> GenreScreen(
                    selectedGenreId = selectedGenreId,
                    onGenreSelected = onGenreSelected,
                    onGenreBack = onGenreBack,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    )
}

@Composable
private fun MoreMenuRow(
    entry: MorePrimaryEntry,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val iconPainter = painterResource(
        if (pressed) entry.pressedIconRes else entry.iconRes,
    )
    val arrowPainter = painterResource(
        if (pressed) R.drawable.arrow3_down else R.drawable.arrow3,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(MoreRowHeight)
            .background(if (pressed) MoreRowPressedBackground else MoreRowBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = MoreRowLeadingPadding, end = MoreRowTrailingPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = iconPainter,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(MoreRowIconTextSpacing))
            Text(
                text = stringResource(entry.labelRes),
                style = MoreRowTitleStyle,
                color = if (pressed) MoreRowPressedTitleColor else MoreRowTitleColor,
                modifier = Modifier.weight(1f),
            )
            Image(
                painter = arrowPainter,
                contentDescription = null,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(MoreRowDivider),
        )
    }
}
