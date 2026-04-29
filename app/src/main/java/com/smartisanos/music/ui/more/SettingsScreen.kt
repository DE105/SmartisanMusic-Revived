package com.smartisanos.music.ui.more

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.R
import com.smartisanos.music.data.settings.PlaybackSettings
import com.smartisanos.music.data.settings.ArtistRecognitionSettings

private val SettingsGroupShape = RoundedCornerShape(12.dp)
private val SettingsPageBackground = Color(0xFFF7F7F7)
private val SettingsGroupBackground = Color(0xFFFCFCFC)
private val SettingsGroupBorder = Color(0x0A000000)
private val SettingsDivider = Color(0xFFEAEAEA)
private val SettingsTitleColor = Color(0xCC000000)
private val SettingsTipsColor = Color(0x66000000)
private val SettingsValueColor = Color(0x99000000)
private val SettingsSwitchTrackColor = Color(0xFFF8F8F8)
private val SettingsSwitchBorderColor = Color(0xFFE6E6E6)
private val SettingsSwitchAccent = Color(0xFF91B4FF)
private val SettingsRowPressedBackground = Color(0xFFCACACA)
private val SettingsTitleStyle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.Medium,
    color = SettingsTitleColor,
)
private val SettingsTipsStyle = TextStyle(
    fontSize = 13.5.sp,
    color = SettingsTipsColor,
    lineHeight = 18.sp,
)
private val SettingsValueStyle = TextStyle(
    fontSize = 15.sp,
    color = SettingsValueColor,
    textAlign = TextAlign.End,
)
@Composable
fun SettingsScreen(
    playbackSettings: PlaybackSettings,
    artistRecognitionSettings: ArtistRecognitionSettings,
    modifier: Modifier = Modifier,
    onScratchEnabledChange: (Boolean) -> Unit = {},
    onHidePlayerAxisEnabledChange: (Boolean) -> Unit = {},
    onPopcornSoundEnabledChange: (Boolean) -> Unit = {},
    onArtistSeparatorsChange: (Set<String>) -> Unit = {},
    onExcludedArtistNamesChange: (Set<String>) -> Unit = {},
) {
    var editingArtistSeparators by remember { mutableStateOf(false) }
    var editingExcludedArtistNames by remember { mutableStateOf(false) }
    val notSetText = stringResource(R.string.not_set)

    SettingsOverview(
        playbackSettings = playbackSettings,
        artistRecognitionSettings = artistRecognitionSettings,
        notSetText = notSetText,
        onScratchEnabledChange = onScratchEnabledChange,
        onHidePlayerAxisEnabledChange = onHidePlayerAxisEnabledChange,
        onPopcornSoundEnabledChange = onPopcornSoundEnabledChange,
        onArtistSeparatorsClick = { editingArtistSeparators = true },
        onExcludedArtistNamesClick = { editingExcludedArtistNames = true },
        modifier = modifier.fillMaxSize(),
    )

    if (editingArtistSeparators) {
        ArtistSeparatorPickerDialog(
            title = stringResource(R.string.artist_separators),
            values = artistRecognitionSettings.separators,
            onDismiss = { editingArtistSeparators = false },
            onValuesChange = onArtistSeparatorsChange,
        )
    }
    if (editingExcludedArtistNames) {
        ArtistNameListDialog(
            title = stringResource(R.string.excluded_artist_names),
            placeholder = stringResource(R.string.excluded_artist_names_hint),
            values = artistRecognitionSettings.excludedArtistNames,
            onDismiss = { editingExcludedArtistNames = false },
            onValuesChange = onExcludedArtistNamesChange,
        )
    }
}

