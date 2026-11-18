package com.eeseka.nimbat.sound

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import kotlin.test.Test

class PhysicsTest {

    @Test
    fun `sound travels 331 m per s at 0 degrees and 343 at 20`() {
        assertThat(speedOfSound(0.0)).isCloseTo(331.3, 0.01)
        assertThat(speedOfSound(20.0)).isCloseTo(343.2, 0.1)
    }

    @Test
    fun `sound travels faster in warmer air`() {
        assertThat(speedOfSound(30.0)).isGreaterThan(speedOfSound(10.0))
    }

    @Test
    fun `temperature for a speed undoes speed of sound`() {
        listOf(-10.0, 0.0, 25.0, 40.0).forEach { temperature ->
            assertThat(temperatureFor(speedOfSound(temperature))).isCloseTo(temperature, 1e-9)
        }
    }

    @Test
    fun `48000 samples last one second`() {
        assertThat(samplesToSeconds(48_000.0)).isEqualTo(1.0)
    }

    @Test
    fun `one sample of sound is about 7 mm at 20 degrees`() {
        assertThat(samplesToMeters(1.0)).isCloseTo(0.00715, 0.00001)
    }

    @Test
    fun `280 samples is 2 m at 20 degrees`() {
        assertThat(samplesToMeters(280.0)).isCloseTo(2.0, 0.01)
    }

    @Test
    fun `ten times bigger is 20 dB and half is minus 6`() {
        assertThat(decibels(10.0)).isCloseTo(20.0, 1e-9)
        assertThat(decibels(1.0)).isCloseTo(0.0, 1e-9)
        assertThat(decibels(0.5)).isCloseTo(-6.02, 0.01)
    }
}
