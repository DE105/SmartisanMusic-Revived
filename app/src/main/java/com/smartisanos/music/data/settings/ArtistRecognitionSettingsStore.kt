package com.smartisanos.music.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val ArtistRecognitionSettingsStoreName = "artist_recognition_settings"

private val Context.artistRecognitionSettingsDataStore by preferencesDataStore(
    name = ArtistRecognitionSettingsStoreName,
)

data class ArtistRecognitionSettings(
    val separators: Set<String> = emptySet(),
    val excludedArtistNames: Set<String> = emptySet(),
)

class ArtistRecognitionSettingsStore(
    private val context: Context,
) {

    val settings: Flow<ArtistRecognitionSettings> = context.artistRecognitionSettingsDataStore.data
        .map { preferences ->
            ArtistRecognitionSettings(
                separators = preferences[ArtistSeparatorsKey]
                    .orEmpty()
                    .normalizedArtistRecognitionValues(),
                excludedArtistNames = preferences[ExcludedArtistNamesKey]
                    .orEmpty()
                    .normalizedArtistRecognitionValues(),
            )
        }
        .distinctUntilChanged()

    suspend fun setSeparators(separators: Set<String>) {
        context.artistRecognitionSettingsDataStore.edit { preferences ->
            preferences[ArtistSeparatorsKey] = separators.normalizedArtistRecognitionValues()
        }
    }

    suspend fun setExcludedArtistNames(names: Set<String>) {
        context.artistRecognitionSettingsDataStore.edit { preferences ->
            preferences[ExcludedArtistNamesKey] = names.normalizedArtistRecognitionValues()
        }
    }
}

fun normalizeArtistRecognitionValue(value: String): String? {
    return value.trim()
        .trimMatchingQuotes()
        .takeIf { it.isNotEmpty() }
}

fun Set<String>.normalizedArtistRecognitionValues(): Set<String> {
    return asSequence()
        .mapNotNull { normalizeArtistRecognitionValue(it) }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .toSet()
}

private fun String.trimMatchingQuotes(): String {
    if (length < 2) {
        return this
    }
    val first = first()
    val last = last()
    return if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
        substring(1, lastIndex).trim()
    } else {
        this
    }
}

private val ArtistSeparatorsKey = stringSetPreferencesKey("artist_separators")
private val ExcludedArtistNamesKey = stringSetPreferencesKey("excluded_artist_names")
