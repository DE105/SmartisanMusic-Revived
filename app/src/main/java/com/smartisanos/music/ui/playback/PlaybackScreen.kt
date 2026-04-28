package com.smartisanos.music.ui.playback

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.data.favorite.FavoriteSongsRepository
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.data.settings.PlaybackSettings
import com.smartisanos.music.playback.EmbeddedLyrics
import com.smartisanos.music.playback.LocalPlaybackController
import com.smartisanos.music.playback.PlaybackSleepTimer
import com.smartisanos.music.playback.cancelSleepTimer
import com.smartisanos.music.playback.extractEmbeddedLyrics
import com.smartisanos.music.playback.loadEmbeddedLyrics
import com.smartisanos.music.playback.setScratchSeekModeEnabled
import com.smartisanos.music.playback.startSleepTimer
import com.smartisanos.music.ui.components.loadEmbeddedArtwork
import com.smartisanos.music.ui.components.SmartisanTitleBarSurface
import com.smartisanos.music.ui.components.SmartisanTitleBarSurfaceStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

private val PlaybackPageBackground = Color(0xFFFDFDFB)
private val PlaybackTopBarDivider = Color(0xFFE6E6E6)
private val PlaybackTitleColor = Color(0xFF6B6B6F)
private val PlaybackSubtitleColor = Color(0x88333333)
private val PlaybackTimeColor = Color(0x88333333)
private val PlaybackTrackColor = Color(0x1F000000)
private val PlaybackTrackFillColor = Color(0xFFBBBBBB)
private val PlaybackVolumeTrackColor = Color(0x11000000)
private val PlaybackVolumeFillColor = Color(0xFF9A9A9A)
private val PlaybackPanelBorder = Color(0xFFE8E8E8)
private val PlaybackPanelBottomEdge = Color(0xFFFDFDFD)
private val PlaybackPanelShadow = Color(0x14000000)

private const val ScratchCycleDurationMs = 3_500f
private const val DiscRotationDegrees = 360f
private const val ScratchHubDeadZoneRatio = 0.06f
private const val ScratchMaxAngleStepDegrees = 54f
private const val ScratchPreviewTimeoutMs = 260L
private const val ScratchPreviewSettleToleranceMs = 24L
private const val OriginalNeedlePivotX = 0.82f
private const val OriginalNeedlePivotY = 0.08f
private const val NeedleRestRotationDegrees = -12f
private const val NeedlePlaybackStartRotationDegrees = 12f
private const val NeedlePlaybackSweepDegrees = 10f
private const val PlaybackAlbumArtDiameterRatio = 405f / 1080f
private const val PlaybackTurntableAxisDiameterRatio = 62f / 1080f
private const val PlaybackTurntableAxisSourceDiameterPx = 60
private val PlaybackSeekBarHeight = 41.dp
private val PlaybackSeekBarSideWidth = 51.3.dp
private val PlaybackSeekTrackHeight = 8.dp
private val PlaybackSeekThumbWidth = 22.3.dp
private val PlaybackSeekThumbHeight = 41.dp
private val PlaybackSeekBarDividerHeight = 0.7.dp
private val PlaybackVolumeBarHeight = 60.dp
private val PlaybackVolumeThumbOffset = 5.dp
private val PlaybackActionButtonSize = 31.dp

private val PlaybackTitleStyle = TextStyle(
    fontSize = 18.sp,
    fontWeight = FontWeight.SemiBold,
    color = PlaybackTitleColor,
)
private val PlaybackArtistStyle = TextStyle(
    fontSize = 11.sp,
    color = PlaybackSubtitleColor,
)
private val PlaybackTimeStyle = TextStyle(
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    color = PlaybackTimeColor,
)

internal data class PlaybackScreenState(
    val mediaItem: MediaItem? = null,
    val isPlaying: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleEnabled: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val volume: Float = 1f,
)

internal enum class PlaybackVisualPage {
    Cover,
    Lyrics,
}

private data class PlaybackCoverPageState(
    val isScratchDragging: Boolean = false,
    val scratchPreviewPositionMs: Long? = null,
    val resumePlaybackAfterScratch: Boolean = false,
)

