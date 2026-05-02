package com.smartisanos.music.ui.playback

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.ui.input.pointer.PointerEventPass
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
import com.smartisanos.music.data.playlist.PlaylistRepository
import com.smartisanos.music.data.settings.PlaybackSettings
import com.smartisanos.music.isExternalAudioLaunchItem
import com.smartisanos.music.playback.EmbeddedLyrics
import com.smartisanos.music.playback.LocalAudioLibrary
import com.smartisanos.music.playback.LocalPlaybackController
import com.smartisanos.music.playback.PlaybackSleepTimer
import com.smartisanos.music.playback.artworkRequestKey
import com.smartisanos.music.playback.await
import com.smartisanos.music.playback.cancelSleepTimer
import com.smartisanos.music.playback.extractEmbeddedLyrics
import com.smartisanos.music.playback.invalidateLibrary
import com.smartisanos.music.playback.loadEmbeddedLyrics
import com.smartisanos.music.playback.removeMediaItemsByMediaIds
import com.smartisanos.music.playback.setScratchSeekModeEnabled
import com.smartisanos.music.playback.startSleepTimer
import com.smartisanos.music.ui.components.loadEmbeddedArtwork
import com.smartisanos.music.ui.components.SmartisanTitleBarSurface
import com.smartisanos.music.ui.components.SmartisanTitleBarSurfaceStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private val PlaybackPageBackground = Color.White
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

private const val PlaybackDiscCycleDurationMs = 15_500f
private const val ScratchCycleDurationMs = 1_800f
private const val DiscRotationDegrees = 360f
private const val ScratchHubDeadZoneRatio = 0.06f
private const val ScratchMinMotionDegrees = 0.18f
private const val ScratchMaxDeltaTimeMs = 72L
private const val ScratchMaxAngleStepDegrees = 54f
private const val ScratchVelocityMaxDegreesPerSecond = 1_000f
private const val ScratchFlingMinVelocityDegreesPerSecond = 160f
private const val ScratchFlingReleaseTimeoutMs = 120L
private const val ScratchFlingVelocitySampleWindowMs = 120L
private const val ScratchFlingMinVelocitySampleMs = 16L
private const val ScratchFlingDurationMultiplier = 1.5f
private const val ScratchFlingPlayingRewindDurationScale = 1.2f
private const val ScratchFlingFrameDivisor = 1_700f
private const val ScratchFlingReadyFraction = 0.7f
private const val ScratchPixelFlingDivisor = 40f
private const val ScratchPlaybackVelocityDegreesPerSecond =
    DiscRotationDegrees * 1_000f / PlaybackDiscCycleDurationMs
private const val CoverPreviewTimeoutMs = 260L
private const val CoverPreviewSettleToleranceMs = 24L
private const val NeedleSeekSettleHoldTimeoutMs = 900L
private const val NeedleLiftHoldAfterReleaseMs = 250L
private const val OriginalTurntableBaseWidthDp = 360f
private const val OriginalLargeNeedleBreakpointScale = 411f / OriginalTurntableBaseWidthDp
private const val OriginalNeedleTouchStartRatio = 0.8f
private const val OriginalNeedleWidthBaseDp = 73.3f
private const val OriginalNeedleHeightBaseDp = 310f
private const val OriginalNeedleTopWidthBaseDp = 41f
private const val OriginalNeedleTopMarginBaseDp = 25.5f
private const val OriginalNeedleRightMarginDp = 2.5f
private const val OriginalNeedleShadowRightMarginDp = 2f
private const val OriginalNeedlePivotXDp = 48f
private const val OriginalNeedlePivotYDp = 28f
private const val OriginalNeedleHeightLargeDp = 354.6953f
private const val OriginalNeedleTopMarginLargeDp = 29.199982f
private const val OriginalNeedleRightMarginLargeDp = 2.7999878f
private const val OriginalNeedleShadowRightMarginLargeDp = 2.2999878f
private const val NeedleSeekOutsideActivationDistanceDp = 36f
private const val NeedleSeekStartPositionGuardMs = 1_000L
private const val NeedleRestRotationDegrees = 0f
private const val NeedlePlaybackStartRotationDegrees = 12f
private const val NeedlePlaybackSweepDegrees = 22.3f
private const val NeedlePlaybackEndRotationDegrees = NeedlePlaybackStartRotationDegrees +
    NeedlePlaybackSweepDegrees
private const val NeedleLiftScaleY = 0.98f
private const val NeedleLiftShadowRotationOffsetDegrees = 4f
private const val PlaybackAlbumArtDiameterRatio = 405f / 1080f
private const val PlaybackTurntableAxisDiameterRatio = 62f / 1080f
private const val PlaybackTurntableAxisSourceDiameterPx = 60
private val PlaybackContentHorizontalPadding = 16.dp
private val PlaybackSeekBarHeight = 48.dp
private val PlaybackSeekTimeWidth = 44.dp
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

private enum class CoverDragMode {
    None,
    DiscScratch,
    NeedleSeek,
}

private data class PlaybackCoverPageState(
    val dragMode: CoverDragMode = CoverDragMode.None,
    val previewPositionMs: Long? = null,
    val resumePlaybackAfterDrag: Boolean = false,
    val needlePreviewRotationDegrees: Float? = null,
    val needleSettlingPositionMs: Long? = null,
    val needleParkedOutside: Boolean = false,
)

