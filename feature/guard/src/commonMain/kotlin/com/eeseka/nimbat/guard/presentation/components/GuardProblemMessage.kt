package com.eeseka.nimbat.guard.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButton
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.domain.guard.model.GuardPhase
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.guard.presentation.GuardAction
import com.eeseka.nimbat.guard.presentation.GuardState
import nimbat.feature.guard.generated.resources.Res
import nimbat.feature.guard.generated.resources.allow_microphone
import nimbat.feature.guard.generated.resources.microphone_blocked
import nimbat.feature.guard.generated.resources.microphone_blocked_browser_message
import nimbat.feature.guard.generated.resources.microphone_blocked_message
import nimbat.feature.guard.generated.resources.open_settings
import nimbat.feature.guard.generated.resources.stopped_listening
import nimbat.feature.guard.generated.resources.stopped_listening_message
import org.jetbrains.compose.resources.stringResource

private enum class GuardProblem { MICROPHONE_BLOCKED, STOPPED }

@Composable
fun GuardProblemMessage(
    state: GuardState,
    canOpenAppSettings: Boolean,
    onAction: (GuardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val permission = state.microphonePermission
    val problem = when {
        permission != null && permission != PermissionState.GRANTED -> GuardProblem.MICROPHONE_BLOCKED
        state.phase == GuardPhase.FAILED -> GuardProblem.STOPPED
        else -> null
    }

    AnimatedContent(
        targetState = problem,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "guard_status",
        modifier = modifier
    ) { shown ->
        if (shown == null) return@AnimatedContent

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isBlockedInBrowser = permission == PermissionState.PERMANENTLY_DENIED && !canOpenAppSettings
            val title = when (shown) {
                GuardProblem.MICROPHONE_BLOCKED -> stringResource(Res.string.microphone_blocked)
                GuardProblem.STOPPED -> stringResource(Res.string.stopped_listening)
            }
            val body = when (shown) {
                GuardProblem.MICROPHONE_BLOCKED -> stringResource(
                    if (isBlockedInBrowser) Res.string.microphone_blocked_browser_message
                    else Res.string.microphone_blocked_message
                )
                GuardProblem.STOPPED -> stringResource(Res.string.stopped_listening_message)
            }

            NimbatText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            NimbatText(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (shown == GuardProblem.MICROPHONE_BLOCKED && !isBlockedInBrowser) {
                NimbatButton(
                    text = stringResource(
                        if (permission == PermissionState.PERMANENTLY_DENIED) Res.string.open_settings
                        else Res.string.allow_microphone
                    ),
                    onClick = { onAction(GuardAction.OnAllowMicrophoneClick) },
                    isLoading = state.isRequestingMicrophone,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun GuardProblemMessagePreview() {
    NimbatTheme {
        GuardProblemMessage(
            state = GuardState(microphonePermission = PermissionState.DENIED),
            canOpenAppSettings = true,
            onAction = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)
        )
    }
}
