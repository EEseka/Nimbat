package com.eeseka.nimbat.sound

import kotlin.math.abs

/** The least a hand's echo must rise above the still room: 4× its power. */
private const val TRIGGER_DB = 6.0

/** In a busy room a hand must also clear the room's recent bumps by this much. */
private const val ABOVE_ROOM_BUMPS_DB = 6.0

/** Per reading: about 2 dB a second. */
private const val ROOM_BUMP_FADE_DB = 0.1

/** Rooms bump about 10 dB and strokes 28–42, so a stroke that just misses can't lift the line past the next. */
private const val MAX_ROOM_BUMPS_DB = 12.0

/** A hand lifts one side far more than the other; talking or swaying lifts both. */
private const val DOMINANCE_DB = 8.0

private const val QUIET_READINGS_TO_RELEASE = 2

/** So one push with a ragged echo can't fire twice. */
private val MIN_STROKE_READINGS = readingsFor(seconds = 0.3)

/** So a stroke that never goes quiet (e.g. a fan switched on mid-gesture) can't block forever. */
private val MAX_STROKE_READINGS = readingsFor(seconds = 1.5)

/** A hand coming back turns straight round: recorded turnarounds rested 4 readings (about 170 ms) at most. */
private const val MAX_TURNAROUND_QUIET_READINGS = 4

private val RETURN_READINGS = readingsFor(seconds = 1.5)

enum class GestureDirection { TOWARD, AWAY }

/** A hand moving toward or away from the device, at [speedMetersPerSecond] (always positive). */
data class Gesture(val direction: GestureDirection, val speedMetersPerSecond: Double)

sealed interface GestureReading {
    val isToneClear: Boolean

    /** Learning the still room; [progress] runs from 0 to 1. */
    data class Calibrating(val progress: Float, override val isToneClear: Boolean) : GestureReading

    /**
     * [towardRiseDb] and [awayRiseDb] are how far the echoes stand above the still room (0 when nothing moves).
     * [gesture] is set on the one reading a gesture starts.
     */
    data class Sensing(
        val towardRiseDb: Double,
        val awayRiseDb: Double,
        val gesture: Gesture?,
        override val isToneClear: Boolean,
    ) : GestureReading
}

/**
 * Learns how loud the echoes are in the still room, then fires when one side rises well above that and above the
 * other side. Each rise is a stroke that fires once, until its own side goes quiet. A push goes and comes back,
 * so a stroke turning straight round from a gesture is its way back; one from a resting hand is a new gesture.
 */
class GestureDetector {
    private val room = StillRoom()
    private var roomBumpsDb = 0.0 // The biggest rise the room made by itself lately, fading

    private var stroke: GestureDirection? = null
    private var strokeReadings = 0
    private var quietInARow = 0
    private var isBlocked = false // After the tone was lost, until the room is quiet again

    // Counted from when the latest stroke started, so a rest counts from when the hand stops
    private var lastStrokeDirection: GestureDirection? = null
    private var didLastStrokeFire = false
    private var readingsSinceLastStroke = 0
    private var quietSinceLastStroke = 0

    fun read(motion: Motion): GestureReading {
        val isToneClear = motion.toneClarityDb >= TONE_CLEAR_DB
        room.learn(motion, isToneClear)?.let { progress ->
            return GestureReading.Calibrating(progress = progress, isToneClear = isToneClear)
        }
        if (!isToneClear) return toneLost()

        val isClatter = room.isClatter(motion)
        val towardRiseDb = room.towardRiseDb(motion)
        val awayRiseDb = room.awayRiseDb(motion)
        return GestureReading.Sensing(
            towardRiseDb = towardRiseDb.coerceAtLeast(0.0),
            awayRiseDb = awayRiseDb.coerceAtLeast(0.0),
            gesture = detect(motion, towardRiseDb, awayRiseDb, isClatter),
            isToneClear = true,
        )
    }

