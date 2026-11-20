package com.eeseka.nimbat.testing.data

import com.eeseka.nimbat.core.domain.settings.AppPreferences
import com.eeseka.nimbat.core.domain.settings.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAppPreferences : AppPreferences {
    private val themeFlow = MutableStateFlow(AppTheme.SYSTEM)
    private val isPureBlackEnabledFlow = MutableStateFlow(false)
    private val areHapticsEnabledFlow = MutableStateFlow(true)
    private val deviceNameFlow = MutableStateFlow<String?>(null)

    override val theme: Flow<AppTheme> = themeFlow
    override val isPureBlackEnabled: Flow<Boolean> = isPureBlackEnabledFlow
    override val areHapticsEnabled: Flow<Boolean> = areHapticsEnabledFlow
    override val deviceName: Flow<String?> = deviceNameFlow

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
}
