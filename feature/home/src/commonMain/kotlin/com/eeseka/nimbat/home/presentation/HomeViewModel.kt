package com.eeseka.nimbat.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eeseka.nimbat.core.domain.audio.SoundCheckProgress
import com.eeseka.nimbat.core.domain.audio.SoundChecker
import com.eeseka.nimbat.core.domain.settings.AppPreferences
import com.eeseka.nimbat.core.domain.util.PlatformUtils
import com.eeseka.nimbat.core.presentation.audio.mappers.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class HomeViewModel(
    private val appPreferences: AppPreferences,
    private val soundChecker: SoundChecker
) : ViewModel() {

    private val eventChannel = Channel<HomeEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(HomeState())

    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                _state.update { it.copy(canHearOwnChirps = !PlatformUtils.isMacSafari()) }
                observeSoundCheckResult()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HomeState()
        )

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.OnRunSoundCheckClick -> runSoundCheck()
        }
    }

    private fun observeSoundCheckResult() {
        appPreferences.isSoundCheckPassed
            .onEach { isPassed ->
                _state.update { it.copy(isSoundCheckPassed = isPassed) }
            }
            .launchIn(viewModelScope)
    }

    private fun runSoundCheck() {
        _state.update { it.copy(isRunningSoundCheck = true) }

        soundChecker.check(includeChirps = _state.value.canHearOwnChirps)
            .onEach { progress ->
                when (progress) {
                    is SoundCheckProgress.Listening -> Unit
                    is SoundCheckProgress.Finished -> finishSoundCheck(progress.isHeard)
                    is SoundCheckProgress.Failed -> {
                        eventChannel.send(HomeEvent.Error(progress.error.toUiText()))
                        finishSoundCheck(isHeard = false)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun finishSoundCheck(isHeard: Boolean) {
        appPreferences.setSoundCheckPassed(isHeard)
        _state.update { it.copy(isRunningSoundCheck = false) }
        eventChannel.send(HomeEvent.SoundCheckFinished(isHeard))
    }
}
