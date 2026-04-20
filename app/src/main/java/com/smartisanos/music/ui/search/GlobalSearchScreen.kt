package com.smartisanos.music.ui.search

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.core.content.ContextCompat
import com.smartisanos.music.R
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.data.search.SearchHistoryStore
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.await
import com.smartisanos.music.ui.album.AlbumSummary
import com.smartisanos.music.ui.artist.ArtistSummary
import com.smartisanos.music.ui.components.GlobalPlaybackBar
import com.smartisanos.music.ui.components.SmartisanBlankState
import com.smartisanos.music.ui.components.SmartisanTopBarTextButton
import com.smartisanos.music.ui.components.SmartisanTopBarTextButtonStyle
import com.smartisanos.music.ui.components.hasAudioPermission
import com.smartisanos.music.ui.components.loadArtwork
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val SearchPageBackground = Color.White
private val SearchFieldHintColor = Color(0x66000000)
private val SearchFieldTextColor = Color(0xCC000000)
private val SearchSectionTitleColor = Color(0x99000000)
private val SearchDividerColor = Color(0xFFE8E8E8)
private val SearchTabTextColor = Color(0x96000000)
private val SearchTabSelectedTextColor = Color(0xCC000000)
private val SearchSongTitleColor = Color(0xCC000000)
private val SearchSongPlayingColor = Color(0xFFE64040)
private val SearchSubtitleColor = Color(0x73000000)

private val SearchFieldTextStyle = TextStyle(
    fontSize = 13.sp,
    color = SearchFieldTextColor,
)
private val SearchSectionTitleStyle = TextStyle(
    fontSize = 15.sp,
    color = SearchSectionTitleColor,
)
private val SearchPrimaryTextStyle = TextStyle(
    fontSize = 16.sp,
    fontWeight = FontWeight.Medium,
    color = SearchSongTitleColor,
)
private val SearchSecondaryTextStyle = TextStyle(
    fontSize = 13.sp,
    color = SearchSubtitleColor,
)

private val SearchTopBarHeight = 50.dp
private val SearchFieldHeight = 39.dp
private val SearchCancelButtonHeight = 39.dp
private val SearchHistoryTopPadding = 19.dp
private val SearchSectionHorizontalPadding = 21.dp
private val SearchHistoryRowSpacing = 10.dp
private val SearchResultRowHeight = 61.dp
private val SearchResultArtworkFrameWidth = 64.dp
private val SearchResultArtworkSize = 50.dp
private val SearchPlaybackBarReservedHeight = 67.dp
private val SearchTopHorizontalPadding = 6.dp
private val SearchTopVerticalPadding = 6.dp

@Composable
fun GlobalSearchScreen(
    query: String,
    selectedTab: SearchTab,
    onQueryChange: (String) -> Unit,
    onTabChange: (SearchTab) -> Unit,
    onDismiss: () -> Unit,
    onOpenPlayback: () -> Unit,
    onAlbumClick: (String, String) -> Unit,
    onArtistClick: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val playbackBrowser = LocalPlaybackBrowser.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val historyStore = remember(context.applicationContext) {
        SearchHistoryStore(context.applicationContext)
    }
    val exclusionsStore = remember(context.applicationContext) {
        LibraryExclusionsStore(context.applicationContext)
    }
    val history by historyStore.history.collectAsState(initial = emptyList())
    val libraryRevision by exclusionsStore.revision.collectAsState(initial = 0)
    val hasPermission = hasAudioPermission(context)
    var permissionVersion by remember { mutableIntStateOf(0) }
    var songs by remember(playbackBrowser) { mutableStateOf(emptyList<MediaItem>()) }
    var currentMediaId by remember(playbackBrowser) {
        mutableStateOf(playbackBrowser?.currentMediaItem?.mediaId)
    }
    val dismissSearch by rememberUpdatedState(
        newValue = {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            onDismiss()
        },
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionVersion += 1
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val unknownAlbumTitle = stringResource(R.string.unknown_album)
    val unknownArtistTitle = stringResource(R.string.unknown_artist)
    val multipleArtistsTitle = stringResource(R.string.many_artist)
    val results = remember(query, songs, unknownAlbumTitle, unknownArtistTitle, multipleArtistsTitle) {
        buildSearchResults(
            query = query,
            songs = songs,
            unknownAlbumTitle = unknownAlbumTitle,
            unknownArtistTitle = unknownArtistTitle,
            multipleArtistsTitle = multipleArtistsTitle,
        )
    }
    val showPlaybackBar = currentMediaId != null

    BackHandler(onBack = dismissSearch)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SearchPageBackground)
            .safeDrawingPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            SearchTopBar(
                query = query,
                focusRequester = focusRequester,
                onQueryChange = onQueryChange,
                onSearch = {
                    scope.launch {
                        historyStore.record(query)
                    }
                    keyboardController?.hide()
                    focusManager.clearFocus()
                },
                onDismiss = dismissSearch,
            )
            if (query.isBlank()) {
                SearchHistoryPage(
                    history = history,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onHistoryClick = { entry ->
                        onTabChange(SearchTab.All)
                        onQueryChange(entry)
                        scope.launch {
                            historyStore.record(entry)
                        }
                    },
                    onClearHistory = {
                        scope.launch {
                            historyStore.clear()
                        }
                    },
                )
            } else {
                SearchResultsPage(
                    results = results,
                    selectedTab = selectedTab,
                    currentMediaId = currentMediaId,
                    showPlaybackBar = showPlaybackBar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    onTabChange = onTabChange,
                    onSongClick = { item ->
                        val targetIndex = songs.indexOfFirst { song -> song.mediaId == item.mediaId }
                        if (targetIndex >= 0) {
                            playbackBrowser?.setMediaItems(songs, targetIndex, 0L)
                            playbackBrowser?.prepare()
                            playbackBrowser?.play()
                            scope.launch {
                                historyStore.record(query)
                            }
                        }
                    },
                    onAlbumClick = { album ->
                        scope.launch {
                            historyStore.record(query)
                        }
                        dismissSearch()
                        onAlbumClick(album.id, album.title)
                    },
                    onArtistClick = { artist ->
                        scope.launch {
                            historyStore.record(query)
                        }
                        dismissSearch()
                        onArtistClick(artist.id, artist.name)
                    },
                )
            }
        }
        if (showPlaybackBar) {
            GlobalPlaybackBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                onOpenPlayback = onOpenPlayback,
            )
        }
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SearchField(
            value = query,
            focusRequester = focusRequester,
            modifier = Modifier.weight(1f),
            onValueChange = onQueryChange,
            onSearch = onSearch,
        )
        Text(
            text = stringResource(R.string.cancel),
            color = Color(0xFF666666),
            fontSize = 15.sp,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(vertical = 8.dp, horizontal = 4.dp)
        )
    }
}

