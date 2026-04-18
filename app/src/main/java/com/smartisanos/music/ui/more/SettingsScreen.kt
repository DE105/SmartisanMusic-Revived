package com.smartisanos.music.ui.more

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.R
import com.smartisanos.music.data.settings.PlaybackSettings

private val SettingsGroupShape = RoundedCornerShape(12.dp)
private val SettingsPageBackground = Color(0xFFF7F7F7)
private val SettingsGroupBackground = Color(0xFFFCFCFC)
private val SettingsGroupBorder = Color(0x0A000000)
private val SettingsDivider = Color(0xFFEAEAEA)
private val SettingsTitleColor = Color(0xCC000000)
private val SettingsTipsColor = Color(0x66000000)
private val SettingsSwitchTrackColor = Color(0xFFF8F8F8)
private val SettingsSwitchBorderColor = Color(0xFFE6E6E6)
private val SettingsSwitchAccent = Color(0xFF91B4FF)

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

@Composable
fun SettingsScreen(
    playbackSettings: PlaybackSettings,
    modifier: Modifier = Modifier,
    onScratchEnabledChange: (Boolean) -> Unit = {},
    onHidePlayerAxisEnabledChange: (Boolean) -> Unit = {},
    onPopcornSoundEnabledChange: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
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
                .offset(x = thumbOffset)
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
