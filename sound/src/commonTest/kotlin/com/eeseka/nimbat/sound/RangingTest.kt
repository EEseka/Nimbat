package com.eeseka.nimbat.sound

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test

class RangingTest {

    // A made-up exchange, in samples on one shared timeline. Each recording starts at its own moment.
    private val trip = 280 // between the devices: 280 samples ≈ 2 m at 20 °C
    private val starterSelf = 5 // the starter's own speaker to its own mic
    private val replierSelf = 7
    private val starterPlays = 10_000
    private val replierPlays = 30_000

    @Test
    fun `a pair is two chirps PAIR_SPACING apart`() {
        val samples = pairSamples(Role.Starter)
        val wave = UP_CHIRP.wave

        assertThat(samples.size).isEqualTo(PAIR_SPACING + wave.size)
        assertThat(samples[PAIR_SPACING + 1_200]).isEqualTo(wave[1_200])
    }

    @Test
    fun `hears both pairs at their middles`() {
        val hearing = hear(recording(startsAt = 0, ownSelf = starterSelf, isStarter = true))

        assertThat(hearing).isNotNull().transform { it.starterAt to it.replierAt }
            .isEqualTo((starterPlays + starterSelf + PAIR_SPACING / 2.0) to (replierPlays + trip + PAIR_SPACING / 2.0))
    }

    @Test
    fun `beepbeep gives the distance between the devices`() {
        val starter = checkNotNull(hear(recording(startsAt = 0, ownSelf = starterSelf, isStarter = true)))
        val replier = checkNotNull(hear(recording(startsAt = 3_000, ownSelf = replierSelf, isStarter = false)))
        val zero = samplesToMeters((starterSelf + replierSelf) / 2.0)

        assertThat(beepBeepMeters(starter, replier, zeroMeters = zero)).isCloseTo(samplesToMeters(trip.toDouble()), 1e-9)
    }

    @Test
    fun `when each device started recording makes no difference`() {
        val starter = checkNotNull(hear(recording(startsAt = 0, ownSelf = starterSelf, isStarter = true)))
        val early = checkNotNull(hear(recording(startsAt = 1_000, ownSelf = replierSelf, isStarter = false)))
        val late = checkNotNull(hear(recording(startsAt = 8_000, ownSelf = replierSelf, isStarter = false)))

        assertThat(beepBeepMeters(starter, early)).isCloseTo(beepBeepMeters(starter, late), 1e-9)
    }

    @Test
    fun `without zeroing the reading is short by the average speaker to mic distance`() {
        val starter = checkNotNull(hear(recording(startsAt = 0, ownSelf = starterSelf, isStarter = true)))
        val replier = checkNotNull(hear(recording(startsAt = 3_000, ownSelf = replierSelf, isStarter = false)))

        assertThat(beepBeepMeters(starter, replier))
            .isCloseTo(samplesToMeters(trip - (starterSelf + replierSelf) / 2.0), 1e-9)
    }

    @Test
    fun `a stuttering recording is rejected`() {
        val stuttered = recording(startsAt = 0, ownSelf = starterSelf, isStarter = true, starterSpacing = PAIR_SPACING + 50)

        assertThat(hear(stuttered)).isNull()
    }

    @Test
    fun `a recording missing the reply is rejected`() {
        val samples = silence(1.0).withPair(UP_CHIRP, at = starterPlays + starterSelf)

        assertThat(hear(samples)).isNull()
    }

    @Test
    fun `a faint starter drowned by the replier's own chirp is rejected`() {
        // The up template's best match is the replier's loud down chirp, which leaves no room for a reply after it
        val samples = silence(1.0)
            .withPair(UP_CHIRP, at = starterPlays + trip, loudness = 0.0001f)
            .withPair(DOWN_CHIRP, at = replierPlays + replierSelf)

        assertThat(hear(samples)).isNull()
    }

    @Test
    fun `median takes the middle reading and ignores one wild ping`() {
        assertThat(median(listOf(2.01, 1.99, 9.5, 2.0, 2.02))).isEqualTo(2.01)
        assertThat(median(listOf(1.0, 3.0))).isEqualTo(2.0)
    }

    /**
     * What one device records: the starter's pair and the replier's pair, each arriving after its trip.
     * [startsAt] is when this device started recording, on the shared timeline.
     */
    private fun recording(
        startsAt: Int,
        ownSelf: Int,
        isStarter: Boolean,
        starterSpacing: Int = PAIR_SPACING,
    ): FloatArray {
        val starterArrives = starterPlays + if (isStarter) ownSelf else trip
        val replierArrives = replierPlays + if (isStarter) trip else ownSelf
        return silence(1.0)
            .withPair(UP_CHIRP, at = starterArrives - startsAt, spacing = starterSpacing)
            .withPair(DOWN_CHIRP, at = replierArrives - startsAt)
    }

    private fun silence(seconds: Double) = FloatArray(sampleCountFor(seconds))

    private fun FloatArray.withPair(
        template: ChirpTemplate,
        at: Int,
        spacing: Int = PAIR_SPACING,
        loudness: Float = 1f,
    ): FloatArray {
        for (start in listOf(at, at + spacing)) {
            template.wave.forEachIndexed { j, value -> this[start + j] += value * loudness }
        }
        return this
    }
}