@Composable
fun PlaybackScreen(
    playbackSettings: PlaybackSettings,
    onScratchEnabledChange: (Boolean) -> Unit,
    onCollapse: () -> Unit,
    onRequestAddToPlaylist: (List<MediaItem>) -> Unit = {},
    onRequestAddToQueue: (List<MediaItem>) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val controller = LocalPlaybackController.current
    val context = LocalContext.current
    val exclusionsStore = remember(context.applicationContext) {
        LibraryExclusionsStore(context.applicationContext)
    }
    val favoriteRepository = remember(context.applicationContext) {
        FavoriteSongsRepository.getInstance(context.applicationContext)
    }
    val favoriteIds by favoriteRepository.observeFavoriteIds().collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scratchSoundController = remember(context) {
        ScratchSoundController(context)
    }
    val popcornSoundController = remember(context) {
        VinylPopcornSoundController(context)
    }
    var state by remember(controller) {
        mutableStateOf(controller.snapshot(context))
    }
    var showMorePanel by rememberSaveable { mutableStateOf(false) }
    var showSleepTimerDialog by rememberSaveable { mutableStateOf(false) }
    var showSetRingtoneDialog by rememberSaveable { mutableStateOf(false) }
    var showWriteSettingsDialog by rememberSaveable { mutableStateOf(false) }
    var showQueueOverlay by rememberSaveable { mutableStateOf(false) }
    var currentVisualPage by rememberSaveable { mutableStateOf(PlaybackVisualPage.Cover) }
    var keepLyricsScreenAwake by rememberSaveable { mutableStateOf(false) }
    var pendingRingtoneUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var sleepTimerWasActive by remember { mutableStateOf(false) }
    var coverPageState by remember(state.mediaItem?.mediaId) {
        mutableStateOf(PlaybackCoverPageState())
    }
    val sleepTimerState by PlaybackSleepTimer.state.collectAsStateWithLifecycle()
    val applyPendingRingtone by rememberUpdatedState(
        newValue = {
            val ringtoneUriString = pendingRingtoneUriString
            pendingRingtoneUriString = null
            val ringtoneUri = ringtoneUriString
                ?.takeIf { it.isNotBlank() }
                ?.let(Uri::parse)
                ?: run {
                    context.toast(R.string.can_not_set_ringtone)
                    return@rememberUpdatedState
                }
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    context.applicationContext.trySetDefaultRingtone(ringtoneUri)
                }
                context.toast(if (success) R.string.ring_success else R.string.ring_fault)
            }
        },
    )
    val writeSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        if (Settings.System.canWrite(context)) {
            applyPendingRingtone()
        } else {
            pendingRingtoneUriString = null
            context.toast(R.string.ringtone_permission_missing)
        }
    }

    BackHandler {
        if (showQueueOverlay) {
            showQueueOverlay = false
        } else if (showMorePanel) {
            showMorePanel = false
        } else {
            onCollapse()
        }
    }

    DisposableEffect(controller) {
        val playbackController = controller ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                state = playbackController.snapshot(context)
            }
        }
        playbackController.addListener(listener)
        onDispose {
            playbackController.removeListener(listener)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                PlaybackSleepTimer.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(showSleepTimerDialog) {
        if (showSleepTimerDialog) {
            PlaybackSleepTimer.refresh()
        }
    }

    LaunchedEffect(sleepTimerState.isActive) {
        if (sleepTimerWasActive && !sleepTimerState.isActive) {
            showSleepTimerDialog = false
        }
        sleepTimerWasActive = sleepTimerState.isActive
    }

    fun resetCoverPageInteraction(resumePlayback: Boolean) {
        val shouldResumePlayback = resumePlayback && coverPageState.resumePlaybackAfterScratch
        coverPageState = PlaybackCoverPageState()
        if (shouldResumePlayback) {
            controller?.play()
        }
        controller?.setScratchSeekModeEnabled(false)
        scratchSoundController.stop()
    }

    fun setVisualPage(targetPage: PlaybackVisualPage) {
        if (currentVisualPage == targetPage) {
            return
        }
        currentVisualPage = targetPage
        if (targetPage != PlaybackVisualPage.Cover) {
            resetCoverPageInteraction(resumePlayback = true)
        }
    }

    DisposableEffect(controller) {
        val playbackController = controller
        playbackController?.volume = 1f
        onDispose {
            playbackController?.volume = 1f
            playbackController?.setScratchSeekModeEnabled(false)
        }
    }

    DisposableEffect(scratchSoundController) {
        onDispose {
            scratchSoundController.release()
        }
    }

    DisposableEffect(popcornSoundController) {
        onDispose {
            popcornSoundController.release()
        }
    }

    LaunchedEffect(controller, state.mediaItem?.mediaId, state.isPlaying) {
        val playbackController = controller ?: return@LaunchedEffect
        while (isActive) {
            state = playbackController.snapshot(context)
            delay(if (state.isPlaying) 80L else 240L)
        }
    }

    LaunchedEffect(controller, playbackSettings.scratchEnabled, currentVisualPage) {
        if (!playbackSettings.scratchEnabled || currentVisualPage != PlaybackVisualPage.Cover) {
            resetCoverPageInteraction(resumePlayback = true)
        }
    }

    LaunchedEffect(
        playbackSettings.popcornSoundEnabled,
        state.isPlaying,
        coverPageState.isScratchDragging,
    ) {
        if (
            !playbackSettings.popcornSoundEnabled ||
            !state.isPlaying ||
            coverPageState.isScratchDragging
        ) {
            popcornSoundController.stop()
            return@LaunchedEffect
        }
        try {
            while (isActive) {
                popcornSoundController.playRandomPop()
                delay(Random.nextLong(from = 860L, until = 1_640L))
            }
        } finally {
            popcornSoundController.stop()
        }
    }

    val mediaMetadata = state.mediaItem?.mediaMetadata
    val scratchSourceUri = state.mediaItem?.localConfiguration?.uri
    val title = mediaMetadata?.displayTitle?.toString()
        ?: mediaMetadata?.title?.toString()
        ?: stringResource(R.string.unknown_song_title)
    val artist = mediaMetadata?.subtitle?.toString()
        ?: mediaMetadata?.artist?.toString()
        ?: stringResource(R.string.unknown_artist)
    val durationMs = state.durationMs.takeIf { it > 0L }
        ?: mediaMetadata?.durationMs
        ?: 0L
    val currentMediaId = state.mediaItem?.mediaId
    val favoriteEnabled = !currentMediaId.isNullOrBlank() && currentMediaId in favoriteIds
    val coverPreviewPositionMs = coverPageState.scratchPreviewPositionMs
    val livePositionMs = state.currentPositionMs.coerceIn(0L, durationMs.coerceAtLeast(0L))
    val displayPositionMs = if (currentVisualPage == PlaybackVisualPage.Cover) {
        coverPreviewPositionMs
            ?.coerceIn(0L, durationMs.coerceAtLeast(0L))
            ?: livePositionMs
    } else {
        livePositionMs
    }
    val primaryLyricLine = stringResource(R.string.playback_more_primary_line)
    val secondaryLyricLine = stringResource(R.string.playback_more_secondary_line)
    val tertiaryLyricLine = stringResource(R.string.playback_more_tertiary_line)
    val fallbackLyricsLines = remember(
        title,
        artist,
        primaryLyricLine,
        secondaryLyricLine,
        tertiaryLyricLine,
    ) {
        listOf(
            title,
            artist,
            primaryLyricLine,
            secondaryLyricLine,
            tertiaryLyricLine,
        )
    }
    val controllerTracks = controller?.currentTracks
    val trackLyrics = remember(state.mediaItem?.mediaId, controllerTracks) {
        controllerTracks?.let(::extractEmbeddedLyrics)
    }
    val embeddedLyrics by produceState<EmbeddedLyrics?>(
        initialValue = trackLyrics,
        key1 = state.mediaItem?.mediaId,
        key2 = state.mediaItem?.localConfiguration?.uri,
        key3 = trackLyrics,
    ) {
        value = trackLyrics ?: state.mediaItem?.let { mediaItem ->
            loadEmbeddedLyrics(context, mediaItem)
        }
    }
    val albumArtwork by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = state.mediaItem?.mediaId,
        key2 = state.mediaItem?.mediaMetadata?.artworkUri,
    ) {
        value = state.mediaItem?.let { mediaItem ->
            loadEmbeddedArtwork(context, mediaItem)
        }
    }

    LaunchedEffect(
        currentVisualPage,
        coverPageState.isScratchDragging,
        coverPageState.scratchPreviewPositionMs,
        state.currentPositionMs,
    ) {
        if (currentVisualPage != PlaybackVisualPage.Cover) {
            return@LaunchedEffect
        }
        val previewPosition = coverPageState.scratchPreviewPositionMs ?: return@LaunchedEffect
        if (
            !coverPageState.isScratchDragging &&
            abs(state.currentPositionMs - previewPosition) <= ScratchPreviewSettleToleranceMs
        ) {
            coverPageState = coverPageState.copy(scratchPreviewPositionMs = null)
        }
    }

    LaunchedEffect(
        currentVisualPage,
        coverPageState.isScratchDragging,
        coverPageState.scratchPreviewPositionMs,
    ) {
        if (currentVisualPage != PlaybackVisualPage.Cover) {
            return@LaunchedEffect
        }
        val previewPosition = coverPageState.scratchPreviewPositionMs ?: return@LaunchedEffect
        if (coverPageState.isScratchDragging) {
            return@LaunchedEffect
        }
        delay(ScratchPreviewTimeoutMs)
        if (
            !coverPageState.isScratchDragging &&
            coverPageState.scratchPreviewPositionMs == previewPosition
        ) {
            coverPageState = coverPageState.copy(scratchPreviewPositionMs = null)
        }
    }

    LaunchedEffect(scratchSourceUri) {
        scratchSoundController.prepareSource(scratchSourceUri)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(PlaybackPageBackground),
    ) {
        val density = LocalDensity.current
        val topInset = with(density) {
            WindowInsets.safeDrawing.getTop(this).toDp()
        }
        val bottomInset = with(density) {
            WindowInsets.safeDrawing.getBottom(this).toDp()
        }
        val turntableWidth = minOf(maxWidth - 8.dp, 360.dp)
        val scale = turntableWidth.value / 360f

        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            PlaybackTopBar(
                title = title,
                artist = artist,
                topInset = topInset,
                onQueueClick = { showQueueOverlay = true },
                onCollapse = onCollapse,
            )
            PlaybackTimeSeekBar(
                durationMs = durationMs,
                currentPositionMs = displayPositionMs,
                thumbRes = R.drawable.playing_control_time,
                modifier = Modifier.fillMaxWidth(),
                onSeek = { positionMs ->
                    controller?.seekTo(positionMs)
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                PlaybackVisualStage(
                    modifier = Modifier.width(turntableWidth),
                    turntableWidth = turntableWidth,
                    scale = scale,
                    currentVisualPage = currentVisualPage,
                    coverPositionMs = displayPositionMs,
                    lyricsPositionMs = livePositionMs,
                    durationMs = durationMs,
                    scratchEnabled = playbackSettings.scratchEnabled,
                    hidePlayerAxisEnabled = playbackSettings.hidePlayerAxisEnabled,
                    albumArtwork = albumArtwork,
                    keepLyricsScreenAwake = keepLyricsScreenAwake,
                    embeddedLyrics = embeddedLyrics,
                    fallbackLyricsLines = fallbackLyricsLines,
                    hasMediaItem = state.mediaItem != null,
                    isPlaying = state.isPlaying,
                    isScratchDragging = coverPageState.isScratchDragging,
                    scratchPreviewPositionMs = coverPageState.scratchPreviewPositionMs,
                    mediaId = state.mediaItem?.mediaId,
                    onMoreClick = {
                        showMorePanel = true
                    },
                    onVisualPageToggle = {
                        setVisualPage(
                            if (currentVisualPage == PlaybackVisualPage.Cover) {
                                PlaybackVisualPage.Lyrics
                            } else {
                                PlaybackVisualPage.Cover
                            },
                        )
                    },
                    onKeepLyricsScreenAwakeToggle = {
                        keepLyricsScreenAwake = !keepLyricsScreenAwake
                    },
                    onScratchStart = {
                        val resumePlaybackAfterScratch = state.isPlaying
                        coverPageState = coverPageState.copy(
                            isScratchDragging = true,
                            scratchPreviewPositionMs = livePositionMs,
                            resumePlaybackAfterScratch = resumePlaybackAfterScratch,
                        )
                        if (resumePlaybackAfterScratch) {
                            controller?.pause()
                        }
                        controller?.setScratchSeekModeEnabled(true)
                        scratchSoundController.onScratchStart(
                            sourceUri = scratchSourceUri,
                            positionMs = livePositionMs,
                        )
                    },
                    onScratchMotion = { positionMs, deltaAngle ->
                        scratchSoundController.onScratchMotion(positionMs, deltaAngle)
                    },
                    onScratchPositionChange = { positionMs, _ ->
                        coverPageState = coverPageState.copy(
                            scratchPreviewPositionMs = positionMs,
                        )
                    },
                    onScratchEnd = { positionMs ->
                        val resumePlaybackAfterScratch = coverPageState.resumePlaybackAfterScratch
                        coverPageState = coverPageState.copy(
                            isScratchDragging = false,
                            scratchPreviewPositionMs = positionMs,
                            resumePlaybackAfterScratch = false,
                        )
                        controller?.seekTo(positionMs)
                        if (resumePlaybackAfterScratch) {
                            controller?.play()
                        }
                        controller?.setScratchSeekModeEnabled(false)
                        scratchSoundController.stop()
                    },
                    onScratchCancel = {
                        resetCoverPageInteraction(resumePlayback = true)
                    },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            PlaybackBottomControls(
                width = turntableWidth,
                bottomInset = bottomInset,
                state = state,
                onRepeatClick = {
                    controller?.repeatMode =
                        if (state.repeatMode == Player.REPEAT_MODE_OFF) {
                            Player.REPEAT_MODE_ALL
                        } else {
                            Player.REPEAT_MODE_OFF
                        }
                },
                onPreviousClick = {
                    controller?.seekToPrevious()
                },
                onPlayPauseClick = {
                    if (state.isPlaying) {
                        controller?.pause()
                    } else {
                        controller?.play()
                    }
                },
                onNextClick = {
                    controller?.seekToNext()
                },
                onShuffleClick = {
                    controller?.shuffleModeEnabled = !state.shuffleEnabled
                },
                onVolumeChange = { volume ->
                    context.setMusicStreamVolumeFraction(volume)
                    state = state.copy(volume = context.musicStreamVolumeFraction())
                },
            )
        }

        AnimatedVisibility(
            visible = showMorePanel,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(160)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x12000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        showMorePanel = false
                    },
            ) {
                PlaybackMoreActionPanel(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    favoriteEnabled = favoriteEnabled,
                    visualPage = currentVisualPage,
                    scratchEnabled = playbackSettings.scratchEnabled,
                    sleepTimerActive = sleepTimerState.isActive,
                    bottomInset = bottomInset,
                    onAddToPlaylistClick = {
                        state.mediaItem?.let { onRequestAddToPlaylist(listOf(it)) }
                        showMorePanel = false
                    },
                    onAddToQueueClick = {
                        state.mediaItem?.let { onRequestAddToQueue(listOf(it)) }
                        showMorePanel = false
                    },
                    onFavoriteToggle = {
                        val mediaId = currentMediaId ?: return@PlaybackMoreActionPanel
                        scope.launch {
                            favoriteRepository.toggle(mediaId)
                        }
                    },
                    onSetRingtoneClick = {
                        showMorePanel = false
                        showSetRingtoneDialog = true
                    },
                    onSleepTimerClick = {
                        showMorePanel = false
                        showSleepTimerDialog = true
                    },
                    onLyricsToggle = {
                        setVisualPage(
                            if (currentVisualPage == PlaybackVisualPage.Cover) {
                                PlaybackVisualPage.Lyrics
                            } else {
                                PlaybackVisualPage.Cover
                            },
                        )
                        showMorePanel = false
                    },
                    onScratchToggle = {
                        onScratchEnabledChange(!playbackSettings.scratchEnabled)
                        showMorePanel = false
                    },
                    onDeleteClick = {
                        val mediaId = state.mediaItem?.mediaId
                        if (!mediaId.isNullOrBlank()) {
                            scope.launch {
                                exclusionsStore.hideMediaIds(setOf(mediaId))
                            }
                            val index = controller?.currentMediaItemIndex ?: -1
                            if (index >= 0) {
                                controller?.removeMediaItem(index)
                            }
                        }
                        showMorePanel = false
                    },
                    onDismiss = {
                        showMorePanel = false
                    },
                )
            }
        }

        if (showSleepTimerDialog) {
            PlaybackSleepTimerDialog(
                state = sleepTimerState,
                onDismiss = {
                    showSleepTimerDialog = false
                },
                onDurationSelected = { durationMs ->
                    showSleepTimerDialog = false
                    if (durationMs > 0L) {
                        controller?.startSleepTimer(durationMs)
                    } else {
                        controller?.cancelSleepTimer()
                        if (sleepTimerState.isActive) {
                            context.toast(R.string.sleep_timer_stopped)
                        }
                    }
                },
            )
        }

        if (showSetRingtoneDialog) {
            PlaybackConfirmDialog(
                title = stringResource(R.string.set_ringtone),
                message = stringResource(R.string.choise_seting_name),
                confirmText = stringResource(R.string.done),
                dismissText = stringResource(R.string.cancel),
                onConfirm = {
                    showSetRingtoneDialog = false
                    val ringtoneUri = state.mediaItem?.localConfiguration?.uri
                    if (ringtoneUri == null) {
                        context.toast(R.string.can_not_set_ringtone)
                        return@PlaybackConfirmDialog
                    }
                    pendingRingtoneUriString = ringtoneUri.toString()
                    if (Settings.System.canWrite(context)) {
                        applyPendingRingtone()
                    } else {
                        showWriteSettingsDialog = true
                    }
                },
                onDismiss = {
                    showSetRingtoneDialog = false
                },
            )
        }

        if (showWriteSettingsDialog) {
            PlaybackConfirmDialog(
                title = stringResource(R.string.set_ringtone),
                message = stringResource(R.string.ringtone_permission_message),
                confirmText = stringResource(R.string.continue_action),
                dismissText = stringResource(R.string.cancel),
                onConfirm = {
                    showWriteSettingsDialog = false
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    if (intent.resolveActivity(context.packageManager) == null) {
                        pendingRingtoneUriString = null
                        context.toast(R.string.ring_fault)
                        return@PlaybackConfirmDialog
                    }
                    writeSettingsLauncher.launch(intent)
                },
                onDismiss = {
                    showWriteSettingsDialog = false
                    pendingRingtoneUriString = null
                },
            )
        }

        AnimatedVisibility(
            visible = showQueueOverlay,
            modifier = Modifier.fillMaxSize().zIndex(10f),
            enter = androidx.compose.animation.slideInVertically(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = { fraction -> 1f - (1f - fraction) * (1f - fraction) }
                ),
                initialOffsetY = { fullHeight -> fullHeight }
            ),
            exit = androidx.compose.animation.slideOutVertically(
                animationSpec = tween(
                    durationMillis = 300,
                    easing = { fraction -> 1f - (1f - fraction) * (1f - fraction) }
                ),
                targetOffsetY = { fullHeight -> fullHeight }
            )
        ) {
            val currentTrack = state.mediaItem?.toPlaybackQueueTrack(context)
            val upcomingItems = controller?.upcomingQueueTracks(context).orEmpty()
            PlaybackQueueScreen(
                state = PlaybackQueueUiState(
                    currentTrack = currentTrack,
                    upcomingTracks = upcomingItems,
                    isCurrentFavorite = favoriteEnabled,
                ),
                onExitFullScreenClick = {
                    showQueueOverlay = false
                    onCollapse()
                },
                onReturnToPlaybackClick = {
                    showQueueOverlay = false
                },
                onItemClick = { index ->
                    val totalIndex = (controller?.currentMediaItemIndex ?: -1) + 1 + index
                    controller?.seekToDefaultPosition(totalIndex)
                    showQueueOverlay = false
                },
                onFavoriteCurrentClick = {
                    val mediaId = currentMediaId ?: return@PlaybackQueueScreen
                    scope.launch {
                        favoriteRepository.toggle(mediaId)
                    }
                },
                onClearUpcomingClick = {
                    val playbackController = controller ?: return@PlaybackQueueScreen
                    val playingIndex = playbackController.currentMediaItemIndex
                    val itemCount = playbackController.mediaItemCount
                    if (playingIndex >= 0 && playingIndex + 1 < itemCount) {
                        playbackController.removeMediaItems(playingIndex + 1, itemCount)
                    }
                },
                onMoveRequest = { from, to ->
                    if (from == to) {
                        return@PlaybackQueueScreen
                    }
                    val playbackController = controller ?: return@PlaybackQueueScreen
                    val playingIndex = playbackController.currentMediaItemIndex
                    val itemCount = playbackController.mediaItemCount
                    if (playingIndex < 0) {
                        return@PlaybackQueueScreen
                    }
                    val absoluteFrom = playingIndex + 1 + from
                    val absoluteTo = playingIndex + 1 + to
                    if (absoluteFrom in 0 until itemCount && absoluteTo in 0 until itemCount) {
                        playbackController.moveMediaItem(absoluteFrom, absoluteTo)
                    }
                },
            )
        }
    }
}

