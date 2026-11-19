package com.eeseka.nimbat.sound

import kotlin.math.abs
import kotlin.math.min

/** How long each ranging chirp lasts, in seconds. */
const val RANGING_CHIRP_SECONDS = 0.05

/** Samples from the start of a pair's first chirp to the start of its second: 0.2 s. */
const val PAIR_SPACING = 9_600

/**
 * How far a pair's spacing may stray from [PAIR_SPACING], in samples, before the recording is rejected.
 * A device's recording can skip or repeat samples (it stutters); a pair heard the wrong distance apart shows it.
 */
const val PAIR_TOLERANCE = 2

/** The chirp the starter plays: sweeping up. */
val UP_CHIRP = ChirpTemplate(18_500.0, 20_000.0, RANGING_CHIRP_SECONDS)

/** The chirp the replier plays: sweeping down, so every device can tell the two apart. */
val DOWN_CHIRP = ChirpTemplate(20_000.0, 18_500.0, RANGING_CHIRP_SECONDS)

/**
 * The two parts in a ranging exchange. The starter plays its chirp pair first; the replier hears it and
 * answers with its own pair. Both record the whole time, so each recording holds all four chirps.
 */
enum class Role(internal val template: ChirpTemplate) {
    Starter(UP_CHIRP),
    Replier(DOWN_CHIRP),
}

/** What a device in [role] plays: its chirp, then the same chirp again [PAIR_SPACING] samples later. */
fun pairSamples(role: Role): FloatArray {
    val wave = role.template.wave
    val samples = FloatArray(PAIR_SPACING + wave.size)
    wave.copyInto(samples, destinationOffset = 0)
    wave.copyInto(samples, destinationOffset = PAIR_SPACING)
    return samples
}

/**
 * Where one device heard each pair, in samples of its own recording. Each is the middle of the pair
 * (the average of its two chirps), which halves the effect of either chirp landing a sample off.
 * [clarityDb] is the least clear of the four chirps.
 */
data class Hearing(val starterAt: Double, val replierAt: Double, val clarityDb: Double)

/**
 * Finds both pairs in [recording], in order: the starter's two chirps, then the replier's two.
 * Returns null if any chirp is missing or either pair isn't [PAIR_SPACING] apart (the recording stuttered).
 *
 * The replier's search only starts after the starter's pair ends, so a device's own loud chirp can't be
 * mistaken for the other one: a wrong match leaves no room for the real pair after it, and the search fails.
 */
fun hear(recording: FloatArray): Hearing? {
    val starter = findPair(Finder(recording, UP_CHIRP), from = 0) ?: return null
    val replierFrom = starter.second.sample + UP_CHIRP.wave.size
    val replier = findPair(Finder(recording, DOWN_CHIRP), from = replierFrom) ?: return null

    val clarityDb = minOf(
        min(starter.first.clarityDb, starter.second.clarityDb),
        min(replier.first.clarityDb, replier.second.clarityDb),
    )
    return Hearing(
        starterAt = (starter.first.sample + starter.second.sample) / 2.0,
        replierAt = (replier.first.sample + replier.second.sample) / 2.0,
        clarityDb = clarityDb,
    )
}

/** Finds a chirp and its partner [PAIR_SPACING] later, or null if either is missing or the spacing is off. */
private fun findPair(finder: Finder, from: Int): Pair<Hit, Hit>? {
    val first = finder.find(from) ?: return null
    val second = finder.find(from = first.sample + PAIR_SPACING / 2) ?: return null
    val spacing = second.sample - first.sample
    return if (abs(spacing - PAIR_SPACING) <= PAIR_TOLERANCE) first to second else null
}

/**
 * The distance between two devices, in metres, from what each heard (the BeepBeep method).
 *
 * Each device measures the gap between the two pairs on its own recording, so neither needs the other's
 * clock or knows when the other started recording. The starter's gap includes the trip to the replier;
 * the replier's gap has the trip taken off. Their difference is two trips, so half of it is one trip.
 *
 * What's left is each device's own speaker-to-mic distance, which the gaps subtract instead of adding.
 * [zeroMeters] puts it back: the average of the two, found by calibrating with the devices touching.
 */
fun beepBeepMeters(
    starter: Hearing,
    replier: Hearing,
    temperatureC: Double = DEFAULT_TEMPERATURE_C,
    zeroMeters: Double = 0.0,
): Double {
    val starterGap = starter.replierAt - starter.starterAt
    val replierGap = replier.replierAt - replier.starterAt
    return samplesToMeters((starterGap - replierGap) / 2, temperatureC) + zeroMeters
}

/**
 * The middle reading of several quick pings, once sorted. One bad ping (an echo, a bump of noise)
 * can't drag it the way it would drag an average.
 */
fun median(readings: List<Double>): Double {
    require(readings.isNotEmpty()) { "need at least one reading" }
    val sorted = readings.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}
