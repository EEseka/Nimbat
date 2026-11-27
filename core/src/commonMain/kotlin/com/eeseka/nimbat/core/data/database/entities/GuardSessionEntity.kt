package com.eeseka.nimbat.core.data.database.entities

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    indices = [
        Index("startedAtMillis")
    ]
)
data class GuardSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mode: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long? = null
)
