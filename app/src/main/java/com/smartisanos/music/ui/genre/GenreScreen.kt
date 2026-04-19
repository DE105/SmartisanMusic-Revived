package com.smartisanos.music.ui.genre

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.data.genre.GenreTagRepository
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.await
import com.smartisanos.music.ui.components.SecondaryPageTransition
import com.smartisanos.music.ui.components.SmartisanBlankState
import com.smartisanos.music.ui.components.audioPermission
import com.smartisanos.music.ui.components.hasAudioPermission
import kotlin.math.roundToInt

private val GenrePageBackground = Color.White
private val GenreListRowBackground = Color(0xFFFDFDFD)
private val GenreListRowPressedBackground = Color(0xFFF1F1F1)
private val GenreRowDivider = Color(0xFFE9E9E9)
private val GenreTitleColor = Color(0xC7000000)
private val GenreSubtitleColor = Color(0x73000000)
private val GenrePlayingColor = Color(0xFFE64040)
private val GenreDetailHeaderBackground = Color(0xFFF6F6F6)
private val GenreShuffleTextColor = Color(0x99000000)
private val GenreShufflePressedTextColor = Color.White

private val GenreTitleStyle = TextStyle(
    fontSize = 17.sp,
    fontWeight = FontWeight.Medium,
    color = GenreTitleColor,
)
private val GenreSubtitleStyle = TextStyle(
    fontSize = 13.sp,
    color = GenreSubtitleColor,
)

private val GenreRowHeight = 61.dp
private val GenreRowHorizontalPadding = 14.dp
private val GenreSongRowHorizontalPadding = 16.dp
private val GenreShuffleHeaderHeight = 59.dp
private val GenreShuffleButtonHeight = 45.dp
private val GenreShuffleButtonHorizontalPadding = 10.dp
private val GenreShuffleIconWidth = 16.dp
private val GenreShuffleIconHeight = 45.dp
private val GenreShuffleIconTextSpacing = 4.dp
private val MiniPlayerReservedHeight = 73.dp

