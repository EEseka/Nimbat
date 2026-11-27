package com.eeseka.nimbat.core.data.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import org.w3c.dom.Worker

actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<NimbatDatabase> {
        return Room.databaseBuilder<NimbatDatabase>(name = NimbatDatabase.DB_NAME)
            .setDriver(WebWorkerSQLiteDriver(sqliteWorker()))
    }
}

// Inside a js() literal so webpack sees new Worker(new URL(...)) and bundles core/sqlite-worker as its own file
@OptIn(ExperimentalWasmJsInterop::class)
private fun sqliteWorker(): Worker =
    js("""new Worker(new URL("nimbat-sqlite-worker/worker.js", import.meta.url), { type: "module" })""")
