package com.eeseka.nimbat.wave.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import com.eeseka.nimbat.core.data.audio.AudioGestureSensor
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.sound.DOPPLER_BLOCK_SIZE
import com.eeseka.nimbat.sound.DOPPLER_CARRIER_HZ
import com.eeseka.nimbat.sound.GestureDirection
import com.eeseka.nimbat.sound.SAMPLE_RATE
import com.eeseka.nimbat.sound.dopplerShiftHz
import com.eeseka.nimbat.sound.dopplerTone
import com.eeseka.nimbat.testing.collectInBackground
import com.eeseka.nimbat.testing.data.FakeAppPreferences
import com.eeseka.nimbat.testing.data.FakeAudioEngine
import com.eeseka.nimbat.testing.data.FakeWaveStorage
import com.eeseka.nimbat.wave.presentation.model.BAT_X
import com.eeseka.nimbat.wave.presentation.model.BatDashPhase
import com.eeseka.nimbat.wave.presentation.model.CaveLane
import com.eeseka.nimbat.wave.presentation.model.WaveDemo
import com.eeseka.nimbat.wave.presentation.model.WaveSensorStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WaveViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var audioEngine: FakeAudioEngine
    private lateinit var waveStorage: FakeWaveStorage

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        audioEngine = FakeAudioEngine().apply { isSpeakerHeard = false }
        waveStorage = FakeWaveStorage()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `nothing plays before the microphone check comes back`() = waveTest { viewModel ->
        viewModel.onAction(WaveAction.OnScreenStarted)

        assertThat(audioEngine.isLooping).isFalse()
    }

    @Test
    fun `an allowed microphone starts the tone while the screen shows`() = waveTest { viewModel ->
        audioEngine.heard = still(blocks = 1)

        viewModel.startListening()

        assertThat(audioEngine.isLooping).isTrue()
        assertThat(viewModel.state.value.sensorStatus).isEqualTo(WaveSensorStatus.CALIBRATING)
    }

    @Test
    fun `leaving the screen stops the tone`() = waveTest { viewModel ->
        audioEngine.heard = still(blocks = 1)
        viewModel.startListening()
        assertThat(audioEngine.isLooping).isTrue()

        viewModel.onAction(WaveAction.OnScreenStopped)

        assertThat(audioEngine.isLooping).isFalse()
    }

    @Test
    fun `an undecided microphone is asked for once by itself`() = waveTest { viewModel ->
        viewModel.onAction(WaveAction.OnMicrophonePermissionChecked(PermissionState.NOT_DETERMINED))
        assertThat(viewModel.state.value.isRequestingMicrophone).isTrue()

        viewModel.onAction(WaveAction.OnMicrophonePermissionResolved(PermissionState.DENIED))
        viewModel.onAction(WaveAction.OnMicrophonePermissionChecked(PermissionState.NOT_DETERMINED))
        assertThat(viewModel.state.value.isRequestingMicrophone).isFalse()
    }

    @Test
    fun `a blocked microphone opens settings from the allow button`() = waveTest { viewModel ->
        viewModel.onAction(WaveAction.OnMicrophonePermissionChecked(PermissionState.PERMANENTLY_DENIED))

        viewModel.events.test {
            viewModel.onAction(WaveAction.OnAllowMicrophoneClick)

            assertThat(awaitItem()).isEqualTo(WaveEvent.OpenMicrophoneSettings)
        }
    }

    @Test
    fun `a still room finishes calibrating`() = waveTest { viewModel ->
        audioEngine.heard = still(blocks = 20)

        viewModel.startListening()

        assertThat(viewModel.state.value.sensorStatus).isEqualTo(WaveSensorStatus.SENSING)
        assertThat(viewModel.state.value.isToneClear).isTrue()
    }

    @Test
    fun `pushing toward the screen shows the next slide`() = waveTest { viewModel ->
        audioEngine.heard = inOrder(still(blocks = 20), push(speed = 1.2), still(blocks = 4))

        viewModel.events.test {
            viewModel.startListening()

            assertThat(awaitItem()).isEqualTo(WaveEvent.GestureDetected)
            assertThat(viewModel.state.value.slideIndex).isEqualTo(1)
            assertThat(viewModel.state.value.lastGesture).isEqualTo(GestureDirection.TOWARD)
        }
    }

    @Test
    fun `pulling away on the first slide stays there`() = waveTest { viewModel ->
        audioEngine.heard = inOrder(still(blocks = 20), push(speed = -1.2), still(blocks = 4))

        viewModel.events.test {
            viewModel.startListening()

            assertThat(awaitItem()).isEqualTo(WaveEvent.GestureDetected)
            assertThat(viewModel.state.value.slideIndex).isEqualTo(0)
            assertThat(viewModel.state.value.gestureCount).isEqualTo(1)
        }
    }

    @Test
    fun `pushing in the gallery shows the next animal`() = waveTest { viewModel ->
        audioEngine.heard = inOrder(still(blocks = 20), push(speed = 1.2), still(blocks = 4))
        viewModel.onAction(WaveAction.OnDemoSelected(WaveDemo.GALLERY))

        viewModel.events.test {
            viewModel.startListening()

            assertThat(awaitItem()).isEqualTo(WaveEvent.GestureDetected)
            assertThat(viewModel.state.value.animalIndex).isEqualTo(1)
            assertThat(viewModel.state.value.slideIndex).isEqualTo(0)
        }
    }

    @Test
    fun `pushing down in bat dash takes off low`() = waveTest { viewModel ->
        audioEngine.heard = inOrder(still(blocks = 20), push(speed = 1.2), still(blocks = 4))
        viewModel.onAction(WaveAction.OnDemoSelected(WaveDemo.BAT_DASH))

        viewModel.events.test {
            viewModel.startListening()

            assertThat(awaitItem()).isEqualTo(WaveEvent.GestureDetected)
            assertThat(viewModel.state.value.batDash.phase).isEqualTo(BatDashPhase.FLYING)
            assertThat(viewModel.state.value.batDash.batLane).isEqualTo(CaveLane.LOW)
        }
    }

    @Test
    fun `lifting the hand in bat dash flies the bat up`() = waveTest { viewModel ->
        audioEngine.heard = inOrder(still(blocks = 20), push(speed = -1.2), still(blocks = 4))
        viewModel.onAction(WaveAction.OnDemoSelected(WaveDemo.BAT_DASH))
        viewModel.onAction(WaveAction.OnCaveTap(CaveLane.LOW))

        viewModel.events.test {
            viewModel.startListening()

            assertThat(awaitItem()).isEqualTo(WaveEvent.GestureDetected)
            assertThat(viewModel.state.value.batDash.batLane).isEqualTo(CaveLane.HIGH)
        }
    }

    @Test
    fun `a dive and a lift after a moment's rest both steer the bat`() = waveTest { viewModel ->
        audioEngine.heard = inOrder(
            still(blocks = 20), push(speed = 1.2), still(blocks = 3), push(speed = -1.2), still(blocks = 4)
        )
        viewModel.onAction(WaveAction.OnDemoSelected(WaveDemo.BAT_DASH))

        viewModel.events.test {
            viewModel.startListening()

            assertThat(awaitItem()).isEqualTo(WaveEvent.GestureDetected)
            assertThat(awaitItem()).isEqualTo(WaveEvent.GestureDetected)
            assertThat(viewModel.state.value.batDash.batLane).isEqualTo(CaveLane.HIGH)
        }
    }

    @Test
    fun `the hand bouncing back after a dive does not lift the bat`() = waveTest { viewModel ->
        audioEngine.heard = inOrder(still(blocks = 20), push(speed = 1.2), push(speed = -1.2), still(blocks = 4))
        viewModel.onAction(WaveAction.OnDemoSelected(WaveDemo.BAT_DASH))

        viewModel.events.test {
            viewModel.startListening()

            assertThat(awaitItem()).isEqualTo(WaveEvent.GestureDetected)
            expectNoEvents()
            assertThat(viewModel.state.value.batDash.batLane).isEqualTo(CaveLane.LOW)
        }
    }

    @Test
    fun `the hand coming back after a slide push does not go back a slide`() = waveTest { viewModel ->
        audioEngine.heard = inOrder(still(blocks = 20), push(speed = 1.2), push(speed = -1.2), still(blocks = 4))

        viewModel.events.test {
            viewModel.startListening()

            assertThat(awaitItem()).isEqualTo(WaveEvent.GestureDetected)
            expectNoEvents()
            assertThat(viewModel.state.value.slideIndex).isEqualTo(1)
        }
    }

    @Test
    fun `frames bring rocks into the cave`() = waveTest { viewModel ->
        viewModel.onAction(WaveAction.OnDemoSelected(WaveDemo.BAT_DASH))
        viewModel.onAction(WaveAction.OnCaveTap(CaveLane.HIGH))

        viewModel.sendFrames(seconds = 2)

        assertThat(viewModel.state.value.batDash.rocks).isNotEmpty()
    }

    @Test
    fun `a bat that never moves crashes`() = waveTest { viewModel ->
        viewModel.onAction(WaveAction.OnDemoSelected(WaveDemo.BAT_DASH))
        viewModel.onAction(WaveAction.OnCaveTap(CaveLane.HIGH))

        viewModel.events.test {
            // At most two rocks in a row block the same lane, so a rock in the high lane comes within a few
            viewModel.sendFrames(seconds = 20)

            assertThat(awaitItem()).isEqualTo(WaveEvent.BatCrashed)
            assertThat(viewModel.state.value.batDash.phase).isEqualTo(BatDashPhase.CRASHED)
        }
    }

    @Test
    fun `leaving bat dash ends the run`() = waveTest { viewModel ->
        viewModel.onAction(WaveAction.OnDemoSelected(WaveDemo.BAT_DASH))
        viewModel.onAction(WaveAction.OnCaveTap(CaveLane.LOW))

        viewModel.onAction(WaveAction.OnDemoSelected(WaveDemo.SLIDES))

        assertThat(viewModel.state.value.batDash.phase).isEqualTo(BatDashPhase.READY)
    }

    @Test
    fun `the saved best score shows in bat dash`() = waveTest { viewModel ->
        waveStorage.setBestBatDashScore(12)

        assertThat(viewModel.state.value.batDash.bestScore).isEqualTo(12)
    }

    @Test
    fun `a new best score is saved`() = waveTest { viewModel ->
        viewModel.onAction(WaveAction.OnDemoSelected(WaveDemo.BAT_DASH))
        viewModel.onAction(WaveAction.OnCaveTap(CaveLane.HIGH))
        viewModel.dodgeRocks(seconds = 10)

        viewModel.onAction(WaveAction.OnScreenStopped)

        assertThat(waveStorage.bestBatDashScore.first()).isGreaterThan(0)
        assertThat(waveStorage.bestBatDashScore.first()).isEqualTo(viewModel.state.value.batDash.bestScore)
    }

    @Test
    fun `a lower score keeps the saved best`() = waveTest { viewModel ->
        waveStorage.setBestBatDashScore(50)
        viewModel.onAction(WaveAction.OnDemoSelected(WaveDemo.BAT_DASH))
        viewModel.onAction(WaveAction.OnCaveTap(CaveLane.HIGH))

        viewModel.sendFrames(seconds = 20)

        assertThat(viewModel.state.value.batDash.phase).isEqualTo(BatDashPhase.CRASHED)
        assertThat(waveStorage.bestBatDashScore.first()).isEqualTo(50)
        assertThat(viewModel.state.value.batDash.bestScore).isEqualTo(50)
    }

    @Test
    fun `a microphone taken mid-calibration starts over by itself`() = waveTest { viewModel ->
        // Like Siri taking the mic: a few blocks arrive, then the recording stops sending anything
        audioEngine.stallingRecordings = 1
        audioEngine.heard = still(blocks = 5)
        viewModel.startListening()
        assertThat(viewModel.state.value.sensorStatus).isEqualTo(WaveSensorStatus.CALIBRATING)

        audioEngine.heard = still(blocks = 20)
        advanceTimeBy(6.seconds)

        assertThat(viewModel.state.value.sensorStatus).isEqualTo(WaveSensorStatus.SENSING)
    }

    @Test
    fun `a microphone that never comes back stops wave with an error`() = waveTest { viewModel ->
        audioEngine.stallingRecordings = 3
        audioEngine.heard = still(blocks = 5)

        viewModel.events.test {
            viewModel.startListening()

            assertThat(awaitItem()).isInstanceOf<WaveEvent.Error>()
            assertThat(viewModel.state.value.sensorStatus).isEqualTo(WaveSensorStatus.FAILED)
        }
    }

    @Test
    fun `a swipe that settles on a slide is kept`() = waveTest { viewModel ->
        viewModel.onAction(WaveAction.OnSlideSettled(3))

        assertThat(viewModel.state.value.slideIndex).isEqualTo(3)
    }

    @Test
    fun `a broken microphone stops wave with an error`() = waveTest { viewModel ->
        audioEngine.microphoneError = AudioError.MICROPHONE_UNAVAILABLE

        viewModel.events.test {
            viewModel.startListening()

            assertThat(awaitItem()).isInstanceOf<WaveEvent.Error>()
            assertThat(viewModel.state.value.sensorStatus).isEqualTo(WaveSensorStatus.FAILED)
        }
    }

    @Test
    fun `trying again starts the tone again`() = waveTest { viewModel ->
        audioEngine.microphoneError = AudioError.MICROPHONE_UNAVAILABLE

        viewModel.events.test {
            viewModel.startListening()
            awaitItem()

            audioEngine.microphoneError = null
            audioEngine.heard = still(blocks = 1)
            viewModel.onAction(WaveAction.OnTryAgainClick)

            assertThat(audioEngine.isLooping).isTrue()
            assertThat(viewModel.state.value.sensorStatus).isEqualTo(WaveSensorStatus.CALIBRATING)
        }
    }

    /** Runs [body] on a fresh ViewModel, then leaves the screen: the fake mic hears silence forever until it does. */
    private fun waveTest(body: suspend TestScope.(WaveViewModel) -> Unit): TestResult = runTest {
        val viewModel = createViewModel()
        try {
            body(viewModel)
        } finally {
            viewModel.onAction(WaveAction.OnScreenStopped)
        }
    }

    private fun TestScope.createViewModel(): WaveViewModel {
        val viewModel = WaveViewModel(
            gestureSensor = AudioGestureSensor(
                audioEngine = audioEngine,
                appPreferences = FakeAppPreferences(),
                soundDispatcher = testDispatcher
            ),
            waveStorage = waveStorage
        )
        collectInBackground(viewModel.state)
        return viewModel
    }

    private fun WaveViewModel.sendFrames(seconds: Int) {
        repeat(seconds * 1_000 / 16) { frame -> onAction(WaveAction.OnBatDashFrame(frame * 16_000_000L)) }
    }

    private fun WaveViewModel.dodgeRocks(seconds: Int) {
        repeat(seconds * 1_000 / 16) { frame ->
            val game = state.value.batDash
            val nextRock = game.rocks.filter { it.x > BAT_X }.minByOrNull { it.x }
            if (nextRock?.lane == game.batLane) {
                onAction(WaveAction.OnCaveTap(if (game.batLane == CaveLane.HIGH) CaveLane.LOW else CaveLane.HIGH))
            }
            onAction(WaveAction.OnBatDashFrame(frame * 16_000_000L))
        }
    }

    private fun WaveViewModel.startListening() {
        onAction(WaveAction.OnScreenStarted)
        onAction(WaveAction.OnMicrophonePermissionChecked(PermissionState.GRANTED))
    }

    private fun inOrder(vararg parts: List<FloatArray>): List<FloatArray> = parts.flatMap { it }

    private fun still(blocks: Int): List<FloatArray> =
        List(blocks) { heardBlock(echoShiftHz = 0.0, echoLoudness = 0.0) }

    /** A hand at [speed] m/s (negative: away) for two blocks, about 170 ms. */
    private fun push(speed: Double): List<FloatArray> =
        List(2) { heardBlock(echoShiftHz = dopplerShiftHz(speed), echoLoudness = 0.01) }

    /** Room hiss, the tone straight from the speaker, and a hand's echo shifted by [echoShiftHz]. */
    private fun heardBlock(echoShiftHz: Double, echoLoudness: Double): FloatArray {
        val tone = dopplerTone()
        val random = Random(seed = 19)
        return FloatArray(DOPPLER_BLOCK_SIZE) { n ->
            val echo = echoLoudness * sin(2 * PI * (DOPPLER_CARRIER_HZ + echoShiftHz) * n / SAMPLE_RATE)
            (tone[n] + echo + random.nextDouble(-0.01, 0.01)).toFloat()
        }
    }
}
