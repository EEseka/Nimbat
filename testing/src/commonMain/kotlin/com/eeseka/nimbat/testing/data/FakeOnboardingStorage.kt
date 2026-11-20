package com.eeseka.nimbat.testing.data

import com.eeseka.nimbat.core.domain.onboarding.OnboardingStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeOnboardingStorage : OnboardingStorage {
    private val hasSeenOnboardingFlow = MutableStateFlow(false)
    override val hasSeenOnboarding: Flow<Boolean> = hasSeenOnboardingFlow

    override suspend fun setOnboardingCompleted() {
        hasSeenOnboardingFlow.value = true
    }
}
