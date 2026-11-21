package com.eeseka.nimbat.core.presentation.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/**
 * Reads the microphone permission, without asking, every time the screen comes back,
 * so a microphone turned on in Settings is noticed.
 */
@Composable
fun MicrophonePermissionResumeEffect(
    onPermissionChecked: (PermissionState) -> Unit
) {
    val permissionController = rememberPermissionController()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()

    LaunchedEffect(lifecycleState) {
        if (lifecycleState != Lifecycle.State.RESUMED) return@LaunchedEffect

        onPermissionChecked(permissionController.getPermissionState(Permission.MICROPHONE))
    }
}
