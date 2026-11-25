package com.eeseka.nimbat.home.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.eeseka.nimbat.core.data.audio.AudioSoundChecker
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.sound.dopplerTone
import com.eeseka.nimbat.testing.collectInBackground
import com.eeseka.nimbat.testing.data.FakeAppPreferences
import com.eeseka.nimbat.testing.data.FakeAudioEngine
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
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var appPreferences: FakeAppPreferences
    private lateinit var audioEngine: FakeAudioEngine

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        appPreferences = FakeAppPreferences()
        audioEngine = FakeAudioEngine()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `a sound check that never ran shows no result`() = runTest {
        val viewModel = createViewModel()

        assertThat(viewModel.state.value.isSoundCheckPassed).isNull()
    }

    @Test
    fun `a failed sound check from earlier shows up in the state`() = runTest {
        appPreferences.setSoundCheckPassed(false)

        val viewModel = createViewModel()

        assertThat(viewModel.state.value.isSoundCheckPassed).isEqualTo(false)
    }

    @Test
    fun `checking again and hearing itself saves a pass`() = runTest {
        appPreferences.setSoundCheckPassed(false)
        audioEngine.heard = List(5) { dopplerTone() }
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAction(HomeAction.OnRunSoundCheckClick)

            assertThat(awaitItem()).isEqualTo(HomeEvent.SoundCheckFinished(isHeard = true))
            assertThat(viewModel.state.value.isRunningSoundCheck).isFalse()
            assertThat(viewModel.state.value.isSoundCheckPassed).isEqualTo(true)
        }
    }

    @Test
    fun `checking again and hearing only silence keeps the fail`() = runTest {
        appPreferences.setSoundCheckPassed(false)
        val viewModel = createViewModel()

        viewModel.onAction(HomeAction.OnRunSoundCheckClick)
        assertThat(viewModel.state.value.isRunningSoundCheck).isTrue()
        advanceUntilIdle()

        assertThat(viewModel.state.value.isRunningSoundCheck).isFalse()
        assertThat(appPreferences.isSoundCheckPassed.first()).isEqualTo(false)
    }

    @Test
    fun `a broken microphone fails the check with an error`() = runTest {
        audioEngine.microphoneError = AudioError.MICROPHONE_UNAVAILABLE
        val viewModel = createViewModel()

        viewModel.events.test {
            viewModel.onAction(HomeAction.OnRunSoundCheckClick)

            assertThat(awaitItem()).isInstanceOf<HomeEvent.Error>()
            assertThat(awaitItem()).isEqualTo(HomeEvent.SoundCheckFinished(isHeard = false))
        }
    }

    private fun TestScope.createViewModel(): HomeViewModel {
        val viewModel = HomeViewModel(
            appPreferences = appPreferences,
            soundChecker = AudioSoundChecker(audioEngine = audioEngine, soundDispatcher = testDispatcher)
        )
        collectInBackground(viewModel.state)
        return viewModel
    }
}
