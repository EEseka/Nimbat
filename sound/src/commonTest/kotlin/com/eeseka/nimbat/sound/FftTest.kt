package com.eeseka.nimbat.sound

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test

class FftTest {

    // With 8 samples at 48,000 per second, tone k is k × 6,000 Hz: tone 1 is 6,000 Hz, tone 2 is 12,000 Hz.
    // A sine of size 1 shows up as n ÷ 2 = 4 at its tone (and 4 at its mirror, tone n − k).
    private val sixK = doubleArrayOf(0.0, 0.707107, 1.0, 0.707107, 0.0, -0.707107, -1.0, -0.707107)
    private val twelveK = doubleArrayOf(0.0, 1.0, 0.0, -1.0, 0.0, 1.0, 0.0, -1.0)

    @Test
    fun `a 12000 Hz tone lands only on tone 2`() {
        val sizes = toneSizes(twelveK)

        assertThat(sizes[2]).isCloseTo(4.0, 0.001)
        assertThat(sizes[6]).isCloseTo(4.0, 0.001)
        listOf(0, 1, 3, 4, 5, 7).forEach { assertThat(sizes[it]).isCloseTo(0.0, 0.001) }
    }

    @Test
    fun `fft unmixes 3 of 6000 Hz plus 2 of 12000 Hz`() {
        val mix = DoubleArray(8) { 3 * sixK[it] + 2 * twelveK[it] }
        val sizes = toneSizes(mix)

        assertThat(sizes[1]).isCloseTo(3 * 4.0, 0.001)
        assertThat(sizes[2]).isCloseTo(2 * 4.0, 0.001)
        assertThat(sizes[3]).isCloseTo(0.0, 0.001)
    }

    @Test
    fun `inverse fft brings the samples back`() {
        val random = Random(1)
        val samples = DoubleArray(64) { random.nextDouble(-1.0, 1.0) }
        val re = samples.copyOf()
        val im = DoubleArray(64)

        fft(re, im)
        fft(re, im, inverse = true)

        samples.forEachIndexed { i, value -> assertThat(re[i] / 64).isCloseTo(value, 1e-9) }
    }

    @Test
    fun `fast scores match slow slide multiply add`() {
        val random = Random(42)
        val recording = FloatArray(1_000) { random.nextFloat() * 2 - 1 }
        val template = FloatArray(100) { random.nextFloat() * 2 - 1 }

        val fast = crossCorrelate(recording, template)
        val slow = slowCorrelate(recording, template)

        assertThat(fast.size).isEqualTo(901)
        slow.forEachIndexed { i, value -> assertThat(fast[i]).isCloseTo(value, 0.001f) }
    }

    @Test
    fun `a template longer than the recording has no scores`() {
        assertThat(crossCorrelate(FloatArray(10), FloatArray(20)).size).isEqualTo(0)
    }

    /** How much of each tone: the length of each (re, im) pair. */
    private fun toneSizes(samples: DoubleArray): List<Double> {
        val re = samples.copyOf()
        val im = DoubleArray(samples.size)
        fft(re, im)
        return re.indices.map { sqrt(re[it] * re[it] + im[it] * im[it]) }
    }

    /** The plain way: slide, multiply, add at every position. */
    private fun slowCorrelate(recording: FloatArray, template: FloatArray): FloatArray =
        FloatArray(recording.size - template.size + 1) { i ->
            var sum = 0f
            for (j in template.indices) sum += recording[i + j] * template[j]
            sum
        }
}
