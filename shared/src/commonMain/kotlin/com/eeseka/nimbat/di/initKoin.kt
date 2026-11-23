package com.eeseka.nimbat.di

import com.eeseka.nimbat.core.data.di.coreDataModule
import com.eeseka.nimbat.onboarding.presentation.di.onboardingPresentationModule
import com.eeseka.nimbat.settings.presentation.di.settingsPresentationModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(config: KoinAppDeclaration? = null) {
    startKoin {
        config?.invoke(this)
        modules(
            appModule,
            coreDataModule,
            onboardingPresentationModule,
            settingsPresentationModule
        )
    }
}