@Composable
private fun PlaybackTopBar(
    title: String,
    artist: String,
    topInset: Dp,
    onQueueClick: () -> Unit,
    onCollapse: () -> Unit,
) {
    SmartisanTitleBarSurface(
        style = SmartisanTitleBarSurfaceStyle.Playback,
        modifier = Modifier
            .fillMaxWidth()
            .height(topInset + 48.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PressedDrawableButton(
                normalRes = R.drawable.btn_playing_back,
                pressedRes = R.drawable.btn_playing_back_down,
                contentDescription = stringResource(R.string.collapse_player),
                modifier = Modifier
                    .width(40.dp)
                    .height(30.dp),
                onClick = onCollapse,
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = PlaybackTitleStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = artist,
                    style = PlaybackArtistStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            PressedDrawableButton(
                normalRes = R.drawable.btn_playing_list,
                pressedRes = R.drawable.btn_playing_list_down,
                contentDescription = stringResource(R.string.playback_queue),
                modifier = Modifier
                    .width(40.dp)
                    .height(30.dp),
                onClick = onQueueClick,
            )
        }
    }
}

@Composable
private fun PlaybackTimeSeekBar(
    durationMs: Long,
    currentPositionMs: Long,
    @DrawableRes thumbRes: Int,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit,
) {
    val duration = durationMs.coerceAtLeast(0L)
    val currentFraction = if (duration > 0L) {
        currentPositionMs.toFloat() / duration.toFloat()
    } else {
        0f
    }.coerceIn(0f, 1f)
    var trackWidthPx by remember { mutableIntStateOf(0) }
    var dragFraction by remember { mutableFloatStateOf(Float.NaN) }
    val density = LocalDensity.current
    val shownFraction = if (dragFraction.isNaN()) currentFraction else dragFraction.coerceIn(0f, 1f)
    val shownPosition = if (duration > 0L) {
        (shownFraction * duration.toFloat()).roundToLong()
    } else {
        0L
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlaybackSeekBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPlaybackTime(shownPosition),
                style = PlaybackTimeStyle.copy(textAlign = TextAlign.Start),
                maxLines = 1,
                modifier = Modifier
                    .width(PlaybackSeekBarSideWidth)
                    .padding(start = 6.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .onSizeChanged { trackWidthPx = it.width }
                    .pointerInput(duration) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (trackWidthPx > 0) {
                                    dragFraction = fractionFromPosition(offset.x, trackWidthPx)
                                    if (duration > 0L) {
                                        onSeek((dragFraction * duration.toFloat()).roundToLong())
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                if (trackWidthPx > 0) {
                                    dragFraction = fractionFromPosition(change.position.x, trackWidthPx)
                                    if (duration > 0L) {
                                        onSeek((dragFraction * duration.toFloat()).roundToLong())
                                    }
                                    change.consume()
                                }
                            },
                            onDragEnd = {
                                val finalFraction = dragFraction.takeUnless { it.isNaN() } ?: currentFraction
                                if (duration > 0L) {
                                    onSeek((finalFraction * duration.toFloat()).roundToLong())
                                }
                                dragFraction = Float.NaN
                            },
                            onDragCancel = {
                                dragFraction = Float.NaN
                            },
                        )
                    },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(PlaybackSeekTrackHeight)
                        .align(Alignment.Center)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(PlaybackTrackColor),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shownFraction)
                        .height(PlaybackSeekTrackHeight)
                        .align(Alignment.CenterStart)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(PlaybackTrackFillColor),
                )
                Image(
                    painter = painterResource(thumbRes),
                    contentDescription = null,
                    modifier = Modifier
                        .width(PlaybackSeekThumbWidth)
                        .height(PlaybackSeekThumbHeight)
                        .align(Alignment.CenterStart)
                        .offset {
                            val thumbWidthPx = with(density) { PlaybackSeekThumbWidth.roundToPx() }
                            IntOffset(
                                x = (
                                    (trackWidthPx * shownFraction) - (thumbWidthPx / 2f)
                                ).roundToInt(),
                                y = 0,
                            )
                        },
                )
            }
            Text(
                text = "-${formatPlaybackTime((duration - shownPosition).coerceAtLeast(0L))}",
                style = PlaybackTimeStyle.copy(textAlign = TextAlign.End),
                maxLines = 1,
                modifier = Modifier
                    .width(PlaybackSeekBarSideWidth)
                    .padding(end = 6.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PlaybackSeekBarDividerHeight)
                .background(PlaybackTopBarDivider.copy(alpha = 0.7f)),
        )
    }
}