@Composable
private fun SettingsOverview(
    playbackSettings: PlaybackSettings,
    artistRecognitionSettings: ArtistRecognitionSettings,
    notSetText: String,
    onScratchEnabledChange: (Boolean) -> Unit,
    onHidePlayerAxisEnabledChange: (Boolean) -> Unit,
    onPopcornSoundEnabledChange: (Boolean) -> Unit,
    onArtistSeparatorsClick: () -> Unit,
    onExcludedArtistNamesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(SettingsPageBackground)
            .verticalScroll(rememberScrollState())
            .padding(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 24.dp),
    ) {
        SettingsGroup {
            SettingsToggleRow(
                title = stringResource(R.string.djing),
                checked = playbackSettings.scratchEnabled,
                onCheckedChange = onScratchEnabledChange,
            )
            HorizontalDivider(
                color = SettingsDivider,
                thickness = 1.dp,
            )
            SettingsToggleRow(
                title = stringResource(R.string.player_axis_enabled),
                checked = playbackSettings.hidePlayerAxisEnabled,
                onCheckedChange = onHidePlayerAxisEnabledChange,
            )
        }
        SettingsGroupGap()
        SettingsGroup {
            SettingsToggleRow(
                title = stringResource(R.string.popcorn_sound),
                checked = playbackSettings.popcornSoundEnabled,
                onCheckedChange = onPopcornSoundEnabledChange,
            )
        }
        Text(
            text = stringResource(R.string.popcorn_sound_tips),
            style = SettingsTipsStyle,
            modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 16.dp),
        )
        SettingsGroupGap()
        SettingsGroup {
            SettingsNavigationRow(
                title = stringResource(R.string.artist_separators),
                value = artistRecognitionSettings.separators.toSettingsSummary(notSetText),
                onClick = onArtistSeparatorsClick,
            )
            HorizontalDivider(
                color = SettingsDivider,
                thickness = 1.dp,
            )
            SettingsNavigationRow(
                title = stringResource(R.string.excluded_artist_names),
                value = artistRecognitionSettings.excludedArtistNames.toSettingsSummary(notSetText),
                onClick = onExcludedArtistNamesClick,
            )
        }
    }
}

@Composable
private fun SettingsGroupGap() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .background(SettingsPageBackground),
    )
}

@Composable
private fun SettingsGroup(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 7.dp,
                shape = SettingsGroupShape,
                ambientColor = Color(0x12000000),
                spotColor = Color(0x12000000),
            )
            .clip(SettingsGroupShape)
            .background(SettingsGroupBackground)
            .border(
                width = 1.dp,
                color = SettingsGroupBorder,
                shape = SettingsGroupShape,
            ),
        content = content,
    )
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val arrowPainter = painterResource(
        if (pressed) R.drawable.arrow3_down else R.drawable.arrow3,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(61.dp)
            .background(if (pressed) SettingsRowPressedBackground else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(start = 16.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = SettingsTitleStyle,
            maxLines = 1,
            modifier = Modifier.weight(0.44f),
        )
        Text(
            text = value,
            style = SettingsValueStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.56f),
        )
        Image(
            painter = arrowPainter,
            contentDescription = null,
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(61.dp)
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(start = 16.dp, end = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = SettingsTitleStyle,
            modifier = Modifier.weight(1f),
        )
        SmartisanSettingsSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SmartisanSettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 2.dp,
        label = "settingsSwitchThumb",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .width(54.dp)
            .height(31.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SettingsSwitchTrackColor)
            .border(
                width = 1.dp,
                color = SettingsSwitchBorderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (checked) {
            Box(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(SettingsSwitchAccent),
            )
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .size(27.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    ambientColor = Color(0x1F000000),
                    spotColor = Color(0x1F000000),
                )
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFFFF),
                            Color(0xFFF7F7F7),
                            Color(0xFFEDEDED),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    color = Color(0x12000000),
                    shape = CircleShape,
                ),
        )
    }
}

private fun Set<String>.toSettingsSummary(emptyText: String): String {
    return takeIf { it.isNotEmpty() }
        ?.sorted()
        ?.joinToString(", ")
        ?: emptyText
}

private fun Set<String>.toggled(value: String): Set<String> {
    return if (value in this) {
        this - value
    } else {
        this + value
    }
}
