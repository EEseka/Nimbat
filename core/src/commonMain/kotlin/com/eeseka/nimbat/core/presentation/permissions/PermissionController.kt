package com.eeseka.nimbat.core.presentation.permissions

import androidx.compose.runtime.Composable

expect class PermissionController {
    suspend fun requestPermission(permission: Permission): PermissionState
    suspend fun getPermissionState(permission: Permission): PermissionState
    fun openAppSettings()
}

@Composable
expect fun rememberPermissionController(): PermissionController
