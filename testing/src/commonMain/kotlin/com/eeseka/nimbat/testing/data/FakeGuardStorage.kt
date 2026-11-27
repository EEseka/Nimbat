package com.eeseka.nimbat.testing.data

import com.eeseka.nimbat.core.domain.guard.GuardStorage
import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntry
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.domain.guard.model.GuardSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeGuardStorage : GuardStorage {
    private val sessionsFlow = MutableStateFlow<List<GuardSession>>(emptyList())
    override val sessions: Flow<List<GuardSession>> = sessionsFlow

    override suspend fun startSession(mode: GuardMode, startedAtMillis: Long): Long {
        val sessionId = (sessionsFlow.value.maxOfOrNull { it.id } ?: 0L) + 1
        val session = GuardSession(
            id = sessionId,
            mode = mode,
            startedAtMillis = startedAtMillis,
            endedAtMillis = null,
            entries = emptyList()
        )
        sessionsFlow.update { listOf(session) + it }
        return sessionId
    }

    override suspend fun endSession(sessionId: Long, endedAtMillis: Long) {
        updateSession(sessionId) { it.copy(endedAtMillis = endedAtMillis) }
    }

    override suspend fun addEntry(sessionId: Long, entry: GuardLogEntry) {
        updateSession(sessionId) { it.copy(entries = it.entries + entry) }
    }

    override suspend fun clear() {
        sessionsFlow.value = emptyList()
    }

    private fun updateSession(sessionId: Long, change: (GuardSession) -> GuardSession) {
        sessionsFlow.update { sessions -> sessions.map { if (it.id == sessionId) change(it) else it } }
    }
}
