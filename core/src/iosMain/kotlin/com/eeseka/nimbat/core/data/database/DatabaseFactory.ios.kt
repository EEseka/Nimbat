@file:OptIn(ExperimentalForeignApi::class)

package com.eeseka.nimbat.core.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<NimbatDatabase> {
        val dbFile = documentDirectory() + "/${NimbatDatabase.DB_NAME}"

        return Room.databaseBuilder<NimbatDatabase>(name = dbFile).setDriver(BundledSQLiteDriver())
    }

    private fun documentDirectory(): String {
        val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )

        return requireNotNull(documentDirectory?.path)
    }
}
