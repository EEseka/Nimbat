package com.eeseka.nimbat.core.domain.settings

import kotlinx.coroutines.flow.Flow

interface AppPreferences {
    val theme: Flow<AppTheme>
    val isPureBlackEnabled: Flow<Boolean>
    val areHapticsEnabled: Flow<Boolean>
    val deviceName: Flow<String?>
    val roomTemperatureCelsius: Flow<Double>
    val volume: Flow<Float>
    val distanceOffsetMeters: Flow<Double>
    val measurementUnits: Flow<MeasurementUnits>
    val isSoundCheckPassed: Flow<Boolean?>

    suspend fun setTheme(theme: AppTheme)
    suspend fun setPureBlackEnabled(isEnabled: Boolean)
    suspend fun setHapticsEnabled(isEnabled: Boolean)
    suspend fun setDeviceName(name: String)
    suspend fun setRoomTemperatureCelsius(celsius: Double)
    suspend fun setVolume(volume: Float)
    suspend fun setDistanceOffsetMeters(meters: Double)
    suspend fun setMeasurementUnits(units: MeasurementUnits)
    suspend fun setSoundCheckPassed(isPassed: Boolean)
    suspend fun clear()
}
