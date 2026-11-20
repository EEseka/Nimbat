package com.eeseka.nimbat.testing.data

import com.eeseka.nimbat.core.domain.device.DeviceNameProvider

class FakeDeviceNameProvider : DeviceNameProvider {
    var name = "iPhone 13 Pro"

    override suspend fun deviceName(): String = name
}
