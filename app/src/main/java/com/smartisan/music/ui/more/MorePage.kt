package com.smartisan.music.ui.more

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.zIndex
import com.smartisan.music.R
import com.smartisan.music.data.settings.ArtistSettings
import com.smartisan.music.data.settings.AudioFxPreset
import com.smartisan.music.data.settings.NavigationSettings
import com.smartisan.music.data.settings.PlaybackSettings
import com.smartisan.music.data.settings.ThemeMode
import com.smartisan.music.ui.navigation.LocalSmartisanTitleVisible
import com.smartisan.music.ui.navigation.MusicDestination
import com.smartisan.music.ui.navigation.SmartisanFullScreenTransition
import com.smartisan.music.ui.settings.SettingsPage
import com.smartisan.music.ui.settings.SettingsSecondaryPage
import com.smartisan.music.ui.settings.SettingsTitleStack

/** `More` 只负责两个职责：列出当前没有固定到底栏的同级目的地，以及承载设置页。 内容目的地由主壳统一渲染，避免在这里复制一套页面栈和状态所有权。 */
@Composable
internal fun MorePage(
    active: Boolean,
    overflowDestinations: List<MusicDestination>,
    playbackSettings: PlaybackSettings,
    artistSettings: ArtistSettings,
    navigationSettings: NavigationSettings,
    themeMode: ThemeMode,
    onDestinationSelected: (MusicDestination) -> Unit,
    onScratchEnabledChange: (Boolean) -> Unit,
    onHidePlayerAxisEnabledChange: (Boolean) -> Unit,
    onPopcornSoundEnabledChange: (Boolean) -> Unit,
    onAudioFxEnabledChange: (Boolean) -> Unit,
    onAudioFxPresetChange: (AudioFxPreset) -> Unit,
    onAudioFxCustomGainDbPointsChange: (List<Float>) -> Unit,
    onArtistSeparatorsChange: (Set<String>) -> Unit,
    onTabPinnedChange: (String, Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onSettingsPageActiveChanged: (Boolean) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var settingsVisible by remember { mutableStateOf(false) }
    var secondaryPage by rememberSaveable { mutableStateOf<SettingsSecondaryPage?>(null) }
    var settingsMounted by remember { mutableStateOf(false) }
    val latestSettingsActiveChanged by rememberUpdatedState(onSettingsPageActiveChanged)

    LaunchedEffect(active, settingsVisible, settingsMounted) {
        latestSettingsActiveChanged(active && (settingsVisible || settingsMounted))
    }
    DisposableEffect(Unit) {
        onDispose { latestSettingsActiveChanged(false) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SmartisanMoreRootPage(
            active = active,
            destinations = overflowDestinations,
            onDestinationSelected = onDestinationSelected,
            onSettingsClick = { settingsVisible = true },
            onSearchClick = onSearchClick,
            modifier = Modifier.fillMaxSize(),
        )
        SmartisanFullScreenTransition(visible = active && settingsVisible) {
            DisposableEffect(Unit) {
                settingsMounted = true
                onDispose {
                    settingsMounted = false
                    secondaryPage = null
                }
            }
            val titleHeight =
                WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
                    dimensionResource(R.dimen.title_bar_height)
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().height(titleHeight).zIndex(1f)) {
                    SettingsTitleStack(
                        secondaryPage,
                        onClose = { settingsVisible = false },
                        onBack = { secondaryPage = null },
                    )
                }
                CompositionLocalProvider(LocalSmartisanTitleVisible provides false) {
                    SettingsPage(
                        secondaryPage = secondaryPage,
                        onSecondaryPageChange = { secondaryPage = it },
                        active = active,
                        playbackSettings = playbackSettings,
                        artistSettings = artistSettings,
                        navigationSettings = navigationSettings,
                        themeMode = themeMode,
                        onClose = { settingsVisible = false },
                        onScratchEnabledChange = onScratchEnabledChange,
                        onHidePlayerAxisEnabledChange = onHidePlayerAxisEnabledChange,
                        onPopcornSoundEnabledChange = onPopcornSoundEnabledChange,
                        onAudioFxEnabledChange = onAudioFxEnabledChange,
                        onAudioFxPresetChange = onAudioFxPresetChange,
                        onAudioFxCustomGainDbPointsChange = onAudioFxCustomGainDbPointsChange,
                        onArtistSeparatorsChange = onArtistSeparatorsChange,
                        onTabPinnedChange = onTabPinnedChange,
                        onThemeModeChange = onThemeModeChange,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                }
            }
        }
    }
}