@Composable
fun GenreScreen(
    selectedGenreId: String?,
    onGenreSelected: (String, String) -> Unit,
    onGenreBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playbackBrowser = LocalPlaybackBrowser.current
    val exclusionsStore = remember(context.applicationContext) {
        LibraryExclusionsStore(context.applicationContext)
    }
    val genreTagRepository = remember(context.applicationContext) {
        GenreTagRepository(context.applicationContext)
    }
    val libraryRevision by exclusionsStore.revision.collectAsState(initial = 0)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }
    var permissionVersion by remember { mutableIntStateOf(0) }
    var songs by remember(playbackBrowser) { mutableStateOf(emptyList<MediaItem>()) }
    var genreMap by remember(playbackBrowser) { mutableStateOf<Map<String, String?>?>(emptyMap()) }
    var currentMediaId by remember(playbackBrowser) {
        mutableStateOf(playbackBrowser?.currentMediaItem?.mediaId)
    }
    val hasPermission = hasAudioPermission(context)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionVersion++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(playbackBrowser) {
        val browser = playbackBrowser ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                currentMediaId = player.currentMediaItem?.mediaId
            }
        }
        browser.addListener(listener)
        currentMediaId = browser.currentMediaItem?.mediaId
        onDispose {
            browser.removeListener(listener)
        }
    }

    LaunchedEffect(playbackBrowser, permissionVersion, libraryRevision, hasPermission) {
        val browser = playbackBrowser ?: run {
            songs = emptyList()
            return@LaunchedEffect
        }
        if (!hasPermission) {
            songs = emptyList()
            return@LaunchedEffect
        }

        val rootResult = browser.getLibraryRoot(null).await(context)
        val rootItem = rootResult.value ?: run {
            songs = emptyList()
            return@LaunchedEffect
        }
        val childrenResult = browser.getChildren(rootItem.mediaId, 0, Int.MAX_VALUE, null).await(context)
        songs = childrenResult.value?.toList().orEmpty()
    }

    LaunchedEffect(hasPermission, songs, genreTagRepository) {
        if (!hasPermission) {
            genreMap = emptyMap()
            return@LaunchedEffect
        }
        if (songs.isEmpty()) {
            genreMap = emptyMap()
            return@LaunchedEffect
        }

        genreMap = null
        genreMap = genreTagRepository.loadGenres(songs)
    }

    if (!hasPermission) {
        ResetGenreDetailIfNeeded(
            shouldReset = shouldResetGenreDetailSelection(
                selectedGenreId = selectedGenreId,
                hasPermission = false,
                hasSongs = songs.isNotEmpty(),
                hasGenres = null,
            ),
            onGenreBack = onGenreBack,
        )
        GenrePermissionState(
            modifier = modifier,
            onGrantPermission = {
                permissionLauncher.launch(audioPermission())
            },
            onOpenSettings = {
                context.openAppSettings()
            },
        )
        return
    }

    if (songs.isEmpty()) {
        ResetGenreDetailIfNeeded(
            shouldReset = shouldResetGenreDetailSelection(
                selectedGenreId = selectedGenreId,
                hasPermission = true,
                hasSongs = false,
                hasGenres = null,
            ),
            onGenreBack = onGenreBack,
        )
        SmartisanBlankState(
            iconRes = R.drawable.blank_style,
            title = stringResource(R.string.no_style),
            subtitle = stringResource(R.string.show_style),
            modifier = modifier,
        )
        return
    }

    val resolvedGenreMap = genreMap
    if (resolvedGenreMap == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(GenrePageBackground),
        )
        return
    }

    val unknownGenreTitle = stringResource(R.string.unknown_style)
    val unknownArtistTitle = stringResource(R.string.unknown_artist)
    val unknownAlbumTitle = stringResource(R.string.unknown_album)
    val genres = remember(songs, resolvedGenreMap, unknownGenreTitle) {
        buildGenreSummaries(
            mediaItems = songs,
            genreMap = resolvedGenreMap,
            unknownGenreTitle = unknownGenreTitle,
        )
    }

    if (genres.isEmpty()) {
        ResetGenreDetailIfNeeded(
            shouldReset = shouldResetGenreDetailSelection(
                selectedGenreId = selectedGenreId,
                hasPermission = true,
                hasSongs = true,
                hasGenres = false,
            ),
            onGenreBack = onGenreBack,
        )
        SmartisanBlankState(
            iconRes = R.drawable.blank_style,
            title = stringResource(R.string.no_style),
            subtitle = stringResource(R.string.show_style),
            modifier = modifier,
        )
        return
    }

    val genreSelectionAvailable = isGenreSelectionAvailable(
        selectedGenreId = selectedGenreId,
        genres = genres,
    )
    ResetGenreDetailIfNeeded(
        shouldReset = selectedGenreId != null && !genreSelectionAvailable,
        onGenreBack = onGenreBack,
    )
    val effectiveSelectedGenreId = selectedGenreId.takeIf { genreSelectionAvailable }

    SecondaryPageTransition(
        secondaryKey = effectiveSelectedGenreId,
        modifier = modifier.fillMaxSize(),
        label = "genre detail page",
        primaryContent = {
            GenreOverview(
                genres = genres,
                currentMediaId = currentMediaId,
                onGenreSelected = onGenreSelected,
                modifier = Modifier.fillMaxSize(),
            )
        },
        secondaryContent = { targetGenreId ->
            val genre = genres.firstOrNull { it.id == targetGenreId }
            if (genre != null) {
                GenreDetail(
                    genre = genre,
                    currentMediaId = currentMediaId,
                    unknownArtistTitle = unknownArtistTitle,
                    unknownAlbumTitle = unknownAlbumTitle,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        },
    )
}

@Composable
private fun ResetGenreDetailIfNeeded(
    shouldReset: Boolean,
    onGenreBack: () -> Unit,
) {
    LaunchedEffect(shouldReset) {
        if (shouldReset) {
            onGenreBack()
        }
    }
}

@Composable
private fun GenrePermissionState(
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SmartisanBlankState(
                iconRes = R.drawable.blank_style,
                title = stringResource(R.string.audio_permission_title),
                subtitle = stringResource(R.string.audio_permission_subtitle),
            )
            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = onGrantPermission) {
                    Text(text = stringResource(R.string.audio_permission_action))
                }
                Button(onClick = onOpenSettings) {
                    Text(text = stringResource(R.string.audio_permission_settings))
                }
            }
        }
    }
}

@Composable
private fun GenreOverview(
    genres: List<GenreSummary>,
    currentMediaId: String?,
    onGenreSelected: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GenrePageBackground),
        contentPadding = PaddingValues(bottom = MiniPlayerReservedHeight),
    ) {
        items(
            items = genres,
            key = { it.id },
        ) { genre ->
            GenreOverviewRow(
                genre = genre,
                highlighted = genre.songs.any { it.mediaId == currentMediaId },
                onClick = {
                    onGenreSelected(genre.id, genre.name)
                },
            )
        }
    }
}