@Composable
private fun PlaybackVisualStage(
    turntableWidth: Dp,
    scale: Float,
    currentVisualPage: PlaybackVisualPage,
    coverPositionMs: Long,
    lyricsPositionMs: Long,
    durationMs: Long,
    scratchEnabled: Boolean,
    hidePlayerAxisEnabled: Boolean,
    albumArtwork: ImageBitmap?,
    keepLyricsScreenAwake: Boolean,
    embeddedLyrics: EmbeddedLyrics?,
    fallbackLyricsLines: List<String>,
    hasMediaItem: Boolean,
    isPlaying: Boolean,
    isScratchDragging: Boolean,
    scratchPreviewPositionMs: Long?,
    mediaId: String?,
    onMoreClick: () -> Unit,
    onVisualPageToggle: () -> Unit,
    onKeepLyricsScreenAwakeToggle: () -> Unit,
    onScratchStart: () -> Unit,
    onScratchMotion: (Long, Float) -> Unit,
    onScratchPositionChange: (Long, Float) -> Unit,
    onScratchEnd: (Long) -> Unit,
    onScratchCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val turntableHeight = 356.5938.dp * scale
    val moreButtonMargin = 12.dp * scale
    val moreButtonTopMargin = 38.dp * scale
    val actionButtonSize = PlaybackActionButtonSize * scale
    val isLyricsPage = currentVisualPage == PlaybackVisualPage.Lyrics

    Box(
        modifier = modifier.height(turntableHeight + 52.dp * scale),
    ) {
        PressedDrawableButton(
            normalRes = R.drawable.more_btn,
            pressedRes = R.drawable.more_btn_down,
            contentDescription = stringResource(R.string.player_more_actions),
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(2f)
                .padding(start = moreButtonMargin, top = moreButtonTopMargin)
                .size(actionButtonSize),
            onClick = onMoreClick,
        )
        if (isLyricsPage) {
            val screenSwitchNormalRes = if (keepLyricsScreenAwake) {
                R.drawable.sun_btn_on
            } else {
                R.drawable.sun_btn_off
            }
            val screenSwitchPressedRes = if (keepLyricsScreenAwake) {
                R.drawable.sun_btn_on_down
            } else {
                R.drawable.sun_btn_off_down
            }
            PressedDrawableButton(
                normalRes = screenSwitchNormalRes,
                pressedRes = screenSwitchPressedRes,
                contentDescription = stringResource(R.string.always_on),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(2f)
                    .padding(end = moreButtonMargin, top = moreButtonTopMargin)
                    .size(actionButtonSize),
                onClick = onKeepLyricsScreenAwakeToggle,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(turntableWidth)
                .height(turntableHeight),
            contentAlignment = Alignment.Center,
        ) {
            when (currentVisualPage) {
                PlaybackVisualPage.Cover -> PlaybackCoverPage(
                    turntableWidth = turntableWidth,
                    scale = scale,
                    currentPositionMs = coverPositionMs,
                    durationMs = durationMs,
                    scratchEnabled = scratchEnabled,
                    hidePlayerAxisEnabled = hidePlayerAxisEnabled,
                    albumArtwork = albumArtwork,
                    hasMediaItem = hasMediaItem,
                    isPlaying = isPlaying,
                    isScratchDragging = isScratchDragging,
                    scratchPreviewPositionMs = scratchPreviewPositionMs,
                    mediaId = mediaId,
                    onVisualPageToggle = onVisualPageToggle,
                    onScratchStart = onScratchStart,
                    onScratchMotion = onScratchMotion,
                    onScratchPositionChange = onScratchPositionChange,
                    onScratchEnd = onScratchEnd,
                    onScratchCancel = onScratchCancel,
                    modifier = Modifier.matchParentSize(),
                )

                PlaybackVisualPage.Lyrics -> PlaybackLyricsPage(
                    mediaId = mediaId,
                    lyrics = embeddedLyrics,
                    fallbackLyricsLines = fallbackLyricsLines,
                    currentPositionMs = lyricsPositionMs,
                    keepLyricsScreenAwake = keepLyricsScreenAwake,
                    onVisualPageToggle = onVisualPageToggle,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
    }
}

@Composable
private fun PlaybackCoverPage(
    turntableWidth: Dp,
    scale: Float,
    currentPositionMs: Long,
    durationMs: Long,
    scratchEnabled: Boolean,
    hidePlayerAxisEnabled: Boolean,
    albumArtwork: ImageBitmap?,
    hasMediaItem: Boolean,
    isPlaying: Boolean,
    isScratchDragging: Boolean,
    scratchPreviewPositionMs: Long?,
    mediaId: String?,
    onVisualPageToggle: () -> Unit,
    onScratchStart: () -> Unit,
    onScratchMotion: (Long, Float) -> Unit,
    onScratchPositionChange: (Long, Float) -> Unit,
    onScratchEnd: (Long) -> Unit,
    onScratchCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = durationMs
        .takeIf { it > 0L }
        ?.let { currentPositionMs.toFloat() / it.toFloat() }
        ?.coerceIn(0f, 1f)
        ?: 0f
    val targetNeedleRotation = if (hasMediaItem && (isPlaying || isScratchDragging)) {
        NeedlePlaybackStartRotationDegrees + (progress * NeedlePlaybackSweepDegrees)
    } else {
        NeedleRestRotationDegrees
    }

    // 切歌时完全冻结唱针角度，不做任何状态改变
    val needleAnimatable = remember { Animatable(targetNeedleRotation) }
    var prevMediaId by remember { mutableStateOf(mediaId) }

    LaunchedEffect(mediaId, targetNeedleRotation) {
        if (prevMediaId != mediaId) {
            prevMediaId = mediaId
            // 切歌：保持当前角度不变，不响应 targetNeedleRotation 变化
            return@LaunchedEffect
        }
        // 非切歌：平滑过渡到目标角度
        needleAnimatable.animateTo(targetNeedleRotation, animationSpec = tween(220))
    }

    val needleRotation = needleAnimatable.value
    var discSize by remember { mutableStateOf(IntSize.Zero) }
    var scratchActive by remember(mediaId) { mutableStateOf(false) }
    var scratchPositionMs by remember(mediaId) {
        mutableLongStateOf(currentPositionMs.coerceIn(0L, durationMs))
    }
    var lastAngleDegrees by remember(mediaId) { mutableFloatStateOf(Float.NaN) }
    val latestDiscSize by rememberUpdatedState(discSize)
    val latestVisualPageToggle by rememberUpdatedState(onVisualPageToggle)
    val scratchAvailable by rememberUpdatedState(scratchEnabled && durationMs > 0L)
    val latestPositionMs by rememberUpdatedState(currentPositionMs)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestScratchStart by rememberUpdatedState(onScratchStart)
    val latestScratchMotion by rememberUpdatedState(onScratchMotion)
    val latestScratchPositionChange by rememberUpdatedState(onScratchPositionChange)
    val latestScratchEnd by rememberUpdatedState(onScratchEnd)
    val latestScratchCancel by rememberUpdatedState(onScratchCancel)
    val discRunning = isPlaying && !isScratchDragging && scratchPreviewPositionMs == null
    val followStoppedDiscPosition = isScratchDragging || scratchPreviewPositionMs != null

    Box(modifier = modifier) {
        PlaybackTurntableDisc(
            albumArtwork = albumArtwork,
            mediaId = mediaId,
            positionMs = currentPositionMs,
            running = discRunning,
            followStoppedPosition = followStoppedDiscPosition,
            hidePlayerAxisEnabled = hidePlayerAxisEnabled,
            turntableWidth = turntableWidth,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .onSizeChanged { discSize = it }
                .zIndex(3f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            val center = discCenter(latestDiscSize)
                            val radius = discRadius(latestDiscSize)
                            if (isWithinDisc(offset, center, radius)) {
                                latestVisualPageToggle()
                            }
                        },
                    )
                }
                .pointerInput(scratchAvailable) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (!scratchAvailable) {
                                return@detectDragGestures
                            }
                            val center = discCenter(discSize)
                            val radius = discRadius(discSize)
                            if (!isWithinScratchRegion(offset, center, radius)) {
                                return@detectDragGestures
                            }
                            scratchActive = true
                            scratchPositionMs = latestPositionMs.coerceIn(0L, latestDurationMs)
                            lastAngleDegrees = angleDegrees(offset, center)
                            latestScratchStart()
                            latestScratchPositionChange(scratchPositionMs, 0f)
                        },
                        onDrag = { change, _ ->
                            if (!scratchActive) {
                                return@detectDragGestures
                            }
                            val center = discCenter(discSize)
                            val currentAngle = angleDegrees(change.position, center)
                            var deltaAngle = normalizeAngleDelta(currentAngle - lastAngleDegrees)
                            deltaAngle = deltaAngle.coerceIn(
                                -ScratchMaxAngleStepDegrees,
                                ScratchMaxAngleStepDegrees,
                            )
                            lastAngleDegrees = currentAngle

                            val targetPosition = (
                                scratchPositionMs + (deltaAngle / 360f) * ScratchCycleDurationMs
                            ).roundToLong().coerceIn(0L, latestDurationMs)
                            latestScratchMotion(targetPosition, deltaAngle)
                            if (targetPosition != scratchPositionMs) {
                                scratchPositionMs = targetPosition
                                latestScratchPositionChange(targetPosition, deltaAngle)
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            if (scratchActive) {
                                latestScratchEnd(scratchPositionMs)
                            }
                            scratchActive = false
                            lastAngleDegrees = Float.NaN
                        },
                        onDragCancel = {
                            if (scratchActive) {
                                latestScratchCancel()
                            }
                            scratchActive = false
                            lastAngleDegrees = Float.NaN
                        },
                    )
                },
        )
        OriginalNeedleStack(
            needleRotation = needleRotation,
            scale = scale,
            modifier = Modifier
                .matchParentSize()
                .zIndex(2f),
        )
    }
}

