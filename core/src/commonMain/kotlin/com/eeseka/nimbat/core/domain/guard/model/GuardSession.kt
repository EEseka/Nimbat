package com.eeseka.nimbat.core.domain.guard.model

data class GuardSession(
    val id: Long,
    val mode: GuardMode,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val entries: List<GuardLogEntry>
)
