package com.eeseka.nimbat.settings.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.eeseka.nimbat.core.data.audio.AudioSoundChecker
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.domain.settings.AppTheme
import com.eeseka.nimbat.core.domain.settings.MeasurementUnits
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.sound.DEFAULT_TEMPERATURE_C
import com.eeseka.nimbat.sound.Role
import com.eeseka.nimbat.sound.dopplerTone
import com.eeseka.nimbat.sound.pairSamples
import com.eeseka.nimbat.testing.collectInBackground
import com.eeseka.nimbat.testing.data.FakeAppPreferences
import com.eeseka.nimbat.testing.data.FakeAudioEngine
import com.eeseka.nimbat.testing.data.FakeGuardStorage
import com.eeseka.nimbat.testing.data.FakeOnboardingStorage
import com.eeseka.nimbat.testing.data.FakeWaveStorage
import com.eeseka.nimbat.testing.typeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var appPreferences: FakeAppPreferences
    private lateinit var onboardingStorage: FakeOnboardingStorage
    private lateinit var waveStorage: FakeWaveStorage
    private lateinit var guardStorage: FakeGuardStorage
    private lateinit var audioEngine: FakeAudioEngine

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        appPreferences = FakeAppPreferences()
        onboardingStorage = FakeOnboardingStorage()
        waveStorage = FakeWaveStorage()
        guardStorage = FakeGuardStorage()
        audioEngine = FakeAudioEngine()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saved preferences show up in the state`() = runTest {
        appPreferences.setDeviceName("Emmanuel's iPhone")
        appPreferences.setRoomTemperatureCelsius(28.0)
        appPreferences.setMeasurementUnits(MeasurementUnits.IMPERIAL)
        appPreferences.setTheme(AppTheme.DARK)

        val viewModel = createViewModel()

        val state = viewModel.state.value
        assertThat(state.deviceName).isEqualTo("Emmanuel's iPhone")
        assertThat(state.roomTemperatureCelsius).isEqualTo(28.0)
        assertThat(state.measurementUnits).isEqualTo(MeasurementUnits.IMPERIAL)
        assertThat(state.appTheme).isEqualTo(AppTheme.DARK)
    }

    @Test
    fun `renaming saves the trimmed name and closes the sheet`() = runTest {
        appPreferences.setDeviceName("Pixel")
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.OnRenameClick)
        assertThat(viewModel.state.value.showRenameSheet).isTrue()
        assertThat(viewModel.state.value.deviceNameTextState.text.toString()).isEqualTo("Pixel")

        viewModel.state.value.deviceNameTextState.typeText("  Kitchen Tablet  ")
        viewModel.onAction(SettingsAction.OnSaveDeviceNameClick)

        assertThat(appPreferences.deviceName.first()).isEqualTo("Kitchen Tablet")
        assertThat(viewModel.state.value.showRenameSheet).isFalse()
        assertThat(viewModel.state.value.isSavingDeviceName).isFalse()
    }

    @Test
    fun `a blank name shows an error and keeps the sheet open`() = runTest {
        appPreferences.setDeviceName("Pixel")
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.OnRenameClick)
        viewModel.state.value.deviceNameTextState.typeText("   ")
        viewModel.onAction(SettingsAction.OnSaveDeviceNameClick)

        assertThat(viewModel.state.value.deviceNameError).isNotNull()
        assertThat(viewModel.state.value.showRenameSheet).isTrue()
        assertThat(appPreferences.deviceName.first()).isEqualTo("Pixel")
    }

    @Test
    fun `reopening the rename sheet hides the old error`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.OnRenameClick)
        viewModel.state.value.deviceNameTextState.typeText("")
        viewModel.onAction(SettingsAction.OnSaveDeviceNameClick)
        viewModel.onAction(SettingsAction.OnDismissRenameSheet)
        viewModel.onAction(SettingsAction.OnRenameClick)

        assertThat(viewModel.state.value.deviceNameError).isNull()
    }

    @Test
    fun `a sound check that hears itself saves a pass`() = runTest {
        audioEngine.heard = List(5) { dopplerTone() }
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAction(SettingsAction.OnRunSoundCheckClick)

            assertThat(awaitItem()).isEqualTo(SettingsEvent.SoundCheckFinished(isHeard = true))
            assertThat(viewModel.state.value.isRunningSoundCheck).isFalse()
            assertThat(viewModel.state.value.isSoundCheckPassed).isEqualTo(true)
        }
    }

    @Test
    fun `a sound check that hears only silence saves a fail`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.OnRunSoundCheckClick)
        assertThat(viewModel.state.value.isRunningSoundCheck).isTrue()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isRunningSoundCheck).isFalse()
        assertThat(appPreferences.isSoundCheckPassed.first()).isEqualTo(false)
    }

    @Test
    fun `a broken microphone fails the sound check with an error`() = runTest {
        audioEngine.microphoneError = AudioError.MICROPHONE_UNAVAILABLE
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAction(SettingsAction.OnRunSoundCheckClick)

            assertThat(awaitItem()).isInstanceOf<SettingsEvent.Error>()
            assertThat(awaitItem()).isEqualTo(SettingsEvent.SoundCheckFinished(isHeard = false))
        }
    }

    @Test
    fun `the temperature is saved only when the drag ends`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.OnRoomTemperatureChange(31.0))

        assertThat(viewModel.state.value.roomTemperatureCelsius).isEqualTo(31.0)
        assertThat(appPreferences.roomTemperatureCelsius.first()).isEqualTo(DEFAULT_TEMPERATURE_C)

        viewModel.onAction(SettingsAction.OnRoomTemperatureChangeFinished)

        assertThat(appPreferences.roomTemperatureCelsius.first()).isEqualTo(31.0)
    }

    @Test
    fun `the volume is saved when the drag ends`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.OnVolumeChange(0.4f))
        viewModel.onAction(SettingsAction.OnVolumeChangeFinished)

        assertThat(appPreferences.volume.first()).isEqualTo(0.4f)
    }

    @Test
    fun `a blocked microphone opens the system settings`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(SettingsAction.OnMicrophonePermissionChecked(PermissionState.PERMANENTLY_DENIED))

        viewModel.events.test {
            viewModel.onAction(SettingsAction.OnAllowMicrophoneClick)

            assertThat(awaitItem()).isEqualTo(SettingsEvent.OpenMicrophoneSettings)
            assertThat(viewModel.state.value.isRequestingMicrophone).isFalse()
        }
    }

    @Test
    fun `a blocked microphone stays blocked until a check grants it`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(SettingsAction.OnMicrophonePermissionResolved(PermissionState.PERMANENTLY_DENIED))

        viewModel.onAction(SettingsAction.OnMicrophonePermissionChecked(PermissionState.NOT_DETERMINED))
        assertThat(viewModel.state.value.microphonePermission).isEqualTo(PermissionState.PERMANENTLY_DENIED)

        viewModel.onAction(SettingsAction.OnMicrophonePermissionChecked(PermissionState.GRANTED))
        assertThat(viewModel.state.value.microphonePermission).isEqualTo(PermissionState.GRANTED)
    }

    @Test
    fun `an undecided microphone asks for permission`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(SettingsAction.OnMicrophonePermissionChecked(PermissionState.NOT_DETERMINED))

        viewModel.onAction(SettingsAction.OnAllowMicrophoneClick)
        assertThat(viewModel.state.value.isRequestingMicrophone).isTrue()

        viewModel.onAction(SettingsAction.OnMicrophonePermissionResolved(PermissionState.GRANTED))

        assertThat(viewModel.state.value.isRequestingMicrophone).isFalse()
        assertThat(viewModel.state.value.microphonePermission).isEqualTo(PermissionState.GRANTED)
    }

    @Test
    fun `clearing data wipes everything and goes back to onboarding`() = runTest {
        appPreferences.setDeviceName("Pixel")
        appPreferences.setTheme(AppTheme.DARK)
        onboardingStorage.setOnboardingCompleted()
        waveStorage.setBestBatDashScore(12)
        guardStorage.startSession(GuardMode.ALARM, startedAtMillis = 0L)
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAction(SettingsAction.OnClearDataClick)
            assertThat(viewModel.state.value.showClearDataDialog).isTrue()

            viewModel.onAction(SettingsAction.OnClearDataConfirmed)

            assertThat(awaitItem()).isEqualTo(SettingsEvent.DataCleared)
            assertThat(viewModel.state.value.showClearDataDialog).isFalse()
            assertThat(viewModel.state.value.isClearingData).isFalse()
            assertThat(appPreferences.deviceName.first()).isNull()
            assertThat(appPreferences.theme.first()).isEqualTo(AppTheme.SYSTEM)
            assertThat(onboardingStorage.hasSeenOnboarding.first()).isFalse()
            assertThat(waveStorage.bestBatDashScore.first()).isEqualTo(0)
            assertThat(guardStorage.sessions.first()).isEmpty()
        }
    }

    @Test
    fun `the logo plays the chirp pair`() = runTest {
        val viewModel = createViewModel()

        viewModel.onAction(SettingsAction.OnLogoClick)

        assertThat(audioEngine.lastPlayed?.toList()).isEqualTo(pairSamples(Role.Starter).toList())
        assertThat(viewModel.state.value.isPlayingChirp).isFalse()
    }

    @Test
    fun `a failed chirp stays silent`() = runTest {
        audioEngine.playError = AudioError.SPEAKER_UNAVAILABLE
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAction(SettingsAction.OnLogoClick)

            expectNoEvents()
            assertThat(viewModel.state.value.isPlayingChirp).isFalse()
        }
    }

    private fun TestScope.createViewModel(): SettingsViewModel {
        val viewModel = SettingsViewModel(
            appPreferences = appPreferences,
            onboardingStorage = onboardingStorage,
            waveStorage = waveStorage,
            guardStorage = guardStorage,
            audioEngine = audioEngine,
            soundChecker = AudioSoundChecker(audioEngine = audioEngine, soundDispatcher = testDispatcher)
        )
        collectInBackground(viewModel.state)
        return viewModel
    }
}
