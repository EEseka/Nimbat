package com.eeseka.nimbat.guard.presentation.mappers

import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntry
import com.eeseka.nimbat.core.domain.guard.model.GuardSession
import com.eeseka.nimbat.guard.presentation.model.GuardLogEntryUi
import com.eeseka.nimbat.guard.presentation.model.GuardSessionUi
import kotlinx.collections.immutable.toImmutableList

/** An echo this far above the still room gets the tallest tick on the timeline: someone walking right past. */
private const val FULL_STRENGTH_DB = 30.0

fun GuardSession.toGuardSessionUi(): GuardSessionUi = GuardSessionUi(
    id = id,
    mode = mode,
    startedAtMillis = startedAtMillis,
    endedAtMillis = endedAtMillis,
    entries = entries.map { it.toGuardLogEntryUi() }.toImmutableList()
)

fun GuardLogEntry.toGuardLogEntryUi(): GuardLogEntryUi = GuardLogEntryUi(
    kind = kind,
    atMillis = atMillis,
    durationMillis = durationMillis,
    strength = peakRiseDb?.let { (it / FULL_STRENGTH_DB).toFloat().coerceIn(0f, 1f) },
    direction = direction
)