@Composable
private fun PlaybackLyricsPage(
    mediaId: String?,
    lyrics: EmbeddedLyrics?,
    fallbackLyricsLines: List<String>,
    currentPositionMs: Long,
    keepLyricsScreenAwake: Boolean,
    onVisualPageToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestVisualPageToggle by rememberUpdatedState(onVisualPageToggle)
    PlaybackLyricsOverlay(
        mediaId = mediaId,
        lyrics = lyrics,
        fallbackLines = fallbackLyricsLines,
        currentPositionMs = currentPositionMs,
        modifier = modifier
            .then(
                if (keepLyricsScreenAwake) {
                    Modifier.keepScreenOn()
                } else {
                    Modifier
                },
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        latestVisualPageToggle()
                    },
                )
            },
    )
}

@Composable
private fun PlaybackTurntableDisc(
    albumArtwork: ImageBitmap?,
    mediaId: String?,
    positionMs: Long,
    running: Boolean,
    followStoppedPosition: Boolean,
    hidePlayerAxisEnabled: Boolean,
    turntableWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val discRotation = rememberSmoothDiscRotation(
        mediaId = mediaId,
        positionMs = positionMs,
        running = running,
        followStoppedPosition = followStoppedPosition,
    )

    // 保留上一个非 null 封面，避免切歌时旧封面瞬间消失
    var lastArtwork by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(albumArtwork) {
        if (albumArtwork != null) lastArtwork = albumArtwork
    }
    val artworkToShow = albumArtwork ?: lastArtwork

    Box(modifier = modifier) {
        Image(
            painter = painterResource(R.drawable.playing_lp),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.matchParentSize(),
        )
        Image(
            painter = painterResource(R.drawable.playing_cover_lp),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationZ = discRotation.value
                },
        )
        if (artworkToShow != null) {
            PlaybackTurntableAlbumArt(
                artwork = artworkToShow,
                turntableWidth = turntableWidth,
                discRotation = discRotation,
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(1f),
            )
            if (!hidePlayerAxisEnabled) {
                PlaybackTurntableAxisOverlay(
                    turntableWidth = turntableWidth,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .zIndex(1.1f),
                )
            }
        }
    }
}