@Composable
private fun SearchField(
    value: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontSize = 14.sp, color = Color(0xFF333333)),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
            .height(36.dp)
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.search_icon),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 2.dp).size(26.dp),
                    colorFilter = ColorFilter.tint(Color(0xFF999999)),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            style = TextStyle(fontSize = 14.sp, color = Color(0xFF999999)),
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Image(
                        painter = painterResource(R.drawable.clear_text), // Changed to correct resource
                        contentDescription = stringResource(R.string.clear_history),
                        modifier = Modifier
                            .size(24.dp) // Adjusted size
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onValueChange("") }
                            )
                            .padding(4.dp) // Optical padding
                    )
                }
            }
        },
    )
}

@Composable
private fun SearchHistoryPage(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.search_history),
                style = TextStyle(fontSize = 15.sp, color = Color(0xFF666666), fontWeight = FontWeight.Medium),
            )
            if (history.isNotEmpty()) {
                Image(
                    painter = painterResource(R.drawable.search_clear),
                    contentDescription = null,
                    modifier = Modifier
                        .size(24.dp) // Make this a bit larger for easier tapping
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClearHistory
                        )
                        .padding(2.dp) // Optical padding for touch target
                )
            }
        }
        if (history.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                history.forEach { entry ->
                    SearchHistoryChip(
                        text = entry,
                        onClick = { onHistoryClick(entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultsPage(
    results: SearchResults,
    selectedTab: SearchTab,
    currentMediaId: String?,
    showPlaybackBar: Boolean,
    onTabChange: (SearchTab) -> Unit,
    onSongClick: (MediaItem) -> Unit,
    onAlbumClick: (AlbumSummary) -> Unit,
    onArtistClick: (ArtistSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        SearchTabRow(
            selectedTab = selectedTab,
            onTabChange = onTabChange,
        )
        if (!results.hasResults) {
            SmartisanBlankState(
                iconRes = R.drawable.blank_song,
                title = stringResource(R.string.search_no_result),
                subtitle = stringResource(R.string.search_no_result_subtitle),
                modifier = Modifier.weight(1f),
            )
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                bottom = if (showPlaybackBar) SearchPlaybackBarReservedHeight + 16.dp else 16.dp,
            ),
        ) {
            when (selectedTab) {
                SearchTab.All -> {
                    appendSongResults(
                        songs = results.songs,
                        currentMediaId = currentMediaId,
                        onSongClick = onSongClick,
                    )
                    appendAlbumResults(
                        albums = results.albums,
                        onAlbumClick = onAlbumClick,
                    )
                    appendArtistResults(
                        artists = results.artists,
                        onArtistClick = onArtistClick,
                    )
                }
                SearchTab.Songs -> appendSongResults(
                    songs = results.songs,
                    currentMediaId = currentMediaId,
                    onSongClick = onSongClick,
                )
                SearchTab.Albums -> appendAlbumResults(
                    albums = results.albums,
                    onAlbumClick = onAlbumClick,
                )
                SearchTab.Artists -> appendArtistResults(
                    artists = results.artists,
                    onArtistClick = onArtistClick,
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appendSongResults(
    songs: List<MediaItem>,
    currentMediaId: String?,
    onSongClick: (MediaItem) -> Unit,
) {
    if (songs.isEmpty()) return
    item(key = "songs-header") { SearchSectionHeader(title = R.string.search_tab_songs) }
    items(
        items = songs,
        key = { item -> "song-${item.mediaId}" },
    ) { item ->
        SearchSongRow(
            mediaItem = item,
            selected = item.mediaId == currentMediaId,
            onClick = { onSongClick(item) },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appendAlbumResults(
    albums: List<AlbumSummary>,
    onAlbumClick: (AlbumSummary) -> Unit,
) {
    if (albums.isEmpty()) return
    item(key = "albums-header") { SearchSectionHeader(title = R.string.search_tab_albums) }
    items(
        items = albums,
        key = { album -> "album-${album.id}" },
    ) { album ->
        SearchEntityRow(
            title = album.title,
            subtitle = album.artist,
            representative = album.representative,
            onClick = { onAlbumClick(album) },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.appendArtistResults(
    artists: List<ArtistSummary>,
    onArtistClick: (ArtistSummary) -> Unit,
) {
    if (artists.isEmpty()) return
    item(key = "artists-header") { SearchSectionHeader(title = R.string.search_tab_artists) }
    items(
        items = artists,
        key = { artist -> "artist-${artist.id}" },
    ) { artist ->
        SearchEntityRow(
            title = artist.name,
            subtitle = stringResource(
                R.string.artist_album_song_count,
                artist.albumCount,
                artist.trackCount,
            ),
            representative = artist.representative,
            onClick = { onArtistClick(artist) },
        )
    }
}

@Composable
private fun SearchTabRow(
    selectedTab: SearchTab,
    onTabChange: (SearchTab) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .background(Color.White)
                .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                .clip(RoundedCornerShape(6.dp)),
        ) {
            SearchTab.entries.forEachIndexed { index, tab ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            color = if (tab == selectedTab) Color(0xFFF5F5F5) else Color.Transparent,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            onTabChange(tab)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when (tab) {
                            SearchTab.All -> stringResource(R.string.search_tab_all)
                            SearchTab.Songs -> stringResource(R.string.search_tab_songs)
                            SearchTab.Albums -> stringResource(R.string.search_tab_albums)
                            SearchTab.Artists -> stringResource(R.string.search_tab_artists)
                        },
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = if (tab == selectedTab) Color(0xFF333333) else Color(0xFF999999),
                            fontWeight = if (tab == selectedTab) FontWeight.Medium else FontWeight.Normal,
                        ),
                    )
                }
                if (index != SearchTab.entries.lastIndex) {
                    Spacer(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0xFFE0E0E0)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(title: Int) {
    Text(
        text = stringResource(title),
        style = TextStyle(fontSize = 14.sp, color = Color(0xFF999999), fontWeight = FontWeight.Medium),
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun SearchSongRow(
    mediaItem: MediaItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    SearchEntityRow(
        title = mediaItem.mediaMetadata.title?.toString()
            ?: mediaItem.mediaMetadata.displayTitle?.toString()
            ?: stringResource(R.string.unknown_song_title),
        subtitle = mediaItem.mediaMetadata.artist?.toString()
            ?: stringResource(R.string.unknown_artist),
        representative = mediaItem,
        titleColor = if (selected) SearchSongPlayingColor else SearchSongTitleColor,
        onClick = onClick,
    )
}

@Composable
private fun SearchEntityRow(
    title: String,
    subtitle: String,
    representative: MediaItem,
    onClick: () -> Unit,
    titleColor: Color = SearchSongTitleColor,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(SearchResultRowHeight)
            .background(Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(SearchResultArtworkFrameWidth)
                .wrapContentHeight()
                .padding(start = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            SearchArtwork(
                mediaItem = representative,
                modifier = Modifier.size(SearchResultArtworkSize),
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = SearchPrimaryTextStyle.copy(color = titleColor),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = SearchSecondaryTextStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .height(0.5.dp)
            .background(Color(0xFFE8E8E8)),
    )
}

@Composable
private fun SearchHistoryChip(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(Color(0xFFF5F5F5), CircleShape)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = Color(0xFF666666),
                fontSize = 13.sp,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchArtwork(
    mediaItem: MediaItem,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val artwork by produceState<ImageBitmap?>(initialValue = null, key1 = mediaItem.mediaId) {
        value = loadArtwork(context, mediaItem)
    }

    if (artwork != null) {
        Image(
            bitmap = artwork!!,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(RoundedCornerShape(4.dp)),
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.blank_album),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}
