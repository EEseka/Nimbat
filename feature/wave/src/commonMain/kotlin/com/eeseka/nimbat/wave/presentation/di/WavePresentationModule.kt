package com.eeseka.nimbat.wave.presentation.di

import com.eeseka.nimbat.wave.presentation.WaveViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val wavePresentationModule = module {
    viewModelOf(::WaveViewModel)
}