@Composable
private fun PlaybackTurntableAlbumArt(
    artwork: ImageBitmap,
    turntableWidth: Dp,
    discRotation: State<Float>,
    modifier: Modifier = Modifier,
) {
    Image(
        bitmap = artwork,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(turntableWidth * PlaybackAlbumArtDiameterRatio)
            .graphicsLayer {
                rotationZ = discRotation.value
            }
            .clip(CircleShape),
    )
}

@Composable
private fun PlaybackTurntableAxisOverlay(
    turntableWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val axisBitmap = ImageBitmap.imageResource(id = R.drawable.playing_lp)
    val srcLeft = (axisBitmap.width - PlaybackTurntableAxisSourceDiameterPx) / 2
    val srcTop = (axisBitmap.height - PlaybackTurntableAxisSourceDiameterPx) / 2

    Canvas(
        modifier = modifier
            .size(turntableWidth * PlaybackTurntableAxisDiameterRatio)
            .clip(CircleShape),
    ) {
        drawImage(
            image = axisBitmap,
            srcOffset = IntOffset(srcLeft, srcTop),
            srcSize = IntSize(
                PlaybackTurntableAxisSourceDiameterPx,
                PlaybackTurntableAxisSourceDiameterPx,
            ),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        )
    }
}

private data class OriginalNeedleViews(
    val base: ImageView,
    val shadow: ImageView,
    val needle: ImageView,
    val top: ImageView,
)

