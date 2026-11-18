package com.eeseka.nimbat.sound

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test

class DopplerTest {

    @Test
    fun `a hand at 1 m per s shifts the tone by about 110 Hz`() {
        assertThat(dopplerShiftHz(1.0)).isCloseTo(110.7, 0.1)
    }

    @Test
    fun `speed for a shift undoes the doppler shift`() {
        assertThat(speedForShift(dopplerShiftHz(1.7))).isCloseTo(1.7, 1e-9)
        assertThat(speedForShift(dopplerShiftHz(-0.6))).isCloseTo(-0.6, 1e-9)
    }

    @Test
    fun `the doppler tone loops without a click`() {
        // Two blocks played back to back are exactly one tone twice as long
        val once = dopplerTone()
        val twiceAsLong = loopingTone(cycles = 2 * 1_621, sampleCount = 2 * DOPPLER_BLOCK_SIZE)

        assertThat(once.size).isEqualTo(DOPPLER_BLOCK_SIZE)
        once.indices.forEach { i ->
            assertThat(twiceAsLong[i]).isCloseTo(once[i], 1e-4f)
            assertThat(twiceAsLong[DOPPLER_BLOCK_SIZE + i]).isCloseTo(once[i], 1e-4f)
        }
    }

    @Test
    fun `a still room shows no motion`() {
        val heard = motion(dopplerTone())

        assertThat(heard.towardDb).isLessThan(-80.0)
        assertThat(heard.awayDb).isLessThan(-80.0)
    }

    @Test
    fun `a hand coming closer raises the echo's pitch`() {
        // An echo 100× quieter than the tone, 117 Hz higher: about 1.06 m/s toward
        val heard = motion(block(echoShiftHz = 117.1875, echoLoudness = 0.01))

        assertThat(heard.towardDb).isCloseTo(-40.0, 0.5)
        assertThat(heard.awayDb).isLessThan(-80.0)
        assertThat(heard.speedMetersPerSecond).isCloseTo(speedForShift(117.1875), 0.01)
    }

    @Test
    fun `a hand moving away lowers the echo's pitch`() {
        val heard = motion(block(echoShiftHz = -117.1875, echoLoudness = 0.01))

        assertThat(heard.awayDb).isGreaterThan(heard.towardDb)
        assertThat(heard.speedMetersPerSecond).isCloseTo(speedForShift(-117.1875), 0.01)
    }

    @Test
    fun `a speed between tone slots is still measured closely`() {
        val heard = motion(block(echoShiftHz = dopplerShiftHz(0.9), echoLoudness = 0.01))

        assertThat(heard.speedMetersPerSecond).isCloseTo(0.9, 0.1)
    }

    /** What the mic hears: the tone straight from the speaker plus one echo shifted by [echoShiftHz]. */
    private fun block(echoShiftHz: Double, echoLoudness: Double): FloatArray {
        val echoHz = DOPPLER_CARRIER_HZ + echoShiftHz
        val tone = dopplerTone()
        return FloatArray(DOPPLER_BLOCK_SIZE) { n ->
            tone[n] + (echoLoudness * sin(2 * PI * echoHz * n / SAMPLE_RATE)).toFloat()
        }
    }
}
