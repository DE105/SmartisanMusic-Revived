package com.smartisanos.music.ui.folder

import android.graphics.drawable.AnimationDrawable
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.data.library.LibraryExclusions
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.playback.LocalAudioLibrary
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.ui.components.SecondaryPageTransition
import com.smartisanos.music.ui.components.SmartisanBlankState
import com.smartisanos.music.ui.components.audioPermission
import com.smartisanos.music.ui.components.hasAudioPermission
import kotlinx.coroutines.launch

private val FolderPageBackground = Color.White
private val FolderListDivider = Color(0xFFE9E9E9)
private val FolderTitleColor = Color(0xC7000000)
private val FolderSubtitleColor = Color(0x73000000)
private val FolderHiddenTitleColor = Color(0x66000000)
private val FolderHiddenSubtitleColor = Color(0x4D000000)
private val FolderPressedBackground = Color(0x0A000000)
private val FolderSelectedColor = Color(0xFFE64040)

private val FolderTitleStyle = TextStyle(
    fontSize = 21.sp,
    fontWeight = FontWeight.Medium,
    color = FolderTitleColor,
)
private val FolderSubtitleStyle = TextStyle(
    fontSize = 13.sp,
    color = FolderSubtitleColor,
)

private val FolderRowHeight = 72.dp
private val FolderRowHorizontalPadding = 16.dp
private val FolderEditCheckboxSize = 26.dp
private val FolderEditCheckboxStroke = 1.5.dp
private val FolderEditCheckboxPaddingEnd = 14.dp
private val FolderArrowSize = 20.dp
private val FolderEyeSize = 28.dp
private val MiniPlayerReservedHeight = 73.dp
private const val EyeToggleAnimationMillis = 180L

private const val StorageLabel = "Phone Storage"

