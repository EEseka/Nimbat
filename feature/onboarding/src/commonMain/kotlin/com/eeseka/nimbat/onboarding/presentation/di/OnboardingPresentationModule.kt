package com.eeseka.nimbat.onboarding.presentation.di

import com.eeseka.nimbat.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val onboardingPresentationModule = module {
    viewModel { OnboardingViewModel(get(), get(), get(), get()) }
}
