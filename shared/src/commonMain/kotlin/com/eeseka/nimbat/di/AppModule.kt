package com.eeseka.nimbat.di

import com.eeseka.nimbat.sound_check.SoundCheckViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::SoundCheckViewModel)
}
