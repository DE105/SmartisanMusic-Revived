package com.smartisan.music.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartisan.music.R
import com.smartisan.music.ui.library.LibrarySummaryRow
import com.smartisan.music.ui.songs.SmartisanSongRow
import com.smartisan.music.ui.songs.SongPlaybackState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListSelectionAppearanceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lightSelectionPersistsAfterReleaseAndClearsOnLeavingEditMode() = checkSelection(false)

    @Test fun darkSelectionUsesTheNightPalette() = checkSelection(true)

    private fun checkSelection(night: Boolean) {
        val selected = mutableStateOf(false)
        val editMode = mutableStateOf(true)
        var plays = 0
        var expectedSelection = 0
        var expectedNormal = 0
        val item =
            MediaItem.Builder()
                .setMediaId("sample")
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle("Song").setArtist("Artist").build()
                )
                .build()
        compose.setContent {
            val context = LocalContext.current
            val configuration =
                Configuration(LocalConfiguration.current).apply {
                    uiMode =
                        (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            if (night) Configuration.UI_MODE_NIGHT_YES
                            else Configuration.UI_MODE_NIGHT_NO
                }
            val themed =
                remember(context, night) { context.createConfigurationContext(configuration) }
            expectedSelection = themed.getColor(R.color.list_selection_background)
            expectedNormal = themed.getColor(R.color.surface_card)
            CompositionLocalProvider(
                LocalContext provides themed,
                LocalConfiguration provides configuration,
            ) {
                Column(Modifier.width(360.dp)) {
                    SmartisanSongRow(
                        item,
                        { plays++ },
                        {},
                        Modifier.testTag("song"),
                        editMode = editMode.value,
                        selected = selected.value,
                        onSelectionChange = { selected.value = it },
                        playback = SongPlaybackState(item.mediaId, true),
                    )
                    LibrarySummaryRow(
                        "Album",
                        "Artist",
                        {},
                        Modifier.testTag("album"),
                        selected = editMode.value && selected.value,
                    )
                }
            }
        }
        assertBackground("song", expectedNormal)
        compose.onNodeWithTag("song").performClick()
        assertBackground("song", expectedSelection)
        assertBackground("album", expectedSelection)
        compose.runOnIdle { assertEquals(0, plays) }
        compose.runOnIdle { editMode.value = false }
        assertBackground("song", expectedNormal)
        assertBackground("album", expectedNormal)
    }

    private fun assertBackground(tag: String, color: Int) {
        val image = compose.onNodeWithTag(tag).captureToImage()
        val pixels = IntArray(image.width * image.height)
        image.readPixels(pixels)
        assertEquals(color, pixels[(image.height - 4) * image.width + image.width / 2])
    }
}
