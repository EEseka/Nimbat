package com.eeseka.nimbat.settings.presentation.util

import com.eeseka.nimbat.core.domain.settings.MeasurementUnits
import kotlin.math.abs
import kotlin.math.roundToInt

private const val CENTIMETERS_PER_INCH = 2.54
private const val FEET_PER_METER = 3.28084

fun formatTemperature(celsius: Double, units: MeasurementUnits): String = when (units) {
    MeasurementUnits.METRIC -> "${celsius.roundToInt()} °C"
    MeasurementUnits.IMPERIAL -> "${(celsius * 9 / 5 + 32).roundToInt()} °F"
}

fun speedInUnits(metersPerSecond: Double, units: MeasurementUnits): Int = when (units) {
    MeasurementUnits.METRIC -> metersPerSecond.roundToInt()
    MeasurementUnits.IMPERIAL -> (metersPerSecond * FEET_PER_METER).roundToInt()
}

fun formatDistanceOffset(meters: Double, units: MeasurementUnits): String {
    val centimeters = meters * 100
    val sign = if (centimeters > 0) "+" else if (centimeters < 0) "−" else ""
    return when (units) {
        MeasurementUnits.METRIC -> "$sign${abs(centimeters).roundToInt()} cm"
        MeasurementUnits.IMPERIAL -> {
            val tenthsOfInch = (abs(centimeters) / CENTIMETERS_PER_INCH * 10).roundToInt()
            "$sign${tenthsOfInch / 10}.${tenthsOfInch % 10} in"
        }
    }
}

fun formatPercent(fraction: Float): String = "${(fraction * 100).roundToInt()}%"
