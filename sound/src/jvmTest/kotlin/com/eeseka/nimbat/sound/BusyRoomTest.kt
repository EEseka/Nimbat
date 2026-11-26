package com.eeseka.nimbat.sound

import assertk.assertThat
import assertk.assertions.isBetween
import assertk.assertions.isEmpty
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isLessThanOrEqualTo
import kotlin.test.Test

/**
 * Real recordings from a laptop at 10% volume in a busy room: a fan, an air conditioner and a TV running, people
 * talking, eating and brushing their teeth beside it. Each starts with sitting still, then hand strokes, each with
 * the hand coming back.
 */
class BusyRoomTest {
    private companion object {
        const val PUSHES = "/busy-room.csv"
        const val PUSHES_STILL_UNTIL = 780 // About 33 s
        const val PUSHES_START = 1_400

        const val SPAM = "/busy-room-spam.csv"
        const val SPAM_STILL_UNTIL = 1_250 // About 53 s
        const val SPAM_LAST_STRETCH = 2_380

        const val VOICES = "/busy-room-voices.csv"
        const val VOICES_STILL_UNTIL = 880 // About 38 s
        const val VOICES_STRAY_PULL = 1_220
    }

    @Test
    fun `sitting still in a busy room fires nothing`() {
        assertThat(gestures(PUSHES).filterKeys { it < PUSHES_STILL_UNTIL }).isEmpty()
        assertThat(gestures(SPAM).filterKeys { it < SPAM_STILL_UNTIL }).isEmpty()
    }

    @Test
    fun `each push fires once and its way back is not a pull`() {
        val gestures = gestures(PUSHES).filterKeys { it >= PUSHES_START }.values

        assertThat(gestures.count { it == GestureDirection.TOWARD }).isBetween(12, 13)
        assertThat(gestures.count { it == GestureDirection.AWAY }).isLessThanOrEqualTo(1)
    }

    @Test
    fun `talking and eating nearby fire nothing`() {
        assertThat(gestures(VOICES).filterKeys { it < VOICES_STILL_UNTIL }).isEmpty()
    }

    @Test
    fun `a stray pull before a run of pushes only costs a couple of them`() {
        // A pull at 1,225 lands just before twelve pushes
        val run = gestures(VOICES).filterKeys { it >= VOICES_STRAY_PULL }.values

        assertThat(run.count { it == GestureDirection.TOWARD }).isGreaterThanOrEqualTo(9)
    }

    @Test
    fun `pushing over and over never locks wave out`() {
        // 20 pushes in a row
        val lastStretch = gestures(SPAM).filterKeys { it >= SPAM_LAST_STRETCH }.values

        assertThat(lastStretch.count { it == GestureDirection.TOWARD }).isGreaterThanOrEqualTo(18)
    }

    private fun gestures(recording: String): Map<Int, GestureDirection> {
        val detector = GestureDetector()
        return readings(recording).withIndex()
            .mapNotNull { (index, motion) ->
                (detector.read(motion) as? GestureReading.Sensing)?.gesture?.let { index to it.direction }
            }
            .toMap()
    }

    private fun readings(recording: String): List<Motion> = checkNotNull(javaClass.getResource(recording)).readText()
        .lines()
        .drop(1)
        .filter { it.isNotBlank() }
        .map { line ->
            val (towardDb, awayDb, speed, clarity) = line.split(",").map { it.toDouble() }
            Motion(towardDb, awayDb, speed, clarity)
        }
}
