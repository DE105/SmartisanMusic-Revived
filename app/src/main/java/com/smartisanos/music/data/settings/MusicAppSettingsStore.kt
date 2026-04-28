package com.smartisanos.music.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val MusicAppSettingsStoreName = "music_app_settings"

private val Context.musicAppSettingsDataStore by preferencesDataStore(
    name = MusicAppSettingsStoreName,
)

class MusicAppSettingsStore(
    private val context: Context,
) {

    val lastMainDestinationRoute: Flow<String?> = context.musicAppSettingsDataStore.data
        .map { preferences ->
            preferences[LastMainDestinationRouteKey]
        }
        .distinctUntilChanged()

    suspend fun setLastMainDestinationRoute(route: String) {
        context.musicAppSettingsDataStore.edit { preferences ->
            preferences[LastMainDestinationRouteKey] = route
        }
    }
}

private val LastMainDestinationRouteKey = stringPreferencesKey("last_main_destination_route")
