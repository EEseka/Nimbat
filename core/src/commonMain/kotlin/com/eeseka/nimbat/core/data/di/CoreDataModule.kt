package com.eeseka.nimbat.core.data.di

import org.koin.core.module.Module
import org.koin.dsl.module

expect val platformCoreDataModule: Module

val coreDataModule = module {
    includes(platformCoreDataModule)
}
