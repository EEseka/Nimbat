package com.eeseka.nimbat.core.presentation.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFAudio.AVAudioApplication
import platform.AVFAudio.AVAudioApplicationRecordPermissionDenied
import platform.AVFAudio.AVAudioApplicationRecordPermissionGranted
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import kotlin.coroutines.resume

actual class PermissionController {

    actual suspend fun requestPermission(permission: Permission): PermissionState {
        val current = getPermissionState(permission)
        if (current != PermissionState.NOT_DETERMINED) return current

        val isGranted = suspendCancellableCoroutine { continuation ->
            AVAudioApplication.requestRecordPermissionWithCompletionHandler { granted ->
                continuation.resume(granted)
            }
        }
        // iOS asks only once; after a "Don't Allow" it can only be changed in Settings
        return if (isGranted) PermissionState.GRANTED else PermissionState.PERMANENTLY_DENIED
    }

    actual suspend fun getPermissionState(permission: Permission): PermissionState =
        when (AVAudioApplication.sharedInstance.recordPermission) {
            AVAudioApplicationRecordPermissionGranted -> PermissionState.GRANTED
            AVAudioApplicationRecordPermissionDenied -> PermissionState.PERMANENTLY_DENIED
            else -> PermissionState.NOT_DETERMINED
        }

    actual fun openAppSettings() {
        val url = NSURL(string = UIApplicationOpenSettingsURLString)
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}

@Composable
actual fun rememberPermissionController(): PermissionController = remember { PermissionController() }
