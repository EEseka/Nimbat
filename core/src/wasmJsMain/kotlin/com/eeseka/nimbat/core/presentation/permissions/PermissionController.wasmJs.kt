@file:OptIn(ExperimentalWasmJsInterop::class)

package com.eeseka.nimbat.core.presentation.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.await
import kotlin.js.JsException
import kotlin.js.Promise

actual class PermissionController {

    actual suspend fun requestPermission(permission: Permission): PermissionState {
        if (getPermissionState(permission) == PermissionState.GRANTED) return PermissionState.GRANTED
        return try {
            askForMicrophone().await()
            PermissionState.GRANTED
        } catch (_: JsException) {
            // A blocked site stays blocked until it's changed in the browser's site settings
            if (getPermissionState(permission) == PermissionState.PERMANENTLY_DENIED) PermissionState.PERMANENTLY_DENIED
            else PermissionState.DENIED
        }
    }

    actual suspend fun getPermissionState(permission: Permission): PermissionState =
        try {
            when (queryMicrophonePermission().await().toString()) {
                "granted" -> PermissionState.GRANTED
                "denied" -> PermissionState.PERMANENTLY_DENIED
                else -> PermissionState.NOT_DETERMINED
            }
        } catch (_: JsException) {
            PermissionState.NOT_DETERMINED // some browsers can't be asked about the microphone without prompting
        }

    /** Web pages can't open the browser's settings; the person changes it from the address bar's site menu. */
    actual fun openAppSettings() = Unit
}

@Composable
actual fun rememberPermissionController(): PermissionController = remember { PermissionController() }

private fun queryMicrophonePermission(): Promise<JsString> =
    js("navigator.permissions.query({ name: 'microphone' }).then((status) => status.state)")

private fun askForMicrophone(): Promise<JsAny?> =
    js("navigator.mediaDevices.getUserMedia({ audio: true }).then((stream) => { stream.getTracks().forEach((track) => track.stop()); return null; })")
