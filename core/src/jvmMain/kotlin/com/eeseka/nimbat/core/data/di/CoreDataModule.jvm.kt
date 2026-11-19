package com.eeseka.nimbat.core.data.di

import com.eeseka.nimbat.core.data.audio.NativeAudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioEngine
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

actual val platformCoreDataModule = module {
    singleOf(::NativeAudioEngine) bind AudioEngine::class
}
