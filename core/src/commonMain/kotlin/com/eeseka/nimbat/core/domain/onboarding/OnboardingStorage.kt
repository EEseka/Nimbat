package com.eeseka.nimbat.core.domain.onboarding

import kotlinx.coroutines.flow.Flow

interface OnboardingStorage {
    val hasSeenOnboarding: Flow<Boolean>

    suspend fun setOnboardingCompleted()
    suspend fun clear()
}
