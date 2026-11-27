package com.eeseka.nimbat.core.data.guard

import com.eeseka.nimbat.core.data.database.NimbatDatabase
import com.eeseka.nimbat.core.data.database.entities.GuardSessionEntity
import com.eeseka.nimbat.core.data.guard.mappers.toGuardLogEntryEntity
import com.eeseka.nimbat.core.data.guard.mappers.toGuardSession
import com.eeseka.nimbat.core.domain.guard.GuardStorage
import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntry
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.domain.guard.model.GuardSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomGuardStorage(
    private val database: NimbatDatabase
) : GuardStorage {

    override val sessions: Flow<List<GuardSession>> = database.guardDao.getSessionsWithEntries().map { sessions ->
        sessions.map { it.toGuardSession() }
    }

    override suspend fun startSession(mode: GuardMode, startedAtMillis: Long): Long {
        val sessionId = database.guardDao.insertSession(
            GuardSessionEntity(mode = mode.name, startedAtMillis = startedAtMillis)
        )
        database.guardDao.deleteOldSessions(keep = GuardStorage.SESSIONS_KEPT)
        return sessionId
    }

    override suspend fun endSession(sessionId: Long, endedAtMillis: Long) {
        database.guardDao.endSession(sessionId, endedAtMillis)
    }

    override suspend fun addEntry(sessionId: Long, entry: GuardLogEntry) {
        database.guardDao.insertEntry(entry.toGuardLogEntryEntity(sessionId))
    }

    override suspend fun clear() {
        database.guardDao.deleteAllSessions()
    }
}