@Composable
fun FolderScreen(
    editMode: Boolean,
    selectedDirectoryKey: String?,
    onDirectorySelected: (String, String) -> Unit,
    onDirectoryBack: () -> Unit,
    onEditSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }
    var permissionVersion by remember { mutableIntStateOf(0) }
    val hasPermission = hasAudioPermission(context)

    val audioLibrary = remember(context.applicationContext) {
        LocalAudioLibrary(context.applicationContext)
    }
    val exclusionsStore = remember(context.applicationContext) {
        LibraryExclusionsStore(context.applicationContext)
    }
    val exclusions by exclusionsStore.exclusions.collectAsState(initial = LibraryExclusions())

    var mediaItems by remember(audioLibrary) { mutableStateOf(emptyList<MediaItem>()) }

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

    LaunchedEffect(audioLibrary, permissionVersion, hasPermission) {
        if (hasPermission) {
            mediaItems = audioLibrary.getAudioItems()
        } else {
            mediaItems = emptyList()
        }
    }

    if (!hasPermission) {
        FolderPermissionState(
            modifier = modifier,
            onGrantPermission = {
                permissionLauncher.launch(audioPermission())
            },
            onOpenSettings = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null),
                    ),
                )
            },
        )
        return
    }

    val allDirectories = remember(mediaItems, exclusions) {
        buildDirectoryEntries(
            mediaItems = mediaItems,
            exclusions = exclusions,
            storageLabel = StorageLabel,
        )
    }
    val directories = remember(allDirectories, editMode) {
        filterDirectoryEntriesForDisplay(
            entries = allDirectories,
            editMode = editMode,
        )
    }

    BackHandler(enabled = selectedDirectoryKey != null, onBack = onDirectoryBack)
    SecondaryPageTransition(
        secondaryKey = selectedDirectoryKey,
        modifier = modifier.fillMaxSize(),
        label = "folder detail page",
        primaryContent = {
            FolderOverview(
                directories = directories,
                exclusionsStore = exclusionsStore,
                editMode = editMode,
                onDirectorySelected = onDirectorySelected,
                onEditSelectionChanged = onEditSelectionChanged,
                modifier = Modifier.fillMaxSize(),
            )
        },
        secondaryContent = { directoryKey ->
            FolderDetail(
                directoryKey = directoryKey,
                mediaItems = mediaItems,
                exclusions = exclusions,
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Composable
private fun FolderPermissionState(
    onGrantPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SmartisanBlankState(
                iconRes = R.drawable.blank_folder,
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
private fun FolderOverview(
    directories: List<DirectoryEntry>,
    exclusionsStore: LibraryExclusionsStore,
    editMode: Boolean,
    onDirectorySelected: (String, String) -> Unit,
    onEditSelectionChanged: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(editMode) {
        if (!editMode && selectedKeys.isNotEmpty()) {
            selectedKeys = emptySet()
            onEditSelectionChanged(emptySet())
        }
    }

    LaunchedEffect(directories, editMode) {
        if (!editMode) {
            return@LaunchedEffect
        }
        val available = directories.asSequence().map { it.key }.toSet()
        val trimmed = selectedKeys.intersect(available)
        if (trimmed.size != selectedKeys.size) {
            selectedKeys = trimmed
            onEditSelectionChanged(trimmed)
        }
    }

    if (directories.isEmpty()) {
        SmartisanBlankState(
            iconRes = R.drawable.blank_folder,
            title = stringResource(R.string.no_folder),
            subtitle = stringResource(R.string.show_folder),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FolderPageBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            bottom = MiniPlayerReservedHeight,
        ),
    ) {
        itemsIndexed(
            items = directories,
            key = { _, entry -> entry.key },
        ) { _, entry ->
            val selected = selectedKeys.contains(entry.key)
            DirectoryRow(
                entry = entry,
                editMode = editMode,
                selected = selected,
                onClick = {
                    if (editMode) {
                        val updated = if (selected) {
                            selectedKeys - entry.key
                        } else {
                            selectedKeys + entry.key
                        }
                        selectedKeys = updated
                        onEditSelectionChanged(updated)
                    } else {
                        onDirectorySelected(entry.key, entry.name)
                    }
                },
                onSelectionToggle = {
                    val updated = if (selected) {
                        selectedKeys - entry.key
                    } else {
                        selectedKeys + entry.key
                    }
                    selectedKeys = updated
                    onEditSelectionChanged(updated)
                },
                onVisibilityToggle = {
                    scope.launch {
                        exclusionsStore.setDirectoryKeysHidden(
                            directoryKeys = setOf(entry.key),
                            hidden = !entry.hidden,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun FolderDetail(
    directoryKey: String,
    mediaItems: List<MediaItem>,
    exclusions: LibraryExclusions,
    modifier: Modifier = Modifier,
) {
    val playbackBrowser = LocalPlaybackBrowser.current
    var currentMediaId by remember(playbackBrowser) {
        mutableStateOf(playbackBrowser?.currentMediaItem?.mediaId)
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

    val directorySongs = remember(mediaItems, exclusions, directoryKey) {
        filterMediaItemsForDirectory(
            mediaItems = mediaItems,
            directoryKey = directoryKey,
            exclusions = exclusions,
        )
    }

    if (directorySongs.isEmpty()) {
        SmartisanBlankState(
            iconRes = R.drawable.blank_song,
            title = stringResource(R.string.no_song),
            subtitle = stringResource(R.string.show_song),
            modifier = modifier,
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(FolderPageBackground),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            bottom = MiniPlayerReservedHeight,
        ),
    ) {
        itemsIndexed(
            items = directorySongs,
            key = { _, item -> item.mediaId },
        ) { index, item ->
            SongRow(
                mediaItem = item,
                selected = item.mediaId == currentMediaId,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    currentMediaId = item.mediaId
                    playbackBrowser?.setMediaItems(directorySongs, index, 0L)
                    playbackBrowser?.prepare()
                    playbackBrowser?.play()
                },
            )
        }
    }
}

@Composable
private fun DirectoryRow(
    entry: DirectoryEntry,
    editMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onSelectionToggle: () -> Unit,
    onVisibilityToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (pressed) FolderPressedBackground else FolderPageBackground)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(FolderRowHeight)
                .padding(horizontal = FolderRowHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (editMode) {
                SelectionCircle(
                    selected = selected,
                    modifier = Modifier
                        .size(FolderEditCheckboxSize)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSelectionToggle,
                        ),
                )
                Spacer(modifier = Modifier.width(FolderEditCheckboxPaddingEnd))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = entry.name,
                    style = FolderTitleStyle,
                    color = if (entry.hidden) FolderHiddenTitleColor else FolderTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val trackCount = stringResource(
                    R.string.album_track_count,
                    if (editMode) entry.totalCount else entry.visibleCount,
                )
                Text(
                    text = "$trackCount ${entry.displayPath}",
                    style = FolderSubtitleStyle,
                    color = if (entry.hidden) FolderHiddenSubtitleColor else FolderSubtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Image(
                painter = painterResource(if (pressed) R.drawable.arrow3_down else R.drawable.arrow3),
                contentDescription = null,
                modifier = Modifier.size(FolderArrowSize),
            )
            if (editMode) {
                Spacer(modifier = Modifier.width(10.dp))
                EyeToggle(
                    hidden = entry.hidden,
                    onClick = onVisibilityToggle,
                    modifier = Modifier.size(FolderEyeSize),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(FolderListDivider),
        )
    }
}

@Composable
private fun SelectionCircle(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val borderColor = Color(0xFFD8D8D8)
    Box(
        modifier = modifier
            .border(
                width = FolderEditCheckboxStroke,
                color = borderColor,
                shape = CircleShape,
            )
            .background(
                color = if (selected) FolderSelectedColor else Color.Transparent,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Canvas(modifier = Modifier.size(14.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.12f, size.height * 0.55f)
                    lineTo(size.width * 0.42f, size.height * 0.82f)
                    lineTo(size.width * 0.9f, size.height * 0.18f)
                }
                drawPath(
                    path = path,
                    color = Color.White,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = size.minDimension * 0.14f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}

@Composable
private fun EyeToggle(
    hidden: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val staticRes = if (hidden) R.drawable.eye_icon_0016 else R.drawable.eye_icon_0001
    AndroidView(
        modifier = modifier,
        factory = { context ->
            ImageView(context).apply {
                setImageResource(staticRes)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            }
        },
        update = { view ->
            val previousHidden = view.tag as? Boolean
            view.tag = hidden
            view.contentDescription = view.context.getString(R.string.toggle_directory_visibility)
            view.setOnClickListener { onClick() }
            if (previousHidden != null && previousHidden != hidden) {
                view.setImageResource(
                    if (hidden) R.drawable.eye_close_anim else R.drawable.eye_open_anim,
                )
                (view.drawable as? AnimationDrawable)?.let { drawable ->
                    drawable.stop()
                    drawable.start()
                }
                view.postDelayed(
                    { view.setImageResource(staticRes) },
                    EyeToggleAnimationMillis,
                )
            } else {
                view.setImageResource(staticRes)
            }
        },
    )
}

@Composable
private fun SongRow(
    mediaItem: MediaItem,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val metadata = mediaItem.mediaMetadata
    val title = metadata.displayTitle ?: metadata.title
    val subtitle = metadata.subtitle ?: metadata.artist

    Column(
        modifier = modifier
            .background(FolderPageBackground)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title?.toString().orEmpty(),
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = FolderTitleColor,
                    ),
                    color = if (selected) FolderSelectedColor else FolderTitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle?.toString().orEmpty(),
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = FolderSubtitleColor,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier.padding(start = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = FolderSelectedColor,
                                shape = CircleShape,
                            ),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(FolderListDivider),
        )
    }
}
