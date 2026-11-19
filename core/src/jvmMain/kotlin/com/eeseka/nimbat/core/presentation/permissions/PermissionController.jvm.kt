package com.eeseka.nimbat.core.presentation.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class PermissionController {

    actual suspend fun requestPermission(permission: Permission): PermissionState = PermissionState.GRANTED

    actual suspend fun getPermissionState(permission: Permission): PermissionState = PermissionState.GRANTED

    /** Desktop has no per-app settings page to open; macOS keeps it under Privacy & Security, Microphone. */
    actual fun openAppSettings() = Unit
}

@Composable
actual fun rememberPermissionController(): PermissionController = remember { PermissionController() }
