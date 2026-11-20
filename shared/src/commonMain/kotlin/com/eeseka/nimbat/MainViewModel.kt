package com.eeseka.nimbat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eeseka.nimbat.core.domain.onboarding.OnboardingStorage
import com.eeseka.nimbat.core.domain.settings.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val appPreferences: AppPreferences,
    private val onboardingStorage: OnboardingStorage
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())

    val state = combine(
        _state,
        appPreferences.theme,
        appPreferences.isPureBlackEnabled,
        appPreferences.areHapticsEnabled
    ) { onboardingState, theme, isPureBlackEnabled, areHapticsEnabled ->
        onboardingState.copy(
            theme = theme,
            isPureBlackEnabled = isPureBlackEnabled,
            areHapticsEnabled = areHapticsEnabled
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = MainState()
        )

    init {
        viewModelScope.launch {
            val hasSeenOnboarding = onboardingStorage.hasSeenOnboarding.first()
            _state.update {
                it.copy(
                    isCheckingOnboarding = false,
                    hasSeenOnboarding = hasSeenOnboarding
                )
            }
        }
    }
}
