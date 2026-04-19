package com.smartisanos.music.ui.playback

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartisanos.music.R
import com.smartisanos.music.playback.PlaybackSleepTimerState
import com.smartisanos.music.ui.components.SmartisanConfirmDialog
import com.smartisanos.music.ui.components.SmartisanDialogActionRow
import com.smartisanos.music.ui.components.SmartisanDialogBodyStyle
import com.smartisanos.music.ui.components.SmartisanDialogCard
import com.smartisanos.music.ui.components.SmartisanDialogInsetDivider
import com.smartisanos.music.ui.components.SmartisanDialogPrimaryActionStyle
import com.smartisanos.music.ui.components.SmartisanDialogSecondaryActionStyle
import com.smartisanos.music.ui.components.SmartisanDialogTitleStyle

private const val MinuteMs = 60_000L

internal val PlaybackSleepTimerOptions = listOf(
    SleepTimerOption(R.string.time_no, 0L),
    SleepTimerOption(R.string.time_15m, 15L * MinuteMs),
    SleepTimerOption(R.string.time_30m, 30L * MinuteMs),
    SleepTimerOption(R.string.time_1h, 60L * MinuteMs),
    SleepTimerOption(R.string.time_1_5h, 90L * MinuteMs),
    SleepTimerOption(R.string.time_2h, 120L * MinuteMs),
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
            SmartisanDialogInsetDivider(
                contentPadding = PaddingValues(top = 16.dp),
            )
            PlaybackSleepTimerOptions.forEachIndexed { index, option ->
                SleepTimerActionRow(
                    label = androidx.compose.ui.res.stringResource(option.labelRes),
                    selected = state.isActive && state.durationMs == option.durationMs,
                    onClick = { onDurationSelected(option.durationMs) },
                )
                if (index < PlaybackSleepTimerOptions.lastIndex) {
                    SmartisanDialogInsetDivider()
                }
            }
            SmartisanDialogInsetDivider()
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
private fun SleepTimerActionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SmartisanDialogActionRow(
        label = label,
        style = if (selected) {
            SmartisanDialogPrimaryActionStyle
        } else {
            SmartisanDialogSecondaryActionStyle.copy(color = SmartisanDialogTitleStyle.color)
        },
        onClick = onClick,
    )
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
