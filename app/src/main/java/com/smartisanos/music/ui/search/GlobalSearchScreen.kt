package com.smartisanos.music.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.smartisanos.music.R
import com.smartisanos.music.data.library.LibraryExclusionsStore
import com.smartisanos.music.data.search.SearchHistoryStore
import com.smartisanos.music.playback.LocalPlaybackBrowser
import com.smartisanos.music.playback.await
import com.smartisanos.music.ui.album.AlbumSummary
import com.smartisanos.music.ui.artist.ArtistSummary
import com.smartisanos.music.ui.components.GlobalPlaybackBar
import com.smartisanos.music.ui.components.SmartisanDrawableBackground
import com.smartisanos.music.ui.components.hasAudioPermission
import com.smartisanos.music.ui.components.loadArtwork
import kotlinx.coroutines.launch

private val SearchPageBackground = Color.White
private val SearchFieldHintColor = Color(0x66000000)
private val SearchFieldTextColor = Color(0xCC000000)
private val SearchSectionTitleColor = Color(0x99000000)
private val SearchDividerColor = Color(0xFFE9E9E9)
private val SearchTabTextColor = Color(0x99000000)
private val SearchTabSelectedTextColor = Color.White
private val SearchSongTitleColor = Color(0xCC000000)
private val SearchSongPlayingColor = Color(0xFFE64040)
private val SearchSubtitleColor = Color(0x66000000)
private val SearchEmptyTextColor = Color(0xFFDBDBDB)

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
private val SearchFieldHeight = 30.dp
private val SearchCancelButtonWidth = 50.dp
private val SearchCancelButtonHeight = SearchFieldHeight - 1.dp
private val SearchCancelButtonCorner = 4.dp
private val SearchClearButtonSize = 33.dp
private val SearchClearIconSize = 32.dp
private val SearchTextStartPadding = 33.dp
private val SearchHistoryTopPadding = 19.dp
private val SearchSectionHorizontalPadding = 21.dp
private val SearchHistoryRowSpacing = 10.dp
private val SearchHistoryChipHeight = 30.dp
private val SearchTabBarHeight = 48.dp
private val SearchTabGroupHeight = 34.dp
private val SearchTabGroupHorizontalPadding = 6.dp
private val SearchResultRowHeight = 61.dp
private val SearchResultArtworkFrameWidth = 62.dp
private val SearchResultArtworkSize = 50.dp
private val SearchPlaybackBarReservedHeight = 67.dp
private val SearchTopHorizontalPadding = 6.dp
private val SearchNoResultTopPadding = 85.dp
private val SearchNoResultArtworkSize = 140.dp

