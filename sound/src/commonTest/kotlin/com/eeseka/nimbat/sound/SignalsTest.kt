package com.eeseka.nimbat.sound

import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.isBetween
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import kotlin.math.sqrt
import kotlin.test.Test

class SignalsTest {

    @Test
    fun `50 ms of sound is 2400 samples`() {
        assertThat(sampleCountFor(0.05)).isEqualTo(2_400)
    }

    @Test
    fun `a 50 ms chirp has 2400 samples`() {
        assertThat(chirp(18_500.0, 20_000.0, 0.05).size).isEqualTo(2_400)
    }

    @Test
    fun `samples stay between minus one and one`() {
        assertThat(chirp(18_500.0, 20_000.0, 0.05).toList()).each { it.isBetween(-1f, 1f) }
        assertThat(tone(19_000.0, 0.5).toList()).each { it.isBetween(-1f, 1f) }
    }

    @Test
    fun `a chirp starts and ends silent`() {
        val samples = chirp(18_500.0, 20_000.0, 0.05)

        assertThat(samples.first()).isCloseTo(0f, 0.001f)
        assertThat(samples.last()).isCloseTo(0f, 0.001f)
    }

    @Test
    fun `a tone starts and ends silent`() {
        val samples = tone(19_000.0, 0.5)

        assertThat(samples.first()).isCloseTo(0f, 0.001f)
        assertThat(samples.last()).isCloseTo(0f, 0.001f)
    }

    @Test
    fun `a tone shorter than its two fades still fades cleanly`() {
        val samples = tone(19_000.0, 0.005)

        assertThat(samples.size).isEqualTo(240)
        assertThat(samples.first()).isCloseTo(0f, 0.001f)
        assertThat(samples.last()).isCloseTo(0f, 0.001f)
        assertThat(samples.toList()).each { it.isBetween(-1f, 1f) }
    }

    @Test
    fun `a tone follows the sine formula between its fades`() {
        // 6,000 Hz moves 45° per sample, so the expected values are easy to check by hand.
        // Sample 2,400 is exactly 300 full cycles in (back to 0°) and well past the fade
        val samples = tone(6_000.0, 0.1)
        val expected = listOf(0f, 0.707f, 1f, 0.707f, 0f, -0.707f, -1f, -0.707f)

        expected.forEachIndexed { i, value ->
            assertThat(samples[2_400 + i]).isCloseTo(value, 0.001f)
        }
    }

    @Test
    fun `a tone reaches full volume between its fades`() {
        val middle = tone(6_000.0, 0.5).copyOfRange(sampleCountFor(0.1), sampleCountFor(0.4))

        assertThat(middle.max()).isCloseTo(1f, 0.001f)
    }

    @Test
    fun `an up chirp rises in pitch`() {
        // More zero crossings in the same stretch of time means more cycles, so a higher frequency
        val samples = chirp(6_000.0, 12_000.0, 0.05)
        val half = samples.size / 2

        assertThat(zeroCrossings(samples, half until samples.size))
            .isGreaterThan(zeroCrossings(samples, 0 until half))
    }

    @Test
    fun `a chirp and its twin together trace the fade`() {
        // sin² + cos² = 1, so √(wave² + twin²) is exactly the fade: 0 at the edges, 1 in the middle
        val wave = chirp(18_500.0, 20_000.0, 0.05)
        val twin = chirpTwin(18_500.0, 20_000.0, 0.05)
        fun outline(n: Int) = sqrt(wave[n] * wave[n] + twin[n] * twin[n])

        assertThat(outline(0)).isCloseTo(0f, 0.001f)
        assertThat(outline(600)).isCloseTo(0.5f, 0.001f)
        assertThat(outline(1_200)).isCloseTo(1f, 0.001f)
    }

    @Test
    fun `a loop fades in from silence and then plays at full volume`() {
        val block = dopplerTone()
        val faded = loopFadeIn(block)
        val fadeEnd = sampleCountFor(LOOP_FADE_SECONDS)

        assertThat(faded.size).isEqualTo(block.size)
        assertThat(faded[1]).isCloseTo(0f, 0.001f)
        (fadeEnd until block.size).forEach { assertThat(faded[it]).isEqualTo(block[it]) }
    }

    @Test
    fun `a loop fade out carries on from the block's start and ends silent`() {
        // A loop always ends where its block begins, so the fade out picks up seamlessly after the last pass
        val block = dopplerTone()
        val tail = loopFadeOut(block)

        assertThat(tail.size).isEqualTo(sampleCountFor(LOOP_FADE_SECONDS))
        assertThat(tail[1]).isCloseTo(block[1], 0.001f)
        assertThat(tail.last()).isCloseTo(0f, 0.001f)
    }

    /** Counts how often the wave crosses from one side of 0 to the other: twice per cycle. */
    private fun zeroCrossings(samples: FloatArray, range: IntRange): Int =
        range.drop(1).count { (samples[it - 1] < 0f) != (samples[it] < 0f) }
}
