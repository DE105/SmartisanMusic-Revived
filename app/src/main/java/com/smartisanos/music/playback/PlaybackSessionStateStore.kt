package com.smartisanos.music.playback

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.media3.common.Player
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val PlaybackSessionStateStoreName = "playback_session_state"
private const val MediaIdSeparator = "\n"

private val Context.playbackSessionStateDataStore by preferencesDataStore(
    name = PlaybackSessionStateStoreName,
)

internal data class PlaybackSessionSnapshot(
    val mediaIds: List<String> = emptyList(),
    val currentMediaId: String? = null,
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val shuffleModeEnabled: Boolean = false,
)

internal class PlaybackSessionStateStore(
    private val context: Context,
) {

    val snapshot: Flow<PlaybackSessionSnapshot> = context.playbackSessionStateDataStore.data
        .map { preferences ->
            PlaybackSessionSnapshot(
                mediaIds = preferences[MediaIdsKey].orEmpty().decodeMediaIds(),
                currentMediaId = preferences[CurrentMediaIdKey]?.takeIf(String::isNotBlank),
                currentIndex = preferences[CurrentIndexKey] ?: 0,
                positionMs = preferences[PositionMsKey] ?: 0L,
                repeatMode = preferences[RepeatModeKey] ?: Player.REPEAT_MODE_OFF,
                shuffleModeEnabled = preferences[ShuffleModeEnabledKey] ?: false,
            )
        }

    suspend fun load(): PlaybackSessionSnapshot = snapshot.first()

    suspend fun save(snapshot: PlaybackSessionSnapshot) {
        context.playbackSessionStateDataStore.edit { preferences ->
            preferences[MediaIdsKey] = snapshot.mediaIds.encodeMediaIds()
            snapshot.currentMediaId?.let { currentMediaId ->
                preferences[CurrentMediaIdKey] = currentMediaId
            } ?: preferences.remove(CurrentMediaIdKey)
            preferences[CurrentIndexKey] = snapshot.currentIndex
            preferences[PositionMsKey] = snapshot.positionMs.coerceAtLeast(0L)
            preferences[RepeatModeKey] = snapshot.repeatMode
            preferences[ShuffleModeEnabledKey] = snapshot.shuffleModeEnabled
        }
    }
}

private fun List<String>.encodeMediaIds(): String {
    return asSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .joinToString(MediaIdSeparator)
}

private fun String.decodeMediaIds(): List<String> {
    return lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toList()
}

private val MediaIdsKey = stringPreferencesKey("media_ids")
private val CurrentMediaIdKey = stringPreferencesKey("current_media_id")
private val CurrentIndexKey = intPreferencesKey("current_index")
private val PositionMsKey = longPreferencesKey("position_ms")
private val RepeatModeKey = intPreferencesKey("repeat_mode")
private val ShuffleModeEnabledKey = booleanPreferencesKey("shuffle_mode_enabled")
