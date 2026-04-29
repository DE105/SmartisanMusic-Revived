package com.smartisanos.music.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.R
import com.smartisanos.music.data.settings.normalizeArtistRecognitionValue
import com.smartisanos.music.ui.components.SmartisanDialogCard
import com.smartisanos.music.ui.components.SmartisanDialogTitleStyle

private val ArtistSettingsTipsColor = Color(0x66000000)
private val ArtistSettingsValueColor = Color(0x99000000)
private val ArtistSettingsDialogTextFieldBorder = Color(0xFFE2E2E2)
private val ArtistSettingsDialogTextFieldBackground = Color(0xFFF7F8F9)
private val ArtistSettingsDialogListBackground = Color(0xFFFAFAFA)
private val ArtistSettingsDialogListBorder = Color(0xFFE3E3E3)
private val ArtistSettingsDialogPrimaryButtonColor = Color(0xFF5E88E8)
private val ArtistSettingsDialogPrimaryPressedButton = Color(0xFF4F77D5)
private val ArtistSettingsDialogPrimaryBorder = Color(0xFF4C73CF)
private val ArtistSettingsDialogSecondaryBorder = Color(0xFFDCDCDC)
private val ArtistSettingsDialogSecondaryPressedBackground = Color(0xFFEFEFEF)
private val ArtistSettingsTipsStyle = TextStyle(
    fontSize = 13.5.sp,
    color = ArtistSettingsTipsColor,
    lineHeight = 18.sp,
)
private val ArtistSettingsValueStyle = TextStyle(
    fontSize = 15.sp,
    color = ArtistSettingsValueColor,
    textAlign = TextAlign.End,
)
private val ArtistSettingsDialogTextFieldStyle = TextStyle(
    fontSize = 16.sp,
    color = Color(0xDE000000),
    lineHeight = 22.sp,
)

@Composable
internal fun ArtistSeparatorPickerDialog(
    title: String,
    values: Set<String>,
    onDismiss: () -> Unit,
    onValuesChange: (Set<String>) -> Unit,
) {
    var visibleValues by rememberSaveable(values) { mutableStateOf(values) }
    val updateValues = { nextValues: Set<String> ->
        visibleValues = nextValues
        onValuesChange(nextValues)
    }
    SmartisanDialogCard(
        onDismiss = onDismiss,
        width = 300.dp,
    ) {
        DialogTitle(title = title)
        Text(
            text = stringResource(R.string.artist_separators_hint),
            style = ArtistSettingsTipsStyle.copy(textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, top = 12.dp, end = 22.dp),
        )
        DialogRemovableChipGroup(
            values = visibleValues,
            emptyText = stringResource(R.string.not_set),
            onRemove = { separator ->
                updateValues(visibleValues - separator)
            },
        )
        ArtistCustomValueInput(
            placeholder = stringResource(R.string.artist_custom_separator_hint),
            buttonText = stringResource(R.string.add),
            onAdd = { value ->
                updateValues(visibleValues + value)
            },
        )
        DialogDoneButton(onClick = onDismiss)
    }
}

