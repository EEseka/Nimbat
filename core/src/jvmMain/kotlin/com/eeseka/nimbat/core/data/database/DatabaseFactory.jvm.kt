package com.eeseka.nimbat.core.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.eeseka.nimbat.core.data.util.appDataDirectory
import java.io.File

actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<NimbatDatabase> {
        val directory = appDataDirectory

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val dbFile = File(directory, NimbatDatabase.DB_NAME)
        return Room.databaseBuilder<NimbatDatabase>(name = dbFile.absolutePath).setDriver(BundledSQLiteDriver())
    }
}
