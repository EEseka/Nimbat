package com.eeseka.nimbat.core.domain.guard

import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntry
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.domain.guard.model.GuardSession
import kotlinx.coroutines.flow.Flow

interface GuardStorage {
    companion object {
        const val SESSIONS_KEPT = 50
    }

    /** Newest first, each with its entries oldest first. */
    val sessions: Flow<List<GuardSession>>

    suspend fun startSession(mode: GuardMode, startedAtMillis: Long): Long
    suspend fun endSession(sessionId: Long, endedAtMillis: Long)
    suspend fun addEntry(sessionId: Long, entry: GuardLogEntry)
    suspend fun clear()
}
