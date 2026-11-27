package com.eeseka.nimbat.core.domain.guard.model

import com.eeseka.nimbat.sound.GestureDirection

/**
 * Something Guard noticed at [atMillis]. [durationMillis] is how long it lasted (for a walkaway, how long you were
 * gone); [peakRiseDb] how strong a movement's echo got; [direction] whether it came closer or moved away.
 */
data class GuardLogEntry(
    val kind: GuardLogEntryKind,
    val atMillis: Long,
    val durationMillis: Long?,
    val peakRiseDb: Double?,
    val direction: GestureDirection?
)

enum class GuardLogEntryKind {
    MOVEMENT,
    ALARM,
    TONE_LOST,
    WALKED_AWAY
}
