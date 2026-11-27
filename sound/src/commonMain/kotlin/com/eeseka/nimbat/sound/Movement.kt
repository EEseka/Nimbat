package com.eeseka.nimbat.sound

/** The stretch of readings a movement must fill to count. */
private val CONFIRM_WINDOW_READINGS = readingsFor(seconds = 0.5)

/** Half the stretch: a click or a clap lifts one or two readings, someone walking lifts most of them. */
private val CONFIRM_READINGS = CONFIRM_WINDOW_READINGS / 2

private val END_QUIET_READINGS = readingsFor(seconds = 1.0)

/** Past this, a "movement" is more likely the room changing (a fan switched on), so the baseline learns again. */
private val LONG_MOVEMENT_READINGS = readingsFor(seconds = 20.0)

/** One side's echoes must add up to twice the other's to give a movement a direction. */
private const val DIRECTION_RATIO = 2.0

/**
 * Getting up pulls the echoes both ways (the chair, standing, turning), but walking off leans away for seconds on end.
 * Measured in a corridor: leaving led by 8–12 dB for about 5 s; typing and walking past never led away for 3 s.
 */
private val LEAN_AWAY_READINGS = readingsFor(seconds = 3.0)
private const val LEAN_AWAY_DB = 6.0

/** Leaving moves the whole body: a far bigger echo than typing or a shrug. */
private const val WALK_AWAY_PEAK_DB = 12.0

private const val WALK_AWAY_MIN_SECONDS = 0.5

/** Quiet after someone leaves before they count as gone, on top of the second that ends their movement. */
private val GONE_QUIET_READINGS = readingsFor(seconds = 2.0)

/** Where ears are most sensitive and small speakers loudest, so the alarm carries. */
private const val SIREN_LOW_HZ = 650.0
private const val SIREN_HIGH_HZ = 1_300.0
private const val SIREN_SWEEP_SECONDS = 0.45

/** One rise and fall of an audible alarm, made to be played on repeat. */
fun alarmSiren(): FloatArray =
    chirp(SIREN_LOW_HZ, SIREN_HIGH_HZ, SIREN_SWEEP_SECONDS) + chirp(SIREN_HIGH_HZ, SIREN_LOW_HZ, SIREN_SWEEP_SECONDS)

/** How far away movement is noticed. A body's echo fades with distance, so the far one listens for smaller rises. */
enum class WatchRange { NEAR, ROOM }

/** About a metre: 16× the still room's power. */
private const val NEAR_TRIGGER_DB = 12.0

/** Across a room: 4× its power, the same line a hand must cross in a gesture. */
private const val ROOM_TRIGGER_DB = 6.0

/**
 * Something that moved for [durationSeconds], at most [peakRiseDb] above the still room; [direction] if one side led
 * overall. [hasLeanedAway] is set if, for a few seconds somewhere in it, things kept clearly moving off.
 */
data class Movement(
    val durationSeconds: Double,
    val peakRiseDb: Double,
    val direction: GestureDirection?,
    val hasLeanedAway: Boolean,
)

sealed interface WatchReading {
    val isToneClear: Boolean

    /** Learning the still room; [progress] runs from 0 to 1. */
    data class Calibrating(val progress: Float, override val isToneClear: Boolean) : WatchReading

    /**
     * [riseDb] is how far the louder echo stands above the still room. [isMoving] holds while a movement lasts;
     * [hasMovementStarted] is set on its first reading and [endedMovement] once a second of quiet has ended it.
     */
    data class Watching(
        val riseDb: Double,
        val isMoving: Boolean,
        val hasMovementStarted: Boolean,
        val endedMovement: Movement?,
        override val isToneClear: Boolean,
    ) : WatchReading
}

/**
 * Learns the still room like [GestureDetector], then notices anything that keeps moving, in either direction, for
 * about half a second. Short sounds (a click, a clap) and the room's own noise don't count.
 */
class MovementDetector(range: WatchRange) {
    private val room = StillRoom()
    private val triggerDb = when (range) {
        WatchRange.NEAR -> NEAR_TRIGGER_DB
        WatchRange.ROOM -> ROOM_TRIGGER_DB
    }

    private val recentActivity = ArrayDeque<Boolean>()
    private var readingIndex = 0
    private var movement: MovementInProgress? = null
    private var quietInARow = 0

