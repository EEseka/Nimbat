package com.eeseka.nimbat.sound

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Slide, multiply, add at every position where the template fits, done with the FFT:
 * `score[i] = recording[i] × template[0] + recording[i + 1] × template[1] + …`
 *
 * Unmix both into tones, multiply tone by tone, then turn the result back into scores.
 * Same answer as sliding position by position, with far less work.
 */
internal fun crossCorrelate(recording: FloatArray, template: FloatArray): FloatArray {
    val positions = recording.size - template.size + 1
    if (positions <= 0 || template.isEmpty()) return FloatArray(0)

    // The FFT treats its input as a loop: sliding off the end comes back in at the start.
    // A size of at least the recording keeps every position we keep clear of that wrap.
    val n = nextPowerOfTwo(recording.size)
    val recordingRe = DoubleArray(n) { if (it < recording.size) recording[it].toDouble() else 0.0 }
    val recordingIm = DoubleArray(n)
    val templateRe = DoubleArray(n) { if (it < template.size) template[it].toDouble() else 0.0 }
    val templateIm = DoubleArray(n)

    fft(recordingRe, recordingIm)
    fft(templateRe, templateIm)

    // Tone by tone: recording × template, with the template's imaginary part flipped (its shift reversed).
    // Without the flip, the template would be slid across backwards, last sample first.
    for (k in 0 until n) {
        val re = recordingRe[k] * templateRe[k] + recordingIm[k] * templateIm[k]
        val im = recordingIm[k] * templateRe[k] - recordingRe[k] * templateIm[k]
        recordingRe[k] = re
        recordingIm[k] = im
    }

    fft(recordingRe, recordingIm, inverse = true)
    return FloatArray(positions) { (recordingRe[it] / n).toFloat() }
}

/**
 * Fast Fourier Transform, in place: turns n samples into n tones, or n tones back into samples with [inverse].
 * Tone k is k × (sample rate ÷ n) Hz; [re] and [im] hold its amount and shift as a complex number.
 * The inverse leaves every value n times too big, so callers divide by n.
 */
internal fun fft(re: DoubleArray, im: DoubleArray, inverse: Boolean = false) {
    val n = re.size
    require(im.size == n) { "re and im must be the same size" }
    require(n > 0 && n and (n - 1) == 0) { "FFT size must be a power of two, was $n" }
    if (n == 1) return

    reorderByReversedBits(re, im)

    // Combine pairs of 1-sample answers into 2-sample answers, those into 4-sample answers, and so on up to n.
    var size = 2
    while (size <= n) {
        val half = size / 2
        val stepAngle = (if (inverse) 2.0 else -2.0) * PI / size
        val cosTable = DoubleArray(half) { cos(stepAngle * it) }
        val sinTable = DoubleArray(half) { sin(stepAngle * it) }
        for (start in 0 until n step size) {
            for (k in 0 until half) {
                val a = start + k
                val b = a + half
                // Turn b's value by the angle for tone k, then add it to a (top) and subtract it from a (bottom)
                val turnedRe = re[b] * cosTable[k] - im[b] * sinTable[k]
                val turnedIm = re[b] * sinTable[k] + im[b] * cosTable[k]
                re[b] = re[a] - turnedRe
                im[b] = im[a] - turnedIm
                re[a] += turnedRe
                im[a] += turnedIm
            }
        }
        size *= 2
    }
}

/**
 * Puts each value at the index whose binary digits are its own index reversed, e.g. with 8 values,
 * index 1 (001) swaps with index 4 (100). This lines up the even and odd samples the FFT combines.
 */
private fun reorderByReversedBits(re: DoubleArray, im: DoubleArray) {
    val bits = re.size.countTrailingZeroBits()
    for (i in re.indices) {
        val j = reverseBits(i, bits)
        if (i < j) {
            re[i] = re[j].also { re[j] = re[i] }
            im[i] = im[j].also { im[j] = im[i] }
        }
    }
}

/** Reads the lowest [bits] binary digits of [value] backwards, e.g. 001 → 100. */
private fun reverseBits(value: Int, bits: Int): Int {
    var remaining = value
    var reversed = 0
    repeat(bits) {
        reversed = (reversed shl 1) or (remaining and 1)
        remaining = remaining shr 1
    }
    return reversed
}

private fun nextPowerOfTwo(atLeast: Int): Int {
    var n = 1
    while (n < atLeast) n *= 2
    return n
}
