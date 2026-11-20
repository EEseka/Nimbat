package com.eeseka.nimbat.onboarding.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.device.DeviceConstants
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.onboarding.presentation.model.SoundCheckStatus
import com.eeseka.nimbat.sound.dopplerTone
import com.eeseka.nimbat.testing.collectInBackground
import com.eeseka.nimbat.testing.data.FakeAppPreferences
import com.eeseka.nimbat.testing.data.FakeAudioEngine
import com.eeseka.nimbat.testing.data.FakeDeviceNameProvider
import com.eeseka.nimbat.testing.data.FakeOnboardingStorage
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
class OnboardingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var audioEngine: FakeAudioEngine
    private lateinit var onboardingStorage: FakeOnboardingStorage
    private lateinit var appPreferences: FakeAppPreferences
    private lateinit var deviceNameProvider: FakeDeviceNameProvider

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        audioEngine = FakeAudioEngine()
        onboardingStorage = FakeOnboardingStorage()
        appPreferences = FakeAppPreferences()
        deviceNameProvider = FakeDeviceNameProvider()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the name field starts with the device's own name`() = runTest {
        deviceNameProvider.name = "Emmanuel's iPhone"

        val viewModel = createViewModel()

        assertThat(viewModel.state.value.deviceNameTextState.text.toString()).isEqualTo("Emmanuel's iPhone")
    }

    @Test
    fun `a granted microphone moves the user on`() = runTest {
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAction(OnboardingAction.OnAllowMicrophoneClick)
            assertThat(viewModel.state.value.isRequestingMicrophone).isTrue()

            viewModel.onAction(OnboardingAction.OnMicrophonePermissionResolved(PermissionState.GRANTED))

            assertThat(awaitItem()).isEqualTo(OnboardingEvent.MicrophoneGranted)
            assertThat(viewModel.state.value.isRequestingMicrophone).isFalse()
            assertThat(viewModel.state.value.microphonePermission).isEqualTo(PermissionState.GRANTED)
        }
    }

    @Test
    fun `a refused microphone keeps the user on the microphone page`() = runTest {
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAction(OnboardingAction.OnAllowMicrophoneClick)
            viewModel.onAction(OnboardingAction.OnMicrophonePermissionResolved(PermissionState.PERMANENTLY_DENIED))

            expectNoEvents()
            assertThat(viewModel.state.value.microphonePermission).isEqualTo(PermissionState.PERMANENTLY_DENIED)
        }
    }

    @Test
    fun `a blocked microphone stays blocked until settings grant it`() = runTest {
        val viewModel = createViewModel()
        viewModel.onAction(OnboardingAction.OnMicrophonePermissionResolved(PermissionState.PERMANENTLY_DENIED))

        viewModel.onAction(OnboardingAction.OnMicrophonePermissionChecked(PermissionState.NOT_DETERMINED))
        assertThat(viewModel.state.value.microphonePermission).isEqualTo(PermissionState.PERMANENTLY_DENIED)

        viewModel.onAction(OnboardingAction.OnMicrophonePermissionChecked(PermissionState.GRANTED))
        assertThat(viewModel.state.value.microphonePermission).isEqualTo(PermissionState.GRANTED)
    }

    @Test
    fun `hearing its own tone and chirps passes the sound check and stops the tone`() = runTest {
        audioEngine.heard = List(5) { dopplerTone() }
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAction(OnboardingAction.OnStartSoundCheckClick)

            assertThat(awaitItem()).isEqualTo(OnboardingEvent.SoundCheckFinished(isHeard = true))
            assertThat(viewModel.state.value.soundCheckStatus).isEqualTo(SoundCheckStatus.PASSED)
            assertThat(audioEngine.isLooping).isFalse()
        }
    }

    @Test
    fun `hearing its tone but not its chirps fails the sound check`() = runTest {
        audioEngine.heard = List(5) { dopplerTone() }
        audioEngine.isSpeakerHeard = false
        val viewModel = createViewModel()

        viewModel.onAction(OnboardingAction.OnStartSoundCheckClick)
        advanceUntilIdle()

        assertThat(viewModel.state.value.soundCheckStatus).isEqualTo(SoundCheckStatus.FAILED)
        assertThat(viewModel.state.value.soundCheckError).isNull()
    }

    @Test
    fun `hearing only silence fails the sound check once time runs out`() = runTest {
        audioEngine.heard = List(5) { FloatArray(4_096) }
        val viewModel = createViewModel()

        viewModel.onAction(OnboardingAction.OnStartSoundCheckClick)
        assertThat(viewModel.state.value.soundCheckStatus).isEqualTo(SoundCheckStatus.LISTENING)
        advanceUntilIdle()

        assertThat(viewModel.state.value.soundCheckStatus).isEqualTo(SoundCheckStatus.FAILED)
        assertThat(viewModel.state.value.soundCheckError).isNull()
        assertThat(audioEngine.isLooping).isFalse()
    }

    @Test
    fun `a broken microphone fails the sound check with a reason`() = runTest {
        audioEngine.microphoneError = AudioError.MICROPHONE_UNAVAILABLE
        val viewModel = createViewModel()

        viewModel.onAction(OnboardingAction.OnStartSoundCheckClick)
        advanceUntilIdle()

        assertThat(viewModel.state.value.soundCheckStatus).isEqualTo(SoundCheckStatus.FAILED)
        assertThat(viewModel.state.value.soundCheckError).isNotNull()
    }

    @Test
    fun `get started saves the trimmed name and finishes onboarding`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.value.deviceNameTextState.typeText("  Living room laptop  ")

        viewModel.events.test {
            viewModel.onAction(OnboardingAction.OnGetStartedClick)

            assertThat(awaitItem()).isEqualTo(OnboardingEvent.Success)
        }
        assertThat(appPreferences.deviceName.first()).isEqualTo("Living room laptop")
        assertThat(onboardingStorage.hasSeenOnboarding.first()).isTrue()
    }

    @Test
    fun `a blank name shows an error instead of finishing`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.value.deviceNameTextState.typeText("   ")

        viewModel.onAction(OnboardingAction.OnGetStartedClick)

        assertThat(viewModel.state.value.deviceNameError).isNotNull()
        assertThat(onboardingStorage.hasSeenOnboarding.first()).isFalse()
        assertThat(viewModel.state.value.isSaving).isFalse()
    }

    @Test
    fun `a too long name shows an error instead of finishing`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.value.deviceNameTextState.typeText("A".repeat(DeviceConstants.MAX_NAME_LENGTH + 1))

        viewModel.onAction(OnboardingAction.OnGetStartedClick)

        assertThat(viewModel.state.value.deviceNameError).isNotNull()
        assertThat(onboardingStorage.hasSeenOnboarding.first()).isFalse()
    }

    @Test
    fun `the name error stays quiet until the first press`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.value.deviceNameTextState.typeText("")

        assertThat(viewModel.state.value.deviceNameError).isNull()
    }

    @Test
    fun `the name error clears as soon as a name is typed`() = runTest {
        val viewModel = createViewModel()
        viewModel.state.value.deviceNameTextState.typeText("")
        viewModel.onAction(OnboardingAction.OnGetStartedClick)

        viewModel.state.value.deviceNameTextState.typeText("Living room laptop")

        assertThat(viewModel.state.value.deviceNameError).isNull()
    }

    private fun TestScope.createViewModel(): OnboardingViewModel {
        val viewModel = OnboardingViewModel(
            audioEngine = audioEngine,
            onboardingStorage = onboardingStorage,
            appPreferences = appPreferences,
            deviceNameProvider = deviceNameProvider,
            soundDispatcher = testDispatcher
        )
        collectInBackground(viewModel.state)
        return viewModel
    }
}