    /**
     * Unlike gestures, the line never rises with the room's bumps and loud handling noise isn't thrown away: a
     * person moving just under the line must not teach the detector to ignore them. A lost tone counts as movement
     * too, because a hand over the mic or someone picking the device up drowns it.
     */
    fun read(motion: Motion): WatchReading {
        // A tone still fading in isn't a lost one
        val isToneClear = motion.toneClarityDb >= TONE_CLEAR_DB || room.isSettling
        room.learn(motion, isToneClear)?.let { progress ->
            return WatchReading.Calibrating(progress = progress, isToneClear = isToneClear)
        }
        readingIndex = readingIndex.saturatingIncrement()

        val towardRiseDb = if (isToneClear) room.towardRiseDb(motion) else 0.0
        val awayRiseDb = if (isToneClear) room.awayRiseDb(motion) else 0.0
        val riseDb = maxOf(towardRiseDb, awayRiseDb, 0.0)
        val isQuiet = isToneClear && towardRiseDb < RELEASE_DB && awayRiseDb < RELEASE_DB
        quietInARow = if (isQuiet) quietInARow + 1 else 0
        if (isQuiet) room.drift(motion, towardRiseDb, awayRiseDb)

        val isActive = !isToneClear || riseDb >= triggerDb
        recentActivity.addLast(isActive)
        if (recentActivity.size > CONFIRM_WINDOW_READINGS) recentActivity.removeFirst()

        val current = movement ?: return watchForStart(towardRiseDb, awayRiseDb, riseDb, isActive, isToneClear)
        current.add(towardRiseDb, awayRiseDb, riseDb, isActive, readingIndex)
        if (readingIndex - current.startIndex > LONG_MOVEMENT_READINGS && isToneClear) {
            room.drift(motion, towardRiseDb, awayRiseDb)
        }
        if (quietInARow < END_QUIET_READINGS) return watching(riseDb, isToneClear, isMoving = true)

        movement = null
        recentActivity.clear()
        return watching(riseDb, isToneClear, endedMovement = current.toMovement())
    }

    private fun watchForStart(
        towardRiseDb: Double,
        awayRiseDb: Double,
        riseDb: Double,
        isActive: Boolean,
        isToneClear: Boolean
    ): WatchReading {
        if (recentActivity.count { it } < CONFIRM_READINGS) return watching(riseDb, isToneClear)

        val readingsSinceFirstActive = recentActivity.size - 1 - recentActivity.indexOf(true)
        movement = MovementInProgress(startIndex = readingIndex - readingsSinceFirstActive).apply {
            add(towardRiseDb, awayRiseDb, riseDb, isActive, readingIndex)
        }
        return watching(riseDb, isToneClear, isMoving = true, hasMovementStarted = true)
    }

    private fun watching(
        riseDb: Double,
        isToneClear: Boolean,
        isMoving: Boolean = false,
        hasMovementStarted: Boolean = false,
        endedMovement: Movement? = null
    ): WatchReading = WatchReading.Watching(
        riseDb = riseDb,
        isMoving = isMoving,
        hasMovementStarted = hasMovementStarted,
        endedMovement = endedMovement,
        isToneClear = isToneClear
    )

    private class MovementInProgress(val startIndex: Int) {
        private var lastActiveIndex = startIndex
        private var peakRiseDb = 0.0
        private var towardTotalDb = 0.0
        private var awayTotalDb = 0.0
        private val recentAwayLeadsDb = ArrayDeque<Double>()
        private var hasLeanedAway = false

        fun add(towardRiseDb: Double, awayRiseDb: Double, riseDb: Double, isActive: Boolean, index: Int) {
            if (isActive) lastActiveIndex = index
            peakRiseDb = maxOf(peakRiseDb, riseDb)
            val toward = towardRiseDb.coerceAtLeast(0.0)
            val away = awayRiseDb.coerceAtLeast(0.0)
            towardTotalDb += toward
            awayTotalDb += away

            recentAwayLeadsDb.addLast(away - toward)
            if (recentAwayLeadsDb.size > LEAN_AWAY_READINGS) recentAwayLeadsDb.removeFirst()
            val isLeaningAway = recentAwayLeadsDb.average() >= LEAN_AWAY_DB
            if (recentAwayLeadsDb.size == LEAN_AWAY_READINGS && isLeaningAway) hasLeanedAway = true
        }

        fun toMovement() = Movement(
            durationSeconds = secondsFor(lastActiveIndex - startIndex + 1),
            peakRiseDb = peakRiseDb,
            direction = when {
                towardTotalDb >= awayTotalDb * DIRECTION_RATIO -> GestureDirection.TOWARD
                awayTotalDb >= towardTotalDb * DIRECTION_RATIO -> GestureDirection.AWAY
                else -> null
            },
            hasLeanedAway = hasLeanedAway
        )
    }
}

/**
 * Tells when the person sitting at the device leaves: one long, strong movement away from it, then quiet.
 * Leaning back or a shrug is followed by more movement; someone passing further off echoes too weakly.
 */
class WalkAwayDetector {
    private var quietSinceLeaving: Int? = null

    /** True on the one reading the person counts as gone. */
    fun read(reading: WatchReading): Boolean {
        if (reading !is WatchReading.Watching) return false
        if (reading.endedMovement?.isWalkingAway() == true) {
            quietSinceLeaving = 0
            return false
        }

        val quietReadings = quietSinceLeaving ?: return false
        if (reading.isMoving) {
            quietSinceLeaving = null
            return false
        }
        if (quietReadings + 1 < GONE_QUIET_READINGS) {
            quietSinceLeaving = quietReadings + 1
            return false
        }
        quietSinceLeaving = null
        return true
    }

    private fun Movement.isWalkingAway(): Boolean = hasLeanedAway &&
        peakRiseDb >= WALK_AWAY_PEAK_DB &&
        durationSeconds >= WALK_AWAY_MIN_SECONDS
}
