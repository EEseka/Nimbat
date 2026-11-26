package com.eeseka.nimbat.sound

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isCloseTo
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test

class GesturesTest {
    private companion object {
        const val STILL_DB = -60.0
    }

    @Test
    fun `a still room calibrates and then fires nothing`() {
        val detector = GestureDetector()

        val readings = List(100) { detector.read(still()) }

        assertThat(readings.first()).isInstanceOf<GestureReading.Calibrating>()
        assertThat(readings.last()).isEqualTo(
            GestureReading.Sensing(towardRiseDb = 0.0, awayRiseDb = 0.0, gesture = null, isToneClear = true)
        )
        assertThat(readings.gestures()).isEmpty()
    }

    @Test
    fun `calibration climbs to full while the room is still`() {
        val detector = GestureDetector()

        val progress = generateSequence { detector.read(still()) }
            .takeWhile { it is GestureReading.Calibrating }
            .map { (it as GestureReading.Calibrating).progress }
            .toList()

        assertThat(progress.zipWithNext().all { (before, after) -> after >= before }).isTrue()
        assertThat(progress.last()).isEqualTo(1f)
    }

    @Test
    fun `readings without the tone do not count toward calibration`() {
        val detector = GestureDetector()

        val readings = List(100) { detector.read(still().copy(toneClarityDb = 5.0)) }

        assertThat(readings.last()).isEqualTo(GestureReading.Calibrating(progress = 0f, isToneClear = false))
    }

