package com.eeseka.nimbat.guard.presentation.model

import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntryKind
import com.eeseka.nimbat.sound.GestureDirection

data class GuardLogEntryUi(
    val kind: GuardLogEntryKind,
    val atMillis: Long,
    val durationMillis: Long?,
    val strength: Float?,
    val direction: GestureDirection?
)
