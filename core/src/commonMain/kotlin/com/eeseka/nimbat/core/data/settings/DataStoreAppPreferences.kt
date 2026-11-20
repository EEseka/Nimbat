package com.eeseka.nimbat.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eeseka.nimbat.core.domain.settings.AppPreferences
import com.eeseka.nimbat.core.domain.settings.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreAppPreferences(
    private val dataStore: DataStore<Preferences>
) : AppPreferences {

    private val themePreferenceKey = stringPreferencesKey("KEY_THEME_PREFERENCE")
    private val isPureBlackEnabledKey = booleanPreferencesKey("KEY_PURE_BLACK_ENABLED")
    private val areHapticsEnabledKey = booleanPreferencesKey("KEY_HAPTICS_ENABLED")
    private val deviceNameKey = stringPreferencesKey("KEY_DEVICE_NAME")

    override val theme: Flow<AppTheme> = dataStore.data.map { preferences ->
        val currentPreference = preferences[themePreferenceKey] ?: AppTheme.SYSTEM.name
        try {
            AppTheme.valueOf(currentPreference)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        }
    }

    override val isPureBlackEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[isPureBlackEnabledKey] ?: false
    }

    override val areHapticsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[areHapticsEnabledKey] ?: true
    }

    override val deviceName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[deviceNameKey]
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[themePreferenceKey] = theme.name
        }
    }

    override suspend fun setPureBlackEnabled(isEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[isPureBlackEnabledKey] = isEnabled
        }
    }

    override suspend fun setHapticsEnabled(isEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[areHapticsEnabledKey] = isEnabled
        }
    }

    override suspend fun setDeviceName(name: String) {
        dataStore.edit { preferences ->
            preferences[deviceNameKey] = name
        }
    }
}