@Composable
private fun OriginalNeedleStack(
    needleRotation: Float,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    AndroidView(
        factory = { context ->
            FrameLayout(context).apply {
                clipChildren = false
                clipToPadding = false

                val base = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_XY
                    setBackgroundResource(R.drawable.playing_stylus_lp_bg_original)
                }
                val shadow = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_END
                    setImageResource(R.drawable.needle_shadow2)
                }
                val needle = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_XY
                    setImageResource(R.drawable.playing_stylus_lp_original)
                }
                val top = ImageView(context).apply {
                    scaleType = ImageView.ScaleType.FIT_START
                    setImageResource(R.drawable.playing_stylus_lp_top_original)
                }

                tag = OriginalNeedleViews(base, shadow, needle, top)
                addView(base)
                addView(shadow)
                addView(needle)
                addView(top)
            }
        },
        modifier = modifier,
        update = { frame ->
            val views = frame.tag as OriginalNeedleViews
            val needleWidthPx = with(density) { (73.3.dp * scale).roundToPx() }
            val needleHeightPx = with(density) { (310.dp * scale).roundToPx() }
            val needleTopWidthPx = with(density) { (41.dp * scale).roundToPx() }
            val needleTopMarginPx = with(density) { (25.5.dp * scale).roundToPx() }
            val needleRightMarginPx = with(density) { (2.5.dp * scale).roundToPx() }
            val needleShadowRightMarginPx = with(density) { (2.dp * scale).roundToPx() }

            updateOriginalNeedleLayout(
                view = views.base,
                widthPx = needleWidthPx,
                heightPx = needleHeightPx,
                topMarginPx = needleTopMarginPx,
                endMarginPx = needleRightMarginPx,
            )
            updateOriginalNeedleLayout(
                view = views.shadow,
                widthPx = needleWidthPx,
                heightPx = needleHeightPx,
                topMarginPx = needleTopMarginPx,
                endMarginPx = needleShadowRightMarginPx,
            )
            updateOriginalNeedleLayout(
                view = views.needle,
                widthPx = needleWidthPx,
                heightPx = needleHeightPx,
                topMarginPx = needleTopMarginPx,
                endMarginPx = needleRightMarginPx,
            )
            updateOriginalNeedleLayout(
                view = views.top,
                widthPx = needleTopWidthPx,
                heightPx = needleHeightPx,
                topMarginPx = needleTopMarginPx,
                endMarginPx = needleRightMarginPx,
            )

            listOf(views.shadow, views.needle).forEach { view ->
                view.pivotX = needleWidthPx * OriginalNeedlePivotX
                view.pivotY = needleHeightPx * OriginalNeedlePivotY
                view.rotation = needleRotation
            }
            views.base.rotation = 0f
            views.top.rotation = 0f
        },
    )
}

private fun updateOriginalNeedleLayout(
    view: ImageView,
    widthPx: Int,
    heightPx: Int,
    topMarginPx: Int,
    endMarginPx: Int,
) {
    val params = (view.layoutParams as? FrameLayout.LayoutParams)
        ?: FrameLayout.LayoutParams(widthPx, heightPx, Gravity.TOP or Gravity.END)
    params.width = widthPx
    params.height = heightPx
    params.gravity = Gravity.TOP or Gravity.END
    params.topMargin = topMarginPx
    params.marginEnd = endMarginPx
    params.rightMargin = endMarginPx
    view.layoutParams = params
}

@Composable
internal fun PlaybackControlButtons(
    isPlaying: Boolean,
    repeatMode: Int,
    shuffleEnabled: Boolean,
    scale: Float,
    onRepeatClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PressedDrawableButton(
            normalRes = if (repeatMode == Player.REPEAT_MODE_OFF) {
                R.drawable.btn_playing_cycle_off
            } else {
                R.drawable.btn_playing_cycle_on
            },
            pressedRes = if (repeatMode == Player.REPEAT_MODE_OFF) {
                R.drawable.btn_playing_cycle_off
            } else {
                R.drawable.btn_playing_cycle_on
            },
            contentDescription = stringResource(R.string.repeat),
            modifier = Modifier
                .width(67.3.dp * scale)
                .height(87.dp * scale),
            onClick = onRepeatClick,
        )
        PressedDrawableButton(
            normalRes = R.drawable.btn_playing_prev,
            pressedRes = R.drawable.btn_playing_prev_down,
            contentDescription = stringResource(R.string.previous_song),
            modifier = Modifier
                .width(70.dp * scale)
                .height(87.dp * scale),
            onClick = onPreviousClick,
        )
        PressedDrawableButton(
            normalRes = if (isPlaying) {
                R.drawable.btn_playing_pause
            } else {
                R.drawable.btn_playing_play
            },
            pressedRes = if (isPlaying) {
                R.drawable.btn_playing_pause_down
            } else {
                R.drawable.btn_playing_play_down
            },
            contentDescription = if (isPlaying) {
                stringResource(R.string.pause)
            } else {
                stringResource(R.string.play)
            },
            modifier = Modifier
                .width(85.3.dp * scale)
                .height(87.dp * scale),
            onClick = onPlayPauseClick,
        )
        PressedDrawableButton(
            normalRes = R.drawable.btn_playing_next,
            pressedRes = R.drawable.btn_playing_next_down,
            contentDescription = stringResource(R.string.next_song),
            modifier = Modifier
                .width(70.dp * scale)
                .height(87.dp * scale),
            onClick = onNextClick,
        )
        PressedDrawableButton(
            normalRes = if (shuffleEnabled) {
                R.drawable.btn_playing_shuffle_on
            } else {
                R.drawable.btn_playing_shuffle_off
            },
            pressedRes = if (shuffleEnabled) {
                R.drawable.btn_playing_shuffle_on
            } else {
                R.drawable.btn_playing_shuffle_off
            },
            contentDescription = stringResource(R.string.shuffle),
            modifier = Modifier
                .width(67.3.dp * scale)
                .height(87.dp * scale),
            onClick = onShuffleClick,
        )
    }
}

@Composable
internal fun PlaybackVolumeBar(
    value: Float,
    width: Dp,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit,
) {
    val density = LocalDensity.current
    val thumbOffsetPx = with(density) { PlaybackVolumeThumbOffset.roundToPx() }
    val latestOnValueChange = rememberUpdatedState(onValueChange)
    AndroidView(
        factory = { context ->
            SeekBar(context).apply {
                max = 100
                splitTrack = false
                progressDrawable = ContextCompat.getDrawable(context, R.drawable.volume_seekbar_progress)
                thumb = ContextCompat.getDrawable(context, R.drawable.playing_control_volume)
                thumbOffset = thumbOffsetPx
                setPadding(0, 0, 0, 0)
                setOnSeekBarChangeListener(
                    object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser) {
                                latestOnValueChange.value(progress / 100f)
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

                        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                    },
                )
            }
        },
        update = { seekBar ->
            val targetProgress = (value.coerceIn(0f, 1f) * 100f).roundToInt()
            if (seekBar.progress != targetProgress) {
                seekBar.progress = targetProgress
            }
            if (seekBar.thumbOffset != thumbOffsetPx) {
                seekBar.thumbOffset = thumbOffsetPx
            }
        },
        modifier = modifier
            .width(width)
            .height(PlaybackVolumeBarHeight)
    )
}

