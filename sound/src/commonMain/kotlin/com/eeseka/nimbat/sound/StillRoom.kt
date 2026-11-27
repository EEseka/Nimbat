package com.eeseka.nimbat.sound

import kotlin.math.abs

/** Readings dropped while the mic wakes up and the tone fades in. */
private val SETTLE_READINGS = readingsFor(seconds = 0.3)

/** Clear readings of the still room that make up its baseline. */
private val CALIBRATION_READINGS = readingsFor(seconds = 1.0)

/** Clatter (cutlery, a clap) drowns the tone across every pitch and can lean either way by chance. */
private const val CLATTER_CLARITY_DROP_DB = 12.0

private const val CLARITY_LEARNING = 0.05

/** Sides this close moved together, as room noise does. */
private const val SIDES_AGREE_DB = 2.0

/** Share of each idle reading the baseline learns, so a fan switching on becomes part of the room in seconds. */
private const val DRIFT = 0.02

/** Below any trigger, so something hovering at the line can't flicker on and off. */
internal const val RELEASE_DB = 3.0

/**
 * What the echoes sound like when nothing moves: learned from about a second of clear readings, then kept up to
 * date with the room's own noise (a fan switching on, a TV), so only real movement stands out.
 */
internal class StillRoom {
    private var settledReadings = 0
    private val towardCalibration = mutableListOf<Double>()
    private val awayCalibration = mutableListOf<Double>()
    private val clarityCalibration = mutableListOf<Double>()
    private var towardBaselineDb = 0.0
    private var awayBaselineDb = 0.0
    private var usualClarityDb = 0.0

    val isSettling: Boolean
        get() = settledReadings < SETTLE_READINGS

    /** How far learning has got, from 0 to 1, or null once the room is learned. Only clear readings count. */
    fun learn(motion: Motion, isToneClear: Boolean): Float? {
        if (isSettling) {
            settledReadings++
            return 0f
        }
        if (towardCalibration.size >= CALIBRATION_READINGS) return null

        if (isToneClear) {
            towardCalibration += motion.towardDb
            awayCalibration += motion.awayDb
            clarityCalibration += motion.toneClarityDb
        }
        if (towardCalibration.size == CALIBRATION_READINGS) {
            towardBaselineDb = median(towardCalibration)
            awayBaselineDb = median(awayCalibration)
            usualClarityDb = median(clarityCalibration)
        }
        return towardCalibration.size.toFloat() / CALIBRATION_READINGS
    }

    /** Clatter readings can't fire; every other reading teaches the room's usual clarity. */
    fun isClatter(motion: Motion): Boolean {
        val isClatter = motion.toneClarityDb < usualClarityDb - CLATTER_CLARITY_DROP_DB
        if (!isClatter) usualClarityDb += (motion.toneClarityDb - usualClarityDb) * CLARITY_LEARNING
        return isClatter
    }

    fun towardRiseDb(motion: Motion): Double = motion.towardDb - towardBaselineDb

    fun awayRiseDb(motion: Motion): Double = motion.awayDb - awayBaselineDb

    /** Room noise moves both sides together, a hand favours one; only room noise is learned. */
    fun drift(motion: Motion, towardRiseDb: Double, awayRiseDb: Double) {
        if (abs(towardRiseDb - awayRiseDb) >= SIDES_AGREE_DB) return
        towardBaselineDb += (motion.towardDb - towardBaselineDb) * DRIFT
        awayBaselineDb += (motion.awayDb - awayBaselineDb) * DRIFT
    }
}
