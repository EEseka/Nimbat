package com.eeseka.nimbat.core.data.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

actual class DatabaseFactory(
    private val context: Context
) {
    actual fun create(): RoomDatabase.Builder<NimbatDatabase> {
        val dbFile = context.applicationContext.getDatabasePath(NimbatDatabase.DB_NAME)

        return Room.databaseBuilder<NimbatDatabase>(
            context = context.applicationContext,
            name = dbFile.absolutePath
        ).setDriver(BundledSQLiteDriver())
    }
}