@Composable
internal fun ArtistNameListDialog(
    title: String,
    placeholder: String,
    values: Set<String>,
    onValuesChange: (Set<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var visibleValues by rememberSaveable(values) { mutableStateOf(values) }
    val updateValues = { nextValues: Set<String> ->
        visibleValues = nextValues
        onValuesChange(nextValues)
    }
    SmartisanDialogCard(
        onDismiss = onDismiss,
        width = 300.dp,
    ) {
        DialogTitle(title = title)
        Text(
            text = stringResource(R.string.excluded_artist_names_tips),
            style = ArtistSettingsTipsStyle.copy(textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, top = 12.dp, end = 22.dp),
        )
        if (visibleValues.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 12.dp, end = 18.dp)
                    .heightIn(max = 190.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(ArtistSettingsDialogListBackground)
                    .border(1.dp, ArtistSettingsDialogListBorder, RoundedCornerShape(7.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
            ) {
                visibleValues.sorted().forEach { name ->
                    DialogFullWidthRemoveRow(
                        text = name,
                        onRemove = {
                            updateValues(visibleValues - name)
                        },
                    )
                }
            }
        }
        ArtistCustomValueInput(
            placeholder = placeholder,
            buttonText = stringResource(R.string.add),
            onAdd = { value ->
                updateValues(visibleValues + value)
            },
        )
        DialogDoneButton(onClick = onDismiss)
    }
}

@Composable
private fun DialogTitle(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 22.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = SmartisanDialogTitleStyle,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DialogEmptyText(text: String) {
    Text(
        text = text,
        style = ArtistSettingsValueStyle.copy(textAlign = TextAlign.Start),
        modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
    )
}

@Composable
private fun DialogRemovableChipGroup(
    values: Set<String>,
    emptyText: String,
    onRemove: (String) -> Unit,
) {
    if (values.isEmpty()) {
        DialogEmptyText(text = emptyText)
        return
    }
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 16.dp, end = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        values.sorted().forEach { value ->
            DialogRemoveChip(
                text = value,
                onClick = { onRemove(value) },
            )
        }
    }
}

@Composable
private fun DialogRemoveChip(
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .height(34.dp)
            .background(
                color = if (pressed) ArtistSettingsDialogSecondaryPressedBackground else ArtistSettingsDialogTextFieldBackground,
                shape = RoundedCornerShape(17.dp),
            )
            .border(1.dp, ArtistSettingsDialogSecondaryBorder, RoundedCornerShape(17.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = text,
                style = ArtistSettingsDialogTextFieldStyle.copy(
                    color = ArtistSettingsValueColor,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "\u00D7",
                style = ArtistSettingsDialogTextFieldStyle.copy(
                    color = ArtistSettingsTipsColor,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}

@Composable
private fun DialogFullWidthRemoveRow(
    text: String,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 5.dp)
            .heightIn(min = 42.dp)
            .background(ArtistSettingsDialogTextFieldBackground, RoundedCornerShape(7.dp))
            .border(1.dp, ArtistSettingsDialogTextFieldBorder, RoundedCornerShape(7.dp))
            .padding(start = 12.dp, end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = ArtistSettingsDialogTextFieldStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        DialogCloseIconButton(onClick = onRemove)
    }
}

@Composable
private fun DialogCloseIconButton(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (pressed) ArtistSettingsDialogSecondaryPressedBackground else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "\u00D7",
            style = ArtistSettingsDialogTextFieldStyle.copy(
                color = ArtistSettingsTipsColor,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun ArtistCustomValueInput(
    placeholder: String,
    buttonText: String,
    onAdd: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 12.dp, end = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .background(ArtistSettingsDialogTextFieldBackground, RoundedCornerShape(6.dp))
                .border(1.dp, ArtistSettingsDialogTextFieldBorder, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                textStyle = ArtistSettingsDialogTextFieldStyle,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = ArtistSettingsDialogTextFieldStyle.copy(color = ArtistSettingsTipsColor),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                },
            )
        }
        DialogPrimaryButton(
            text = buttonText,
            modifier = Modifier.width(58.dp),
            onClick = {
                val normalizedValue = normalizeArtistRecognitionValue(value) ?: return@DialogPrimaryButton
                onAdd(normalizedValue)
                value = ""
            },
        )
    }
}

@Composable
private fun DialogDoneButton(onClick: () -> Unit) {
    DialogPrimaryButton(
        text = stringResource(R.string.done),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 14.dp, end = 18.dp),
        onClick = onClick,
    )
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun DialogPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = modifier
            .height(38.dp)
            .background(
                if (pressed) ArtistSettingsDialogPrimaryPressedButton else ArtistSettingsDialogPrimaryButtonColor,
                RoundedCornerShape(6.dp),
            )
            .border(1.dp, ArtistSettingsDialogPrimaryBorder, RoundedCornerShape(6.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = ArtistSettingsDialogTextFieldStyle.copy(
                color = Color.White,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}
