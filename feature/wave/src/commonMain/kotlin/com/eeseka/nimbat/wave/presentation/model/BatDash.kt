package com.eeseka.nimbat.wave.presentation.model

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.random.Random

/** Where the bat flies across the cave, from 0 (left edge) to 1 (right edge). */
const val BAT_X = 0.2f

private const val ROCK_START_X = 1.15f
private const val ROCK_GONE_X = -0.15f

/** Cave widths a second. */
private const val START_SPEED = 0.4f
private const val TOP_SPEED = 0.8f
private const val SPEED_UP_MILLIS = 60_000L

private const val FIRST_ROCK_MILLIS = 1_000L
private const val START_GAP_MILLIS = 2_000L

/** Enough time to push, let the hand come back, then lift for the next rock. */
private const val MIN_GAP_MILLIS = 1_200L

private const val MAX_SAME_LANE = 2
private const val RESTART_PAUSE_MILLIS = 1_000L

enum class CaveLane {
    HIGH,
    LOW
}

enum class BatDashPhase {
    READY,
    FLYING,
    CRASHED
}

data class CaveRock(
    val id: Int,
    val lane: CaveLane,
    val x: Float
)

data class BatDash(
    val phase: BatDashPhase = BatDashPhase.READY,
    val batLane: CaveLane = CaveLane.HIGH,
    val rocks: ImmutableList<CaveRock> = persistentListOf(),
    val score: Int = 0,
    val bestScore: Int = 0,
    val flownMillis: Long = 0,
    val untilNextRockMillis: Long = FIRST_ROCK_MILLIS,
    val nextRockId: Int = 0,
    val sameLaneRun: Int = 0, // How many rocks in a row have blocked the newest one's lane
    val crashedMillis: Long = 0,
    val seed: Int = 0
) {
    val canTakeOff: Boolean get() = phase != BatDashPhase.CRASHED || crashedMillis >= RESTART_PAUSE_MILLIS
}

/** Before a run or after a crash, starts a new one; [seed] picks its rocks. */
fun BatDash.fly(lane: CaveLane, seed: Int): BatDash = when {
    phase == BatDashPhase.FLYING -> copy(batLane = lane)
    canTakeOff -> BatDash(
        phase = BatDashPhase.FLYING,
        batLane = lane,
        bestScore = bestScore,
        seed = seed
    )
    else -> this
}

fun BatDash.advance(elapsedMillis: Long): BatDash = when (phase) {
    BatDashPhase.READY -> this
    BatDashPhase.CRASHED -> if (canTakeOff) this else copy(crashedMillis = crashedMillis + elapsedMillis)
    BatDashPhase.FLYING -> flyOn(elapsedMillis)
}

fun BatDash.land(): BatDash = BatDash(bestScore = maxOf(bestScore, score))

private fun BatDash.flyOn(elapsedMillis: Long): BatDash {
    val distance = speed() * elapsedMillis / 1_000f
    val moved = rocks.map { it.copy(x = it.x - distance) }
    val passed = moved.filter { it.x <= BAT_X && it.x + distance > BAT_X }

    if (passed.any { it.lane == batLane }) {
        return copy(
            phase = BatDashPhase.CRASHED,
            rocks = moved.toImmutableList(),
            bestScore = maxOf(bestScore, score)
        )
    }

    val flying = copy(
        rocks = moved.filter { it.x > ROCK_GONE_X }.toImmutableList(),
        score = score + passed.size,
        flownMillis = flownMillis + elapsedMillis,
        untilNextRockMillis = untilNextRockMillis - elapsedMillis
    )
    return if (flying.untilNextRockMillis <= 0) flying.withNewRock() else flying
}

private fun BatDash.withNewRock(): BatDash {
    val random = Random(seed)
    val lastLane = rocks.lastOrNull()?.lane
    val lane = when {
        lastLane != null && sameLaneRun >= MAX_SAME_LANE -> lastLane.other()
        random.nextBoolean() -> CaveLane.HIGH
        else -> CaveLane.LOW
    }
    return copy(
        rocks = (rocks + CaveRock(
            id = nextRockId,
            lane = lane,
            x = ROCK_START_X
        )).toImmutableList(),
        untilNextRockMillis = untilNextRockMillis + gapMillis(),
        nextRockId = nextRockId + 1,
        sameLaneRun = if (lane == lastLane) sameLaneRun + 1 else 1,
        seed = random.nextInt()
    )
}

private fun BatDash.speed(): Float = START_SPEED + (TOP_SPEED - START_SPEED) * speedUp()

private fun BatDash.gapMillis(): Long = START_GAP_MILLIS - ((START_GAP_MILLIS - MIN_GAP_MILLIS) * speedUp()).toLong()

private fun BatDash.speedUp(): Float = (flownMillis.toFloat() / SPEED_UP_MILLIS).coerceAtMost(1f)

private fun CaveLane.other(): CaveLane = if (this == CaveLane.HIGH) CaveLane.LOW else CaveLane.HIGH
