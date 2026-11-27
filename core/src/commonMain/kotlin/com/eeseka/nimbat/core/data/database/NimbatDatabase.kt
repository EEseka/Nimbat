package com.eeseka.nimbat.core.data.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.eeseka.nimbat.core.data.database.dao.GuardDao
import com.eeseka.nimbat.core.data.database.entities.GuardLogEntryEntity
import com.eeseka.nimbat.core.data.database.entities.GuardSessionEntity

@Database(
    entities = [
        GuardSessionEntity::class,
        GuardLogEntryEntity::class,
    ],
    version = 1,
)
@ConstructedBy(NimbatDatabaseConstructor::class)
abstract class NimbatDatabase : RoomDatabase() {
    companion object {
        const val DB_NAME = "nimbat.db"
    }

    abstract val guardDao: GuardDao
}
