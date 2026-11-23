package com.eeseka.nimbat.onboarding.presentation.di

import com.eeseka.nimbat.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val onboardingPresentationModule = module {
    viewModelOf(::OnboardingViewModel)
}
