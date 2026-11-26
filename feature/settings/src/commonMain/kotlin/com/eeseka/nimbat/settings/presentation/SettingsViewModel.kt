package com.eeseka.nimbat.settings.presentation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.SoundCheckProgress
import com.eeseka.nimbat.core.domain.audio.SoundChecker
import com.eeseka.nimbat.core.domain.device.validation.DeviceNameValidationState
import com.eeseka.nimbat.core.domain.device.validation.DeviceNameValidator
import com.eeseka.nimbat.core.domain.onboarding.OnboardingStorage
import com.eeseka.nimbat.core.domain.settings.AppPreferences
import com.eeseka.nimbat.core.domain.settings.AppTheme
import com.eeseka.nimbat.core.domain.settings.MeasurementUnits
import com.eeseka.nimbat.core.domain.util.PlatformUtils
import com.eeseka.nimbat.core.domain.wave.WaveStorage
import com.eeseka.nimbat.core.presentation.audio.mappers.toUiText
import com.eeseka.nimbat.core.presentation.device.mappers.toUiText
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.sound.Role
import com.eeseka.nimbat.sound.pairSamples
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val appPreferences: AppPreferences,
    private val onboardingStorage: OnboardingStorage,
    private val waveStorage: WaveStorage,
    private val audioEngine: AudioEngine,
    private val soundChecker: SoundChecker
) : ViewModel() {

    private val eventChannel = Channel<SettingsEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(SettingsState())

    val state = _state
        .onStart {
            if (!hasLoadedInitialData) {
                _state.update {
                    it.copy(
                        canHearOwnChirps = !PlatformUtils.isMacSafari(),
                        canUseHaptics = PlatformUtils.hasHaptics()
                    )
                }
                observeAppearanceAndFeel()
                observeMeasurementSettings()
                observeDeviceName()
                observeSoundCheckResult()
                observeDeviceNameValidation()
                hasLoadedInitialData = true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SettingsState()
        )

    private var hasAttemptedSave = false // Errors stay quiet until the first press, then track every keystroke.

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.OnMicrophonePermissionChecked -> onMicrophonePermissionChecked(action.state)
            SettingsAction.OnAllowMicrophoneClick -> allowMicrophone()
            is SettingsAction.OnMicrophonePermissionResolved -> _state.update {
                it.copy(microphonePermission = action.state, isRequestingMicrophone = false)
            }

            SettingsAction.OnRenameClick -> openRenameSheet()
            SettingsAction.OnSaveDeviceNameClick -> saveDeviceName()
            SettingsAction.OnDismissRenameSheet -> _state.update { it.copy(showRenameSheet = false) }

            SettingsAction.OnRunSoundCheckClick -> runSoundCheck()

            is SettingsAction.OnRoomTemperatureChange -> _state.update { it.copy(roomTemperatureCelsius = action.celsius) }
            SettingsAction.OnRoomTemperatureChangeFinished -> saveRoomTemperature()
            is SettingsAction.OnVolumeChange -> _state.update { it.copy(volume = action.volume) }
            SettingsAction.OnVolumeChangeFinished -> saveVolume()
            is SettingsAction.OnMeasurementUnitsSelected -> selectMeasurementUnits(action.units)

            is SettingsAction.OnThemeSelected -> selectTheme(action.theme)
            is SettingsAction.OnPureBlackToggled -> togglePureBlack(action.isEnabled)
            is SettingsAction.OnHapticsToggled -> toggleHaptics(action.isEnabled)

            SettingsAction.OnClearDataClick -> _state.update { it.copy(showClearDataDialog = true) }
            SettingsAction.OnClearDataConfirmed -> clearData()
            SettingsAction.OnDismissClearDataDialog -> _state.update { it.copy(showClearDataDialog = false) }

            SettingsAction.OnLogoClick -> playChirp()
        }
    }

    private fun observeAppearanceAndFeel() {
        combine(
            appPreferences.theme,
            appPreferences.isPureBlackEnabled,
            appPreferences.areHapticsEnabled
        ) { theme, isPureBlackEnabled, areHapticsEnabled ->
            _state.update {
                it.copy(
                    appTheme = theme,
                    isPureBlackEnabled = isPureBlackEnabled,
                    areHapticsEnabled = areHapticsEnabled
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observeMeasurementSettings() {
        combine(
            appPreferences.roomTemperatureCelsius,
            appPreferences.volume,
            appPreferences.distanceOffsetMeters,
            appPreferences.measurementUnits
        ) { roomTemperatureCelsius, volume, distanceOffsetMeters, measurementUnits ->
            _state.update {
                it.copy(
                    roomTemperatureCelsius = roomTemperatureCelsius,
                    volume = volume,
                    distanceOffsetMeters = distanceOffsetMeters,
                    measurementUnits = measurementUnits
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun observeDeviceName() {
        appPreferences.deviceName
            .onEach { name ->
                _state.update { it.copy(deviceName = name.orEmpty()) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSoundCheckResult() {
        appPreferences.isSoundCheckPassed
            .onEach { isPassed ->
                _state.update { it.copy(isSoundCheckPassed = isPassed) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeDeviceNameValidation() {
        snapshotFlow { _state.value.deviceNameTextState.text.toString() }
            .onEach { name ->
                val validationState = DeviceNameValidator.validate(name)

                _state.update {
                    it.copy(deviceNameError = if (hasAttemptedSave) validationState.toUiText() else null)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun onMicrophonePermissionChecked(permission: PermissionState) {
        // On Android a quiet check reports a blocked mic as NOT_DETERMINED, so keep PERMANENTLY_DENIED until a check says GRANTED
        val isStillBlocked = _state.value.microphonePermission == PermissionState.PERMANENTLY_DENIED && permission != PermissionState.GRANTED
        if (isStillBlocked) return

        _state.update { it.copy(microphonePermission = permission) }
    }

    private fun allowMicrophone() {
        if (_state.value.microphonePermission == PermissionState.PERMANENTLY_DENIED) {
            viewModelScope.launch {
                eventChannel.send(SettingsEvent.OpenMicrophoneSettings)
            }
        } else {
            _state.update { it.copy(isRequestingMicrophone = true) }
        }
    }

    private fun openRenameSheet() {
        hasAttemptedSave = false
        _state.value.deviceNameTextState.setTextAndPlaceCursorAtEnd(_state.value.deviceName)
        _state.update { it.copy(showRenameSheet = true, deviceNameError = null) }
    }

    private fun saveDeviceName() {
        hasAttemptedSave = true

        if (_state.value.isSavingDeviceName || !validateFormInputs()) return

        _state.update { it.copy(isSavingDeviceName = true) }

        viewModelScope.launch {
            val name = _state.value.deviceNameTextState.text.toString().trim()
            appPreferences.setDeviceName(name)
            _state.update { it.copy(showRenameSheet = false, isSavingDeviceName = false) }
        }
    }

    private fun validateFormInputs(): Boolean {
        val name = _state.value.deviceNameTextState.text.toString()
        val nameState = DeviceNameValidator.validate(name)

        _state.update { it.copy(deviceNameError = nameState.toUiText()) }

        return nameState == DeviceNameValidationState.VALID
    }

    private fun runSoundCheck() {
        _state.update { it.copy(isRunningSoundCheck = true) }

        soundChecker.check(includeChirps = _state.value.canHearOwnChirps)
            .onEach { progress ->
                when (progress) {
                    is SoundCheckProgress.Listening -> Unit
                    is SoundCheckProgress.Finished -> finishSoundCheck(progress.isHeard)
                    is SoundCheckProgress.Failed -> {
                        eventChannel.send(SettingsEvent.Error(progress.error.toUiText()))
                        finishSoundCheck(isHeard = false)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun finishSoundCheck(isHeard: Boolean) {
        appPreferences.setSoundCheckPassed(isHeard)
        _state.update { it.copy(isRunningSoundCheck = false) }
        eventChannel.send(SettingsEvent.SoundCheckFinished(isHeard))
    }

    private fun saveRoomTemperature() {
        viewModelScope.launch {
            appPreferences.setRoomTemperatureCelsius(_state.value.roomTemperatureCelsius)
        }
    }

    private fun saveVolume() {
        viewModelScope.launch {
            appPreferences.setVolume(_state.value.volume)
        }
    }

    private fun selectMeasurementUnits(units: MeasurementUnits) {
        viewModelScope.launch {
            appPreferences.setMeasurementUnits(units)
        }
    }

    private fun selectTheme(theme: AppTheme) {
        viewModelScope.launch {
            appPreferences.setTheme(theme)
        }
    }

    private fun togglePureBlack(isEnabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setPureBlackEnabled(isEnabled)
        }
    }

    private fun toggleHaptics(isEnabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setHapticsEnabled(isEnabled)
        }
    }

    private fun clearData() {
        _state.update { it.copy(showClearDataDialog = false, isClearingData = true) }

        viewModelScope.launch {
            appPreferences.clear()
            onboardingStorage.clear()
            waveStorage.clear()
            _state.update { it.copy(isClearingData = false) }
            eventChannel.send(SettingsEvent.DataCleared)
        }
    }

    private fun playChirp() {
        _state.update { it.copy(isPlayingChirp = true) }

        viewModelScope.launch {
            audioEngine.play(pairSamples(Role.Starter))

            _state.update { it.copy(isPlayingChirp = false) }
        }
    }
}
