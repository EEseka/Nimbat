package com.eeseka.nimbat.guard.presentation.di

import com.eeseka.nimbat.guard.presentation.GuardViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val guardPresentationModule = module {
    viewModelOf(::GuardViewModel)
}