    /** Losing the tone looks like a huge rise, so nothing fires or is learned until the room is quiet again. */
    private fun toneLost(): GestureReading {
        didLastStrokeFire = false
        isBlocked = true
        stroke = null
        strokeReadings = 0
        quietInARow = 0
        return GestureReading.Sensing(towardRiseDb = 0.0, awayRiseDb = 0.0, gesture = null, isToneClear = false)
    }

    private fun detect(
        motion: Motion,
        towardRiseDb: Double,
        awayRiseDb: Double,
        isClatter: Boolean
    ): Gesture? {
        if (isBlocked) {
            strokeReadings++
            quietInARow = if (towardRiseDb < RELEASE_DB && awayRiseDb < RELEASE_DB) quietInARow + 1 else 0
            if (quietInARow >= QUIET_READINGS_TO_RELEASE || strokeReadings >= MAX_STROKE_READINGS) {
                isBlocked = false
                strokeReadings = 0
            }
            return null
        }

        stroke?.let { endStrokeIfQuiet(riseDb = if (it == GestureDirection.TOWARD) towardRiseDb else awayRiseDb) }
        // A busy room never sits under the release line, so quiet means down at its own level
        val quietDb = maxOf(RELEASE_DB, roomBumpsDb)
        val isQuiet = towardRiseDb < quietDb && awayRiseDb < quietDb
        readingsSinceLastStroke = readingsSinceLastStroke.saturatingIncrement()
        if (isQuiet) quietSinceLastStroke = quietSinceLastStroke.saturatingIncrement()
        if (isClatter) return null

        val triggerDb = maxOf(TRIGGER_DB, roomBumpsDb + ABOVE_ROOM_BUMPS_DB)
        val rising = when {
            towardRiseDb >= triggerDb && towardRiseDb >= awayRiseDb + DOMINANCE_DB -> GestureDirection.TOWARD
            awayRiseDb >= triggerDb && awayRiseDb >= towardRiseDb + DOMINANCE_DB -> GestureDirection.AWAY
            else -> null
        }
        if (rising == null) {
            if (stroke == null) {
                learnRoomBumps(towardRiseDb, awayRiseDb)
                room.drift(motion, towardRiseDb, awayRiseDb)
            }
            return null
        }
        if (rising == stroke) return null

        val isHandComingBack = isComingBack(rising)
        stroke = rising
        strokeReadings = 0
        quietInARow = 0
        lastStrokeDirection = rising
        didLastStrokeFire = !isHandComingBack
        readingsSinceLastStroke = 0
        quietSinceLastStroke = 0
        if (isHandComingBack) return null
        return Gesture(direction = rising, speedMetersPerSecond = abs(motion.speedMetersPerSecond))
    }

    /**
     * Turning straight round from a stroke that fired, or carrying on a way back after a hitch. Never the opposite
     * of a way back, so one stray stroke swallows one gesture at most instead of throwing every push out of step.
     */
    private fun isComingBack(direction: GestureDirection): Boolean {
        val isSoon = quietSinceLastStroke <= MAX_TURNAROUND_QUIET_READINGS && readingsSinceLastStroke <= RETURN_READINGS
        val isTurningRound = didLastStrokeFire && direction != lastStrokeDirection
        val isCarryingOn = !didLastStrokeFire && direction == lastStrokeDirection
        return isSoon && (isTurningRound || isCarryingOn)
    }

    private fun endStrokeIfQuiet(riseDb: Double) {
        strokeReadings++
        quietInARow = if (riseDb < RELEASE_DB) quietInARow + 1 else 0
        val hasSettled = quietInARow >= QUIET_READINGS_TO_RELEASE && strokeReadings >= MIN_STROKE_READINGS
        if (hasSettled || strokeReadings >= MAX_STROKE_READINGS) {
            stroke = null
            quietInARow = 0
        }
    }

    private fun learnRoomBumps(towardRiseDb: Double, awayRiseDb: Double) {
        val bumpDb = maxOf(towardRiseDb, awayRiseDb, roomBumpsDb - ROOM_BUMP_FADE_DB, 0.0)
        roomBumpsDb = bumpDb.coerceAtMost(MAX_ROOM_BUMPS_DB)
    }
}
