package com.eeseka.nimbat.core.data.di

import com.eeseka.nimbat.core.data.audio.AudioGestureSensor
import com.eeseka.nimbat.core.data.audio.AudioMovementSensor
import com.eeseka.nimbat.core.data.audio.AudioSoundChecker
import com.eeseka.nimbat.core.data.database.DatabaseFactory
import com.eeseka.nimbat.core.data.guard.AudioGuardMonitor
import com.eeseka.nimbat.core.data.guard.NativeComputerLock
import com.eeseka.nimbat.core.data.guard.RoomGuardStorage
import com.eeseka.nimbat.core.data.onboarding.DataStoreOnboardingStorage
import com.eeseka.nimbat.core.data.settings.DataStoreAppPreferences
import com.eeseka.nimbat.core.data.wave.DataStoreWaveStorage
import com.eeseka.nimbat.core.domain.audio.GestureSensor
import com.eeseka.nimbat.core.domain.audio.MovementSensor
import com.eeseka.nimbat.core.domain.audio.SoundChecker
import com.eeseka.nimbat.core.domain.guard.ComputerLock
import com.eeseka.nimbat.core.domain.guard.GuardMonitor
import com.eeseka.nimbat.core.domain.guard.GuardStorage
import com.eeseka.nimbat.core.domain.onboarding.OnboardingStorage
import com.eeseka.nimbat.core.domain.settings.AppPreferences
import com.eeseka.nimbat.core.domain.wave.WaveStorage
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformCoreDataModule: Module

val coreDataModule = module {
    includes(platformCoreDataModule)
    singleOf(::DataStoreOnboardingStorage) bind OnboardingStorage::class
    singleOf(::DataStoreAppPreferences) bind AppPreferences::class
    singleOf(::DataStoreWaveStorage) bind WaveStorage::class
    single<SoundChecker> { AudioSoundChecker(audioEngine = get()) }
    single<GestureSensor> { AudioGestureSensor(audioEngine = get(), appPreferences = get()) }
    single<MovementSensor> { AudioMovementSensor(audioEngine = get(), appPreferences = get()) }
    single {
        get<DatabaseFactory>()
            .create()
            .build()
    }
    singleOf(::RoomGuardStorage) bind GuardStorage::class
    singleOf(::NativeComputerLock) bind ComputerLock::class
    single<GuardMonitor> {
        AudioGuardMonitor(
            movementSensor = get(),
            audioEngine = get(),
            guardStorage = get(),
            computerLock = get(),
            applicationScope = get()
        )
    }
}
