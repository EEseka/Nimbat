package com.eeseka.nimbat.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.eeseka.nimbat.core.domain.settings.AppPreferences
import com.eeseka.nimbat.core.domain.settings.AppTheme
import com.eeseka.nimbat.core.domain.settings.MeasurementUnits
import com.eeseka.nimbat.sound.DEFAULT_TEMPERATURE_C
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreAppPreferences(
    private val dataStore: DataStore<Preferences>
) : AppPreferences {

    private companion object {
        val THEME_PREFERENCE_KEY = stringPreferencesKey("KEY_THEME_PREFERENCE")
        val PURE_BLACK_ENABLED_KEY = booleanPreferencesKey("KEY_PURE_BLACK_ENABLED")
        val HAPTICS_ENABLED_KEY = booleanPreferencesKey("KEY_HAPTICS_ENABLED")
        val DEVICE_NAME_KEY = stringPreferencesKey("KEY_DEVICE_NAME")
        val ROOM_TEMPERATURE_CELSIUS_KEY = doublePreferencesKey("KEY_ROOM_TEMPERATURE_CELSIUS")
        val VOLUME_KEY = floatPreferencesKey("KEY_VOLUME")
        val DISTANCE_OFFSET_METERS_KEY = doublePreferencesKey("KEY_DISTANCE_OFFSET_METERS")
        val MEASUREMENT_UNITS_KEY = stringPreferencesKey("KEY_MEASUREMENT_UNITS")
        val SOUND_CHECK_PASSED_KEY = booleanPreferencesKey("KEY_SOUND_CHECK_PASSED")
    }

    override val theme: Flow<AppTheme> = dataStore.data.map { preferences ->
        val currentPreference = preferences[THEME_PREFERENCE_KEY] ?: AppTheme.SYSTEM.name
        try {
            AppTheme.valueOf(currentPreference)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        }
    }

    override val isPureBlackEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[PURE_BLACK_ENABLED_KEY] ?: false
    }

    override val areHapticsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAPTICS_ENABLED_KEY] ?: true
    }

    override val deviceName: Flow<String?> = dataStore.data.map { preferences ->
        preferences[DEVICE_NAME_KEY]
    }

    override val roomTemperatureCelsius: Flow<Double> = dataStore.data.map { preferences ->
        preferences[ROOM_TEMPERATURE_CELSIUS_KEY] ?: DEFAULT_TEMPERATURE_C
    }

    override val volume: Flow<Float> = dataStore.data.map { preferences ->
        preferences[VOLUME_KEY] ?: 1f
    }

    override val distanceOffsetMeters: Flow<Double> = dataStore.data.map { preferences ->
        preferences[DISTANCE_OFFSET_METERS_KEY] ?: 0.0
    }

    override val measurementUnits: Flow<MeasurementUnits> = dataStore.data.map { preferences ->
        val currentPreference = preferences[MEASUREMENT_UNITS_KEY] ?: MeasurementUnits.METRIC.name
        try {
            MeasurementUnits.valueOf(currentPreference)
        } catch (_: Exception) {
            MeasurementUnits.METRIC
        }
    }

    override val isSoundCheckPassed: Flow<Boolean?> = dataStore.data.map { preferences ->
        preferences[SOUND_CHECK_PASSED_KEY]
    }

    override suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[THEME_PREFERENCE_KEY] = theme.name
        }
    }

    override suspend fun setPureBlackEnabled(isEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PURE_BLACK_ENABLED_KEY] = isEnabled
        }
    }

    override suspend fun setHapticsEnabled(isEnabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAPTICS_ENABLED_KEY] = isEnabled
        }
    }

    override suspend fun setDeviceName(name: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_NAME_KEY] = name
        }
    }

    override suspend fun setRoomTemperatureCelsius(celsius: Double) {
        dataStore.edit { preferences ->
            preferences[ROOM_TEMPERATURE_CELSIUS_KEY] = celsius
        }
    }

    override suspend fun setVolume(volume: Float) {
        dataStore.edit { preferences ->
            preferences[VOLUME_KEY] = volume
        }
    }

    override suspend fun setDistanceOffsetMeters(meters: Double) {
        dataStore.edit { preferences ->
            preferences[DISTANCE_OFFSET_METERS_KEY] = meters
        }
    }

    override suspend fun setMeasurementUnits(units: MeasurementUnits) {
        dataStore.edit { preferences ->
            preferences[MEASUREMENT_UNITS_KEY] = units.name
        }
    }

    override suspend fun setSoundCheckPassed(isPassed: Boolean) {
        dataStore.edit { preferences ->
            preferences[SOUND_CHECK_PASSED_KEY] = isPassed
        }
    }

    override suspend fun clear() {
        dataStore.edit { preferences ->
            preferences.remove(THEME_PREFERENCE_KEY)
            preferences.remove(PURE_BLACK_ENABLED_KEY)
            preferences.remove(HAPTICS_ENABLED_KEY)
            preferences.remove(DEVICE_NAME_KEY)
            preferences.remove(ROOM_TEMPERATURE_CELSIUS_KEY)
            preferences.remove(VOLUME_KEY)
            preferences.remove(DISTANCE_OFFSET_METERS_KEY)
            preferences.remove(MEASUREMENT_UNITS_KEY)
            preferences.remove(SOUND_CHECK_PASSED_KEY)
        }
    }
}
