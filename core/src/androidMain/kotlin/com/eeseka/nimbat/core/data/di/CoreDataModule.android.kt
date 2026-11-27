package com.eeseka.nimbat.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.eeseka.nimbat.core.data.audio.NativeAudioEngine
import com.eeseka.nimbat.core.data.database.DatabaseFactory
import com.eeseka.nimbat.core.data.device.NativeDeviceNameProvider
import com.eeseka.nimbat.core.data.util.createDataStore
import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.device.DeviceNameProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformCoreDataModule = module {
    single<DataStore<Preferences>> { createDataStore(androidContext()) }
    singleOf(::NativeAudioEngine) bind AudioEngine::class
    singleOf(::NativeDeviceNameProvider) bind DeviceNameProvider::class
    single { DatabaseFactory(androidContext()) }
}
