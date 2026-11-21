package com.eeseka.nimbat.core.domain.device

interface DeviceNameProvider {
    suspend fun deviceName(): String
}
