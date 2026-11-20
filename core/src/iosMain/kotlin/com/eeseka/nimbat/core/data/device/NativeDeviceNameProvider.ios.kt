package com.eeseka.nimbat.core.data.device

import com.eeseka.nimbat.core.domain.device.DeviceNameProvider
import platform.UIKit.UIDevice

actual class NativeDeviceNameProvider : DeviceNameProvider {
    actual override suspend fun deviceName(): String = UIDevice.currentDevice.name
}
