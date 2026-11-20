package com.eeseka.nimbat.core.presentation.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred

actual class PermissionController internal constructor(private val context: Context) {
    internal var launcher: ActivityResultLauncher<String>? = null
    private var pendingResult: CompletableDeferred<Boolean>? = null

    actual val canOpenAppSettings = true

    actual suspend fun requestPermission(permission: Permission): PermissionState {
        if (getPermissionState(permission) == PermissionState.GRANTED) return PermissionState.GRANTED
        val launcher = launcher ?: return PermissionState.DENIED

        val result = CompletableDeferred<Boolean>().also { pendingResult = it }
        launcher.launch(permission.manifestName())
        val isGranted = result.await()

        return when {
            isGranted -> PermissionState.GRANTED
            // Android shows a rationale only while it would still show the dialog again; after that it's final
            context.findActivity()?.shouldShowRequestPermissionRationale(permission.manifestName()) == true -> PermissionState.DENIED
            else -> PermissionState.PERMANENTLY_DENIED
        }
    }

    actual suspend fun getPermissionState(permission: Permission): PermissionState {
        val isGranted = ContextCompat.checkSelfPermission(context, permission.manifestName()) == PackageManager.PERMISSION_GRANTED
        // Without asking, Android can't tell "never asked" from "denied", so both count as not determined
        return if (isGranted) PermissionState.GRANTED else PermissionState.NOT_DETERMINED
    }

    actual fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    internal fun onResult(isGranted: Boolean) {
        pendingResult?.complete(isGranted)
        pendingResult = null
    }
}

@Composable
actual fun rememberPermissionController(): PermissionController {
    val context = LocalContext.current
    val controller = remember(context) { PermissionController(context) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        controller.onResult(isGranted)
    }
    controller.launcher = launcher
    return controller
}

private fun Permission.manifestName(): String = when (this) {
    Permission.MICROPHONE -> Manifest.permission.RECORD_AUDIO
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
