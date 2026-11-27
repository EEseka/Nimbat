package com.eeseka.nimbat.sound

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isBetween
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class MovementTest {
    private companion object {
        const val STILL_DB = -60.0
        const val READINGS_PER_SECOND = 23
        const val TOWARD_RISE_DB = 12.0
        const val AWAY_RISE_DB = 18.0
    }

    @Test
    fun `a still room calibrates and then notices nothing`() {
        val detector = MovementDetector(WatchRange.ROOM)

        val readings = List(200) { detector.read(still()) }

        assertThat(readings.first()).isInstanceOf<WatchReading.Calibrating>()
        assertThat(readings.starts()).isEmpty()
        assertThat(readings.ends()).isEmpty()
    }

    @Test
    fun `a tone still fading in is not taken for a lost one`() {
        val readings = MovementDetector(WatchRange.ROOM).readAll(toneLost(times = READINGS_PER_SECOND))

        assertThat(readings.first().isToneClear).isTrue()
        assertThat(readings.last().isToneClear).isFalse()
    }

    @Test
    fun `someone walking past starts one movement`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(mixed(riseDb = 10.0, times = READINGS_PER_SECOND * 2), still(times = 40))

        assertThat(readings.starts()).hasSize(1)
        assertThat(readings.ends()).hasSize(1)
    }

    @Test
    fun `a movement is noticed within half a second`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(mixed(riseDb = 10.0, times = READINGS_PER_SECOND))

        assertThat(readings.indexOfFirst { (it as WatchReading.Watching).hasMovementStarted }).isBetween(0, 12)
    }

    @Test
    fun `an ended movement knows how long it lasted`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(mixed(riseDb = 10.0, times = READINGS_PER_SECOND * 2), still(times = 40))

        assertThat(readings.ends().single().durationSeconds).isBetween(1.8, 2.2)
    }

    @Test
    fun `a movement only ends after a second of quiet`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(
            mixed(riseDb = 10.0, times = READINGS_PER_SECOND),
            still(times = READINGS_PER_SECOND / 2),
            mixed(riseDb = 10.0, times = READINGS_PER_SECOND),
            still(times = 40)
        )

        assertThat(readings.starts()).hasSize(1)
        assertThat(readings.ends()).hasSize(1)
    }

    @Test
    fun `a click is too short to count`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(mixed(riseDb = 30.0, times = 2), still(times = 40))

        assertThat(readings.starts()).isEmpty()
    }

    @Test
    fun `a clap that drowns the tone is too short to count`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(toneLost(times = 2), still(times = 40))

        assertThat(readings.starts()).isEmpty()
    }

    @Test
    fun `movement just under the line never raises it`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(
            mixed(riseDb = 5.0, times = READINGS_PER_SECOND * 10),
            mixed(riseDb = 7.0, times = READINGS_PER_SECOND)
        )

        assertThat(readings.starts()).hasSize(1)
    }

    @Test
    fun `near listens only for big echoes`() {
        val near = calibratedDetector(WatchRange.NEAR)
        val room = calibratedDetector(WatchRange.ROOM)
        val faint = mixed(riseDb = 8.0, times = READINGS_PER_SECOND)

        assertThat(near.readAll(faint).starts()).isEmpty()
        assertThat(room.readAll(faint).starts()).hasSize(1)
    }

    @Test
    fun `a movement that mostly comes closer has a direction`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(toward(times = READINGS_PER_SECOND), still(times = 40))

        assertThat(readings.ends().single().direction).isEqualTo(GestureDirection.TOWARD)
    }

    @Test
    fun `a movement with both sides alike has no direction`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(mixed(riseDb = 10.0, times = READINGS_PER_SECOND), still(times = 40))

        assertThat(readings.ends().single().direction).isNull()
    }

    @Test
    fun `picking the device up drowns the tone and counts as movement`() {
        val detector = calibratedDetector()

        val readings = detector.readAll(toneLost(times = READINGS_PER_SECOND), still(times = 40))

        assertThat(readings.filter { !it.isToneClear }).hasSize(READINGS_PER_SECOND)
        assertThat(readings.starts()).hasSize(1)
        assertThat(readings.ends()).hasSize(1)
    }

    @Test
    fun `someone getting up and leaving counts as gone once`() {
        val (movement, walkAway) = calibratedPair()

        val gone = listOf(away(times = READINGS_PER_SECOND * 4), still(times = READINGS_PER_SECOND * 5))
            .flatten()
            .count { walkAway.read(movement.read(it)) }

        assertThat(gone).isEqualTo(1)
    }

    @Test
    fun `leaning back then moving again is not leaving`() {
        val (movement, walkAway) = calibratedPair()

        val gone = listOf(
            away(times = READINGS_PER_SECOND),
            still(times = READINGS_PER_SECOND + 10),
            toward(times = READINGS_PER_SECOND),
            still(times = READINGS_PER_SECOND * 5)
        )
            .flatten()
            .any { walkAway.read(movement.read(it)) }

        assertThat(gone).isFalse()
    }

    @Test
    fun `typing at the desk is never leaving`() {
        val (movement, walkAway) = calibratedPair()
        val typing = List(READINGS_PER_SECOND * 30) { i -> if (i % 3 == 0) mixed(riseDb = 8.0) else still() }

        val gone = typing.any { walkAway.read(movement.read(it)) }

        assertThat(gone).isFalse()
    }

    private fun calibratedDetector(range: WatchRange = WatchRange.ROOM): MovementDetector =
        MovementDetector(range).apply {
            do {
                val reading = read(still())
            } while (reading !is WatchReading.Watching)
        }

    private fun calibratedPair(): Pair<MovementDetector, WalkAwayDetector> = calibratedDetector() to WalkAwayDetector()

    private fun MovementDetector.readAll(vararg parts: List<Motion>): List<WatchReading> =
        parts.flatMap { it }.map { read(it) }

    private fun List<WatchReading>.starts(): List<WatchReading> =
        filter { (it as? WatchReading.Watching)?.hasMovementStarted == true }

    private fun List<WatchReading>.ends(): List<Movement> = mapNotNull { (it as? WatchReading.Watching)?.endedMovement }

    private fun still(): Motion =
        Motion(towardDb = STILL_DB, awayDb = STILL_DB, speedMetersPerSecond = 0.0, toneClarityDb = 40.0)

    private fun still(times: Int): List<Motion> = List(times) { still() }

    private fun toneLost(times: Int): List<Motion> =
        List(times) { still().copy(towardDb = STILL_DB + 30, toneClarityDb = 5.0) }

    private fun mixed(riseDb: Double): Motion = still().copy(towardDb = STILL_DB + riseDb, awayDb = STILL_DB + riseDb)

    private fun mixed(riseDb: Double, times: Int): List<Motion> = List(times) { mixed(riseDb) }

    private fun toward(times: Int): List<Motion> =
        List(times) { still().copy(towardDb = STILL_DB + TOWARD_RISE_DB, speedMetersPerSecond = 1.0) }

    private fun away(times: Int): List<Motion> =
        List(times) { still().copy(awayDb = STILL_DB + AWAY_RISE_DB, speedMetersPerSecond = -1.0) }
}
