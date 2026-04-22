package com.smartisanos.music.ui.playback

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartisanos.music.R
import com.smartisanos.music.playback.PlaybackSleepTimerState
import com.smartisanos.music.ui.components.SmartisanConfirmDialog
import com.smartisanos.music.ui.components.SmartisanDialogActionRow
import com.smartisanos.music.ui.components.SmartisanDialogBodyStyle
import com.smartisanos.music.ui.components.SmartisanDialogCard
import com.smartisanos.music.ui.components.SmartisanDialogDividerColor
import com.smartisanos.music.ui.components.SmartisanDialogSecondaryActionStyle
import com.smartisanos.music.ui.components.SmartisanDialogTitleStyle

private const val MinuteMs = 60_000L

internal val PlaybackSleepTimerOptions = listOf(
    SleepTimerOption(R.string.time_15m, 15L * MinuteMs),
    SleepTimerOption(R.string.time_30m, 30L * MinuteMs),
    SleepTimerOption(R.string.time_1h, 60L * MinuteMs),
    SleepTimerOption(R.string.time_1_5h, 90L * MinuteMs),
    SleepTimerOption(R.string.time_2h, 120L * MinuteMs),
    SleepTimerOption(R.string.time_no, 0L),
)

internal data class SleepTimerOption(
    @field:StringRes val labelRes: Int,
    val durationMs: Long,
)

@Composable
internal fun PlaybackSleepTimerDialog(
    state: PlaybackSleepTimerState,
    onDismiss: () -> Unit,
    onDurationSelected: (Long) -> Unit,
) {
    SmartisanDialogCard(onDismiss = onDismiss) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.setting_stop_time),
                style = SmartisanDialogTitleStyle,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp, start = 20.dp, end = 20.dp),
            )
            if (state.isActive) {
                Text(
                    text = "${androidx.compose.ui.res.stringResource(R.string.remain_time)} ${formatSleepTimerRemaining(state.remainingMs)}",
                    style = SmartisanDialogBodyStyle,
                    modifier = Modifier.padding(top = 8.dp, start = 20.dp, end = 20.dp),
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 16.dp),
                color = SmartisanDialogDividerColor,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        val first = PlaybackSleepTimerOptions[row * 2]
                        val second = PlaybackSleepTimerOptions[row * 2 + 1]
                        SleepTimerGridItem(
                            label = androidx.compose.ui.res.stringResource(first.labelRes),
                            selected = state.isActive && state.durationMs == first.durationMs,
                            modifier = Modifier.weight(1f),
                            onClick = { onDurationSelected(first.durationMs) },
                        )
                        SleepTimerGridItem(
                            label = androidx.compose.ui.res.stringResource(second.labelRes),
                            selected = state.isActive && state.durationMs == second.durationMs,
                            modifier = Modifier.weight(1f),
                            onClick = { onDurationSelected(second.durationMs) },
                        )
                    }
                }
            }
            HorizontalDivider(color = SmartisanDialogDividerColor)
            SmartisanDialogActionRow(
                label = androidx.compose.ui.res.stringResource(R.string.cancel),
                style = SmartisanDialogSecondaryActionStyle,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
internal fun PlaybackConfirmDialog(
    title: String,
    message: String? = null,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    SmartisanConfirmDialog(
        title = title,
        message = message,
        confirmText = confirmText,
        dismissText = dismissText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun SleepTimerGridItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor = when {
        selected -> Color(0xFF5E88E8)
        isPressed -> Color(0xFFE8E8E8)
        else -> Color(0xFFF5F5F5)
    }
    val contentColor = if (selected) Color.White else Color(0xCC000000)

    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = label,
                    color = contentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            Text(
                text = label,
                color = contentColor,
                fontSize = 14.sp,
            )
        }
    }
}

internal fun formatSleepTimerRemaining(remainingMs: Long): String {
    val totalSeconds = ((remainingMs.coerceAtLeast(0L) + 999L) / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}
