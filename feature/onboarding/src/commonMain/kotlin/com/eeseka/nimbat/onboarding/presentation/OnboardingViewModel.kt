package com.eeseka.nimbat.onboarding.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.audio.util.inBlocksOf
import com.eeseka.nimbat.core.domain.audio.util.recordWhileLooping
import com.eeseka.nimbat.core.domain.audio.util.recordWhilePlaying
import com.eeseka.nimbat.core.domain.device.DeviceConstants
import com.eeseka.nimbat.core.domain.device.DeviceNameProvider
import com.eeseka.nimbat.core.domain.device.validation.DeviceNameValidationState
import com.eeseka.nimbat.core.domain.device.validation.DeviceNameValidator
import com.eeseka.nimbat.core.domain.onboarding.OnboardingStorage
import com.eeseka.nimbat.core.domain.settings.AppPreferences
import com.eeseka.nimbat.core.domain.util.PlatformUtils
import com.eeseka.nimbat.core.domain.util.onFailure
import com.eeseka.nimbat.core.domain.util.onSuccess
import com.eeseka.nimbat.core.presentation.audio.mappers.toUiText
import com.eeseka.nimbat.core.presentation.device.mappers.toUiText
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.onboarding.presentation.model.SoundCheckStatus
import com.eeseka.nimbat.sound.DOPPLER_BLOCK_SIZE
import com.eeseka.nimbat.sound.Finder
import com.eeseka.nimbat.sound.Role
import com.eeseka.nimbat.sound.UP_CHIRP
import com.eeseka.nimbat.sound.dopplerTone
import com.eeseka.nimbat.sound.pairSamples
import com.eeseka.nimbat.sound.toneClarityDb
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

class OnboardingViewModel(
    private val audioEngine: AudioEngine,
    private val onboardingStorage: OnboardingStorage,
    private val appPreferences: AppPreferences,
    private val deviceNameProvider: DeviceNameProvider,
    private val soundDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private companion object {
        /** The pass mark: half of the 40 dB a clean hearing scores, so quiet speakers still pass and hiss (under 10) can't. */
        const val TONE_CLEAR_DB = 20.0
        /** Needing several clear readings in a row stops one lucky reading from passing; three take about a quarter of a second. */
        const val TONE_CLEAR_READINGS_IN_A_ROW = 3
        /** The clarity ring fills at this score. */
        const val TONE_FULL_CLARITY_DB = 40.0
        val TONE_CHECK_TIMEOUT = 5.seconds
        /** The chirp search always finds its best match, so this is the pass mark: just above noise (12–14 dB), far below a real chirp (50+). */
        const val CHIRP_CLEAR_DB = 20.0
    }

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

        viewModelScope.launch {
            val isHeard = listenForOwnSounds()

            _state.update {
                it.copy(
                    soundCheckStatus = if (isHeard) SoundCheckStatus.PASSED else SoundCheckStatus.FAILED,
                    soundCheckClarity = if (isHeard) 1f else 0f
                )
            }
            eventChannel.send(OnboardingEvent.SoundCheckFinished(isHeard))
        }
    }

    private suspend fun listenForOwnSounds(): Boolean {
        val isToneHeard = listenForOwnTone()
        if (!isToneHeard) return false

        if (!_state.value.canHearOwnChirps) return true

        return listenForOwnChirps()
    }

    private suspend fun listenForOwnTone(): Boolean {
        var clearInARow = 0

        return try {
            withTimeoutOrNull(TONE_CHECK_TIMEOUT) {
                audioEngine.recordWhileLooping(dopplerTone())
                    .inBlocksOf(DOPPLER_BLOCK_SIZE)
                    .map { toneClarityDb(it) }
                    .flowOn(soundDispatcher)
                    .first { clarityDb ->
                        _state.update { it.copy(soundCheckClarity = (clarityDb / TONE_FULL_CLARITY_DB).toFloat().coerceIn(0f, 1f)) }
                        clearInARow = if (clarityDb >= TONE_CLEAR_DB) clearInARow + 1 else 0
                        clearInARow >= TONE_CLEAR_READINGS_IN_A_ROW
                    }
                true
            } ?: false
        } catch (e: AudioErrorException) {
            _state.update { it.copy(soundCheckError = e.error.toUiText()) }
            false
        }
    }

    private suspend fun listenForOwnChirps(): Boolean {
        var isHeard = false

        audioEngine.recordWhilePlaying(pairSamples(Role.Starter))
            .onSuccess { recording ->
                val hit = withContext(soundDispatcher) { Finder(recording, UP_CHIRP).find() }
                isHeard = hit != null && hit.clarityDb >= CHIRP_CLEAR_DB
            }
            .onFailure { error ->
                _state.update { it.copy(soundCheckError = error.toUiText()) }
            }

        return isHeard
    }

    private fun completeOnboarding() {
        hasAttemptedSubmit = true

        if (_state.value.isSaving || !validateFormInputs()) return

        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val name = _state.value.deviceNameTextState.text.toString().trim()
            appPreferences.setDeviceName(name)
            onboardingStorage.setOnboardingCompleted()
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
