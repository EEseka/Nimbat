package com.eeseka.nimbat.core.domain.settings

import kotlinx.coroutines.flow.Flow

interface AppPreferences {
    val theme: Flow<AppTheme>
    val isPureBlackEnabled: Flow<Boolean>
    val areHapticsEnabled: Flow<Boolean>
    val deviceName: Flow<String?>

    suspend fun setTheme(theme: AppTheme)
    suspend fun setPureBlackEnabled(isEnabled: Boolean)
    suspend fun setHapticsEnabled(isEnabled: Boolean)
    suspend fun setDeviceName(name: String)
}
