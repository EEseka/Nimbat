package com.eeseka.nimbat.onboarding.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eeseka.nimbat.core.domain.audio.SoundCheckProgress
import com.eeseka.nimbat.core.domain.audio.SoundChecker
import com.eeseka.nimbat.core.domain.device.DeviceConstants
import com.eeseka.nimbat.core.domain.device.DeviceNameProvider
import com.eeseka.nimbat.core.domain.device.validation.DeviceNameValidationState
import com.eeseka.nimbat.core.domain.device.validation.DeviceNameValidator
import com.eeseka.nimbat.core.domain.onboarding.OnboardingStorage
import com.eeseka.nimbat.core.domain.settings.AppPreferences
import com.eeseka.nimbat.core.domain.util.PlatformUtils
import com.eeseka.nimbat.core.presentation.audio.mappers.toUiText
import com.eeseka.nimbat.core.presentation.device.mappers.toUiText
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.onboarding.presentation.model.SoundCheckStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val soundChecker: SoundChecker,
    private val onboardingStorage: OnboardingStorage,
    private val appPreferences: AppPreferences,
    private val deviceNameProvider: DeviceNameProvider
) : ViewModel() {

    private var hasLoadedInitialData = false

    private val eventChannel = Channel<OnboardingEvent>()
    val events = eventChannel.receiveAsFlow()

    private val _state = MutableStateFlow(OnboardingState())

    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                loadInitialData()
                observeDeviceNameValidation()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = OnboardingState()
        )

    private var hasAttemptedSubmit = false // Errors stay quiet until the first press, then track every keystroke.
    private var hasCheckedMicrophone = false

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.OnAllowMicrophoneClick -> _state.update { it.copy(isRequestingMicrophone = true) }
            is OnboardingAction.OnMicrophonePermissionChecked -> onMicrophonePermissionChecked(action.state)
            is OnboardingAction.OnMicrophonePermissionResolved -> onMicrophonePermissionResolved(action.state)
            OnboardingAction.OnStartSoundCheckClick -> startSoundCheck()
            OnboardingAction.OnGetStartedClick -> completeOnboarding()
        }
    }

    private fun loadInitialData() {
        _state.update { it.copy(canHearOwnChirps = !PlatformUtils.isMacSafari()) }

        viewModelScope.launch {
            val name = deviceNameProvider.deviceName()
            _state.value.deviceNameTextState.setTextAndPlaceCursorAtEnd(name.take(DeviceConstants.MAX_NAME_LENGTH))
        }
    }

    private fun observeDeviceNameValidation() {
        snapshotFlow { _state.value.deviceNameTextState.text.toString() }
            .onEach { name ->
                val validationState = DeviceNameValidator.validate(name)

                _state.update {
                    it.copy(deviceNameError = if (hasAttemptedSubmit) validationState.toUiText() else null)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onMicrophonePermissionChecked(permission: PermissionState) {
        // iOS restarts the app when the mic is changed in Settings, so a decided mic on the first check means the person had reached it
        if (!hasCheckedMicrophone && PlatformUtils.isIOS() && permission != PermissionState.NOT_DETERMINED) {
            viewModelScope.launch {
                eventChannel.send(OnboardingEvent.ResumeAtMicrophone)
            }
        }
        hasCheckedMicrophone = true

        // On Android a quiet check reports a blocked mic as NOT_DETERMINED, so keep PERMANENTLY_DENIED until a check says GRANTED
        val isStillBlocked = _state.value.microphonePermission == PermissionState.PERMANENTLY_DENIED && permission != PermissionState.GRANTED
        if (isStillBlocked) return

        _state.update { it.copy(microphonePermission = permission) }
    }

    private fun onMicrophonePermissionResolved(permission: PermissionState) {
        _state.update { it.copy(microphonePermission = permission, isRequestingMicrophone = false) }

        if (permission == PermissionState.GRANTED) {
            viewModelScope.launch {
                eventChannel.send(OnboardingEvent.MicrophoneGranted)
            }
        }
    }

    private fun startSoundCheck() {
        _state.update {
            it.copy(
                soundCheckStatus = SoundCheckStatus.LISTENING,
                soundCheckClarity = 0f,
                soundCheckError = null
            )
        }

        soundChecker.check(includeChirps = _state.value.canHearOwnChirps)
            .onEach { progress ->
                when (progress) {
                    is SoundCheckProgress.Listening -> _state.update { it.copy(soundCheckClarity = progress.clarity) }
                    is SoundCheckProgress.Finished -> finishSoundCheck(progress.isHeard)
                    is SoundCheckProgress.Failed -> {
                        _state.update { it.copy(soundCheckError = progress.error.toUiText()) }
                        finishSoundCheck(false)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun finishSoundCheck(isHeard: Boolean) {
        appPreferences.setSoundCheckPassed(isHeard)
        _state.update {
            it.copy(
                soundCheckStatus = if (isHeard) SoundCheckStatus.PASSED else SoundCheckStatus.FAILED,
                soundCheckClarity = if (isHeard) 1f else 0f
            )
        }
        eventChannel.send(OnboardingEvent.SoundCheckFinished(isHeard))
    }

    private fun completeOnboarding() {
        hasAttemptedSubmit = true

        if (_state.value.isSaving || !validateFormInputs()) return

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val name = _state.value.deviceNameTextState.text.toString().trim()
            appPreferences.setDeviceName(name)
            onboardingStorage.setOnboardingCompleted()
            _state.update { it.copy(isSaving = false) }
            eventChannel.send(OnboardingEvent.Success)
        }
    }

    private fun validateFormInputs(): Boolean {
        val name = _state.value.deviceNameTextState.text.toString()
        val nameState = DeviceNameValidator.validate(name)

        _state.update { it.copy(deviceNameError = nameState.toUiText()) }

        return nameState == DeviceNameValidationState.VALID
    }
}