@Composable
private fun GenreOverviewRow(
    genre: GenreSummary,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (pressed) GenreListRowPressedBackground else GenreListRowBackground,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(GenreRowHeight)
                .padding(horizontal = GenreRowHorizontalPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = genre.name,
                style = GenreTitleStyle,
                color = if (highlighted) GenrePlayingColor else GenreTitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.album_track_count, genre.trackCount),
                style = GenreSubtitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GenreRowDivider),
        )
    }
}

@Composable
private fun GenreDetail(
    genre: GenreSummary,
    currentMediaId: String?,
    unknownArtistTitle: String,
    unknownAlbumTitle: String,
    modifier: Modifier = Modifier,
) {
    val playbackBrowser = LocalPlaybackBrowser.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(GenrePageBackground),
        contentPadding = PaddingValues(bottom = MiniPlayerReservedHeight),
    ) {
        item(key = "${genre.id}:shuffle") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GenreShuffleHeaderHeight)
                    .background(GenreDetailHeaderBackground)
                    .padding(horizontal = GenreShuffleButtonHorizontalPadding, vertical = 7.dp),
            ) {
                GenreShuffleButton(
                    modifier = Modifier.fillMaxSize(),
                    onClick = {
                        val shuffled = genre.songs.shuffled()
                        playbackBrowser?.setMediaItems(shuffled, 0, 0L)
                        playbackBrowser?.prepare()
                        playbackBrowser?.play()
                    },
                )
            }
        }
        itemsIndexed(
            items = genre.songs,
            key = { _, item -> item.mediaId },
        ) { index, item ->
            GenreSongRow(
                mediaItem = item,
                subtitle = item.genreSongSubtitle(
                    unknownArtistTitle = unknownArtistTitle,
                    unknownAlbumTitle = unknownAlbumTitle,
                ),
                highlighted = item.mediaId == currentMediaId,
                onClick = {
                    playbackBrowser?.setMediaItems(genre.songs, index, 0L)
                    playbackBrowser?.prepare()
                    playbackBrowser?.play()
                },
            )
        }
    }
}

@Composable
private fun GenreShuffleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .height(GenreShuffleButtonHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val drawable = context.getDrawable(
                if (pressed) R.drawable.btn_folder_shuffle_down else R.drawable.btn_folder_shuffle,
            ) ?: return@Canvas
            drawable.setBounds(0, 0, size.width.roundToInt(), size.height.roundToInt())
            drawIntoCanvas { canvas ->
                drawable.draw(canvas.nativeCanvas)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(
                    if (pressed) R.drawable.btn_shuffle2_down else R.drawable.btn_shuffle2,
                ),
                contentDescription = null,
                modifier = Modifier
                    .width(GenreShuffleIconWidth)
                    .height(GenreShuffleIconHeight),
            )
            Spacer(modifier = Modifier.width(GenreShuffleIconTextSpacing))
            Text(
                text = stringResource(R.string.play_shuffle),
                style = GenreTitleStyle.copy(
                    fontSize = 16.sp,
                    color = if (pressed) GenreShufflePressedTextColor else GenreShuffleTextColor,
                ),
            )
        }
    }
}

@Composable
private fun GenreSongRow(
    mediaItem: MediaItem,
    subtitle: String,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val title = mediaItem.mediaMetadata.displayTitle ?: mediaItem.mediaMetadata.title

    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (pressed) GenreListRowPressedBackground else GenreListRowBackground,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(GenreRowHeight)
                .padding(horizontal = GenreSongRowHorizontalPadding),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title?.toString().orEmpty(),
                style = GenreTitleStyle,
                color = if (highlighted) GenrePlayingColor else GenreTitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = GenreSubtitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GenreRowDivider),
        )
    }
}

private fun MediaItem.genreSongSubtitle(
    unknownArtistTitle: String,
    unknownAlbumTitle: String,
): String {
    val metadata = mediaMetadata
    val artist = metadata.artist?.toString()?.trim()?.takeIf(String::isNotEmpty) ?: unknownArtistTitle
    val album = metadata.albumTitle?.toString()?.trim()?.takeIf(String::isNotEmpty) ?: unknownAlbumTitle
    return "$artist · $album"
}

private fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        ),
    )
}

internal fun shouldResetGenreDetailSelection(
    selectedGenreId: String?,
    hasPermission: Boolean,
    hasSongs: Boolean,
    hasGenres: Boolean?,
): Boolean {
    if (selectedGenreId == null) {
        return false
    }
    if (!hasPermission) {
        return true
    }
    if (!hasSongs) {
        return true
    }
    return hasGenres == false
}

internal fun isGenreSelectionAvailable(
    selectedGenreId: String?,
    genres: List<GenreSummary>,
): Boolean {
    if (selectedGenreId == null) {
        return true
    }
    return genres.any { it.id == selectedGenreId }
}
