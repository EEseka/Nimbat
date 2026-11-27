package com.eeseka.nimbat.guard.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.single
import com.eeseka.nimbat.core.data.audio.AudioMovementSensor
import com.eeseka.nimbat.core.data.guard.AudioGuardMonitor
import com.eeseka.nimbat.core.domain.audio.AudioError
import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntryKind
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.domain.guard.model.GuardPhase
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.sound.DOPPLER_BLOCK_SIZE
import com.eeseka.nimbat.sound.DOPPLER_CARRIER_HZ
import com.eeseka.nimbat.sound.SAMPLE_RATE
import com.eeseka.nimbat.sound.alarmSiren
import com.eeseka.nimbat.sound.dopplerShiftHz
import com.eeseka.nimbat.sound.dopplerTone
import com.eeseka.nimbat.testing.collectInBackground
import com.eeseka.nimbat.testing.data.FakeAppPreferences
import com.eeseka.nimbat.testing.data.FakeAudioEngine
import com.eeseka.nimbat.testing.data.FakeComputerLock
import com.eeseka.nimbat.testing.data.FakeGuardStorage
import kotlinx.coroutines.CoroutineScope
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
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class GuardViewModelTest {
    private companion object {
        val COUNTDOWN = 10.seconds
    }

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var audioEngine: FakeAudioEngine
    private lateinit var guardStorage: FakeGuardStorage
    private lateinit var computerLock: FakeComputerLock

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // A still room after each script, since silence means a covered mic and rings the alarm
        audioEngine = FakeAudioEngine().apply {
            isSpeakerHeard = false
            background = heardBlock(echoShiftHz = 0.0, echoLoudness = 0.0)
        }
        guardStorage = FakeGuardStorage()
        computerLock = FakeComputerLock()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saved sessions show up in the log`() = guardTest { viewModel ->
        guardStorage.startSession(GuardMode.ALARM, startedAtMillis = 1_000L)

        assertThat(viewModel.state.value.sessions).single().transform { it.startedAtMillis }.isEqualTo(1_000L)
    }

    @Test
    fun `starting counts down before the tone plays`() = guardTest { viewModel ->
        viewModel.onAction(GuardAction.OnStartClick)

        assertThat(viewModel.state.value.phase).isEqualTo(GuardPhase.COUNTDOWN)
        assertThat(viewModel.state.value.countdownSeconds).isEqualTo(10)
        assertThat(audioEngine.isLooping).isFalse()
    }

    @Test
    fun `after the countdown it learns the room and watches`() = guardTest { viewModel ->
        audioEngine.heard = still(blocks = 30)

        viewModel.onAction(GuardAction.OnStartClick)
        advanceTimeBy(COUNTDOWN + 1.seconds)

        assertThat(audioEngine.isLooping).isTrue()
        assertThat(viewModel.state.value.phase).isEqualTo(GuardPhase.WATCHING)
    }

    @Test
    fun `stopping ends the session in the log`() = guardTest { viewModel ->
        viewModel.onAction(GuardAction.OnStartClick)
        advanceTimeBy(3.seconds)

        viewModel.onAction(GuardAction.OnStopClick)

        assertThat(viewModel.state.value.phase).isEqualTo(GuardPhase.IDLE)
        assertThat(audioEngine.isLooping).isFalse()
        assertThat(guardStorage.sessions.first().single().endedAtMillis).isNotNull()
    }

    @Test
    fun `a movement rings the alarm`() = guardTest { viewModel ->
        audioEngine.heard = inOrder(still(blocks = 20), walking(speed = 1.2, blocks = 12))

        viewModel.events.test {
            viewModel.onAction(GuardAction.OnStartClick)
            advanceTimeBy(COUNTDOWN + 1.seconds)

            assertThat(awaitItem()).isEqualTo(GuardEvent.AlarmStarted)
            assertThat(viewModel.state.value.phase).isEqualTo(GuardPhase.RINGING)
            assertThat(audioEngine.lastLooped?.toList()).isEqualTo(alarmSiren().toList())
            assertThat(guardStorage.sessions.first().single().entries.map { it.kind })
                .containsExactly(GuardLogEntryKind.ALARM)
        }
    }

    @Test
    fun `silent mode logs a movement and keeps watching`() = guardTest { viewModel ->
        audioEngine.heard = inOrder(still(blocks = 20), walking(speed = 1.2, blocks = 12), still(blocks = 30))
        viewModel.onAction(GuardAction.OnModeSelected(GuardMode.SILENT))

        viewModel.onAction(GuardAction.OnStartClick)
        advanceTimeBy(COUNTDOWN + 1.seconds)

        assertThat(viewModel.state.value.phase).isEqualTo(GuardPhase.WATCHING)
        assertThat(viewModel.state.value.movementCount).isEqualTo(1)
        assertThat(guardStorage.sessions.first().single().entries.map { it.kind })
            .containsExactly(GuardLogEntryKind.MOVEMENT)
        assertThat(audioEngine.lastLooped?.toList()).isNotEqualTo(alarmSiren().toList())
    }

    @Test
    fun `turning the alarm off stops guarding`() = guardTest { viewModel ->
        audioEngine.heard = inOrder(still(blocks = 20), walking(speed = 1.2, blocks = 12))
        viewModel.onAction(GuardAction.OnStartClick)
        advanceTimeBy(COUNTDOWN + 1.seconds)

        viewModel.onAction(GuardAction.OnStopClick)

        assertThat(viewModel.state.value.phase).isEqualTo(GuardPhase.IDLE)
        assertThat(audioEngine.isLooping).isFalse()
    }

    @Test
    fun `covering the mic rings the alarm`() = guardTest { viewModel ->
        audioEngine.heard = still(blocks = 20)
        audioEngine.background = FloatArray(DOPPLER_BLOCK_SIZE)

        viewModel.onAction(GuardAction.OnStartClick)
        advanceTimeBy(COUNTDOWN + 4.seconds)

        assertThat(viewModel.state.value.phase).isEqualTo(GuardPhase.RINGING)
    }

    @Test
    fun `losing the tone for a while is logged`() = guardTest { viewModel ->
        audioEngine.heard = still(blocks = 20)
        audioEngine.background = FloatArray(DOPPLER_BLOCK_SIZE)
        viewModel.onAction(GuardAction.OnModeSelected(GuardMode.CURTAIN))

        viewModel.onAction(GuardAction.OnStartClick)
        advanceTimeBy(4.seconds)

        assertThat(guardStorage.sessions.first().single().entries.map { it.kind })
            .containsExactly(GuardLogEntryKind.TONE_LOST)
        assertThat(viewModel.state.value.isToneClear).isFalse()
    }

    @Test
    fun `a microphone that fails stops with an error`() = guardTest { viewModel ->
        audioEngine.microphoneError = AudioError.MICROPHONE_UNAVAILABLE

        viewModel.events.test {
            viewModel.onAction(GuardAction.OnStartClick)
            advanceTimeBy(COUNTDOWN + 1.seconds)

            assertThat(awaitItem()).isInstanceOf<GuardEvent.Error>()
            assertThat(viewModel.state.value.phase).isEqualTo(GuardPhase.FAILED)
        }
    }

    @Test
    fun `leaving the screen stops watching`() = guardTest { viewModel ->
        viewModel.onAction(GuardAction.OnStartClick)

        viewModel.onAction(GuardAction.OnScreenStopped)

        assertThat(viewModel.state.value.phase).isEqualTo(GuardPhase.IDLE)
    }

    @Test
    fun `the curtain falls once you walk away and locks the computer`() = guardTest { viewModel ->
        audioEngine.heard = inOrder(still(blocks = 20), walking(speed = -1.2, blocks = 40), still(blocks = 40))
        viewModel.onAction(GuardAction.OnModeSelected(GuardMode.CURTAIN))
        viewModel.onAction(GuardAction.OnLockComputerToggled(isOn = true))

        viewModel.onAction(GuardAction.OnStartClick)
        advanceTimeBy(1.seconds)

        assertThat(viewModel.state.value.phase).isEqualTo(GuardPhase.CURTAIN)
        assertThat(computerLock.lockCount).isEqualTo(1)
    }

    @Test
    fun `lifting the curtain logs the walk away`() = guardTest { viewModel ->
        audioEngine.heard = inOrder(still(blocks = 20), walking(speed = -1.2, blocks = 40), still(blocks = 40))
        viewModel.onAction(GuardAction.OnModeSelected(GuardMode.CURTAIN))
        viewModel.onAction(GuardAction.OnStartClick)
        advanceTimeBy(1.seconds)

        viewModel.onAction(GuardAction.OnCurtainLifted)
        advanceTimeBy(1.seconds)

        assertThat(viewModel.state.value.phase).isEqualTo(GuardPhase.CALIBRATING)
        assertThat(guardStorage.sessions.first().single().entries.map { it.kind })
            .containsExactly(GuardLogEntryKind.WALKED_AWAY)
    }

    @Test
    fun `sitting still never drops the curtain`() = guardTest { viewModel ->
        audioEngine.heard = still(blocks = 80)
        viewModel.onAction(GuardAction.OnModeSelected(GuardMode.CURTAIN))

        viewModel.onAction(GuardAction.OnStartClick)
        advanceTimeBy(1.seconds)

        assertThat(viewModel.state.value.phase).isEqualTo(GuardPhase.WATCHING)
    }

    @Test
    fun `clearing the log asks first and then empties it`() = guardTest { viewModel ->
        guardStorage.startSession(GuardMode.ALARM, startedAtMillis = 1_000L)

        viewModel.onAction(GuardAction.OnClearLogClick)
        assertThat(viewModel.state.value.showClearLogDialog).isTrue()

        viewModel.onAction(GuardAction.OnClearLogConfirmed)

        assertThat(viewModel.state.value.showClearLogDialog).isFalse()
        assertThat(viewModel.state.value.sessions).isEmpty()
    }

    @Test
    fun `a blocked microphone opens settings from the allow button`() = guardTest { viewModel ->
        viewModel.onAction(GuardAction.OnMicrophonePermissionChecked(PermissionState.PERMANENTLY_DENIED))

        viewModel.events.test {
            viewModel.onAction(GuardAction.OnAllowMicrophoneClick)

            assertThat(awaitItem()).isEqualTo(GuardEvent.OpenMicrophoneSettings)
        }
    }

    private fun guardTest(body: suspend TestScope.(GuardViewModel) -> Unit): TestResult = runTest {
        val viewModel = createViewModel()
        try {
            body(viewModel)
        } finally {
            viewModel.onAction(GuardAction.OnStopClick)
        }
    }

    private fun TestScope.createViewModel(): GuardViewModel {
        val viewModel = GuardViewModel(
            guardMonitor = AudioGuardMonitor(
                movementSensor = AudioMovementSensor(
                    audioEngine = audioEngine,
                    appPreferences = FakeAppPreferences(),
                    soundDispatcher = testDispatcher
                ),
                audioEngine = audioEngine,
                guardStorage = guardStorage,
                computerLock = computerLock,
                applicationScope = CoroutineScope(backgroundScope.coroutineContext + testDispatcher),
                timeSource = testScheduler.timeSource
            ),
            guardStorage = guardStorage,
            computerLock = computerLock
        )
        collectInBackground(viewModel.state)
        return viewModel
    }

    private fun inOrder(vararg parts: List<FloatArray>): List<FloatArray> = parts.flatMap { it }

    private fun still(blocks: Int): List<FloatArray> =
        List(blocks) { heardBlock(echoShiftHz = 0.0, echoLoudness = 0.0) }

    /** Someone moving at [speed] m/s (negative: away) for [blocks] blocks of about 85 ms. */
    private fun walking(speed: Double, blocks: Int): List<FloatArray> =
        List(blocks) { heardBlock(echoShiftHz = dopplerShiftHz(speed), echoLoudness = 0.01) }

    /** Room hiss, the tone straight from the speaker, and an echo shifted by [echoShiftHz]. */
    private fun heardBlock(echoShiftHz: Double, echoLoudness: Double): FloatArray {
        val tone = dopplerTone()
        val random = Random(seed = 19)
        return FloatArray(DOPPLER_BLOCK_SIZE) { n ->
            val echo = echoLoudness * sin(2 * PI * (DOPPLER_CARRIER_HZ + echoShiftHz) * n / SAMPLE_RATE)
            (tone[n] + echo + random.nextDouble(-0.01, 0.01)).toFloat()
        }
    }
}