@Composable
internal fun PressedDrawableButton(
    @DrawableRes normalRes: Int,
    @DrawableRes pressedRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AndroidDrawableImage(
            drawableRes = if (pressed) pressedRes else normalRes,
            modifier = Modifier.matchParentSize(),
            contentDescription = contentDescription,
        )
    }
}

@Composable
internal fun AndroidDrawableImage(
    @DrawableRes drawableRes: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_XY
                this.contentDescription = contentDescription
            }
        },
        modifier = modifier,
        update = { imageView ->
            imageView.setImageResource(drawableRes)
            imageView.contentDescription = contentDescription
        },
    )
}

@Composable
internal fun AndroidNeedleBackground(
    @DrawableRes drawableRes: Int,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_XY
            }
        },
        modifier = modifier,
        update = { imageView ->
            imageView.setBackgroundResource(drawableRes)
        },
    )
}

@Composable
internal fun AndroidImageView(
    @DrawableRes drawableRes: Int,
    scaleType: ImageView.ScaleType,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                this.scaleType = scaleType
                this.contentDescription = contentDescription
            }
        },
        modifier = modifier,
        update = { imageView ->
            imageView.setImageResource(drawableRes)
            imageView.contentDescription = contentDescription
        },
    )
}

private fun Player?.snapshot(context: Context): PlaybackScreenState {
    val player = this ?: return PlaybackScreenState()
    return PlaybackScreenState(
        mediaItem = player.currentMediaItem,
        isPlaying = player.isPlaying,
        repeatMode = player.repeatMode,
        shuffleEnabled = player.shuffleModeEnabled,
        currentPositionMs = player.currentPosition.coerceAtLeast(0L),
        durationMs = player.duration.takeIf { it > 0L } ?: 0L,
        volume = context.musicStreamVolumeFraction(),
    )
}

private fun Player.upcomingQueueTracks(context: Context): List<PlaybackQueueTrack> {
    val startIndex = currentMediaItemIndex + 1
    if (currentMediaItemIndex < 0 || startIndex >= mediaItemCount) {
        return emptyList()
    }
    return buildList(mediaItemCount - startIndex) {
        for (index in startIndex until mediaItemCount) {
            add(getMediaItemAt(index).toPlaybackQueueTrack(context))
        }
    }
}

private fun MediaItem.toPlaybackQueueTrack(context: Context): PlaybackQueueTrack {
    return PlaybackQueueTrack(
        id = mediaId,
        title = mediaMetadata.displayTitle?.toString()
            ?: mediaMetadata.title?.toString()
            ?: context.getString(R.string.unknown_song_title),
        artist = mediaMetadata.subtitle?.toString()
            ?: mediaMetadata.artist?.toString()
            ?: context.getString(R.string.unknown_artist),
        mediaItem = this,
    )
}

@Composable
private fun rememberSmoothDiscRotation(
    mediaId: String?,
    positionMs: Long,
    running: Boolean,
    followStoppedPosition: Boolean,
): State<Float> {
    val rotation = remember {
        mutableFloatStateOf(discRotationFromPosition(positionMs))
    }

    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect

        var anchorFrameTimeNanos = Long.MIN_VALUE
        val anchorRotation = rotation.floatValue
        val degreesPerMs = DiscRotationDegrees / ScratchCycleDurationMs
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                if (anchorFrameTimeNanos == Long.MIN_VALUE) {
                    anchorFrameTimeNanos = frameTimeNanos
                }
                val elapsedMs = (frameTimeNanos - anchorFrameTimeNanos) / 1_000_000f
                rotation.floatValue = anchorRotation + elapsedMs * degreesPerMs
            }
        }
    }

    LaunchedEffect(mediaId, positionMs, followStoppedPosition, running) {
        if (followStoppedPosition && !running) {
            rotation.floatValue = discRotationFromPosition(positionMs)
        }
    }

    return rotation
}

private fun discRotationFromPosition(positionMs: Long): Float {
    val cycleMs = ScratchCycleDurationMs.toLong()
    val cyclePositionMs = positionMs.floorMod(cycleMs)
    return (cyclePositionMs.toFloat() / ScratchCycleDurationMs) * DiscRotationDegrees
}

private fun Long.floorMod(divisor: Long): Long = ((this % divisor) + divisor) % divisor

private fun Context.musicStreamVolumeFraction(): Float {
    val audioManager = getSystemService(AudioManager::class.java) ?: return 1f
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(0)
    return (currentVolume.toFloat() / maxVolume.toFloat()).coerceIn(0f, 1f)
}

private fun Context.setMusicStreamVolumeFraction(value: Float) {
    val audioManager = getSystemService(AudioManager::class.java) ?: return
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    val targetVolume = (value.coerceIn(0f, 1f) * maxVolume.toFloat())
        .roundToInt()
        .coerceIn(0, maxVolume)
    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
}

private fun Context.toast(stringRes: Int) {
    android.widget.Toast.makeText(this, getString(stringRes), android.widget.Toast.LENGTH_SHORT).show()
}

private fun Context.trySetDefaultRingtone(ringtoneUri: Uri): Boolean {
    return runCatching {
        RingtoneManager.setActualDefaultRingtoneUri(
            this,
            RingtoneManager.TYPE_RINGTONE,
            ringtoneUri,
        )
    }.isSuccess
}

private fun formatPlaybackTime(positionMs: Long): String {
    val totalSeconds = (positionMs / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

private fun fractionFromPosition(positionX: Float, trackWidthPx: Int): Float {
    if (trackWidthPx <= 0) return 0f
    return (positionX / trackWidthPx.toFloat()).coerceIn(0f, 1f)
}

private fun discCenter(size: IntSize): Offset = Offset(size.width / 2f, size.height / 2f)

private fun discRadius(size: IntSize): Float = min(size.width, size.height) / 2f

private fun isWithinDisc(
    point: Offset,
    center: Offset,
    radius: Float,
): Boolean {
    if (radius <= 0f) {
        return false
    }
    val dx = point.x - center.x
    val dy = point.y - center.y
    return sqrt((dx * dx) + (dy * dy)) <= radius
}

private fun isWithinScratchRegion(
    point: Offset,
    center: Offset,
    radius: Float,
): Boolean {
    if (radius <= 0f) {
        return false
    }
    val dx = point.x - center.x
    val dy = point.y - center.y
    val distance = sqrt((dx * dx) + (dy * dy))
    return distance in (radius * ScratchHubDeadZoneRatio)..radius
}

private fun angleDegrees(
    point: Offset,
    center: Offset,
): Float = Math.toDegrees(
    atan2(
        y = (point.y - center.y).toDouble(),
        x = (point.x - center.x).toDouble(),
    ),
).toFloat()

private fun normalizeAngleDelta(deltaDegrees: Float): Float {
    var normalized = deltaDegrees
    while (normalized > 180f) {
        normalized -= 360f
    }
    while (normalized < -180f) {
        normalized += 360f
    }
    return normalized
}
