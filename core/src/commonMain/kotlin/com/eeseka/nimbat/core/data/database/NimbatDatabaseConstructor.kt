package com.eeseka.nimbat.core.data.database

import androidx.room3.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object NimbatDatabaseConstructor : RoomDatabaseConstructor<NimbatDatabase> {
    override fun initialize(): NimbatDatabase
}
