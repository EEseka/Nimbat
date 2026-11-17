package com.eeseka.nimbat.core.design_system.components.modals_and_overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.Trash2
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.domain.util.PlatformUtils.isIOS
import com.mohamedrejeb.calf.ui.ExperimentalCalfUiApi
import com.mohamedrejeb.calf.ui.dialog.AdaptiveBasicAlertDialog
import com.mohamedrejeb.calf.ui.dialog.uikit.AlertDialogIosAction
import com.mohamedrejeb.calf.ui.dialog.uikit.AlertDialogIosActionStyle
import com.mohamedrejeb.calf.ui.dialog.uikit.AlertDialogIosStyle
import com.mohamedrejeb.calf.ui.dialog.uikit.rememberAlertDialogIosProperties
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import nimbat.core.generated.resources.Res
import nimbat.core.generated.resources.cancel
import org.jetbrains.compose.resources.stringResource

data class NimbatActionSheetItem(
    val text: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val isDestructive: Boolean = false
)

@OptIn(ExperimentalCalfUiApi::class)
@Composable
fun NimbatActionSheet(
    onDismissRequest: () -> Unit,
    items: ImmutableList<NimbatActionSheetItem>,
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String? = null,
    cancelText: String = stringResource(Res.string.cancel)
) {
    if (isIOS()) {
        val iosProperties = rememberAlertDialogIosProperties(
            title = title ?: "",
            text = message ?: "",
            style = AlertDialogIosStyle.ActionSheet,
            actions = items.map { item ->
                AlertDialogIosAction(
                    title = item.text,
                    style = if (item.isDestructive) AlertDialogIosActionStyle.Destructive
                    else AlertDialogIosActionStyle.Default,
                    onClick = {
                        item.onClick()
                        onDismissRequest()
                    }
                )
            } + AlertDialogIosAction(
                title = cancelText,
                style = AlertDialogIosActionStyle.Cancel,
                onClick = onDismissRequest
            )
        )

        AdaptiveBasicAlertDialog(
            onDismissRequest = onDismissRequest,
            modifier = modifier,
            iosProperties = iosProperties,
            materialContent = {}
        )
    } else {
        NimbatAdaptiveSheet(
            onDismissRequest = onDismissRequest,
            modifier = modifier
        ) {
            NimbatActionSheetContent(
                items = items,
                title = title,
                message = message,
                onDismissRequest = onDismissRequest
            )
        }
    }
}

@Composable
private fun NimbatActionSheetContent(
    items: ImmutableList<NimbatActionSheetItem>,
    title: String?,
    message: String?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title != null) {
            NimbatText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, bottom = if (message != null) 8.dp else 16.dp)
            )
        }

        if (message != null) {
            NimbatText(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        items.forEach { item ->
            val contentColor = if (item.isDestructive) scheme.error
            else scheme.onSurface

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            item.onClick()
                            onDismissRequest()
                        }
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.icon != null) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(20.dp)
                    )
                }
                NimbatText(
                    text = item.text,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun NimbatActionSheetPreview() {
    NimbatTheme {
        NimbatActionSheetContent(
            items = previewItems,
            title = "What would you like to do?",
            message = "This shows the message renders too.",
            onDismissRequest = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        )
    }
}

private val previewItems = persistentListOf(
    NimbatActionSheetItem(
        text = "Rename",
        icon = Lucide.Pencil,
        onClick = {}
    ),
    NimbatActionSheetItem(
        text = "Share",
        icon = Lucide.Share2,
        onClick = {}
    ),
    NimbatActionSheetItem(
        text = "Delete",
        icon = Lucide.Trash2,
        isDestructive = true,
        onClick = {}
    )
)
