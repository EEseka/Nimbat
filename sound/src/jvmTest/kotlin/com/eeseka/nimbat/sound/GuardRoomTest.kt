package com.eeseka.nimbat.sound

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEmpty
import assertk.assertions.single
import kotlin.test.Test

/**
 * A real recording from a laptop on a desk at one end of a long, narrow room. Sitting still, typing, getting up and
 * walking to the far end, walking back past the laptop, sitting down, then lifting the
 * laptop. Each part is followed by standing or sitting still.
 */
class GuardRoomTest {
    private companion object {
        const val RECORDING = "/guard-corridor.csv"
        const val READINGS_PER_SECOND = 23.4

        val STILL = listOf(9.0..25.0, 83.0..92.0, 138.0..148.0)
        val MOVING = listOf(26.0, 55.0, 74.0, 94.0, 123.0, 150.0)
        val LEFT = 71.0..76.0
    }

    @Test
    fun `sitting still notices nothing`() {
        val starts = movementStarts()

        assertThat(starts.filter { second -> STILL.any { second in it } }).isEmpty()
    }

    @Test
    fun `each part noticed within a second`() {
        val starts = movementStarts()

        MOVING.forEach { began ->
            assertThat(starts.first { it >= began - 1 }).isBetween(began - 1, began + 1)
        }
    }

    @Test
    fun `walking off counts as gone once and nothing else does`() {
        val movement = MovementDetector(WatchRange.NEAR)
        val walkAway = WalkAwayDetector()

        val gone = readings().withIndex()
            .filter { (_, motion) -> walkAway.read(movement.read(motion)) }
            .map { (index, _) -> index / READINGS_PER_SECOND }

        assertThat(gone).single().isBetween(LEFT.start, LEFT.endInclusive)
    }

    private fun movementStarts(): List<Double> {
        val detector = MovementDetector(WatchRange.ROOM)
        return readings().withIndex()
            .filter { (_, motion) -> (detector.read(motion) as? WatchReading.Watching)?.hasMovementStarted == true }
            .map { (index, _) -> index / READINGS_PER_SECOND }
    }

    private fun readings(): List<Motion> = checkNotNull(javaClass.getResource(RECORDING)).readText()
        .lines()
        .drop(1)
        .filter { it.isNotBlank() }
        .map { line ->
            val (towardDb, awayDb, speed, clarity) = line.split(",").map { it.toDouble() }
            Motion(towardDb, awayDb, speed, clarity)
        }
}
