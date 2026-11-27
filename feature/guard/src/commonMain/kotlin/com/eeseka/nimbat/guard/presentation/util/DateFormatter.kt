package com.eeseka.nimbat.guard.presentation.util

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import nimbat.feature.guard.generated.resources.Res
import nimbat.feature.guard.generated.resources.am
import nimbat.feature.guard.generated.resources.minutes_short
import nimbat.feature.guard.generated.resources.pm
import nimbat.feature.guard.generated.resources.seconds_short
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Instant

private const val MILLIS_PER_SECOND = 1_000.0
private const val SECONDS_PER_MINUTE = 60
private const val NOON_HOUR = 12

// 8:02 PM, marker added after formatting: on the web it's still loading (empty) on the first frame
@Composable
fun Long.toTimeLabel(): String {
    val time = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).time
    val marker = stringResource(if (time.hour < NOON_HOUR) Res.string.am else Res.string.pm)
    val clock = time.format(
        LocalTime.Format {
            amPmHour(Padding.NONE)
            char(':')
            minute()
        }
    )
    return "$clock $marker"
}

// "8:02 PM" when it's today, "Sat 5 Sep · 8:02 PM" any other day
@Composable
fun Long.toShortDateTimeLabel(): String {
    val timeZone = TimeZone.currentSystemDefault()
    val date = Instant.fromEpochMilliseconds(this).toLocalDateTime(timeZone).date
    val isToday = date == Clock.System.now().toLocalDateTime(timeZone).date
    return if (isToday) toTimeLabel() else "${date.toDateLabel()} · ${toTimeLabel()}"
}

// "2.4 s" under a minute, "6 min" after
@Composable
fun Long.toDurationLabel(): String {
    val seconds = this / MILLIS_PER_SECOND
    if (seconds < SECONDS_PER_MINUTE) {
        val tenths = (seconds * 10).roundToInt()
        return stringResource(Res.string.seconds_short, "${tenths / 10}.${tenths % 10}")
    }
    return stringResource(Res.string.minutes_short, (seconds / SECONDS_PER_MINUTE).roundToInt())
}

// Sat 5 Sep, or Sat 5 Sep 2025 if it's not the current year
private fun LocalDate.toDateLabel(): String {
    val currentYear = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    val label = format(
        LocalDate.Format {
            dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
            char(' ')
            day(Padding.NONE)
            char(' ')
            monthName(MonthNames.ENGLISH_ABBREVIATED)
        }
    )
    return if (year == currentYear) label else "$label $year"
}
