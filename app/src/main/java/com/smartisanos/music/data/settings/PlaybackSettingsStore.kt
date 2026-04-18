package com.smartisanos.music.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val PlaybackSettingsStoreName = "playback_settings"

private val Context.playbackSettingsDataStore by preferencesDataStore(
    name = PlaybackSettingsStoreName,
)

data class PlaybackSettings(
    val scratchEnabled: Boolean = true,
    val hidePlayerAxisEnabled: Boolean = false,
    val popcornSoundEnabled: Boolean = false,
)

class PlaybackSettingsStore(
    private val context: Context,
) {

    val settings: Flow<PlaybackSettings> = context.playbackSettingsDataStore.data
        .map { preferences ->
            PlaybackSettings(
                scratchEnabled = preferences[ScratchEnabledKey] ?: true,
                hidePlayerAxisEnabled = preferences[HidePlayerAxisEnabledKey] ?: false,
                popcornSoundEnabled = preferences[PopcornSoundEnabledKey] ?: false,
            )
        }
        .distinctUntilChanged()

    suspend fun setScratchEnabled(enabled: Boolean) {
        setBoolean(ScratchEnabledKey, enabled)
    }

    suspend fun setHidePlayerAxisEnabled(enabled: Boolean) {
        setBoolean(HidePlayerAxisEnabledKey, enabled)
    }

    suspend fun setPopcornSoundEnabled(enabled: Boolean) {
        setBoolean(PopcornSoundEnabledKey, enabled)
    }

    private suspend fun setBoolean(
        key: Preferences.Key<Boolean>,
        value: Boolean,
    ) {
        context.playbackSettingsDataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}

private val ScratchEnabledKey = booleanPreferencesKey("scratch_enabled")
private val HidePlayerAxisEnabledKey = booleanPreferencesKey("hide_player_axis_enabled")
private val PopcornSoundEnabledKey = booleanPreferencesKey("popcorn_sound_enabled")
