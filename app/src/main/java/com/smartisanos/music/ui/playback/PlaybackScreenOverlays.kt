package com.smartisanos.music.ui.playback

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import com.smartisanos.music.R
import com.smartisanos.music.playback.PlaybackSleepTimerState

@Composable
internal fun PlaybackMoreActionOverlays(
    showMorePanel: Boolean,
    favoriteEnabled: Boolean,
    currentVisualPage: PlaybackVisualPage,
    scratchEnabled: Boolean,
    sleepTimerActive: Boolean,
    addToPlaylistEnabled: Boolean,
    showSleepTimerDialog: Boolean,
    sleepTimerState: PlaybackSleepTimerState,
    bottomInsetPx: Int,
    showSetRingtoneDialog: Boolean,
    showWriteSettingsDialog: Boolean,
    onAddToPlaylistClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSetRingtoneClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onLyricsToggle: () -> Unit,
    onScratchToggle: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissMorePanel: () -> Unit,
    onSleepTimerDismiss: () -> Unit,
    onSleepTimerDurationSelected: (Long) -> Unit,
    onSetRingtoneConfirm: () -> Unit,
    onSetRingtoneDismiss: () -> Unit,
    onWriteSettingsConfirm: () -> Unit,
    onWriteSettingsDismiss: () -> Unit,
) {
    LegacyPlaybackMoreActionsOverlay(
        visible = showMorePanel,
        favoriteEnabled = favoriteEnabled,
        visualPage = currentVisualPage,
        scratchEnabled = scratchEnabled,
        sleepTimerActive = sleepTimerActive,
        addToPlaylistEnabled = addToPlaylistEnabled,
        callbacks = LegacyPlaybackMoreActionCallbacks(
            onAddToPlaylistClick = onAddToPlaylistClick,
            onAddToQueueClick = onAddToQueueClick,
            onFavoriteToggle = onFavoriteToggle,
            onLyricsToggle = onLyricsToggle,
            onSleepTimerClick = onSleepTimerClick,
            onSetRingtoneClick = onSetRingtoneClick,
            onScratchToggle = onScratchToggle,
            onDeleteClick = onDeleteClick,
            onDismissRequest = onDismissMorePanel,
        ),
        modifier = Modifier
            .fillMaxSize()
            .zIndex(8f),
    )

    LegacyPlaybackSleepTimerDialog(
        visible = showSleepTimerDialog,
        state = sleepTimerState,
        bottomInsetPx = bottomInsetPx,
        onDismissRequest = onSleepTimerDismiss,
        onDurationSelected = onSleepTimerDurationSelected,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(9f),
    )

    if (showSetRingtoneDialog) {
        PlaybackConfirmDialog(
            title = stringResource(R.string.set_ringtone),
            message = stringResource(R.string.choise_seting_name),
            confirmText = stringResource(R.string.done),
            dismissText = stringResource(R.string.cancel),
            onConfirm = onSetRingtoneConfirm,
            onDismiss = onSetRingtoneDismiss,
        )
    }

    if (showWriteSettingsDialog) {
        PlaybackConfirmDialog(
            title = stringResource(R.string.set_ringtone),
            message = stringResource(R.string.ringtone_permission_message),
            confirmText = stringResource(R.string.continue_action),
            dismissText = stringResource(R.string.cancel),
            onConfirm = onWriteSettingsConfirm,
            onDismiss = onWriteSettingsDismiss,
        )
    }
}

@Composable
internal fun PlaybackQueueOverlayHost(
    showQueueOverlay: Boolean,
    currentTrackProvider: () -> PlaybackQueueTrack?,
    upcomingItemsProvider: () -> List<PlaybackQueueTrack>,
    isCurrentFavorite: Boolean,
    reorderEnabled: Boolean,
    onExitFullScreenClick: () -> Unit,
    onReturnToPlaybackClick: () -> Unit,
    onItemClick: (Int) -> Unit,
    onFavoriteCurrentClick: () -> Unit,
    onClearUpcomingClick: () -> Unit,
    onMoveRequest: (Int, Int) -> Unit,
) {
    AnimatedVisibility(
        visible = showQueueOverlay,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f),
        enter = androidx.compose.animation.slideInVertically(
            animationSpec = tween(
                durationMillis = 300,
                easing = { fraction -> 1f - (1f - fraction) * (1f - fraction) },
            ),
            initialOffsetY = { fullHeight -> fullHeight },
        ),
        exit = androidx.compose.animation.slideOutVertically(
            animationSpec = tween(
                durationMillis = 300,
                easing = { fraction -> 1f - (1f - fraction) * (1f - fraction) },
            ),
            targetOffsetY = { fullHeight -> fullHeight },
        ),
    ) {
        val currentTrack = currentTrackProvider()
        val upcomingItems = upcomingItemsProvider()
        PlaybackQueueScreen(
            state = PlaybackQueueUiState(
                currentTrack = currentTrack,
                upcomingTracks = upcomingItems,
                isCurrentFavorite = isCurrentFavorite,
                reorderEnabled = reorderEnabled,
            ),
            onExitFullScreenClick = onExitFullScreenClick,
            onReturnToPlaybackClick = onReturnToPlaybackClick,
            onItemClick = onItemClick,
            onFavoriteCurrentClick = onFavoriteCurrentClick,
            onClearUpcomingClick = onClearUpcomingClick,
            onMoveRequest = onMoveRequest,
        )
    }
}