    @Test
    fun `a hand coming closer fires toward once`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(toward(riseDb = 12.0, times = 3), still(times = 10))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD)
    }

    @Test
    fun `a gesture carries the speed of the hand`() {
        val detector = calibratedDetector()

        val gesture = detector.read(still().copy(towardDb = STILL_DB + 12, speedMetersPerSecond = -1.4))

        assertThat((gesture as GestureReading.Sensing).gesture?.speedMetersPerSecond ?: 0.0).isCloseTo(1.4, 1e-9)
    }

    @Test
    fun `a hand pulling back from the device fires away`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(away(riseDb = 12.0, times = 3), still(times = 10))

        assertThat(readings.gestures()).containsExactly(GestureDirection.AWAY)
    }

    @Test
    fun `the hand coming back after a push is not a second gesture`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(toward(12.0, 3), away(12.0, 3), still(10))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD)
    }

    @Test
    fun `the hand coming back after a hitch at the turn is not a second gesture`() {
        // Recorded turnarounds paused at most 4 readings (about 170 ms)
        val detector = calibratedDetector()

        val readings = detector.readAll(toward(12.0, 3), still(3), away(12.0, 3), still(10))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD)
    }

    @Test
    fun `pushing again after the hand comes back fires again`() {
        val detector = calibratedDetector()

        val push = inOrder(toward(12.0, 3), away(12.0, 3), still(4))
        val readings = detector.readAll(push, push)

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD, GestureDirection.TOWARD)
    }

    @Test
    fun `pulling away a while after a push fires away`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(toward(12.0, 3), still(30), away(12.0, 3))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD, GestureDirection.AWAY)
    }

    @Test
    fun `an echo just above the room is not a gesture`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(toward(riseDb = 4.0, times = 10))

        assertThat(readings.gestures()).isEmpty()
    }

    @Test
    fun `both directions rising together is not a gesture`() {
        val detector = calibratedDetector()

        val swaying = List(10) { still().copy(towardDb = STILL_DB + 10, awayDb = STILL_DB + 9) }
        val readings = detector.readAll(swaying)

        assertThat(readings.gestures()).isEmpty()
    }

    @Test
    fun `losing the tone never fires and shows no motion`() {
        val detector = calibratedDetector()

        val muffled = List(10) {
            Motion(towardDb = 0.0, awayDb = -30.0, speedMetersPerSecond = 2.0, toneClarityDb = 3.0)
        }
        val readings = detector.readAll(muffled)

        assertThat(readings.gestures()).isEmpty()
        assertThat(readings.last()).isEqualTo(
            GestureReading.Sensing(towardRiseDb = 0.0, awayRiseDb = 0.0, gesture = null, isToneClear = false)
        )
    }

    @Test
    fun `a push works again once the tone comes back`() {
        val detector = calibratedDetector()

        val muffled = List(10) { still().copy(toneClarityDb = 3.0) }
        val readings = detector.readAll(muffled, still(10), toward(12.0, 3))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD)
    }

    @Test
    fun `a slow way back that outlasts the return window is still the hand coming back`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(toward(12.0, 3), away(12.0, 20), still(10))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD)
    }

    @Test
    fun `a hand jiggling the other way after a push does not hold up the next push`() {
        // Under the trigger, over the quiet line
        val detector = calibratedDetector()
        val jiggle = List(10) { still().copy(awayDb = STILL_DB + 3.5) }

        val readings = detector.readAll(toward(12.0, 3), jiggle, toward(12.0, 3))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD, GestureDirection.TOWARD)
    }

    @Test
    fun `a hand hovering near the screen does not wear the trigger down`() {
        val detector = calibratedDetector()
        val hovering = List(200) { still().copy(towardDb = STILL_DB + 4.0) }

        val readings = detector.readAll(hovering, still(4), toward(riseDb = 12.0, times = 3))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD)
    }

    @Test
    fun `a pull after a moment's rest fires straight after a push`() {
        // About 200 ms of rest: longer than any recorded turnaround
        val detector = calibratedDetector()

        val readings = detector.readAll(toward(12.0, 3), still(5), away(12.0, 3))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD, GestureDirection.AWAY)
    }

    @Test
    fun `a long push fires only once`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(toward(12.0, 20), still(4))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD)
    }

    @Test
    fun `an away echo just above the room is not a gesture`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(away(riseDb = 4.0, times = 10))

        assertThat(readings.gestures()).isEmpty()
    }

    @Test
    fun `a busy room raises the line a hand must clear`() {
        // A fan or a TV lifts both sides together
        val detector = calibratedDetector()
        val bumps = List(20) { i ->
            if (i % 4 == 0) still().copy(towardDb = STILL_DB + 8, awayDb = STILL_DB + 8) else still()
        }

        val readings = detector.readAll(bumps, away(10.0, 3), still(10), away(25.0, 3))

        val firstGesture = readings.indexOfFirst { (it as? GestureReading.Sensing)?.gesture != null }
        assertThat(readings.gestures()).containsExactly(GestureDirection.AWAY)
        assertThat(firstGesture).isEqualTo(bumps.size + 13)
    }

    @Test
    fun `the line comes back down once the room settles`() {
        val detector = calibratedDetector()
        val bump = List(3) { still().copy(towardDb = STILL_DB + 9, awayDb = STILL_DB + 9) }

        val readings = detector.readAll(bump, still(150), toward(riseDb = 8.0, times = 3))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD)
    }

    @Test
    fun `one stray pull costs at most one push then pushes are back in step`() {
        // Each push turns straight round into its way back, then the hand rests
        val detector = calibratedDetector()
        val push = inOrder(toward(25.0, 3), away(25.0, 3), still(8))

        val readings = detector.readAll(away(25.0, 2), push, push, push, push)

        assertThat(readings.gestures().takeLast(3)).containsExactly(
            GestureDirection.TOWARD, GestureDirection.TOWARD, GestureDirection.TOWARD
        )
    }

    @Test
    fun `talking near the device is not a gesture`() {
        // Both sides up, leaning a few dB one way
        val detector = calibratedDetector()
        val talking = List(20) { i ->
            still().copy(towardDb = STILL_DB + 11, awayDb = STILL_DB + if (i % 3 == 0) 18 else 11)
        }

        val readings = detector.readAll(talking)

        assertThat(readings.gestures()).isEmpty()
    }

    @Test
    fun `a way back with a hitch in it is still the way back`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(toward(25.0, 3), away(25.0, 3), still(2), away(20.0, 2), still(10))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD)
    }

    @Test
    fun `a hand that keeps trying never locks itself out`() {
        // Strokes that just miss must not raise the line past the next ones
        val detector = calibratedDetector()
        val busy = List(3) { still().copy(towardDb = STILL_DB + 10, awayDb = STILL_DB + 10) }
        val tries = listOf(15.0, 20.0, 25.0, 30.0, 35.0).flatMap { riseDb ->
            inOrder(toward(riseDb, 3), away(riseDb, 3), still(8))
        }

        val readings = detector.readAll(busy, tries)

        assertThat(readings.gestures().size).isGreaterThanOrEqualTo(4)
    }

    @Test
    fun `a burst of clatter never fires`() {
        // Cutlery or a clap: noise across every pitch drowns the tone
        val detector = calibratedDetector()
        val clatter = List(3) { still().copy(awayDb = STILL_DB + 20, toneClarityDb = 25.0) }

        val readings = detector.readAll(clatter, still(10))

        assertThat(readings.gestures()).isEmpty()
    }

    @Test
    fun `a room slowly getting noisier fires nothing`() {
        val detector = calibratedDetector()

        val louder = List(400) { i -> still().copy(towardDb = STILL_DB + i * 0.05, awayDb = STILL_DB + i * 0.04) }
        val readings = detector.readAll(louder)

        assertThat(readings.gestures()).isEmpty()
    }

    @Test
    fun `the room level comes back down when the noise stops`() {
        // A TV on long enough to be learned, then switched off: a push measured from the quiet room fires again
        val detector = calibratedDetector()
        val tvOn = List(300) { still().copy(towardDb = STILL_DB + 10, awayDb = STILL_DB + 10) }

        val readings = detector.readAll(tvOn, still(150), toward(12.0, 3))

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD)
    }

    @Test
    fun `a fan switching on mid-gesture does not block gestures for good`() {
        val detector = calibratedDetector()

        val fanOn = still().copy(towardDb = STILL_DB + 10, awayDb = STILL_DB + 10)
        val pushWithFan = fanOn.copy(towardDb = fanOn.towardDb + 12)
        val readings = detector.readAll(toward(12.0, 3), List(300) { fanOn }, List(3) { pushWithFan })

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD, GestureDirection.TOWARD)
    }

    @Test
    fun `a real hand echo sweeping past the tone fires toward`() {
        // A hand speeding up to 1.5 m/s toward the device, then stopping, heard through the mic
        val detector = GestureDetector()
        val stillBlocks = List(40) { blockWithEcho(shiftHz = 0.0, echoLoudness = 0.0) }
        val pushBlocks = listOf(0.6, 1.2, 1.5, 0.9).map { speed -> blockWithEcho(dopplerShiftHz(speed)) }

        val readings = detector.hearAll(stillBlocks, pushBlocks, stillBlocks)

        assertThat(readings.gestures()).containsExactly(GestureDirection.TOWARD)
        assertThat(readings.all { it.isToneClear }).isTrue()
    }

    @Test
    fun `a hand far away is too faint to fire`() {
        // Each doubling of distance makes the echo 4× quieter, so a hand 4× farther is 16× fainter
        val detector = GestureDetector()
        val stillBlocks = List(40) { blockWithEcho(shiftHz = 0.0, echoLoudness = 0.0) }
        val faintPush = List(4) { blockWithEcho(dopplerShiftHz(1.2), echoLoudness = 0.01 / 16) }

        val readings = detector.hearAll(stillBlocks, faintPush)

        assertThat(readings.gestures()).isEmpty()
        assertThat(readings.last().isToneClear).isTrue()
    }

    @Test
    fun `hiss alone is never a clear tone`() {
        assertThat(GestureDetector().read(motion(hiss())).isToneClear).isFalse()
    }

    private fun calibratedDetector(): GestureDetector = GestureDetector().apply {
        do {
            val reading = read(still())
        } while (reading !is GestureReading.Sensing)
    }

    private fun GestureDetector.readAll(vararg parts: List<Motion>): List<GestureReading> =
        parts.flatMap { it }.map { read(it) }

    private fun GestureDetector.hearAll(vararg parts: List<FloatArray>): List<GestureReading> =
        parts.flatMap { it }.map { read(motion(it)) }

    private fun inOrder(vararg parts: List<Motion>): List<Motion> = parts.flatMap { it }

    private fun List<GestureReading>.gestures(): List<GestureDirection> =
        mapNotNull { (it as? GestureReading.Sensing)?.gesture?.direction }

    private fun still(): Motion =
        Motion(towardDb = STILL_DB, awayDb = STILL_DB, speedMetersPerSecond = 0.0, toneClarityDb = 40.0)

    private fun still(times: Int): List<Motion> = List(times) { still() }

    private fun toward(riseDb: Double, times: Int): List<Motion> =
        List(times) { still().copy(towardDb = STILL_DB + riseDb, speedMetersPerSecond = 1.0) }

    private fun away(riseDb: Double, times: Int): List<Motion> =
        List(times) { still().copy(awayDb = STILL_DB + riseDb, speedMetersPerSecond = -1.0) }

    /** What the mic hears: room hiss, the tone straight from the speaker, and a hand's echo shifted by [shiftHz]. */
    private fun blockWithEcho(shiftHz: Double, echoLoudness: Double = 0.01): FloatArray {
        val tone = dopplerTone()
        val hiss = hiss()
        return FloatArray(DOPPLER_BLOCK_SIZE) { n ->
            val echo = echoLoudness * sin(2 * PI * (DOPPLER_CARRIER_HZ + shiftHz) * n / SAMPLE_RATE)
            hiss[n] + tone[n] + echo.toFloat()
        }
    }

    /** Random noise, the same every run, 100× quieter than the tone. */
    private fun hiss(): FloatArray {
        val random = Random(seed = 19)
        return FloatArray(DOPPLER_BLOCK_SIZE) { random.nextDouble(-0.01, 0.01).toFloat() }
    }
}
