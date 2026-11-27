package com.eeseka.nimbat.core.data.guard.mappers

import com.eeseka.nimbat.core.data.database.entities.GuardLogEntryEntity
import com.eeseka.nimbat.core.data.database.entities.GuardSessionWithEntries
import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntry
import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntryKind
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.domain.guard.model.GuardSession
import com.eeseka.nimbat.sound.GestureDirection

fun GuardSessionWithEntries.toGuardSession(): GuardSession = GuardSession(
    id = session.id,
    mode = GuardMode.valueOf(session.mode),
    startedAtMillis = session.startedAtMillis,
    endedAtMillis = session.endedAtMillis,
    entries = entries.sortedBy { it.atMillis }.map { it.toGuardLogEntry() }
)

fun GuardLogEntryEntity.toGuardLogEntry(): GuardLogEntry = GuardLogEntry(
    kind = GuardLogEntryKind.valueOf(kind),
    atMillis = atMillis,
    durationMillis = durationMillis,
    peakRiseDb = peakRiseDb,
    direction = direction?.let { GestureDirection.valueOf(it) }
)

fun GuardLogEntry.toGuardLogEntryEntity(sessionId: Long): GuardLogEntryEntity = GuardLogEntryEntity(
    sessionId = sessionId,
    kind = kind.name,
    atMillis = atMillis,
    durationMillis = durationMillis,
    peakRiseDb = peakRiseDb,
    direction = direction?.name
)
