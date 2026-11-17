package com.eeseka.nimbat.core.design_system.components.modals_and_overlays

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.mohamedrejeb.calf.ui.dialog.AdaptiveAlertDialog
import com.mohamedrejeb.calf.ui.dialog.uikit.AlertDialogIosActionStyle

@Composable
fun NimbatDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
    isDestructive: Boolean = false,
) {
    AdaptiveAlertDialog(
        onConfirm = {
            onConfirm()
            onDismissRequest()
        },
        onDismiss = {
            onDismiss?.invoke()
            onDismissRequest()
        },
        confirmText = confirmText,
        dismissText = dismissText,
        title = title,
        text = message,
        modifier = modifier,
        iosConfirmButtonStyle = if (isDestructive) AlertDialogIosActionStyle.Destructive
        else AlertDialogIosActionStyle.Default,
        iosDismissButtonStyle = AlertDialogIosActionStyle.Cancel,
        materialConfirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismissRequest()
                }
            ) {
                NimbatText(
                    text = confirmText,
                    color = if (isDestructive) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        },
        materialDismissButton = {
            TextButton(
                onClick = {
                    onDismiss?.invoke()
                    onDismissRequest()
                }
            ) {
                NimbatText(
                    text = dismissText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@PreviewLightDark
@Composable
private fun NimbatDialogPreview() {
    NimbatTheme {
        NimbatDialog(
            onDismissRequest = {},
            title = "Allow the microphone",
            message = "Nimbat listens for sound too high to hear. Nothing is recorded or stored.",
            confirmText = "Allow",
            dismissText = "Not now",
            onConfirm = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun NimbatDestructiveDialogPreview() {
    NimbatTheme {
        NimbatDialog(
            onDismissRequest = { },
            title = "Delete this measurement?",
            message = "It will be removed from your saved list. This can't be undone.",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {},
            onDismiss = {}
        )
    }
}
