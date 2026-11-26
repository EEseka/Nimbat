package com.eeseka.nimbat.wave.presentation.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThanOrEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isTrue
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test

class BatDashTest {
    private companion object {
        const val FRAME_MILLIS = 16L
    }

    @Test
    fun `nothing moves before take-off`() {
        val ready = BatDash()

        assertThat(ready.advance(elapsedMillis = 5_000)).isEqualTo(ready)
    }

    @Test
    fun `taking off starts a run in that lane`() {
        val flying = BatDash(bestScore = 4).fly(CaveLane.LOW, seed = 1)

        assertThat(flying.phase).isEqualTo(BatDashPhase.FLYING)
        assertThat(flying.batLane).isEqualTo(CaveLane.LOW)
        assertThat(flying.bestScore).isEqualTo(4)
    }

    @Test
    fun `rocks come in from the right after take-off`() {
        val flying = BatDash().fly(CaveLane.HIGH, seed = 1).fly(times = 70)

        assertThat(flying.rocks).isNotEmpty()
        assertThat(flying.rocks.all { it.x > BAT_X }).isTrue()
    }

    @Test
    fun `a rock in the other lane scores a point`() {
        val game = flyingAt(lane = CaveLane.HIGH, rock = CaveLane.LOW)

        val after = game.advance(elapsedMillis = 50)

        assertThat(after.phase).isEqualTo(BatDashPhase.FLYING)
        assertThat(after.score).isEqualTo(1)
    }

    @Test
    fun `a rock in the bat's lane crashes it and keeps the best score`() {
        val game = flyingAt(lane = CaveLane.HIGH, rock = CaveLane.HIGH).copy(score = 9, bestScore = 3)

        val after = game.advance(elapsedMillis = 50)

        assertThat(after.phase).isEqualTo(BatDashPhase.CRASHED)
        assertThat(after.bestScore).isEqualTo(9)
    }

    @Test
    fun `a crash holds for a moment before the next run`() {
        val crashed = flyingAt(lane = CaveLane.HIGH, rock = CaveLane.HIGH).advance(elapsedMillis = 50)

        assertThat(crashed.fly(CaveLane.LOW, seed = 2).phase).isEqualTo(BatDashPhase.CRASHED)

        val rested = crashed.fly(times = 64) // just over a second
        assertThat(rested.canTakeOff).isTrue()
        assertThat(rested.fly(CaveLane.LOW, seed = 2).phase).isEqualTo(BatDashPhase.FLYING)
    }

    @Test
    fun `rocks never come closer together than a push and a pull can dodge`() {
        var game = BatDash().fly(CaveLane.HIGH, seed = 7)
        val spawnTimes = mutableListOf<Long>()
        repeat(120_000 / FRAME_MILLIS.toInt()) {
            val before = game.nextRockId
            game = game.advance(FRAME_MILLIS)
            if (game.phase == BatDashPhase.CRASHED) {
                game = game.copy(phase = BatDashPhase.FLYING, rocks = persistentListOf())
            }
            if (game.nextRockId > before) spawnTimes += game.flownMillis
        }

        val gaps = spawnTimes.zipWithNext { a, b -> b - a }
        assertThat(gaps.min()).isGreaterThanOrEqualTo(1_200L - FRAME_MILLIS)
        assertThat(gaps.first()).isGreaterThanOrEqualTo(gaps.last())
    }

    @Test
    fun `no more than two rocks in a row block the same lane`() {
        var game = BatDash().fly(CaveLane.HIGH, seed = 3)
        val lanes = mutableListOf<CaveLane>()
        repeat(60_000 / FRAME_MILLIS.toInt()) {
            val before = game.nextRockId
            game = game.advance(FRAME_MILLIS).copy(phase = BatDashPhase.FLYING)
            if (game.nextRockId > before) lanes += game.rocks.last().lane
        }

        assertThat(lanes.size).isGreaterThanOrEqualTo(20)
        assertThat(lanes.windowed(3).none { window -> window.distinct().size == 1 }).isTrue()
    }

    @Test
    fun `landing ends the run and keeps the best score`() {
        val landed = flyingAt(lane = CaveLane.HIGH, rock = CaveLane.LOW).copy(score = 5).land()

        assertThat(landed.phase).isEqualTo(BatDashPhase.READY)
        assertThat(landed.bestScore).isEqualTo(5)
        assertThat(landed.score).isEqualTo(0)
    }

    @Test
    fun `the cave speeds up the longer the bat flies`() {
        val early = BatDash()
            .fly(CaveLane.HIGH, seed = 1)
            .copy(rocks = persistentListOf(CaveRock(0, CaveLane.LOW, x = 1f)))
        val late = early.copy(flownMillis = 60_000)

        val earlyStep = 1f - early.advance(FRAME_MILLIS).rocks.first().x
        val lateStep = 1f - late.advance(FRAME_MILLIS).rocks.first().x

        assertThat(lateStep).isGreaterThanOrEqualTo(earlyStep * 1.9f)
    }

    @Test
    fun `a fresh crash can't take off yet`() {
        assertThat(BatDash(phase = BatDashPhase.CRASHED).canTakeOff).isFalse()
    }

    /** A run with one rock just about to reach the bat. */
    private fun flyingAt(lane: CaveLane, rock: CaveLane): BatDash = BatDash().fly(lane, seed = 1)
        .copy(rocks = persistentListOf(CaveRock(id = 0, lane = rock, x = BAT_X + 0.001f)), untilNextRockMillis = 10_000)

    private fun BatDash.fly(times: Int): BatDash = (1..times).fold(this) { game, _ -> game.advance(FRAME_MILLIS) }
}
