package com.eeseka.nimbat.core.data.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import com.eeseka.nimbat.core.data.database.entities.GuardLogEntryEntity
import com.eeseka.nimbat.core.data.database.entities.GuardSessionEntity
import com.eeseka.nimbat.core.data.database.entities.GuardSessionWithEntries
import kotlinx.coroutines.flow.Flow

@Dao
interface GuardDao {

    @Insert
    suspend fun insertSession(session: GuardSessionEntity): Long

    @Insert
    suspend fun insertEntry(entry: GuardLogEntryEntity)

    @Query("UPDATE guardsessionentity SET endedAtMillis = :endedAtMillis WHERE id = :sessionId")
    suspend fun endSession(sessionId: Long, endedAtMillis: Long)

    @Transaction
    @Query("SELECT * FROM guardsessionentity ORDER BY startedAtMillis DESC")
    fun getSessionsWithEntries(): Flow<List<GuardSessionWithEntries>>

    @Query(
        """
        DELETE FROM guardsessionentity
        WHERE id NOT IN (
            SELECT id FROM guardsessionentity ORDER BY startedAtMillis DESC LIMIT :keep
        )
    """
    )
    suspend fun deleteOldSessions(keep: Int)

    @Query("DELETE FROM guardsessionentity")
    suspend fun deleteAllSessions()
}
