package com.smartisanos.music.playback

import android.Manifest
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import androidx.core.content.ContextCompat
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.ListeningExecutorService
import com.google.common.util.concurrent.MoreExecutors
import com.smartisanos.music.MainActivity
import java.util.concurrent.Executors

class PlaybackService : MediaLibraryService() {

    private var player: ExoPlayer? = null
    private var mediaLibrarySession: MediaLibrarySession? = null
    private lateinit var localAudioLibrary: LocalAudioLibrary
    private lateinit var libraryExecutor: ListeningExecutorService

    override fun onCreate() {
        super.onCreate()
        localAudioLibrary = LocalAudioLibrary(this)
        libraryExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor())

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player = exoPlayer
        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            exoPlayer,
            PlaybackLibrarySessionCallback(),
        )
            .setSessionActivity(createSessionActivityPendingIntent())
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaLibrarySession? = mediaLibrarySession

    override fun onDestroy() {
        mediaLibrarySession?.release()
        mediaLibrarySession = null

        player?.release()
        player = null
        libraryExecutor.shutdown()

        super.onDestroy()
    }

    private fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun getAudioItems(): List<MediaItem> {
        if (!hasAudioPermission()) {
            return emptyList()
        }
        return localAudioLibrary.getAudioItems()
    }

    private fun createSessionActivityPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            PlaybackSessionActivityRequestCode,
            MainActivity.createOpenPlaybackIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private inner class PlaybackLibrarySessionCallback : MediaLibrarySession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS
                .buildUpon()
                .add(ScratchSeekModeCommand)
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setAvailablePlayerCommands(MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
                .build()
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return Futures.immediateFuture(
                LibraryResult.ofItem(localAudioLibrary.getRootItem(), params),
            )
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            return libraryExecutor.submit<LibraryResult<ImmutableList<MediaItem>>> {
                if (parentId != LocalAudioLibrary.ROOT_ID) {
                    return@submit LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE, params)
                }

                val items = getAudioItems()
                if (items.isEmpty() && !hasAudioPermission()) {
                    return@submit LibraryResult.ofError(
                        LibraryResult.RESULT_ERROR_PERMISSION_DENIED,
                        params,
                    )
                }

                val fromIndex = (page * pageSize).coerceAtMost(items.size)
                val toIndex = (fromIndex + pageSize).coerceAtMost(items.size)
                LibraryResult.ofItemList(items.subList(fromIndex, toIndex), params)
            }
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> {
            return libraryExecutor.submit<LibraryResult<MediaItem>> {
                if (!hasAudioPermission() && mediaId != LocalAudioLibrary.ROOT_ID) {
                    return@submit LibraryResult.ofError(LibraryResult.RESULT_ERROR_PERMISSION_DENIED)
                }

                val item = if (mediaId == LocalAudioLibrary.ROOT_ID) {
                    localAudioLibrary.getRootItem()
                } else {
                    getAudioItems().firstOrNull { it.mediaId == mediaId }
                }
                if (item == null) {
                    LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
                } else {
                    LibraryResult.ofItem(item, null)
                }
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            return libraryExecutor.submit<MutableList<MediaItem>> {
                val itemsById = getAudioItems().associateBy { it.mediaId }
                mediaItems.mapTo(mutableListOf()) { item ->
                    itemsById[item.mediaId] ?: item
                }
            }
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: androidx.media3.session.SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ScratchSeekModeAction) {
                val enabled = args.getBoolean(ScratchSeekModeEnabledKey, false)
                player?.setSeekParameters(
                    if (enabled) SeekParameters.EXACT else SeekParameters.DEFAULT,
                )
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }
    }

    private companion object {
        private const val PlaybackSessionActivityRequestCode = 1001
    }
}
