package com.eeseka.nimbat.sound

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * How tall a spike must be, as a share of the biggest one, to count as an arrival.
 * Low enough to catch a direct sound weakened to a third of its echo, high enough to stay above noise bumps.
 */
const val ARRIVAL_BAR = 0.3f

/** The chirp the Finder searches for, plus its twin: the same chirp a quarter cycle ahead. */
class ChirpTemplate(startHz: Double, endHz: Double, durationSeconds: Double) {
    internal val wave = chirp(startHz, endHz, durationSeconds)
    internal val twin = chirpTwin(startHz, endHz, durationSeconds)
}

/**
 * One chirp found in a recording: [sample] is where it starts, and [clarityDb] is how far its spike
 * stands above the typical score (higher means heard more clearly).
 */
data class Hit(val sample: Int, val clarityDb: Double)

/**
 * Searches one recording for one chirp. The scores for every position are worked out once, up front,
 * so [find] can be called again, e.g. to look for a second chirp further along.
 */
class Finder(recording: FloatArray, template: ChirpTemplate) {

    /** Slide-multiply-add scores against the chirp. They wiggle + and − at the chirp's own frequency. */
    private val raw = crossCorrelate(recording, template.wave)

    /** The smooth outline of [raw]: raw and twin scores combined as √(raw² + twin²), so the wiggle is gone. */
    private val outline: FloatArray = crossCorrelate(recording, template.twin).let { twin ->
        FloatArray(raw.size) { sqrt(raw[it] * raw[it] + twin[it] * twin[it]) }
    }

    /**
     * The first chirp at or after sample [from] whose spike reaches [bar] × the biggest spike,
     * or null when there's nothing to find.
     */
    fun find(from: Int = 0, bar: Float = ARRIVAL_BAR): Hit? {
        require(bar > 0f && bar <= 1f) { "bar must be between 0 and 1, was $bar" }
        val start = from.coerceAtLeast(0)
        val top = firstSpikeTop(start, bar) ?: return null
        return Hit(sample = pinToWiggle(top), clarityDb = clarityDb(top, start))
    }

    /** Walks right to the first outline score over the bar, then climbs that spike to its top. */
    private fun firstSpikeTop(start: Int, bar: Float): Int? {
        if (start >= outline.size) return null
        val biggest = (start until outline.size).maxOf { outline[it] }
        if (biggest == 0f) return null
        val threshold = bar * biggest

        var i = (start until outline.size).first { outline[it] >= threshold }
        var top = i
        while (i < outline.size && outline[i] >= threshold) {
            if (outline[i] > outline[top]) top = i
            i++
        }
        return top
    }

    /**
     * The outline finds the right neighbourhood; the raw wiggle pins the exact sample. At perfect alignment
     * every product is positive, so the raw score is tallest there. A wiggle repeats about every 2.5 samples
     * (48,000 ÷ ~19,000 Hz), so ±2 samples always holds one. abs() because a mic can flip the wave upside down.
     */
    private fun pinToWiggle(top: Int): Int =
        (maxOf(0, top - 2)..minOf(raw.lastIndex, top + 2)).maxBy { abs(raw[it]) }

    /**
     * The spike's height over the median outline score, in [decibels]: 20 dB means the spike is 10× taller
     * than a typical score. A perfectly silent background (typical score 0) is infinitely clear.
     */
    private fun clarityDb(top: Int, start: Int): Double {
        val sorted = outline.copyOfRange(start, outline.size).apply { sort() }
        val typical = sorted[sorted.size / 2]
        if (typical == 0f) return Double.POSITIVE_INFINITY
        return decibels((outline[top] / typical).toDouble())
    }
}
