package com.eeseka.nimbat.core.data.di

import com.eeseka.nimbat.core.data.audio.AudioSoundChecker
import com.eeseka.nimbat.core.data.onboarding.DataStoreOnboardingStorage
import com.eeseka.nimbat.core.data.settings.DataStoreAppPreferences
import com.eeseka.nimbat.core.domain.audio.SoundChecker
import com.eeseka.nimbat.core.domain.onboarding.OnboardingStorage
import com.eeseka.nimbat.core.domain.settings.AppPreferences
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformCoreDataModule: Module

val coreDataModule = module {
    includes(platformCoreDataModule)
    singleOf(::DataStoreOnboardingStorage) bind OnboardingStorage::class
    singleOf(::DataStoreAppPreferences) bind AppPreferences::class
    single<SoundChecker> { AudioSoundChecker(audioEngine = get()) }
}