@Composable
fun PlaybackScreen(
    playbackSettings: PlaybackSettings,
    onScratchEnabledChange: (Boolean) -> Unit,
    onCollapse: () -> Unit,
    onRequestAddToPlaylist: (List<MediaItem>) -> Unit = {},
    onRequestAddToQueue: (List<MediaItem>) -> Unit = {},
    onLibraryChanged: () -> Unit = {},
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val controller = LocalPlaybackController.current
    val context = LocalContext.current
    val favoriteRepository = remember(context.applicationContext) {
        FavoriteSongsRepository.getInstance(context.applicationContext)
    }
    val playlistRepository = remember(context.applicationContext) {
        PlaylistRepository.getInstance(context.applicationContext)
    }
    val favoriteIds by favoriteRepository.observeFavoriteIds().collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnLibraryChanged by rememberUpdatedState(onLibraryChanged)
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
    var pendingDeleteMediaId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var sleepTimerWasActive by remember { mutableStateOf(false) }
    var coverPageState by remember(state.mediaItem?.mediaId) {
        mutableStateOf(PlaybackCoverPageState())
    }
    var scratchFlingJob by remember { mutableStateOf<Job?>(null) }
    var discManualRotationOffsetDegrees by remember { mutableFloatStateOf(0f) }
    val sleepTimerState by PlaybackSleepTimer.state.collectAsStateWithLifecycle()

    fun clearPendingDeleteTarget() {
        pendingDeleteMediaId = null
        pendingDeleteUriString = null
    }

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
    val deleteMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val mediaId = pendingDeleteMediaId
        clearPendingDeleteTarget()
        if (result.resultCode == Activity.RESULT_OK && !mediaId.isNullOrBlank()) {
            controller.removeMediaItemsByMediaIds(setOf(mediaId))
            scope.launch {
                runCatching {
                    favoriteRepository.remove(mediaId)
                }
                runCatching {
                    playlistRepository.removeMediaIdsFromAll(setOf(mediaId))
                }
                runCatching {
                    controller?.invalidateLibrary()?.await(context)
                }
                currentOnLibraryChanged()
            }
            context.toast(R.string.playback_delete_success)
        }
    }

    fun launchDeleteRequest(target: PlaybackDeleteTarget) {
        pendingDeleteMediaId = target.mediaId
        pendingDeleteUriString = target.uri.toString()
        val request = runCatching {
            val pendingIntent = MediaStore.createDeleteRequest(
                context.contentResolver,
                listOf(target.uri),
            )
            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
        }.getOrElse {
            clearPendingDeleteTarget()
            context.toast(R.string.playback_delete_failed)
            return
        }

        runCatching {
            deleteMediaLauncher.launch(request)
        }.onFailure {
            clearPendingDeleteTarget()
            context.toast(R.string.playback_delete_failed)
        }
    }

    BackHandler {
        if (showSleepTimerDialog) {
            showSleepTimerDialog = false
        } else if (showQueueOverlay) {
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
        scratchFlingJob?.cancel()
        scratchFlingJob = null
        val shouldResumePlayback = resumePlayback && coverPageState.resumePlaybackAfterDrag
        coverPageState = PlaybackCoverPageState()
        if (shouldResumePlayback) {
            controller?.play()
        }
        controller?.setScratchSeekModeEnabled(false)
        scratchSoundController.stop()
    }

    fun finishDiscScratch(
        positionMs: Long,
        resumePlaybackAfterDrag: Boolean,
    ) {
        coverPageState = coverPageState.copy(
            dragMode = CoverDragMode.None,
            previewPositionMs = positionMs,
            resumePlaybackAfterDrag = false,
            needlePreviewRotationDegrees = null,
            needleSettlingPositionMs = null,
            needleParkedOutside = false,
        )
        controller?.seekTo(positionMs)
        if (resumePlaybackAfterDrag) {
            controller?.play()
        }
        controller?.setScratchSeekModeEnabled(false)
        scratchSoundController.stop()
    }

    fun launchDiscScratchFling(
        startPositionMs: Long,
        initialVelocityDegreesPerSecond: Float,
        resumePlaybackAfterDrag: Boolean,
        durationMs: Long,
    ) {
        scratchFlingJob?.cancel()
        scratchFlingJob = null
        val clampedVelocity = initialVelocityDegreesPerSecond
            .coerceIn(-ScratchVelocityMaxDegreesPerSecond, ScratchVelocityMaxDegreesPerSecond)
        if (abs(clampedVelocity) < ScratchFlingMinVelocityDegreesPerSecond || durationMs <= 0L) {
            finishDiscScratch(startPositionMs, resumePlaybackAfterDrag)
            return
        }

        val flingDurationMs = scratchFlingDurationMs(
            velocityDegreesPerSecond = clampedVelocity,
            resumePlaybackAfterDrag = resumePlaybackAfterDrag,
        )
        val velocityKeyframes = scratchFlingVelocityKeyframes(
            velocityDegreesPerSecond = clampedVelocity,
            resumePlaybackAfterDrag = resumePlaybackAfterDrag,
        )
        coverPageState = coverPageState.copy(
            dragMode = CoverDragMode.DiscScratch,
            previewPositionMs = startPositionMs,
            resumePlaybackAfterDrag = resumePlaybackAfterDrag,
            needlePreviewRotationDegrees = null,
            needleSettlingPositionMs = null,
            needleParkedOutside = false,
        )
        scratchFlingJob = scope.launch {
            var positionMs = startPositionMs.coerceIn(0L, durationMs)
            var previousFrameNanos = Long.MIN_VALUE
            var previousVelocity = velocityKeyframes.first()
            var elapsedMs = 0f
            var readyToEndHandled = false
            while (isActive && elapsedMs < flingDurationMs) {
                val frameNanos = withFrameNanos { it }
                if (previousFrameNanos == Long.MIN_VALUE) {
                    previousFrameNanos = frameNanos
                    continue
                }
                val frameDeltaMs = ((frameNanos - previousFrameNanos) / 1_000_000f)
                    .coerceIn(1f, ScratchMaxDeltaTimeMs.toFloat())
                previousFrameNanos = frameNanos
                elapsedMs = (elapsedMs + frameDeltaMs).coerceAtMost(flingDurationMs.toFloat())

                val currentVelocity = scratchFlingVelocityAt(
                    keyframes = velocityKeyframes,
                    elapsedMs = elapsedMs,
                    durationMs = flingDurationMs,
                )
                val deltaAngle = ((previousVelocity + currentVelocity) * frameDeltaMs) /
                    ScratchFlingFrameDivisor
                previousVelocity = currentVelocity

                if (abs(deltaAngle) >= ScratchMinMotionDegrees) {
                    discManualRotationOffsetDegrees += deltaAngle
                    val targetPosition = scratchPositionAfterAngle(
                        positionMs = positionMs,
                        deltaAngleDegrees = deltaAngle,
                        durationMs = durationMs,
                    )
                    scratchSoundController.onScratchMotion(targetPosition, deltaAngle)
                    if (targetPosition != positionMs) {
                        positionMs = targetPosition
                        coverPageState = coverPageState.copy(previewPositionMs = positionMs)
                    }
                }

                if (!readyToEndHandled && elapsedMs > flingDurationMs * ScratchFlingReadyFraction) {
                    controller?.seekTo(positionMs)
                    readyToEndHandled = true
                }
            }
            finishDiscScratch(positionMs, resumePlaybackAfterDrag)
            scratchFlingJob = null
        }
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
            scratchFlingJob?.cancel()
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
        coverPageState.dragMode,
    ) {
        if (
            !playbackSettings.popcornSoundEnabled ||
            !state.isPlaying ||
            coverPageState.dragMode != CoverDragMode.None
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
    val currentMediaItem = state.mediaItem
    val currentMediaId = currentMediaItem?.mediaId
    val currentIsExternalAudio = currentMediaItem?.isExternalAudioLaunchItem() == true
    val favoriteEnabled = !currentIsExternalAudio &&
        !currentMediaId.isNullOrBlank() &&
        currentMediaId in favoriteIds
    val coverPreviewPositionMs = coverPageState.previewPositionMs
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
    val artworkRequestKey = state.mediaItem?.artworkRequestKey()
    val albumArtwork by produceState<ImageBitmap?>(
        initialValue = null,
        artworkRequestKey,
    ) {
        value = state.mediaItem?.let { mediaItem ->
            loadEmbeddedArtwork(context, mediaItem)
        }
    }

    LaunchedEffect(
        currentVisualPage,
        coverPageState.dragMode,
        coverPageState.previewPositionMs,
        state.currentPositionMs,
    ) {
        if (currentVisualPage != PlaybackVisualPage.Cover) {
            return@LaunchedEffect
        }
        val previewPosition = coverPageState.previewPositionMs ?: return@LaunchedEffect
        if (
            coverPageState.dragMode == CoverDragMode.None &&
            abs(state.currentPositionMs - previewPosition) <= CoverPreviewSettleToleranceMs
        ) {
            coverPageState = coverPageState.copy(previewPositionMs = null)
        }
    }

    LaunchedEffect(
        currentVisualPage,
        coverPageState.dragMode,
        coverPageState.previewPositionMs,
    ) {
        if (currentVisualPage != PlaybackVisualPage.Cover) {
            return@LaunchedEffect
        }
        val previewPosition = coverPageState.previewPositionMs ?: return@LaunchedEffect
        if (coverPageState.dragMode != CoverDragMode.None) {
            return@LaunchedEffect
        }
        delay(CoverPreviewTimeoutMs)
        if (
            coverPageState.dragMode == CoverDragMode.None &&
            coverPageState.previewPositionMs == previewPosition
        ) {
            coverPageState = coverPageState.copy(previewPositionMs = null)
        }
    }

    LaunchedEffect(
        currentVisualPage,
        coverPageState.dragMode,
        coverPageState.needleSettlingPositionMs,
        state.currentPositionMs,
    ) {
        if (currentVisualPage != PlaybackVisualPage.Cover) {
            return@LaunchedEffect
        }
        val settlingPosition = coverPageState.needleSettlingPositionMs ?: return@LaunchedEffect
        if (
            coverPageState.dragMode == CoverDragMode.None &&
            abs(state.currentPositionMs - settlingPosition) <= CoverPreviewSettleToleranceMs
        ) {
            coverPageState = coverPageState.copy(
                needlePreviewRotationDegrees = null,
                needleSettlingPositionMs = null,
            )
        }
    }

    LaunchedEffect(
        currentVisualPage,
        coverPageState.dragMode,
        coverPageState.needleSettlingPositionMs,
    ) {
        if (currentVisualPage != PlaybackVisualPage.Cover) {
            return@LaunchedEffect
        }
        val settlingPosition = coverPageState.needleSettlingPositionMs ?: return@LaunchedEffect
        if (coverPageState.dragMode != CoverDragMode.None) {
            return@LaunchedEffect
        }
        delay(NeedleSeekSettleHoldTimeoutMs)
        if (
            coverPageState.dragMode == CoverDragMode.None &&
            coverPageState.needleSettlingPositionMs == settlingPosition
        ) {
            coverPageState = coverPageState.copy(
                needlePreviewRotationDegrees = null,
                needleSettlingPositionMs = null,
            )
        }
    }

    LaunchedEffect(scratchSourceUri) {
        scratchFlingJob?.cancel()
        scratchFlingJob = null
        scratchSoundController.stop()
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
        val turntableWidth = maxWidth
        val bottomControlsWidth = minOf(
            (maxWidth - PlaybackContentHorizontalPadding - PlaybackContentHorizontalPadding)
                .coerceAtLeast(0.dp),
            OriginalTurntableBaseWidthDp.dp,
        )
        val scale = turntableWidth.value / OriginalTurntableBaseWidthDp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .consumePlaybackTouchFallthrough(),
        )
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            if (showTopBar) {
                PlaybackTopBar(
                    title = title,
                    artist = artist,
                    topInset = topInset,
                    onQueueClick = { showQueueOverlay = true },
                    onCollapse = onCollapse,
                )
            }
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
                    coverDragMode = coverPageState.dragMode,
                    previewPositionMs = coverPageState.previewPositionMs,
                    needlePreviewRotationDegrees = coverPageState.needlePreviewRotationDegrees,
                    needleParkedOutside = coverPageState.needleParkedOutside,
                    discManualRotationOffsetDegrees = discManualRotationOffsetDegrees,
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
                    onDiscScratchStart = {
                        scratchFlingJob?.cancel()
                        scratchFlingJob = null
                        val resumePlaybackAfterDrag = state.isPlaying ||
                            coverPageState.resumePlaybackAfterDrag
                        coverPageState = coverPageState.copy(
                            dragMode = CoverDragMode.DiscScratch,
                            previewPositionMs = livePositionMs,
                            resumePlaybackAfterDrag = resumePlaybackAfterDrag,
                            needlePreviewRotationDegrees = null,
                            needleSettlingPositionMs = null,
                            needleParkedOutside = false,
                        )
                        if (resumePlaybackAfterDrag) {
                            controller?.pause()
                        }
                        controller?.setScratchSeekModeEnabled(true)
                        scratchSoundController.onScratchStart(
                            sourceUri = scratchSourceUri,
                            positionMs = livePositionMs,
                        )
                    },
                    onDiscScratchMotion = { positionMs, deltaAngle ->
                        discManualRotationOffsetDegrees += deltaAngle
                        scratchSoundController.onScratchMotion(positionMs, deltaAngle)
                    },
                    onDiscScratchPositionChange = { positionMs, _ ->
                        coverPageState = coverPageState.copy(
                            previewPositionMs = positionMs,
                        )
                    },
                    onDiscScratchEnd = { positionMs, flingVelocityDegreesPerSecond ->
                        val resumePlaybackAfterDrag = coverPageState.resumePlaybackAfterDrag
                        launchDiscScratchFling(
                            startPositionMs = positionMs,
                            initialVelocityDegreesPerSecond = flingVelocityDegreesPerSecond,
                            resumePlaybackAfterDrag = resumePlaybackAfterDrag,
                            durationMs = durationMs,
                        )
                    },
                    onDiscScratchCancel = {
                        resetCoverPageInteraction(resumePlayback = true)
                    },
                    onNeedleSeekStart = { rotationDegrees, positionMs ->
                        LegacyPlaybackHaptics.vibrateEffect(context)
                        val resumePlaybackAfterDrag = state.isPlaying
                        coverPageState = coverPageState.copy(
                            dragMode = CoverDragMode.NeedleSeek,
                            previewPositionMs = positionMs ?: 0L,
                            resumePlaybackAfterDrag = resumePlaybackAfterDrag,
                            needlePreviewRotationDegrees = rotationDegrees,
                            needleSettlingPositionMs = null,
                            needleParkedOutside = false,
                        )
                        if (resumePlaybackAfterDrag) {
                            controller?.pause()
                        }
                        controller?.setScratchSeekModeEnabled(true)
                    },
                    onNeedleSeekPositionChange = { rotationDegrees, positionMs ->
                        coverPageState = coverPageState.copy(
                            previewPositionMs = positionMs ?: 0L,
                            needlePreviewRotationDegrees = rotationDegrees,
                            needleSettlingPositionMs = null,
                        )
                    },
                    onNeedleSeekEnd = { rotationDegrees, positionMs ->
                        LegacyPlaybackHaptics.vibrateEffect(context)
                        if (positionMs == null) {
                            coverPageState = coverPageState.copy(
                                dragMode = CoverDragMode.None,
                                previewPositionMs = 0L,
                                resumePlaybackAfterDrag = false,
                                needlePreviewRotationDegrees = null,
                                needleSettlingPositionMs = null,
                                needleParkedOutside = true,
                            )
                            controller?.seekTo(0L)
                            controller?.pause()
                        } else {
                            coverPageState = coverPageState.copy(
                                dragMode = CoverDragMode.None,
                                previewPositionMs = positionMs,
                                resumePlaybackAfterDrag = false,
                                needlePreviewRotationDegrees = rotationDegrees,
                                needleSettlingPositionMs = positionMs,
                                needleParkedOutside = false,
                            )
                            controller?.seekTo(positionMs)
                            controller?.play()
                        }
                        controller?.setScratchSeekModeEnabled(false)
                        scratchSoundController.stop()
                    },
                    onNeedleSeekCancel = {
                        LegacyPlaybackHaptics.vibrateEffect(context)
                        resetCoverPageInteraction(resumePlayback = true)
                    },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            PlaybackBottomControls(
                width = bottomControlsWidth,
                bottomInset = bottomInset,
                state = state,
                onRepeatClick = {
                    val nextRepeatMode = nextPlaybackRepeatMode(state.repeatMode)
                    controller?.repeatMode = nextRepeatMode
                    context.toast(repeatToastRes(nextRepeatMode))
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
                    val shuffleEnabled = !state.shuffleEnabled
                    controller?.shuffleModeEnabled = shuffleEnabled
                    context.toast(shuffleToastRes(shuffleEnabled))
                },
                onVolumeChange = { volume ->
                    context.setMusicStreamVolumeFraction(volume)
                    state = state.copy(volume = context.musicStreamVolumeFraction())
                },
            )
        }

        LegacyPlaybackMoreActionsOverlay(
            visible = showMorePanel,
            favoriteEnabled = favoriteEnabled,
            visualPage = currentVisualPage,
            scratchEnabled = playbackSettings.scratchEnabled,
            sleepTimerActive = sleepTimerState.isActive,
            addToPlaylistEnabled = !currentIsExternalAudio,
            callbacks = LegacyPlaybackMoreActionCallbacks(
                onAddToPlaylistClick = {
                    state.mediaItem?.let { onRequestAddToPlaylist(listOf(it)) }
                    showMorePanel = false
                },
                onAddToQueueClick = {
                    state.mediaItem?.let { onRequestAddToQueue(listOf(it)) }
                    showMorePanel = false
                },
                onFavoriteToggle = {
                    val mediaId = currentMediaId
                    if (!mediaId.isNullOrBlank() && !currentIsExternalAudio) {
                        scope.launch {
                            favoriteRepository.toggle(mediaId)
                        }
                    }
                    showMorePanel = false
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
                    when (val result = state.mediaItem?.resolveDeleteTarget()
                        ?: PlaybackDeleteTargetResult.Unavailable) {
                        is PlaybackDeleteTargetResult.Available -> {
                            launchDeleteRequest(result.target)
                        }
                        PlaybackDeleteTargetResult.CueFile -> {
                            context.toast(R.string.can_not_delete_cue_file)
                        }
                        PlaybackDeleteTargetResult.Unavailable -> {
                            context.toast(R.string.can_not_delete_song)
                        }
                    }
                    showMorePanel = false
                },
                onDismissRequest = {
                    showMorePanel = false
                },
            ),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(8f),
        )

        LegacyPlaybackSleepTimerDialog(
            visible = showSleepTimerDialog,
            state = sleepTimerState,
            bottomInsetPx = (bottomInset.value * density.density).roundToInt(),
            onDismissRequest = {
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
                    if (currentIsExternalAudio) {
                        return@PlaybackQueueScreen
                    }
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
                .height(PlaybackSeekBarHeight)
                .padding(horizontal = PlaybackContentHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatPlaybackTime(shownPosition),
                style = PlaybackTimeStyle.copy(textAlign = TextAlign.Start),
                maxLines = 1,
                modifier = Modifier.width(PlaybackSeekTimeWidth),
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
                modifier = Modifier.width(PlaybackSeekTimeWidth),
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
    coverDragMode: CoverDragMode,
    previewPositionMs: Long?,
    needlePreviewRotationDegrees: Float?,
    needleParkedOutside: Boolean,
    discManualRotationOffsetDegrees: Float,
    mediaId: String?,
    onMoreClick: () -> Unit,
    onVisualPageToggle: () -> Unit,
    onKeepLyricsScreenAwakeToggle: () -> Unit,
    onDiscScratchStart: () -> Unit,
    onDiscScratchMotion: (Long, Float) -> Unit,
    onDiscScratchPositionChange: (Long, Float) -> Unit,
    onDiscScratchEnd: (Long, Float) -> Unit,
    onDiscScratchCancel: () -> Unit,
    onNeedleSeekStart: (Float, Long?) -> Unit,
    onNeedleSeekPositionChange: (Float, Long?) -> Unit,
    onNeedleSeekEnd: (Float, Long?) -> Unit,
    onNeedleSeekCancel: () -> Unit,
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
                    coverDragMode = coverDragMode,
                    previewPositionMs = previewPositionMs,
                    needlePreviewRotationDegrees = needlePreviewRotationDegrees,
                    needleParkedOutside = needleParkedOutside,
                    discManualRotationOffsetDegrees = discManualRotationOffsetDegrees,
                    mediaId = mediaId,
                    onVisualPageToggle = onVisualPageToggle,
                    onDiscScratchStart = onDiscScratchStart,
                    onDiscScratchMotion = onDiscScratchMotion,
                    onDiscScratchPositionChange = onDiscScratchPositionChange,
                    onDiscScratchEnd = onDiscScratchEnd,
                    onDiscScratchCancel = onDiscScratchCancel,
                    onNeedleSeekStart = onNeedleSeekStart,
                    onNeedleSeekPositionChange = onNeedleSeekPositionChange,
                    onNeedleSeekEnd = onNeedleSeekEnd,
                    onNeedleSeekCancel = onNeedleSeekCancel,
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
    coverDragMode: CoverDragMode,
    previewPositionMs: Long?,
    needlePreviewRotationDegrees: Float?,
    needleParkedOutside: Boolean,
    discManualRotationOffsetDegrees: Float,
    mediaId: String?,
    onVisualPageToggle: () -> Unit,
    onDiscScratchStart: () -> Unit,
    onDiscScratchMotion: (Long, Float) -> Unit,
    onDiscScratchPositionChange: (Long, Float) -> Unit,
    onDiscScratchEnd: (Long, Float) -> Unit,
    onDiscScratchCancel: () -> Unit,
    onNeedleSeekStart: (Float, Long?) -> Unit,
    onNeedleSeekPositionChange: (Float, Long?) -> Unit,
    onNeedleSeekEnd: (Float, Long?) -> Unit,
    onNeedleSeekCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = durationMs
        .takeIf { it > 0L }
        ?.let { currentPositionMs.toFloat() / it.toFloat() }
        ?.coerceIn(0f, 1f)
        ?: 0f
    val targetNeedleRotation = needlePreviewRotationDegrees ?: run {
        if (hasMediaItem && !(needleParkedOutside && !isPlaying)) {
            NeedlePlaybackStartRotationDegrees + (progress * NeedlePlaybackSweepDegrees)
        } else {
            NeedleRestRotationDegrees
        }
    }

    val needleAnimatable = remember { Animatable(targetNeedleRotation) }
    var prevMediaId by remember { mutableStateOf(mediaId) }
    val needleSeekDragging = coverDragMode == CoverDragMode.NeedleSeek
    var needleLiftHeldAfterSeek by remember { mutableStateOf(false) }

    LaunchedEffect(needleSeekDragging, needleParkedOutside) {
        if (needleSeekDragging) {
            needleLiftHeldAfterSeek = true
            return@LaunchedEffect
        }
        if (needleLiftHeldAfterSeek && !needleParkedOutside) {
            delay(NeedleLiftHoldAfterReleaseMs)
        }
        needleLiftHeldAfterSeek = false
    }

    val needleLiftFraction by animateFloatAsState(
        targetValue = if (needleSeekDragging || needleLiftHeldAfterSeek) 1f else 0f,
        animationSpec = tween(if (needleSeekDragging || needleLiftHeldAfterSeek) 125 else 250),
        label = "needleLiftFraction",
    )

    LaunchedEffect(mediaId, targetNeedleRotation, needleSeekDragging) {
        if (prevMediaId != mediaId) {
            prevMediaId = mediaId
            return@LaunchedEffect
        }
        if (needleSeekDragging) {
            needleAnimatable.snapTo(targetNeedleRotation)
        } else {
            needleAnimatable.animateTo(targetNeedleRotation, animationSpec = tween(220))
        }
    }

    val needleRotation = needleAnimatable.value
    val density = LocalDensity.current
    val densityPxPerDp = density.density
    var discSize by remember { mutableStateOf(IntSize.Zero) }
    val latestDiscSize by rememberUpdatedState(discSize)
    val latestVisualPageToggle by rememberUpdatedState(onVisualPageToggle)
    val scratchAvailable by rememberUpdatedState(scratchEnabled && durationMs > 0L)
    val needleSeekAvailable by rememberUpdatedState(scratchEnabled && hasMediaItem && durationMs > 0L)
    val latestNeedleRotation by rememberUpdatedState(needleRotation)
    val latestPositionMs by rememberUpdatedState(currentPositionMs)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestDiscScratchStart by rememberUpdatedState(onDiscScratchStart)
    val latestDiscScratchMotion by rememberUpdatedState(onDiscScratchMotion)
    val latestDiscScratchPositionChange by rememberUpdatedState(onDiscScratchPositionChange)
    val latestDiscScratchEnd by rememberUpdatedState(onDiscScratchEnd)
    val latestDiscScratchCancel by rememberUpdatedState(onDiscScratchCancel)
    val latestNeedleSeekStart by rememberUpdatedState(onNeedleSeekStart)
    val latestNeedleSeekPositionChange by rememberUpdatedState(onNeedleSeekPositionChange)
    val latestNeedleSeekEnd by rememberUpdatedState(onNeedleSeekEnd)
    val latestNeedleSeekCancel by rememberUpdatedState(onNeedleSeekCancel)
    val discRunning = isPlaying &&
        coverDragMode == CoverDragMode.None &&
        previewPositionMs == null
    val discRotationCycleDurationMs = if (coverDragMode == CoverDragMode.DiscScratch) {
        ScratchCycleDurationMs
    } else {
        PlaybackDiscCycleDurationMs
    }

    Box(modifier = modifier) {
        PlaybackTurntableDisc(
            albumArtwork = albumArtwork,
            running = discRunning,
            rotationCycleDurationMs = discRotationCycleDurationMs,
            manualRotationOffsetDegrees = discManualRotationOffsetDegrees,
            hidePlayerAxisEnabled = hidePlayerAxisEnabled,
            turntableWidth = turntableWidth,
            modifier = Modifier.matchParentSize(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .onSizeChanged { discSize = it }
                .zIndex(3f)
                .pointerInput(scratchAvailable, needleSeekAvailable, densityPxPerDp, scale) {
                    val tapTouchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val size = latestDiscSize
                        val center = discCenter(size)
                        val radius = discRadius(size)
                        val withinNeedleSeekRegion =
                            needleSeekAvailable && isWithinNeedleSeekRegion(
                                point = down.position,
                                containerSize = size,
                                densityPxPerDp = densityPxPerDp,
                                turntableScale = scale,
                                rotationDegrees = latestNeedleRotation,
                            )
                        val withinScratchRegion =
                            scratchAvailable && isWithinScratchRegion(down.position, center, radius)
                        val dragMode = when {
                            withinNeedleSeekRegion -> CoverDragMode.NeedleSeek
                            withinScratchRegion -> {
                                CoverDragMode.DiscScratch
                            }
                            else -> CoverDragMode.None
                        }

                        when (dragMode) {
                            CoverDragMode.None -> {
                                val initialPosition = down.position
                                var finalPosition = initialPosition
                                var maxMoveDistance = 0f
                                val pointerId = down.id
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId }
                                        ?: break
                                    finalPosition = change.position
                                    val moveDistance = distanceBetween(initialPosition, finalPosition)
                                    if (moveDistance > maxMoveDistance) {
                                        maxMoveDistance = moveDistance
                                    }
                                    if (!change.pressed) {
                                        if (
                                            maxMoveDistance <= tapTouchSlop &&
                                            isWithinDisc(initialPosition, center, radius) &&
                                            isWithinDisc(finalPosition, center, radius)
                                        ) {
                                            latestVisualPageToggle()
                                        }
                                        break
                                    }
                                }
                            }
                            CoverDragMode.DiscScratch -> {
                                val initialPosition = down.position
                                var finalPosition = initialPosition
                                var maxMoveDistance = 0f
                                var scratchPositionMs = 0L
                                var lastAngleDegrees = angleDegrees(initialPosition, center)
                                var lastMotionUptimeMs = down.uptimeMillis
                                var lastAngularVelocityDegreesPerSecond = 0f
                                var lastScratchDirection = 0
                                val scratchMotionSamples = ArrayDeque<ScratchMotionSample>()
                                val pointerId = down.id
                                var scratchStarted = false
                                var cancelled = true
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId }
                                    if (change == null) {
                                        break
                                    }
                                    finalPosition = change.position
                                    maxMoveDistance = max(
                                        maxMoveDistance,
                                        distanceBetween(initialPosition, finalPosition),
                                    )
                                    if (!change.pressed) {
                                        if (scratchStarted) {
                                            latestDiscScratchEnd(
                                                scratchPositionMs,
                                                scratchReleaseVelocityDegreesPerSecond(
                                                    angularVelocityDegreesPerSecond =
                                                        lastAngularVelocityDegreesPerSecond,
                                                    pixelFlingVelocityDegreesPerSecond =
                                                        scratchPixelFlingVelocityDegreesPerSecond(
                                                            samples = scratchMotionSamples,
                                                            releasePosition = change.position,
                                                            releaseUptimeMs = change.uptimeMillis,
                                                        ),
                                                    releaseDelayMs = change.uptimeMillis - lastMotionUptimeMs,
                                                    directionHint = lastScratchDirection,
                                                ),
                                            )
                                            change.consume()
                                        } else if (
                                            isDiscTapWithinSlop(
                                                initialPosition = initialPosition,
                                                finalPosition = finalPosition,
                                                maxMoveDistance = maxMoveDistance,
                                                center = center,
                                                radius = radius,
                                                tapTouchSlop = tapTouchSlop,
                                            )
                                        ) {
                                            latestVisualPageToggle()
                                        }
                                        cancelled = false
                                        break
                                    }
                                    if (!scratchStarted) {
                                        if (maxMoveDistance <= tapTouchSlop) {
                                            continue
                                        }
                                        scratchStarted = true
                                        scratchPositionMs = scratchStartPosition(
                                            positionMs = latestPositionMs,
                                            durationMs = latestDurationMs,
                                        )
                                        lastAngleDegrees = angleDegrees(change.position, center)
                                        lastMotionUptimeMs = change.uptimeMillis
                                        lastAngularVelocityDegreesPerSecond = 0f
                                        lastScratchDirection = 0
                                        scratchMotionSamples.clear()
                                        recordScratchMotionSample(
                                            samples = scratchMotionSamples,
                                            position = change.position,
                                            uptimeMs = change.uptimeMillis,
                                        )
                                        latestDiscScratchStart()
                                        latestDiscScratchPositionChange(scratchPositionMs, 0f)
                                        change.consume()
                                        continue
                                    }

                                    val currentAngle = angleDegrees(change.position, center)
                                    val deltaAngle = normalizeAngleDelta(currentAngle - lastAngleDegrees)
                                        .coerceIn(
                                            -ScratchMaxAngleStepDegrees,
                                            ScratchMaxAngleStepDegrees,
                                        )
                                    lastAngleDegrees = currentAngle
                                    val deltaTimeMs = (change.uptimeMillis - lastMotionUptimeMs)
                                        .coerceAtLeast(1L)
                                    lastAngularVelocityDegreesPerSecond = (
                                        deltaAngle * 1_000f / deltaTimeMs.toFloat()
                                    ).coerceIn(
                                        -ScratchVelocityMaxDegreesPerSecond,
                                        ScratchVelocityMaxDegreesPerSecond,
                                    )
                                    if (deltaAngle != 0f) {
                                        lastScratchDirection = if (deltaAngle < 0f) -1 else 1
                                    }
                                    lastMotionUptimeMs = change.uptimeMillis
                                    recordScratchMotionSample(
                                        samples = scratchMotionSamples,
                                        position = change.position,
                                        uptimeMs = change.uptimeMillis,
                                    )

                                    val targetPosition = scratchPositionAfterAngle(
                                        positionMs = scratchPositionMs,
                                        deltaAngleDegrees = deltaAngle,
                                        durationMs = latestDurationMs,
                                    )
                                    latestDiscScratchMotion(targetPosition, deltaAngle)
                                    if (targetPosition != scratchPositionMs) {
                                        scratchPositionMs = targetPosition
                                        latestDiscScratchPositionChange(targetPosition, deltaAngle)
                                    }
                                    change.consume()
                                }
                                if (cancelled && scratchStarted) {
                                    latestDiscScratchCancel()
                                }
                            }
                            CoverDragMode.NeedleSeek -> {
                                val initialPosition = down.position
                                var maxMoveDistance = 0f
                                var needleRotationDegrees = latestNeedleRotation
                                    .coerceIn(NeedleRestRotationDegrees, NeedlePlaybackEndRotationDegrees)
                                var needlePositionMs = needleSeekPositionFromRotation(
                                    rotationDegrees = needleRotationDegrees,
                                    durationMs = latestDurationMs,
                                )
                                var needleSeekHadPlayablePosition = needlePositionMs != null
                                var needlePivot = playbackNeedleGeometry(
                                    containerSize = size,
                                    densityPxPerDp = densityPxPerDp,
                                    turntableScale = scale,
                                    rotationDegrees = needleRotationDegrees,
                                ).pivot
                                var lastNeedleAngleDegrees = angleDegrees(initialPosition, needlePivot)
                                val pointerId = down.id
                                var needleSeekStarted = false
                                var cancelled = true
                                val outsideActivationDistancePx =
                                    NeedleSeekOutsideActivationDistanceDp * densityPxPerDp
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == pointerId }
                                    if (change == null) {
                                        break
                                    }
                                    maxMoveDistance = max(
                                        maxMoveDistance,
                                        distanceBetween(initialPosition, change.position),
                                    )
                                    if (!change.pressed) {
                                        if (needleSeekStarted) {
                                            if (needleSeekHadPlayablePosition || needlePositionMs != null) {
                                                latestNeedleSeekEnd(needleRotationDegrees, needlePositionMs)
                                            } else {
                                                latestNeedleSeekCancel()
                                            }
                                            change.consume()
                                        }
                                        cancelled = false
                                        break
                                    }
                                    if (!needleSeekStarted) {
                                        if (maxMoveDistance <= tapTouchSlop) {
                                            continue
                                        }
                                        val candidateNeedleAngleDegrees =
                                            angleDegrees(change.position, needlePivot)
                                        val candidateDeltaNeedleAngle = normalizeAngleDelta(
                                            candidateNeedleAngleDegrees - lastNeedleAngleDegrees,
                                        ).coerceIn(
                                            -ScratchMaxAngleStepDegrees,
                                            ScratchMaxAngleStepDegrees,
                                        )
                                        val candidateNeedleRotationDegrees =
                                            (needleRotationDegrees + candidateDeltaNeedleAngle)
                                                .coerceIn(
                                                    NeedleRestRotationDegrees,
                                                    NeedlePlaybackEndRotationDegrees,
                                                )
                                        val candidateNeedlePositionMs = needleSeekPositionFromRotation(
                                            rotationDegrees = candidateNeedleRotationDegrees,
                                            durationMs = latestDurationMs,
                                        )
                                        if (
                                            !shouldStartNeedleSeekDrag(
                                                initialPositionMs = needlePositionMs,
                                                candidatePositionMs = candidateNeedlePositionMs,
                                                maxMoveDistance = maxMoveDistance,
                                                outsideActivationDistancePx =
                                                    outsideActivationDistancePx,
                                            )
                                        ) {
                                            continue
                                        }
                                        needleSeekStarted = true
                                        needlePivot = playbackNeedleGeometry(
                                            containerSize = size,
                                            densityPxPerDp = densityPxPerDp,
                                            turntableScale = scale,
                                            rotationDegrees = candidateNeedleRotationDegrees,
                                        ).pivot
                                        needleRotationDegrees = candidateNeedleRotationDegrees
                                        needlePositionMs = candidateNeedlePositionMs
                                        if (needlePositionMs != null) {
                                            needleSeekHadPlayablePosition = true
                                        }
                                        lastNeedleAngleDegrees = angleDegrees(change.position, needlePivot)
                                        latestNeedleSeekStart(needleRotationDegrees, needlePositionMs)
                                        change.consume()
                                        continue
                                    }

                                    val currentNeedleAngleDegrees = angleDegrees(change.position, needlePivot)
                                    val deltaNeedleAngle = normalizeAngleDelta(
                                        currentNeedleAngleDegrees - lastNeedleAngleDegrees,
                                    ).coerceIn(
                                        -ScratchMaxAngleStepDegrees,
                                        ScratchMaxAngleStepDegrees,
                                    )
                                    lastNeedleAngleDegrees = currentNeedleAngleDegrees
                                    needleRotationDegrees = (needleRotationDegrees + deltaNeedleAngle)
                                        .coerceIn(
                                            NeedleRestRotationDegrees,
                                            NeedlePlaybackEndRotationDegrees,
                                        )
                                    needlePositionMs = needleSeekPositionFromRotation(
                                        rotationDegrees = needleRotationDegrees,
                                        durationMs = latestDurationMs,
                                    )
                                    if (needlePositionMs != null) {
                                        needleSeekHadPlayablePosition = true
                                    }
                                    latestNeedleSeekPositionChange(needleRotationDegrees, needlePositionMs)
                                    change.consume()
                                }
                                if (cancelled && needleSeekStarted) {
                                    latestNeedleSeekCancel()
                                }
                            }
                        }
                    }
                },
        )
        OriginalNeedleStack(
            needleRotation = needleRotation,
            needleLiftFraction = needleLiftFraction,
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
    running: Boolean,
    rotationCycleDurationMs: Float,
    manualRotationOffsetDegrees: Float,
    hidePlayerAxisEnabled: Boolean,
    turntableWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val discRotation = rememberSmoothDiscRotation(
        running = running,
        cycleDurationMs = rotationCycleDurationMs,
        manualRotationOffsetDegrees = manualRotationOffsetDegrees,
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

private data class OriginalNeedleMetrics(
    val widthDp: Float,
    val heightDp: Float,
    val topWidthDp: Float,
    val topMarginDp: Float,
    val rightMarginDp: Float,
    val shadowRightMarginDp: Float,
    val pivotXDp: Float,
    val pivotYDp: Float,
)

private val OriginalNeedleBaseMetrics = OriginalNeedleMetrics(
    widthDp = OriginalNeedleWidthBaseDp,
    heightDp = OriginalNeedleHeightBaseDp,
    topWidthDp = OriginalNeedleTopWidthBaseDp,
    topMarginDp = OriginalNeedleTopMarginBaseDp,
    rightMarginDp = OriginalNeedleRightMarginDp,
    shadowRightMarginDp = OriginalNeedleShadowRightMarginDp,
    pivotXDp = OriginalNeedlePivotXDp,
    pivotYDp = OriginalNeedlePivotYDp,
)

private val OriginalNeedleLargeMetrics = OriginalNeedleMetrics(
    widthDp = OriginalNeedleWidthBaseDp,
    heightDp = OriginalNeedleHeightLargeDp,
    topWidthDp = OriginalNeedleTopWidthBaseDp,
    topMarginDp = OriginalNeedleTopMarginLargeDp,
    rightMarginDp = OriginalNeedleRightMarginLargeDp,
    shadowRightMarginDp = OriginalNeedleShadowRightMarginLargeDp,
    pivotXDp = OriginalNeedlePivotXDp,
    pivotYDp = OriginalNeedlePivotYDp,
)

private fun originalNeedleMetrics(turntableScale: Float): OriginalNeedleMetrics =
    if (turntableScale >= OriginalLargeNeedleBreakpointScale) {
        OriginalNeedleLargeMetrics
    } else {
        OriginalNeedleBaseMetrics
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
    needleLiftFraction: Float,
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
            val metrics = originalNeedleMetrics(scale)
            val needleWidthPx = with(density) { metrics.widthDp.dp.roundToPx() }
            val needleHeightPx = with(density) { metrics.heightDp.dp.roundToPx() }
            val needleTopWidthPx = with(density) { metrics.topWidthDp.dp.roundToPx() }
            val needleTopMarginPx = with(density) { metrics.topMarginDp.dp.roundToPx() }
            val needleRightMarginPx = with(density) { metrics.rightMarginDp.dp.roundToPx() }
            val needleShadowRightMarginPx = with(density) {
                metrics.shadowRightMarginDp.dp.roundToPx()
            }
            val needlePivotXPx = with(density) { metrics.pivotXDp.dp.toPx() }
            val needlePivotYPx = with(density) { metrics.pivotYDp.dp.toPx() }

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

            val liftedScaleY = 1f - ((1f - NeedleLiftScaleY) * needleLiftFraction)
            val shadowRotation = needleRotation -
                (NeedleLiftShadowRotationOffsetDegrees * needleLiftFraction)
            listOf(views.shadow, views.needle).forEach { view ->
                view.pivotX = needlePivotXPx
                view.pivotY = needlePivotYPx
                view.scaleY = liftedScaleY
            }
            views.shadow.rotation = shadowRotation
            views.needle.rotation = needleRotation
            views.base.rotation = 0f
            views.base.scaleY = 1f
            views.top.rotation = 0f
            views.top.scaleY = 1f
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
        val repeatIconRes = playbackRepeatButtonRes(repeatMode)
        PressedDrawableButton(
            normalRes = repeatIconRes,
            pressedRes = repeatIconRes,
            contentDescription = stringResource(repeatContentDescriptionRes(repeatMode)),
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
                contentDescription = context.getString(R.string.volume)
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

private data class PlaybackDeleteTarget(
    val mediaId: String,
    val uri: Uri,
)

private sealed interface PlaybackDeleteTargetResult {
    data class Available(val target: PlaybackDeleteTarget) : PlaybackDeleteTargetResult
    object CueFile : PlaybackDeleteTargetResult
    object Unavailable : PlaybackDeleteTargetResult
}

private fun MediaItem.resolveDeleteTarget(): PlaybackDeleteTargetResult {
    val targetMediaId = mediaId.trim()
    if (targetMediaId.isEmpty()) {
        return PlaybackDeleteTargetResult.Unavailable
    }
    val audioQualityBadge = mediaMetadata.extras
        ?.getString(LocalAudioLibrary.AudioQualityBadgeExtraKey)
    if (audioQualityBadge == LocalAudioLibrary.AudioQualityBadgeCue) {
        return PlaybackDeleteTargetResult.CueFile
    }
    val deleteUri = localConfiguration?.uri
        ?.takeIf(Uri::isMediaStoreUri)
        ?: targetMediaId.toLongOrNull()?.let { id ->
            ContentUris.withAppendedId(
                MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                id,
            )
        }
        ?: return PlaybackDeleteTargetResult.Unavailable
    return PlaybackDeleteTargetResult.Available(
        PlaybackDeleteTarget(
            mediaId = targetMediaId,
            uri = deleteUri,
        ),
    )
}

private fun Uri.isMediaStoreUri(): Boolean {
    return scheme == ContentResolver.SCHEME_CONTENT && authority == MediaStore.AUTHORITY
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
    running: Boolean,
    cycleDurationMs: Float,
    manualRotationOffsetDegrees: Float,
): State<Float> {
    val rotation = remember {
        mutableFloatStateOf(0f)
    }
    var lastManualRotationOffsetDegrees by remember {
        mutableFloatStateOf(manualRotationOffsetDegrees)
    }

    LaunchedEffect(running, cycleDurationMs) {
        if (!running) return@LaunchedEffect

        var anchorFrameTimeNanos = Long.MIN_VALUE
        val anchorRotation = rotation.floatValue
        val degreesPerMs = DiscRotationDegrees / cycleDurationMs
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

    LaunchedEffect(manualRotationOffsetDegrees) {
        val deltaDegrees = manualRotationOffsetDegrees - lastManualRotationOffsetDegrees
        lastManualRotationOffsetDegrees = manualRotationOffsetDegrees
        if (deltaDegrees != 0f) {
            rotation.floatValue += deltaDegrees
        }
    }

    return rotation
}

internal fun nextPlaybackRepeatMode(repeatMode: Int): Int {
    return when (repeatMode) {
        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
        else -> Player.REPEAT_MODE_OFF
    }
}

@DrawableRes
internal fun playbackRepeatButtonRes(repeatMode: Int): Int {
    return when (repeatMode) {
        Player.REPEAT_MODE_ONE -> R.drawable.btn_playing_repeat_on
        Player.REPEAT_MODE_ALL -> R.drawable.btn_playing_cycle_on
        else -> R.drawable.btn_playing_cycle_off
    }
}

internal fun repeatContentDescriptionRes(repeatMode: Int): Int {
    return when (repeatMode) {
        Player.REPEAT_MODE_ONE -> R.string.repeat_single
        Player.REPEAT_MODE_ALL -> R.string.repeat_all
        else -> R.string.repeat_none
    }
}

internal fun repeatToastRes(repeatMode: Int): Int = repeatContentDescriptionRes(repeatMode)

internal fun shuffleToastRes(shuffleEnabled: Boolean): Int {
    return if (shuffleEnabled) {
        R.string.shuffle_on
    } else {
        R.string.shuffle_off
    }
}

private fun Modifier.consumePlaybackTouchFallthrough(): Modifier = pointerInput(Unit) {
    // 播放页叠在 legacy 主壳上，空白区域也要消费触摸，避免点到后方歌曲列表。
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Final)
            event.changes.forEach { change ->
                if (change.pressed || change.previousPressed) {
                    change.consume()
                }
            }
        }
    }
}

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

internal fun isDiscTapWithinSlop(
    initialPosition: Offset,
    finalPosition: Offset,
    maxMoveDistance: Float,
    center: Offset,
    radius: Float,
    tapTouchSlop: Float,
): Boolean {
    return maxMoveDistance <= tapTouchSlop &&
        isWithinDisc(initialPosition, center, radius) &&
        isWithinDisc(finalPosition, center, radius)
}

internal fun scratchStartPosition(
    positionMs: Long,
    durationMs: Long,
): Long = positionMs.coerceIn(0L, durationMs)

private fun scratchReleaseVelocityDegreesPerSecond(
    angularVelocityDegreesPerSecond: Float,
    pixelFlingVelocityDegreesPerSecond: Float,
    releaseDelayMs: Long,
    directionHint: Int,
): Float {
    val recentAngularVelocity = if (releaseDelayMs <= ScratchFlingReleaseTimeoutMs) {
        angularVelocityDegreesPerSecond
    } else {
        0f
    }
    val direction = when {
        recentAngularVelocity < 0f -> -1
        recentAngularVelocity > 0f -> 1
        directionHint < 0 -> -1
        directionHint > 0 -> 1
        else -> 0
    }
    if (direction == 0) {
        return 0f
    }
    val blendedVelocity = (
        abs(recentAngularVelocity) +
            pixelFlingVelocityDegreesPerSecond.coerceAtLeast(0f)
    ) / 2f
    if (blendedVelocity < ScratchFlingMinVelocityDegreesPerSecond) {
        return 0f
    }
    val signedVelocity = blendedVelocity * direction
    return signedVelocity.coerceIn(
        -ScratchVelocityMaxDegreesPerSecond,
        ScratchVelocityMaxDegreesPerSecond,
    )
}

private data class ScratchMotionSample(
    val position: Offset,
    val uptimeMs: Long,
)

private fun recordScratchMotionSample(
    samples: ArrayDeque<ScratchMotionSample>,
    position: Offset,
    uptimeMs: Long,
) {
    samples.addLast(ScratchMotionSample(position, uptimeMs))
    while (
        samples.size > 2 &&
        uptimeMs - samples.first().uptimeMs > ScratchFlingVelocitySampleWindowMs
    ) {
        samples.removeFirst()
    }
}

private fun scratchPixelFlingVelocityDegreesPerSecond(
    samples: ArrayDeque<ScratchMotionSample>,
    releasePosition: Offset,
    releaseUptimeMs: Long,
): Float {
    val sample = samples.firstOrNull {
        val ageMs = releaseUptimeMs - it.uptimeMs
        ageMs in ScratchFlingMinVelocitySampleMs..ScratchFlingVelocitySampleWindowMs
    } ?: samples.lastOrNull {
        releaseUptimeMs - it.uptimeMs >= ScratchFlingMinVelocitySampleMs
    } ?: return 0f

    val deltaTimeMs = (releaseUptimeMs - sample.uptimeMs).coerceAtLeast(1L).toFloat()
    val velocityX = (releasePosition.x - sample.position.x) * 1_000f / deltaTimeMs
    val velocityY = (releasePosition.y - sample.position.y) * 1_000f / deltaTimeMs
    return ((abs(velocityX) + abs(velocityY)) / ScratchPixelFlingDivisor)
        .coerceIn(0f, ScratchVelocityMaxDegreesPerSecond)
}

private fun scratchFlingDurationMs(
    velocityDegreesPerSecond: Float,
    resumePlaybackAfterDrag: Boolean,
): Long {
    var duration = abs(velocityDegreesPerSecond) * ScratchFlingDurationMultiplier
    if (velocityDegreesPerSecond < 0f && resumePlaybackAfterDrag) {
        duration *= ScratchFlingPlayingRewindDurationScale
    }
    return duration.roundToLong().coerceAtLeast(1L)
}

private fun scratchFlingVelocityKeyframes(
    velocityDegreesPerSecond: Float,
    resumePlaybackAfterDrag: Boolean,
): FloatArray {
    return when {
        velocityDegreesPerSecond < 0f && resumePlaybackAfterDrag -> floatArrayOf(
            velocityDegreesPerSecond,
            (2f * velocityDegreesPerSecond) / 3f,
            velocityDegreesPerSecond / 3f,
            0f,
            -velocityDegreesPerSecond * 0.2f,
        )
        velocityDegreesPerSecond < 0f -> floatArrayOf(velocityDegreesPerSecond, 0f)
        else -> floatArrayOf(velocityDegreesPerSecond, ScratchPlaybackVelocityDegreesPerSecond)
    }
}

private fun scratchFlingVelocityAt(
    keyframes: FloatArray,
    elapsedMs: Float,
    durationMs: Long,
): Float {
    if (keyframes.isEmpty()) {
        return 0f
    }
    if (keyframes.size == 1 || durationMs <= 0L) {
        return keyframes.last()
    }
    val scaledFraction = (elapsedMs / durationMs.toFloat())
        .coerceIn(0f, 1f) * (keyframes.size - 1)
    val startIndex = scaledFraction.toInt().coerceAtMost(keyframes.size - 2)
    val segmentFraction = scaledFraction - startIndex
    return keyframes[startIndex] +
        ((keyframes[startIndex + 1] - keyframes[startIndex]) * segmentFraction)
}

private fun scratchPositionAfterAngle(
    positionMs: Long,
    deltaAngleDegrees: Float,
    durationMs: Long,
): Long {
    return (
        positionMs + (deltaAngleDegrees / DiscRotationDegrees) * ScratchCycleDurationMs
    ).roundToLong().coerceIn(0L, durationMs)
}

internal data class PlaybackNeedleGeometry(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val pivotLocal: Offset,
    val pivot: Offset,
)

@Suppress("UNUSED_PARAMETER")
internal fun playbackNeedleGeometry(
    containerSize: IntSize,
    densityPxPerDp: Float,
    turntableScale: Float,
    rotationDegrees: Float,
): PlaybackNeedleGeometry {
    val metrics = originalNeedleMetrics(turntableScale)
    val needleWidthPx = metrics.widthDp * densityPxPerDp
    val needleHeightPx = metrics.heightDp * densityPxPerDp
    val needleLeftPx = containerSize.width -
        (metrics.rightMarginDp * densityPxPerDp) -
        needleWidthPx
    val needleTopPx = metrics.topMarginDp * densityPxPerDp
    val pivotLocal = Offset(
        x = metrics.pivotXDp * densityPxPerDp,
        y = metrics.pivotYDp * densityPxPerDp,
    )
    val pivot = Offset(
        x = needleLeftPx + pivotLocal.x,
        y = needleTopPx + pivotLocal.y,
    )
    return PlaybackNeedleGeometry(
        left = needleLeftPx,
        top = needleTopPx,
        width = needleWidthPx,
        height = needleHeightPx,
        pivotLocal = pivotLocal,
        pivot = pivot,
    )
}

internal fun isWithinNeedleSeekRegion(
    point: Offset,
    containerSize: IntSize,
    densityPxPerDp: Float,
    turntableScale: Float,
    rotationDegrees: Float,
): Boolean {
    if (containerSize.width <= 0 || containerSize.height <= 0 || densityPxPerDp <= 0f) {
        return false
    }
    val geometry = playbackNeedleGeometry(
        containerSize = containerSize,
        densityPxPerDp = densityPxPerDp,
        turntableScale = turntableScale,
        rotationDegrees = rotationDegrees,
    )
    val localPoint = needleLocalPoint(
        point = point,
        geometry = geometry,
        rotationDegrees = rotationDegrees,
    )
    return localPoint.x >= 0f &&
        localPoint.x <= geometry.width &&
        localPoint.y >= geometry.height * OriginalNeedleTouchStartRatio &&
        localPoint.y <= geometry.height
}

internal fun needleSeekRotationFromPoint(
    point: Offset,
    containerSize: IntSize,
    densityPxPerDp: Float,
    turntableScale: Float,
): Float {
    if (containerSize.width <= 0 || containerSize.height <= 0 || densityPxPerDp <= 0f) {
        return NeedleRestRotationDegrees
    }
    val neutralGeometry = playbackNeedleGeometry(
        containerSize = containerSize,
        densityPxPerDp = densityPxPerDp,
        turntableScale = turntableScale,
        rotationDegrees = 0f,
    )
    val neutralAngle = angleDegrees(
        point = Offset(
            x = neutralGeometry.left + (neutralGeometry.width / 2f),
            y = neutralGeometry.top + (neutralGeometry.height * OriginalNeedleTouchStartRatio),
        ),
        center = neutralGeometry.pivot,
    )
    val pointAngle = angleDegrees(point, neutralGeometry.pivot)
    return normalizeAngleDelta(pointAngle - neutralAngle)
        .coerceIn(NeedleRestRotationDegrees, NeedlePlaybackEndRotationDegrees)
}

internal fun needleLocalPoint(
    point: Offset,
    geometry: PlaybackNeedleGeometry,
    rotationDegrees: Float,
): Offset {
    val unrotatedOffset = rotateOffset(
        offset = Offset(
            x = point.x - geometry.pivot.x,
            y = point.y - geometry.pivot.y,
        ),
        rotationDegrees = -rotationDegrees,
    )
    return Offset(
        x = geometry.pivotLocal.x + unrotatedOffset.x,
        y = geometry.pivotLocal.y + unrotatedOffset.y,
    )
}

internal fun needleSeekPositionFromRotation(
    rotationDegrees: Float,
    durationMs: Long,
): Long? {
    if (durationMs <= 0L || rotationDegrees < NeedlePlaybackStartRotationDegrees) {
        return null
    }
    val fraction = (
        (rotationDegrees - NeedlePlaybackStartRotationDegrees) /
            NeedlePlaybackSweepDegrees
    ).coerceIn(0f, 1f)
    return (durationMs.toFloat() * fraction)
        .roundToLong()
        .coerceIn(0L, durationMs)
}

internal fun shouldStartNeedleSeekDrag(
    initialPositionMs: Long?,
    candidatePositionMs: Long?,
    maxMoveDistance: Float,
    outsideActivationDistancePx: Float,
): Boolean {
    if (initialPositionMs == null) {
        return true
    }
    val movingToOutsideOrStart = candidatePositionMs == null ||
        candidatePositionMs <= NeedleSeekStartPositionGuardMs
    return !movingToOutsideOrStart || maxMoveDistance >= outsideActivationDistancePx
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

internal fun normalizeAngleDelta(deltaDegrees: Float): Float {
    var normalized = deltaDegrees
    while (normalized > 180f) {
        normalized -= 360f
    }
    while (normalized < -180f) {
        normalized += 360f
    }
    return normalized
}

private fun rotateOffset(
    offset: Offset,
    rotationDegrees: Float,
): Offset {
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val cos = cos(radians).toFloat()
    val sin = sin(radians).toFloat()
    return Offset(
        x = (offset.x * cos) - (offset.y * sin),
        y = (offset.x * sin) + (offset.y * cos),
    )
}

private fun distanceBetween(
    first: Offset,
    second: Offset,
): Float {
    val dx = first.x - second.x
    val dy = first.y - second.y
    return sqrt((dx * dx) + (dy * dy))
}
