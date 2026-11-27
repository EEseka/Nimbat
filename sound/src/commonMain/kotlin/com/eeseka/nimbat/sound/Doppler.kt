package com.eeseka.nimbat.sound

import kotlin.math.roundToInt
import kotlin.math.sqrt

/** How many mic samples each Doppler reading looks at: 4,096 ≈ 85 ms. */
const val DOPPLER_BLOCK_SIZE = 4_096

/**
 * How far each reading moves on from the last: half a block, so readings overlap and arrive every ≈ 43 ms
 * (about 23 a second). Overlap makes readings more frequent; it can't make the FFT slots any narrower.
 */
const val DOPPLER_HOP = DOPPLER_BLOCK_SIZE / 2

/** The pass mark for [toneClarityDb]: half of the 40 dB a clean hearing scores, so quiet speakers still pass and hiss (under 10) can't. */
const val TONE_CLEAR_DB = 20.0

/** The FFT slot the Doppler tone sits in. */
private const val CARRIER_SLOT = 1_621

/**
 * The tone played for Doppler sensing: ≈ 18,996 Hz, inaudible. It's exactly tone slot 1,621, so it fits
 * 1,621 whole cycles into one block: it loops without a click, and the FFT sees it as one clean tone.
 */
const val DOPPLER_CARRIER_HZ = CARRIER_SLOT.toDouble() * SAMPLE_RATE / DOPPLER_BLOCK_SIZE

/** Nearest slot to the tone counted as motion: 4 slots ≈ 47 Hz ≈ 0.4 m/s. Closer slots hold the tone itself. */
private const val NEAREST_SLOT = 4

/** Farthest slot counted as motion: 30 slots ≈ 352 Hz ≈ 3.2 m/s, faster than a hand waves. */
private const val FARTHEST_SLOT = 30

private const val CARRIER_SLOT_COUNT = 3

/** Nearest slot counted as background hiss: 40 slots ≈ 470 Hz, past the fastest hand's echo. */
private const val BACKGROUND_NEAREST_SLOT = 40
private const val BACKGROUND_FARTHEST_SLOT = 120

/** The width of one FFT slot: 48,000 ÷ 4,096 ≈ 11.7 Hz. */
private const val SLOT_HZ = SAMPLE_RATE.toDouble() / DOPPLER_BLOCK_SIZE

/**
 * What one block heard moving. [towardDb] and [awayDb] are how loud the echoes from things moving
 * toward and away from the device are, compared with the tone itself (−40 dB = 100× quieter).
 * [speedMetersPerSecond] is their average speed: positive toward, negative away.
 * [toneClarityDb] is how far the tone itself stands above the background hiss: when the tone isn't heard, the echo levels mean nothing.
 */
data class Motion(
    val towardDb: Double,
    val awayDb: Double,
    val speedMetersPerSecond: Double,
    val toneClarityDb: Double,
)

/** What the device plays on repeat while sensing: one block of the Doppler tone. */
fun dopplerTone(): FloatArray = loopingTone(cycles = CARRIER_SLOT, sampleCount = DOPPLER_BLOCK_SIZE)

/**
 * How much the pitch of an echo shifts when it bounces off something moving at [metersPerSecond]
 * (positive toward). Doubled, because the sound meets the moving thing on the way there and leaves it on
 * the way back: 2 × speed × frequency ÷ speed of sound. A hand at 1 m/s shifts 19 kHz by about 110 Hz.
 */
fun dopplerShiftHz(
    metersPerSecond: Double,
    carrierHz: Double = DOPPLER_CARRIER_HZ,
    temperatureC: Double = DEFAULT_TEMPERATURE_C,
): Double = 2 * metersPerSecond * carrierHz / speedOfSound(temperatureC)

/** The speed of whatever made an echo shift by [shiftHz]: [dopplerShiftHz] run backwards. */
fun speedForShift(
    shiftHz: Double,
    carrierHz: Double = DOPPLER_CARRIER_HZ,
    temperatureC: Double = DEFAULT_TEMPERATURE_C,
): Double = shiftHz * speedOfSound(temperatureC) / (2 * carrierHz)

/** What moved while [dopplerTone] played: echoes just above the tone come closer, just below it move away. */
fun motion(block: FloatArray, temperatureC: Double = DEFAULT_TEMPERATURE_C): Motion {
    val power = slotPowers(block)
    val carrierPower = carrierPower(power)

    var towardPower = 0.0
    var awayPower = 0.0
    var weightedShift = 0.0
    for (offset in NEAREST_SLOT..FARTHEST_SLOT) {
        val toward = power[CARRIER_SLOT + offset]
        val away = power[CARRIER_SLOT - offset]
        towardPower += toward
        awayPower += away
        weightedShift += (toward - away) * offset * SLOT_HZ
    }
    val movingPower = towardPower + awayPower
    val averageShiftHz = if (movingPower > 0) weightedShift / movingPower else 0.0

    return Motion(
        towardDb = relativeDb(towardPower, carrierPower),
        awayDb = relativeDb(awayPower, carrierPower),
        speedMetersPerSecond = speedForShift(averageShiftHz, temperatureC = temperatureC),
        toneClarityDb = clarityDb(power),
    )
}

/** How far [dopplerTone] stands above the background hiss around it, in dB. */
fun toneClarityDb(block: FloatArray): Double = clarityDb(slotPowers(block))

/** How many readings, one every [DOPPLER_HOP] samples, fit in [seconds]. */
internal fun readingsFor(seconds: Double): Int = (seconds * SAMPLE_RATE / DOPPLER_HOP).roundToInt()

/** How long [readings] readings last. */
internal fun secondsFor(readings: Int): Double = readings.toDouble() * DOPPLER_HOP / SAMPLE_RATE

internal fun Int.saturatingIncrement(): Int = if (this == Int.MAX_VALUE) this else this + 1

private fun clarityDb(power: DoubleArray): Double {
    val background = (BACKGROUND_NEAREST_SLOT..BACKGROUND_FARTHEST_SLOT)
        .sumOf { power[CARRIER_SLOT + it] + power[CARRIER_SLOT - it] } /
        (2 * (BACKGROUND_FARTHEST_SLOT - BACKGROUND_NEAREST_SLOT + 1))
    return relativeDb(carrierPower(power) / CARRIER_SLOT_COUNT, background)
}

/** The power in each FFT slot of a Hann-faded [block], so a loud tone doesn't spill into its neighbours. */
private fun slotPowers(block: FloatArray): DoubleArray {
    require(block.size == DOPPLER_BLOCK_SIZE) { "block must be $DOPPLER_BLOCK_SIZE samples, was ${block.size}" }

    val windowed = block.copyOf().also { fadeEdges(samples = it, fadeSamples = it.size / 2) }
    val re = DoubleArray(windowed.size) { windowed[it].toDouble() }
    val im = DoubleArray(windowed.size)
    fft(re, im)
    return DoubleArray(windowed.size) { re[it] * re[it] + im[it] * im[it] }
}

/** The Hann window spreads a clean tone over its own slot and one each side. */
private fun carrierPower(power: DoubleArray): Double =
    (CARRIER_SLOT - 1..CARRIER_SLOT + 1).sumOf { power[it] }

/** Compares two powers in [decibels]. Power is amplitude squared, so the amplitude ratio is its square root. */
private fun relativeDb(power: Double, reference: Double): Double =
    if (reference > 0) decibels(sqrt(power / reference)) else Double.NEGATIVE_INFINITY
