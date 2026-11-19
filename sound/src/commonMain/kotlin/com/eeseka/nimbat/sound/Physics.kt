package com.eeseka.nimbat.sound

import kotlin.math.log10
import kotlin.math.sqrt

/** Speed of sound in dry air at 0 °C, in metres per second. */
const val SPEED_OF_SOUND_AT_0_C = 331.3

/** Room temperature, used until the user sets their own. */
const val DEFAULT_TEMPERATURE_C = 20.0

/** 0 °C on the absolute (kelvin) scale. */
private const val KELVIN_AT_0_C = 273.15

/**
 * How fast sound travels through air at [temperatureC], in metres per second: about 343 at 20 °C.
 * Warmer air molecules move faster and pass the sound on sooner. The speed grows with the square root
 * of the absolute temperature, so it's 331.3 × √(kelvin ÷ 273.15).
 */
fun speedOfSound(temperatureC: Double = DEFAULT_TEMPERATURE_C): Double =
    SPEED_OF_SOUND_AT_0_C * sqrt((temperatureC + KELVIN_AT_0_C) / KELVIN_AT_0_C)

/** The air temperature, in °C, at which sound travels at [metersPerSecond]: [speedOfSound] run backwards. */
fun temperatureFor(metersPerSecond: Double): Double {
    val ratio = metersPerSecond / SPEED_OF_SOUND_AT_0_C
    return KELVIN_AT_0_C * ratio * ratio - KELVIN_AT_0_C
}

/** How long [samples] of sound last, e.g. 48,000 → 1 s. A fraction of a sample is allowed. */
fun samplesToSeconds(samples: Double): Double = samples / SAMPLE_RATE

/** How far sound travels during [samples] at [temperatureC]. One sample is about 7 mm at 20 °C. */
fun samplesToMeters(samples: Double, temperatureC: Double = DEFAULT_TEMPERATURE_C): Double =
    samplesToSeconds(samples) * speedOfSound(temperatureC)

/**
 * A ratio of two amplitudes (e.g. two scores or two sample sizes) in decibels: 20 × log10(ratio).
 * 10× bigger is +20 dB, the same is 0 dB, half is about −6 dB.
 */
fun decibels(amplitudeRatio: Double): Double = 20 * log10(amplitudeRatio)
