package com.eeseka.nimbat.testing.data

import com.eeseka.nimbat.core.domain.settings.AppPreferences
import com.eeseka.nimbat.core.domain.settings.AppTheme
import com.eeseka.nimbat.core.domain.settings.MeasurementUnits
import com.eeseka.nimbat.sound.DEFAULT_TEMPERATURE_C
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAppPreferences : AppPreferences {
    private val themeFlow = MutableStateFlow(AppTheme.SYSTEM)
    private val isPureBlackEnabledFlow = MutableStateFlow(false)
    private val areHapticsEnabledFlow = MutableStateFlow(true)
    private val deviceNameFlow = MutableStateFlow<String?>(null)
    private val roomTemperatureCelsiusFlow = MutableStateFlow(DEFAULT_TEMPERATURE_C)
    private val volumeFlow = MutableStateFlow(1f)
    private val distanceOffsetMetersFlow = MutableStateFlow(0.0)
    private val measurementUnitsFlow = MutableStateFlow(MeasurementUnits.METRIC)
    private val isSoundCheckPassedFlow = MutableStateFlow<Boolean?>(null)

    override val theme: Flow<AppTheme> = themeFlow
    override val isPureBlackEnabled: Flow<Boolean> = isPureBlackEnabledFlow
    override val areHapticsEnabled: Flow<Boolean> = areHapticsEnabledFlow
    override val deviceName: Flow<String?> = deviceNameFlow
    override val roomTemperatureCelsius: Flow<Double> = roomTemperatureCelsiusFlow
    override val volume: Flow<Float> = volumeFlow
    override val distanceOffsetMeters: Flow<Double> = distanceOffsetMetersFlow
    override val measurementUnits: Flow<MeasurementUnits> = measurementUnitsFlow
    override val isSoundCheckPassed: Flow<Boolean?> = isSoundCheckPassedFlow

    override suspend fun setTheme(theme: AppTheme) {
        themeFlow.value = theme
    }

    override suspend fun setPureBlackEnabled(isEnabled: Boolean) {
        isPureBlackEnabledFlow.value = isEnabled
    }

    override suspend fun setHapticsEnabled(isEnabled: Boolean) {
        areHapticsEnabledFlow.value = isEnabled
    }

    override suspend fun setDeviceName(name: String) {
        deviceNameFlow.value = name
    }

    override suspend fun setRoomTemperatureCelsius(celsius: Double) {
        roomTemperatureCelsiusFlow.value = celsius
    }

    override suspend fun setVolume(volume: Float) {
        volumeFlow.value = volume
    }

    override suspend fun setDistanceOffsetMeters(meters: Double) {
        distanceOffsetMetersFlow.value = meters
    }

    override suspend fun setMeasurementUnits(units: MeasurementUnits) {
        measurementUnitsFlow.value = units
    }

    override suspend fun setSoundCheckPassed(isPassed: Boolean) {
        isSoundCheckPassedFlow.value = isPassed
    }

    override suspend fun clear() {
        themeFlow.value = AppTheme.SYSTEM
        isPureBlackEnabledFlow.value = false
        areHapticsEnabledFlow.value = true
        deviceNameFlow.value = null
        roomTemperatureCelsiusFlow.value = DEFAULT_TEMPERATURE_C
        volumeFlow.value = 1f
        distanceOffsetMetersFlow.value = 0.0
        measurementUnitsFlow.value = MeasurementUnits.METRIC
        isSoundCheckPassedFlow.value = null
    }
}
