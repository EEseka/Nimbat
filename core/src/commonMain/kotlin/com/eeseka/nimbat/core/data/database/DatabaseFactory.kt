package com.eeseka.nimbat.core.data.database

import androidx.room3.RoomDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<NimbatDatabase>
}
