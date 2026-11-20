package com.eeseka.nimbat.core.data.device

import com.eeseka.nimbat.core.domain.device.DeviceNameProvider

expect class NativeDeviceNameProvider : DeviceNameProvider {
    override suspend fun deviceName(): String
}
