package com.eeseka.nimbat.core.data.database.entities

import androidx.room3.Embedded
import androidx.room3.Relation

data class GuardSessionWithEntries(
    @Embedded
    val session: GuardSessionEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["sessionId"]
    )
    val entries: List<GuardLogEntryEntity>
)
