package com.eeseka.nimbat.sound

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import kotlin.random.Random
import kotlin.test.Test

class FinderTest {

    private val up = ChirpTemplate(18_500.0, 20_000.0, 0.05)
    private val down = ChirpTemplate(20_000.0, 18_500.0, 0.05)

    @Test
    fun `finds a chirp at the exact sample it starts`() {
        val recording = silence(0.5).withChirp(up, at = 10_000)

        assertThat(Finder(recording, up).find()).isNotNull().prop(Hit::sample).isEqualTo(10_000)
    }

    @Test
    fun `finds a quiet chirp under noise twice as loud`() {
        val recording = noise(0.5, loudness = 0.2f).withChirp(up, at = 7_777, loudness = 0.1f)

        assertThat(Finder(recording, up).find()).isNotNull().prop(Hit::sample).isEqualTo(7_777)
    }

    @Test
    fun `picks the first arrival over a louder echo`() {
        // Direct sound 2 m away (280 samples), weakened; echo off a wall 5 m away (700 samples), louder
        val recording = silence(0.5)
            .withChirp(up, at = 280, loudness = 0.4f)
            .withChirp(up, at = 700, loudness = 1f)

        assertThat(Finder(recording, up).find()).isNotNull().prop(Hit::sample).isEqualTo(280)
    }

    @Test
    fun `finds its own sweep and ignores the opposite one`() {
        val recording = silence(0.5)
            .withChirp(down, at = 3_000)
            .withChirp(up, at = 15_000)

        assertThat(Finder(recording, up).find()).isNotNull().prop(Hit::sample).isEqualTo(15_000)
        assertThat(Finder(recording, down).find()).isNotNull().prop(Hit::sample).isEqualTo(3_000)
    }

    @Test
    fun `from skips earlier chirps`() {
        val recording = silence(1.0)
            .withChirp(up, at = 2_000)
            .withChirp(up, at = 30_000)
        val finder = Finder(recording, up)

        assertThat(finder.find()).isNotNull().prop(Hit::sample).isEqualTo(2_000)
        assertThat(finder.find(from = 10_000)).isNotNull().prop(Hit::sample).isEqualTo(30_000)
    }

    @Test
    fun `a flipped recording is found at the same sample`() {
        val recording = silence(0.5).withChirp(up, at = 4_321, loudness = -1f)

        assertThat(Finder(recording, up).find()).isNotNull().prop(Hit::sample).isEqualTo(4_321)
    }

    @Test
    fun `nothing is found in silence`() {
        assertThat(Finder(silence(0.5), up).find()).isNull()
    }

    @Test
    fun `nothing is found in a recording shorter than the chirp`() {
        assertThat(Finder(silence(0.01), up).find()).isNull()
    }

    @Test
    fun `a clear chirp scores higher clarity than a buried one`() {
        val clear = noise(0.5, loudness = 0.05f).withChirp(up, at = 5_000)
        val buried = noise(0.5, loudness = 0.5f).withChirp(up, at = 5_000, loudness = 0.1f)

        val clearDb = checkNotNull(Finder(clear, up).find()).clarityDb
        val buriedDb = checkNotNull(Finder(buried, up).find()).clarityDb

        assertThat(clearDb).isGreaterThan(buriedDb)
    }

    private fun silence(seconds: Double) = FloatArray(sampleCountFor(seconds))

    private fun noise(seconds: Double, loudness: Float): FloatArray {
        val random = Random(7)
        return FloatArray(sampleCountFor(seconds)) { (random.nextFloat() * 2 - 1) * loudness }
    }

    /** Adds the template's chirp into this recording, starting at sample [at]. */
    private fun FloatArray.withChirp(template: ChirpTemplate, at: Int, loudness: Float = 1f): FloatArray {
        template.wave.forEachIndexed { j, value -> this[at + j] += value * loudness }
        return this
    }
}