@Composable
fun GlobalSearchScreen(
    query: String,
    selectedTab: SearchTab,
    libraryRefreshVersion: Int = 0,
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

    LaunchedEffect(playbackBrowser, permissionVersion, libraryRevision, libraryRefreshVersion, hasPermission) {
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
    val density = LocalDensity.current
    val topInset = with(density) {
        WindowInsets.safeDrawing.getTop(this).toDp()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SearchTopBarHeight + topInset),
    ) {
        SmartisanDrawableBackground(
            drawableRes = R.drawable.list_item,
            modifier = Modifier.matchParentSize(),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topInset)
                .padding(horizontal = SearchTopHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SearchTopHorizontalPadding),
        ) {
            SearchField(
                value = query,
                focusRequester = focusRequester,
                modifier = Modifier.weight(1f),
                onValueChange = onQueryChange,
                onSearch = onSearch,
            )
            SearchCancelButton(onDismiss = onDismiss)
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(SearchDividerColor),
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
        textStyle = SearchFieldTextStyle,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
            .height(SearchFieldHeight)
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            val clearInteractionSource = remember { MutableInteractionSource() }
            val clearPressed by clearInteractionSource.collectIsPressedAsState()
            Box(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                SmartisanDrawableBackground(
                    drawableRes = R.drawable.search_bar_field_bg,
                    modifier = Modifier.matchParentSize(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(
                            start = SearchTextStartPadding,
                            end = if (value.isNotEmpty()) SearchClearButtonSize else 12.dp,
                        ),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_hint),
                            style = SearchFieldTextStyle.copy(color = SearchFieldHintColor),
                        )
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(SearchClearButtonSize)
                            .align(Alignment.CenterEnd)
                            .clickable(
                                interactionSource = clearInteractionSource,
                                indication = null,
                                onClick = { onValueChange("") }
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(
                                if (clearPressed) R.drawable.clear_text_down else R.drawable.clear_text
                            ),
                            contentDescription = stringResource(R.string.clear_search_text),
                            modifier = Modifier.size(SearchClearIconSize),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SearchCancelButton(onDismiss: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .width(SearchCancelButtonWidth)
            .height(SearchCancelButtonHeight)
            .smartisanSearchCancelBackground(pressed)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.cancel),
            style = TextStyle(fontSize = 14.sp, color = if (pressed) Color(0xFF4E4E4E) else Color(0xFF666666)),
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

private fun Modifier.smartisanSearchCancelBackground(pressed: Boolean): Modifier = drawWithCache {
    val radiusPx = SearchCancelButtonCorner.toPx()
    val strokeWidth = 1.dp.toPx()
    val halfStroke = strokeWidth / 2f
    val normalBrush = Brush.verticalGradient(
        0.0f to Color(0xFFFFFFFF),
        0.52f to Color(0xFFF9F9F9),
        1.0f to Color(0xFFF0F0F0),
    )
    val pressedBrush = Brush.verticalGradient(
        0.0f to Color(0xFFE6E6E6),
        1.0f to Color(0xFFF4F4F4),
    )

    onDrawBehind {
        drawRoundRect(
            brush = if (pressed) pressedBrush else normalBrush,
            size = size,
            cornerRadius = CornerRadius(radiusPx, radiusPx),
        )
        drawRoundRect(
            color = if (pressed) Color(0xFFD6D6D6) else Color(0xFFDCDCDC),
            topLeft = Offset(halfStroke, halfStroke),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(radiusPx - halfStroke, radiusPx - halfStroke),
            style = Stroke(strokeWidth),
        )
    }
}

@Composable
private fun SearchHistoryPage(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (history.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White),
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(
                start = SearchSectionHorizontalPadding,
                top = SearchHistoryTopPadding,
                end = 20.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.search_history),
                style = SearchSectionTitleStyle,
                modifier = Modifier.padding(start = 7.dp),
            )
            Image(
                painter = painterResource(R.drawable.search_clear),
                contentDescription = stringResource(R.string.clear_history),
                modifier = Modifier
                    .size(20.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClearHistory,
                    ),
            )
        }
        FlowRow(
            modifier = Modifier.padding(top = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(SearchHistoryRowSpacing),
            verticalArrangement = Arrangement.spacedBy(SearchHistoryRowSpacing),
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
            SearchNoResultState(
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
            .height(SearchTabBarHeight),
    ) {
        SmartisanDrawableBackground(
            drawableRes = R.drawable.list_item,
            modifier = Modifier.matchParentSize(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(SearchTabGroupHeight)
                .padding(horizontal = SearchTabGroupHorizontalPadding)
                .align(Alignment.Center),
        ) {
            SearchTab.entries.forEachIndexed { index, tab ->
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                val selected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(SearchTabGroupHeight)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                        ) {
                            onTabChange(tab)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    SmartisanDrawableBackground(
                        drawableRes = searchTabBackgroundRes(
                            index = index,
                            selected = selected || pressed,
                        ),
                        modifier = Modifier.matchParentSize(),
                    )
                    Text(
                        text = when (tab) {
                            SearchTab.All -> stringResource(R.string.search_tab_all)
                            SearchTab.Songs -> stringResource(R.string.search_tab_songs)
                            SearchTab.Albums -> stringResource(R.string.search_tab_albums)
                            SearchTab.Artists -> stringResource(R.string.search_tab_artists)
                        },
                        style = TextStyle(
                            fontSize = 13.sp,
                            color = if (selected || pressed) SearchTabSelectedTextColor else SearchTabTextColor,
                        ),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(SearchDividerColor),
        )
    }
}

private fun searchTabBackgroundRes(index: Int, selected: Boolean): Int {
    return when {
        index == 0 && selected -> R.drawable.search_tab_left_bg_down
        index == 0 -> R.drawable.search_tab_left_bg
        index == SearchTab.entries.lastIndex && selected -> R.drawable.search_tab_right_bg_down
        index == SearchTab.entries.lastIndex -> R.drawable.search_tab_right_bg
        selected -> R.drawable.search_tab_middle_bg_down
        else -> R.drawable.search_tab_middle_bg
    }
}

@Composable
private fun SearchSectionHeader(title: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SearchTabBarHeight),
    ) {
        SmartisanDrawableBackground(
            drawableRes = R.drawable.list_item,
            modifier = Modifier.matchParentSize(),
        )
        Text(
            text = stringResource(title),
            style = TextStyle(fontSize = 14.sp, color = SearchSectionTitleColor),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(SearchDividerColor),
        )
    }
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SearchResultRowHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        SmartisanDrawableBackground(
            drawableRes = if (pressed) R.drawable.list_item_shadow else R.drawable.list_item_bg,
            modifier = Modifier.matchParentSize(),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(SearchResultArtworkFrameWidth)
                    .height(SearchResultRowHeight)
                    .padding(start = 12.dp, top = 5.dp, bottom = 5.dp),
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
                    .padding(start = 12.dp),
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
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SearchDividerColor),
    )
}

@Composable
private fun SearchHistoryChip(
    text: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .height(SearchHistoryChipHeight)
            .defaultMinSize(minWidth = 48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        SmartisanDrawableBackground(
            drawableRes = if (pressed) R.drawable.search_badge_grey_p else R.drawable.search_badge_grey,
            modifier = Modifier.matchParentSize(),
        )
        Text(
            text = text,
            style = TextStyle(
                color = Color(0x66000000),
                fontSize = 13.5.sp,
            ),
            modifier = Modifier.padding(horizontal = 14.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SearchNoResultState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(top = SearchNoResultTopPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.blank_search),
            contentDescription = null,
            modifier = Modifier.size(SearchNoResultArtworkSize),
        )
        Text(
            text = stringResource(R.string.search_no_result),
            style = TextStyle(fontSize = 23.sp, color = SearchEmptyTextColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 25.dp),
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
        Box(modifier = modifier) {
            Image(
                bitmap = artwork!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Image(
                painter = painterResource(R.drawable.mask_albumcover_list),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
            )
        }
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.noalbumcover_120),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            Image(
                painter = painterResource(R.drawable.mask_albumcover_list),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}
