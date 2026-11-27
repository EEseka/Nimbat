package com.eeseka.nimbat.core.data.guard

import com.eeseka.nimbat.core.domain.audio.AudioEngine
import com.eeseka.nimbat.core.domain.audio.AudioErrorException
import com.eeseka.nimbat.core.domain.audio.MovementSensor
import com.eeseka.nimbat.core.domain.guard.ComputerLock
import com.eeseka.nimbat.core.domain.guard.GuardMonitor
import com.eeseka.nimbat.core.domain.guard.GuardStorage
import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntry
import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntryKind
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.domain.guard.model.GuardPhase
import com.eeseka.nimbat.core.domain.guard.model.GuardStatus
import com.eeseka.nimbat.sound.Movement
import com.eeseka.nimbat.sound.WalkAwayDetector
import com.eeseka.nimbat.sound.WatchRange
import com.eeseka.nimbat.sound.WatchReading
import com.eeseka.nimbat.sound.alarmSiren
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class AudioGuardMonitor(
    private val movementSensor: MovementSensor,
    private val audioEngine: AudioEngine,
    private val guardStorage: GuardStorage,
    private val computerLock: ComputerLock,
    applicationScope: CoroutineScope,
    private val timeSource: TimeSource = TimeSource.Monotonic
) : GuardMonitor {

    private companion object {
        const val COUNTDOWN_SECONDS = 10
        val TONE_LOSS_GRACE_PERIOD = 2.seconds
        val LONGEST_RING = 2.minutes
    }

    private val _status = MutableStateFlow(GuardStatus())
    override val status: StateFlow<GuardStatus> = _status.asStateFlow()

    private val currentRun = MutableStateFlow<Int?>(null)
    private val liftRequests = MutableStateFlow(0)

    init {
        @OptIn(ExperimentalCoroutinesApi::class)
        currentRun
            .flatMapLatest { attempt -> if (attempt == null) emptyFlow() else flow<Unit> { guard() } }
            .launchIn(applicationScope)
    }

    override fun setMode(mode: GuardMode) {
        _status.update { it.copy(mode = mode) }
    }

    override fun setLocksComputer(isOn: Boolean) {
        _status.update { it.copy(locksComputer = isOn) }
    }

    override fun start() {
        _status.update { it.copy(movementCount = 0, isToneClear = true, error = null) }
        currentRun.update { (it ?: 0) + 1 }
    }

    override fun stop() {
        currentRun.value = null
    }

    override fun liftCurtain() {
        liftRequests.update { it + 1 }
    }

    private suspend fun guard() {
        val settings = _status.value
        val sessionId = guardStorage.startSession(settings.mode, now())
        try {
            when (settings.mode) {
                GuardMode.ALARM -> guardWithAlarm(sessionId)
                GuardMode.SILENT -> guardSilently(sessionId)
                GuardMode.CURTAIN -> guardWithCurtain(sessionId, settings)
            }
        } catch (error: AudioErrorException) {
            _status.update { it.copy(phase = GuardPhase.FAILED, error = error.error, riseDb = 0.0) }
        } finally {
            withContext(NonCancellable) { guardStorage.endSession(sessionId, now()) }
            _status.update {
                if (it.phase == GuardPhase.FAILED) it else it.copy(phase = GuardPhase.IDLE, riseDb = 0.0)
            }
        }
    }

    /** Until [stop]: rings on the first movement, and if nobody turns it off, watches again once it gives up. */
    private suspend fun guardWithAlarm(sessionId: Long) {
        while (true) {
            countDown()
            watch(sessionId, WatchRange.ROOM, GuardPhase.WATCHING, logsMovement = false) { reading ->
                if (reading.hasMovementStarted) guardStorage.addEntry(sessionId, alarmEntry(reading.riseDb))
                reading.hasMovementStarted
            }
            ring()
        }
    }

    /** Never rings: logs every movement until [stop], for whoever comes back to read. */
    private suspend fun guardSilently(sessionId: Long) {
        countDown()
        watch(sessionId, WatchRange.ROOM, GuardPhase.WATCHING, logsMovement = true) { false }
    }

    /** Only someone leaving matters while you sit there; once you're gone, anyone coming near is logged. */
    private suspend fun guardWithCurtain(sessionId: Long, settings: GuardStatus) {
        while (true) {
            val walkAway = WalkAwayDetector()
            watch(sessionId, WatchRange.NEAR, GuardPhase.WATCHING, logsMovement = false) { walkAway.read(it) }

            val leftAtMillis = now()
            _status.update { it.copy(phase = GuardPhase.CURTAIN, riseDb = 0.0) }
            if (settings.locksComputer) computerLock.lock()
            untilRequested(liftRequests) {
                watch(sessionId, WatchRange.ROOM, GuardPhase.CURTAIN, logsMovement = true) { false }
            }
            guardStorage.addEntry(sessionId, walkedAwayEntry(leftAtMillis))
        }
    }

    private suspend fun countDown() {
        for (secondsLeft in COUNTDOWN_SECONDS downTo 1) {
            _status.update { it.copy(phase = GuardPhase.COUNTDOWN, countdownSeconds = secondsLeft) }
            delay(1.seconds)
        }
    }

    /** Listens and logs in [watchingPhase] until [isDone] ends it on a reading. */
    private suspend fun watch(
        sessionId: Long,
        range: WatchRange,
        watchingPhase: GuardPhase,
        logsMovement: Boolean,
        isDone: suspend (WatchReading.Watching) -> Boolean
    ) {
        var toneLostAt: TimeMark? = null
        var toneLostAtMillis = 0L
        var hasLoggedToneLost = false

        movementSensor.readings(range)
            .takeWhile { reading ->
                when (reading) {
                    is WatchReading.Calibrating -> {
                        val calibratingPhase =
                            if (watchingPhase == GuardPhase.WATCHING) GuardPhase.CALIBRATING else watchingPhase
                        _status.update {
                            it.copy(
                                phase = calibratingPhase,
                                calibrationProgress = reading.progress,
                                isToneClear = reading.isToneClear
                            )
                        }
                        true
                    }

                    is WatchReading.Watching -> {
                        _status.update {
                            it.copy(phase = watchingPhase, isToneClear = reading.isToneClear, riseDb = reading.riseDb)
                        }

                        if (reading.isToneClear) {
                            toneLostAt = null
                            hasLoggedToneLost = false
                        } else {
                            val lostAt = toneLostAt ?: timeSource.markNow().also {
                                toneLostAt = it
                                toneLostAtMillis = now()
                            }
                            if (lostAt.elapsedNow() >= TONE_LOSS_GRACE_PERIOD && !hasLoggedToneLost) {
                                hasLoggedToneLost = true
                                guardStorage.addEntry(sessionId, toneLostEntry(toneLostAtMillis))
                            }
                        }

                        if (logsMovement) {
                            if (reading.hasMovementStarted) {
                                _status.update { it.copy(movementCount = it.movementCount + 1) }
                            }
                            reading.endedMovement?.let { guardStorage.addEntry(sessionId, movementEntry(it)) }
                        }
                        !isDone(reading)
                    }
                }
            }
            .collect()
    }

    private suspend fun ring() {
        _status.update { it.copy(phase = GuardPhase.RINGING, riseDb = 0.0) }
        withTimeoutOrNull(LONGEST_RING) { audioEngine.loop(alarmSiren()) }
    }

    /** Runs [block] until the next request, then cancels it. */
    private suspend fun untilRequested(requests: MutableStateFlow<Int>, block: suspend () -> Unit) {
        val requestsSoFar = requests.value
        coroutineScope {
            launch { block() }
            requests.first { it > requestsSoFar }
            coroutineContext.cancelChildren()
        }
    }

    private fun movementEntry(movement: Movement): GuardLogEntry {
        val durationMillis = (movement.durationSeconds * 1_000).roundToLong()
        return GuardLogEntry(
            kind = GuardLogEntryKind.MOVEMENT,
            atMillis = now() - durationMillis,
            durationMillis = durationMillis,
            peakRiseDb = movement.peakRiseDb,
            direction = movement.direction
        )
    }

    private fun alarmEntry(riseDb: Double) = GuardLogEntry(
        kind = GuardLogEntryKind.ALARM,
        atMillis = now(),
        durationMillis = null,
        peakRiseDb = riseDb,
        direction = null
    )

    private fun toneLostEntry(lostAtMillis: Long) = GuardLogEntry(
        kind = GuardLogEntryKind.TONE_LOST,
        atMillis = lostAtMillis,
        durationMillis = null,
        peakRiseDb = null,
        direction = null
    )

    private fun walkedAwayEntry(leftAtMillis: Long) = GuardLogEntry(
        kind = GuardLogEntryKind.WALKED_AWAY,
        atMillis = leftAtMillis,
        durationMillis = now() - leftAtMillis,
        peakRiseDb = null,
        direction = null
    )

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
