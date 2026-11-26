package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "mis_colchas_prefs")

class PreferencesRepository(private val context: Context) {
    private val keyLargeText = booleanPreferencesKey("large_text_enabled")
    private val keyThemeMode = stringPreferencesKey("theme_mode") // "light", "dark"
    private val keyVisualStyle = stringPreferencesKey("visual_style") // "terracota", "azul", "apple"

    val largeTextEnabled: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[keyLargeText] ?: false
    }

    val themeMode: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[keyThemeMode] ?: "light"
    }

    val visualStyle: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[keyVisualStyle] ?: "terracota"
    }

    suspend fun setLargeTextEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyLargeText] = enabled
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyThemeMode] = mode
        }
    }

    suspend fun setVisualStyle(style: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[keyVisualStyle] = style
        }
    }
}
