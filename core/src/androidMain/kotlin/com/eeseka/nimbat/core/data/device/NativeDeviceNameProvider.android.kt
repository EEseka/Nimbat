package com.eeseka.nimbat.core.data.device

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.eeseka.nimbat.core.domain.device.DeviceNameProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class NativeDeviceNameProvider(private val context: Context) : DeviceNameProvider {

    actual override suspend fun deviceName(): String = withContext(Dispatchers.IO) {
        val ownerName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        } else {
            null
        }
        ownerName?.takeIf { it.isNotBlank() } ?: modelName()
    }

    private fun modelName(): String {
        val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        return if (Build.MODEL.startsWith(manufacturer, ignoreCase = true)) {
            Build.MODEL
        } else {
            "$manufacturer ${Build.MODEL}"
        }
    }
}
