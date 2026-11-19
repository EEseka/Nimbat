package com.eeseka.nimbat.sound

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Samples per second, the standard rate on Android, iOS, desktop and web. */
const val SAMPLE_RATE = 48_000

/** How long a tone takes to fade in and out, so the speaker doesn't click. */
const val TONE_FADE_SECONDS = 0.01

/** How long a loop takes to fade in when it starts and out when it stops, so the speaker doesn't click. */
const val LOOP_FADE_SECONDS = 0.02

/** How many samples [durationSeconds] of sound takes, e.g. 0.05 s → 2,400. */
fun sampleCountFor(durationSeconds: Double): Int = (durationSeconds * SAMPLE_RATE).roundToInt()

/**
 * A steady tone: a chirp whose frequency never changes.
 * Only its first and last [TONE_FADE_SECONDS] fade, so a long tone stays at full volume in between.
 */
fun tone(frequencyHz: Double, durationSeconds: Double): FloatArray {
    val samples = rawWave(startHz = frequencyHz, endHz = frequencyHz, sampleCount = sampleCountFor(durationSeconds))
    fadeEdges(samples = samples, fadeSamples = sampleCountFor(TONE_FADE_SECONDS))
    return samples
}

/**
 * A chirp: the frequency slides steadily from [startHz] to [endHz].
 * The fade covers the whole chirp, a smooth hill from silence to full volume and back.
 */
fun chirp(startHz: Double, endHz: Double, durationSeconds: Double): FloatArray {
    val samples = rawWave(startHz = startHz, endHz = endHz, sampleCount = sampleCountFor(durationSeconds))
    fadeEdges(samples = samples, fadeSamples = samples.size / 2)
    return samples
}

/**
 * A tone with exactly [cycles] whole cycles in [sampleCount] samples, with no fades, so played on repeat it
 * runs on forever without a click: the last sample leads straight back into the first.
 * Its frequency is [cycles] × sample rate ÷ [sampleCount].
 */
fun loopingTone(cycles: Int, sampleCount: Int): FloatArray {
    val frequencyHz = cycles.toDouble() * SAMPLE_RATE / sampleCount
    return rawWave(startHz = frequencyHz, endHz = frequencyHz, sampleCount = sampleCount)
}

/** A copy of a looping [block] that fades in from silence: the loop's first pass. */
fun loopFadeIn(block: FloatArray): FloatArray {
    val fadeLength = loopFadeLength(block)
    return block.copyOf().also { faded ->
        for (n in 0 until fadeLength) faded[n] *= hannRise(n, fadeLength)
    }
}

/** The start of a looping [block] fading out to silence: played after the loop's last pass. */
fun loopFadeOut(block: FloatArray): FloatArray {
    val fadeLength = loopFadeLength(block)
    return FloatArray(fadeLength) { n -> block[n] * (1 - hannRise(n, fadeLength)) }
}

private fun loopFadeLength(block: FloatArray) = sampleCountFor(LOOP_FADE_SECONDS).coerceAtMost(block.size)

/**
 * A chirp's twin: the same chirp a quarter cycle ahead (cos instead of sin), with the same fade.
 * The Finder scores a recording against both, because together they trace the smooth outline of the score.
 */
internal fun chirpTwin(startHz: Double, endHz: Double, durationSeconds: Double): FloatArray {
    val samples = rawWave(startHz = startHz, endHz = endHz, sampleCount = sampleCountFor(durationSeconds), shape = ::cos)
    fadeEdges(samples = samples, fadeSamples = samples.size / 2)
    return samples
}

/**
 * The wave itself, before any fading. Each sample is [shape] of the angle (sin, or cos for a twin), then the
 * angle grows by one step: 2π × (frequency ÷ sample rate). The frequency is recalculated every sample, so a
 * chirp's steps grow (or shrink) as it goes, and a tone's steps stay the same.
 */
private fun rawWave(
    startHz: Double,
    endHz: Double,
    sampleCount: Int,
    shape: (Double) -> Double = ::sin,
): FloatArray {
    val samples = FloatArray(sampleCount)
    var angle = 0.0
    for (n in 0 until sampleCount) {
        val frequencyHz = startHz + (endHz - startHz) * n / sampleCount
        samples[n] = shape(angle).toFloat()
        angle += 2 * PI * frequencyHz / SAMPLE_RATE
    }
    return samples
}

/**
 * Fades the volume in over the first [fadeSamples] and out over the last [fadeSamples], along the Hann curve
 * (0.5 - 0.5 × cos), so the sound starts and stops without a click.
 * The fade in is the rising half of the Hann hill (0° to 180°), and the fade out is the same half mirrored.
 * With [fadeSamples] at half the size, the whole thing becomes one Hann hill (a Hann window).
 */
internal fun fadeEdges(samples: FloatArray, fadeSamples: Int) {
    val fadeLength = fadeSamples.coerceAtMost(samples.size / 2)
    if (fadeLength == 0) return
    val last = samples.lastIndex
    for (n in 0 until fadeLength) {
        val volume = hannRise(n, fadeLength)
        samples[n] *= volume
        samples[last - n] *= volume
    }
}

/** The rising half of the Hann curve: 0 at [n] = 0, climbing smoothly towards 1 at [n] = [length]. */
private fun hannRise(n: Int, length: Int): Float = (0.5 - 0.5 * cos(PI * n / length)).toFloat()
