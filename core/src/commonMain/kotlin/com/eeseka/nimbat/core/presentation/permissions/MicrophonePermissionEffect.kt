package com.eeseka.nimbat.core.presentation.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Asks for the microphone permission once [isEnabled] turns true and reports where it landed,
 * so the caller can start listening, show a settings dialog, or explain why it's needed.
 */
@Composable
fun MicrophonePermissionEffect(
    isEnabled: Boolean,
    onPermissionResolved: (PermissionState) -> Unit
) {
    val permissionController = rememberPermissionController()

    LaunchedEffect(isEnabled) {
        if (!isEnabled) return@LaunchedEffect

        var permissionState = permissionController.getPermissionState(Permission.MICROPHONE)
        if (permissionState == PermissionState.NOT_DETERMINED || permissionState == PermissionState.DENIED) {
            permissionState = permissionController.requestPermission(Permission.MICROPHONE)
        }
        onPermissionResolved(permissionState)
    }
}
