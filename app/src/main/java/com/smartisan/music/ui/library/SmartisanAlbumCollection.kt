package com.smartisan.music.ui.library

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import com.smartisan.music.R
import com.smartisan.music.ui.album.AlbumSummary
import com.smartisan.music.ui.album.AlbumViewMode
import com.smartisan.music.ui.artwork.AlbumArtworkLoader
import com.smartisan.music.ui.components.rememberSmartisanDrawablePainter
import com.smartisan.music.ui.components.smartisanCheckboxBounds
import com.smartisan.music.ui.components.smartisanCheckboxHit
import com.smartisan.music.ui.components.smartisanClick
import com.smartisan.music.ui.components.smartisanSlideSelection
import com.smartisan.music.ui.components.smartisanVerticalScrollbar
import kotlinx.coroutines.flow.first

internal data class LibraryAlbumEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val album: AlbumSummary?,
    val songs: List<MediaItem>,
)

/** Album and artist pages share geometry and an interruptible layout-mode transition. */
@Composable
internal fun SmartisanAlbumCollection(
    entries: List<LibraryAlbumEntry>,
    active: Boolean,
    viewMode: AlbumViewMode,
    currentMediaId: String?,
    onClick: (LibraryAlbumEntry) -> Unit,
    modifier: Modifier = Modifier,
    editMode: Boolean = false,
    selectedIds: Set<String> = emptySet(),
    onSelectionChange: (String, Boolean) -> Unit = { _, _ -> },
    artistAlbums: Boolean = false,
    animateInitialAppearance: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
) {
    val loader = rememberAlbumArtworkLoader()
    val list = rememberLazyListState()
    val grid = rememberLazyGridState()
    val entrance =
        remember(animateInitialAppearance) { AlbumCollectionEntrance(animateInitialAppearance) }
    val entryIds = remember(entries) { entries.map { it.id } }
    LaunchedEffect(active, viewMode, entryIds, entrance) {
        if (entrance.phase == AlbumEntrancePhase.Complete || !active || entryIds.isEmpty())
            return@LaunchedEffect
        val keys = entryIds.toSet()
        val visibleIds = snapshotFlow {
            if (viewMode == AlbumViewMode.Tile) {
                grid.layoutInfo.visibleItemsInfo
                    .map { it.key }
                    .filterIsInstance<String>()
                    .filter { it in keys }
            } else {
                list.layoutInfo.visibleItemsInfo
                    .map { it.key }
                    .filterIsInstance<String>()
                    .filter { it in keys }
            }
        }
            .first { it.isNotEmpty() }
        entrance.reveal(visibleIds, viewMode)
    }
    var displayedMode by remember { mutableStateOf(viewMode) }
    val modeTransition = updateTransition(displayedMode, label = "album layout")
    val checkboxBounds = remember { mutableMapOf<String, Rect>() }
    var listOrigin by remember { mutableStateOf(Offset.Zero) }
    val touchSlop = LocalViewConfiguration.current.touchSlop
    LaunchedEffect(viewMode, active) {
        if (active && displayedMode != viewMode && entries.isNotEmpty()) entrance.finish()
        if (
            active &&
                displayedMode != viewMode &&
                modeTransition.currentState == modeTransition.targetState
        ) {
            val source =
                if (displayedMode == AlbumViewMode.List) {
                    list.layoutInfo.visibleItemsInfo.firstOrNull()?.key
                } else {
                    grid.layoutInfo.visibleItemsInfo.firstOrNull()?.key
                }
            val fallback =
                if (displayedMode == AlbumViewMode.List) {
                    list.firstVisibleItemIndex
                } else grid.firstVisibleItemIndex
            val anchor = entries.indexOfFirst { it.id == source }.takeIf { it >= 0 } ?: fallback
            val index = anchor.coerceIn(0, (entries.size - 1).coerceAtLeast(0))
            // Both layouts coexist during motion. Only position a fresh destination;
            // reversing in flight must preserve its already measured bounds.
            if (viewMode == AlbumViewMode.List) list.requestScrollToItem(index)
            else grid.requestScrollToItem(index)
        }
        displayedMode = viewMode
    }
    val sharedConfig =
        remember(modeTransition) {
            object : SharedTransitionScope.SharedContentConfig {
                override val SharedTransitionScope.SharedContentState.isEnabled: Boolean
                    get() = modeTransition.targetState == AlbumViewMode.List
            }
        }

    SharedTransitionLayout(
        modifier.fillMaxSize().clipToBounds().background(colorResource(R.color.page_background))
    ) {
        if (!active) return@SharedTransitionLayout
        val sharedScope = this
        modeTransition.AnimatedContent(
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                (EnterTransition.None togetherWith ExitTransition.None).using(null)
            },
            contentAlignment = Alignment.TopStart,
        ) { layoutMode ->
            val visibilityScope = this
            val interactive = layoutMode == modeTransition.targetState && !modeTransition.isRunning
            val layoutModifier =
                Modifier.fillMaxSize()
                    .testTag("album-${layoutMode.name}")
                    .then(
                        if (layoutMode != modeTransition.targetState)
                            Modifier.clearAndSetSemantics {}
                        else Modifier
                    )
            val firstIndex by
                remember(layoutMode) {
                    derivedStateOf {
                        if (layoutMode == AlbumViewMode.List) list.firstVisibleItemIndex
                        else grid.firstVisibleItemIndex
                    }
                }
            @Composable
            fun coverMotion(id: String, index: Int): Modifier =
                with(sharedScope) {
                    Modifier.sharedBounds(
                        rememberSharedContentState(id, sharedConfig),
                        animatedVisibilityScope = visibilityScope,
                        boundsTransform = { _, _ ->
                            tween(150, delayMillis = (index - firstIndex).coerceIn(0, 20) * 10)
                        },
                        resizeMode =
                            SharedTransitionScope.ResizeMode.scaleToBounds(ContentScale.FillBounds),
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(150)),
                    )
                }
            if (layoutMode == AlbumViewMode.List) {
                LazyColumn(
                    state = list,
                    userScrollEnabled = interactive,
                    modifier =
                        layoutModifier
                            .onGloballyPositioned { listOrigin = it.positionInRoot() }
                            .smartisanVerticalScrollbar(list)
                            .smartisanSlideSelection(
                                enabled = editMode && interactive,
                                itemAt = { point ->
                                    list.layoutInfo.visibleItemsInfo
                                        .firstOrNull {
                                            point.y >= it.offset && point.y < it.offset + it.size
                                        }
                                        ?.index
                                },
                                keyAt = { entries.getOrNull(it)?.id },
                                selectedKeys = selectedIds,
                                onSelectionChange = onSelectionChange,
                                canStart = { point ->
                                    val index =
                                        list.layoutInfo.visibleItemsInfo
                                            .firstOrNull {
                                                point.y >= it.offset &&
                                                    point.y < it.offset + it.size
                                            }
                                            ?.index
                                    val key = index?.let { entries.getOrNull(it)?.id }
                                    smartisanCheckboxHit(
                                        point + listOrigin,
                                        key?.let { checkboxBounds[it] },
                                        touchSlop,
                                    )
                                },
                                scrollBy = { list.scrollBy(it) },
                                edgeItemAt = { top ->
                                    list.layoutInfo.visibleItemsInfo
                                        .filter { it.index in entries.indices }
                                        .let {
                                            if (top) it.firstOrNull()?.index
                                            else it.lastOrNull()?.index
                                        }
                                },
                            ),
                ) {
                    itemsIndexed(entries, key = { _, item -> item.id }) { index, item ->
                        LibraryAlbumListRow(
                            item,
                            loader,
                            editMode,
                            item.id in selectedIds,
                            item.songs.any { it.mediaId == currentMediaId },
                            artistAlbums,
                            coverModifier = coverMotion(item.id, index),
                            modifier =
                                Modifier.animateEnterExit(
                                        enter = fadeIn(tween(150)),
                                        exit = fadeOut(tween(90)),
                                    )
                                    .albumCollectionEntrance(entrance, item.id),
                            onCheckboxBounds = { bounds ->
                                if (bounds == null) checkboxBounds.remove(item.id)
                                else checkboxBounds[item.id] = bounds
                            },
                            onClick = {
                                if (!interactive) return@LibraryAlbumListRow
                                if (editMode) onSelectionChange(item.id, item.id !in selectedIds)
                                else onClick(item)
                            },
                        )
                        LibraryDivider()
                    }
                    if (footer != null) item(key = "album-footer") { footer() }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(integerResource(R.integer.gridview_columns)),
                    state = grid,
                    userScrollEnabled = interactive,
                    modifier =
                        layoutModifier
                            .smartisanVerticalScrollbar(grid)
                            .smartisanSlideSelection(
                                enabled = editMode && interactive,
                                itemAt = { point ->
                                    grid.layoutInfo.visibleItemsInfo
                                        .firstOrNull {
                                            point.x >= it.offset.x &&
                                                point.x < it.offset.x + it.size.width &&
                                                point.y >= it.offset.y &&
                                                point.y < it.offset.y + it.size.height
                                        }
                                        ?.index
                                },
                                keyAt = { entries.getOrNull(it)?.id },
                                selectedKeys = selectedIds,
                                onSelectionChange = onSelectionChange,
                                canStart = { true },
                                scrollBy = { grid.scrollBy(it) },
                                horizontalActivation = true,
                                edgeItemAt = { top ->
                                    grid.layoutInfo.visibleItemsInfo
                                        .filter { it.index in entries.indices }
                                        .let {
                                            if (top) it.firstOrNull()?.index
                                            else it.lastOrNull()?.index
                                        }
                                },
                                activationThreshold = { point ->
                                    grid.layoutInfo.visibleItemsInfo
                                        .firstOrNull {
                                            point.x >= it.offset.x &&
                                                point.x < it.offset.x + it.size.width &&
                                                point.y >= it.offset.y &&
                                                point.y < it.offset.y + it.size.height
                                        }
                                        ?.let { Offset(it.size.width / 2f, it.size.height / 2f) }
                                        ?: Offset.Zero
                                },
                            ),
                    contentPadding =
                        PaddingValues(
                            start = dimensionResource(R.dimen.gridview_margin),
                            end = dimensionResource(R.dimen.gridview_margin),
                            bottom =
                                if (artistAlbums) 0.dp
                                else dimensionResource(R.dimen.gridview_margin),
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(dimensionResource(R.dimen.gridview_horizontalSpacing)),
                    verticalArrangement =
                        Arrangement.spacedBy(dimensionResource(R.dimen.gridview_verticalSpacing)),
                ) {
                    itemsIndexed(entries, key = { _, item -> item.id }) { index, item ->
                        LibraryAlbumTile(
                            item,
                            loader,
                            editMode,
                            item.id in selectedIds,
                            item.songs.any { it.mediaId == currentMediaId },
                            coverModifier = coverMotion(item.id, index),
                            modifier =
                                Modifier.animateEnterExit(
                                        enter =
                                            fadeIn(
                                                tween(
                                                    300,
                                                    delayMillis =
                                                        (index - firstIndex).coerceIn(0, 20) * 40,
                                                )
                                            ),
                                        exit = fadeOut(tween(150)),
                                    )
                                    .albumCollectionEntrance(entrance, item.id),
                            onClick = {
                                if (!interactive) return@LibraryAlbumTile
                                if (editMode) onSelectionChange(item.id, item.id !in selectedIds)
                                else onClick(item)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryAlbumListRow(
    item: LibraryAlbumEntry,
    loader: AlbumArtworkLoader,
    editMode: Boolean,
    selected: Boolean,
    playing: Boolean,
    artistAlbums: Boolean,
    onClick: () -> Unit,
    onCheckboxBounds: (Rect?) -> Unit,
    modifier: Modifier = Modifier,
    coverModifier: Modifier = Modifier,
) {
    val checkbox =
        rememberSmartisanDrawablePainter(R.drawable.check_box_selector, checked = selected)
    val checkboxWidth = with(LocalDensity.current) { checkbox.intrinsicSize.width.toDp() }
    val editWidth by
        animateDpAsState(
            if (editMode) checkboxWidth + dimensionResource(R.dimen.check_box_margin_left)
            else 0.dp,
            tween(200),
            label = "Album selection inset",
        )
    LibrarySummaryRow(
        item.title,
        item.subtitle,
        onClick,
        modifier = modifier.semantics { if (editMode) this.selected = selected },
        titleColor =
            colorResource(if (playing) R.color.playing_red else R.color.setting_item_text_color),
        leadingContent = {
            Box(Modifier.width(editWidth).clipToBounds(), contentAlignment = Alignment.CenterEnd) {
                if (editWidth > 0.dp) {
                    Image(
                        checkbox,
                        null,
                        Modifier.width(checkboxWidth).smartisanCheckboxBounds(onCheckboxBounds),
                    )
                }
            }
            LibraryAlbumCover(
                item,
                loader,
                tile = false,
                modifier =
                    Modifier.padding(start = dimensionResource(R.dimen.listview_items_margin_left))
                        .then(coverModifier)
                        .size(dimensionResource(R.dimen.album_list_item_image_width)),
            )
        },
        textInset =
            dimensionResource(
                if (artistAlbums) R.dimen.listview_items_margin_left
                else R.dimen.common_padding_left
            ),
    )
}

@Composable
private fun LibraryAlbumTile(
    item: LibraryAlbumEntry,
    loader: AlbumArtworkLoader,
    editMode: Boolean,
    selected: Boolean,
    playing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    coverModifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val tileSize = dimensionResource(R.dimen.gridview_item_ccontainer_height)
    Column(
        modifier
            .padding(top = dimensionResource(R.dimen.gridview_padding_top2))
            .clickable(
                interaction,
                null,
                role = if (editMode) Role.Checkbox else Role.Button,
                onClick = smartisanClick(onClick),
            )
            .semantics { if (editMode) this.selected = selected },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(coverModifier.size(tileSize)) {
            LibraryAlbumCover(
                item,
                loader,
                tile = true,
                pressed = pressed,
                modifier = Modifier.fillMaxSize(),
            )
            if (editMode) {
                Image(
                    rememberSmartisanDrawablePainter(R.drawable.albums_selected_large_empty),
                    null,
                    Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds,
                )
                if (selected)
                    Image(
                        rememberSmartisanDrawablePainter(R.drawable.albums_selected_large),
                        null,
                        Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds,
                    )
            }
        }
        BasicText(
            item.title,
            Modifier.width(tileSize),
            style =
                TextStyle(
                    color =
                        colorResource(if (playing) R.color.playing_red else R.color.text_emphasis),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    platformStyle = PlatformTextStyle(includeFontPadding = true),
                ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun LibraryAlbumCover(
    item: LibraryAlbumEntry,
    loader: AlbumArtworkLoader,
    tile: Boolean,
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
) {
    val padding = if (tile) dimensionResource(R.dimen.gridview_padding) else 0.dp
    val size =
        dimensionResource(
            if (tile) R.dimen.gridview_item_ccontainer_height
            else R.dimen.album_list_item_image_width
        )
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    Box(modifier) {
        SmartisanAlbumArtwork(
            item.album,
            sizePx,
            if (item.album == null) R.drawable.noalbumcover_all_songs2
            else if (tile) R.drawable.noalbumcover_220 else R.drawable.noalbumcover_120,
            Modifier.fillMaxSize().padding(padding),
            loader,
        )
        val mask =
            if (tile) {
                if (item.album == null) R.drawable.no_mask_albumcover_tile_selector
                else R.drawable.mask_albumcover_tile_selector
            } else if (item.album == null) null else R.drawable.mask_albumcover_list
        if (mask != null)
            Image(
                rememberSmartisanDrawablePainter(mask, pressed = pressed),
                null,
                Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
    }
}
