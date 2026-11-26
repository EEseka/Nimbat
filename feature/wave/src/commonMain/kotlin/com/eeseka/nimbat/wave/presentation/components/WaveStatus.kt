package com.eeseka.nimbat.wave.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButton
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButtonStyle
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.wave.presentation.WaveAction
import com.eeseka.nimbat.wave.presentation.WaveState
import com.eeseka.nimbat.wave.presentation.mappers.getHint
import com.eeseka.nimbat.wave.presentation.model.WaveSensorStatus
import nimbat.feature.wave.generated.resources.Res
import nimbat.feature.wave.generated.resources.allow_microphone
import nimbat.feature.wave.generated.resources.cant_hear_tone
import nimbat.feature.wave.generated.resources.cant_hear_tone_message
import nimbat.feature.wave.generated.resources.hold_still
import nimbat.feature.wave.generated.resources.hold_still_message
import nimbat.feature.wave.generated.resources.hold_still_message_upright
import nimbat.feature.wave.generated.resources.microphone_blocked
import nimbat.feature.wave.generated.resources.microphone_blocked_browser_message
import nimbat.feature.wave.generated.resources.microphone_blocked_message
import nimbat.feature.wave.generated.resources.open_settings
import nimbat.feature.wave.generated.resources.stopped_listening
import nimbat.feature.wave.generated.resources.stopped_listening_message
import nimbat.feature.wave.generated.resources.try_again
import org.jetbrains.compose.resources.stringResource

private enum class WaveMessage { MICROPHONE_BLOCKED, STOPPED, TONE_QUIET, CALIBRATING, SENSING }

@Composable
fun WaveStatus(
    state: WaveState,
    canOpenAppSettings: Boolean,
    isScreenUpright: Boolean,
    onAction: (WaveAction) -> Unit,
    modifier: Modifier = Modifier,
    isCentered: Boolean = false
) {
    val permission = state.microphonePermission ?: return
    val message = when {
        permission != PermissionState.GRANTED -> WaveMessage.MICROPHONE_BLOCKED
        state.sensorStatus == WaveSensorStatus.FAILED -> WaveMessage.STOPPED
        !state.isToneClear -> WaveMessage.TONE_QUIET
        state.sensorStatus == WaveSensorStatus.CALIBRATING -> WaveMessage.CALIBRATING
        else -> WaveMessage.SENSING
    }
    val horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (isCentered) TextAlign.Center else TextAlign.Start

    AnimatedContent(
        targetState = message,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        contentAlignment = if (isCentered) Alignment.Center else Alignment.CenterStart,
        label = "wave_status",
        modifier = modifier
    ) { shown ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val title = when (shown) {
                WaveMessage.MICROPHONE_BLOCKED -> stringResource(Res.string.microphone_blocked)
                WaveMessage.STOPPED -> stringResource(Res.string.stopped_listening)
                WaveMessage.TONE_QUIET -> stringResource(Res.string.cant_hear_tone)
                WaveMessage.CALIBRATING -> stringResource(Res.string.hold_still)
                WaveMessage.SENSING -> null
            }
            val isBlockedInBrowser = permission == PermissionState.PERMANENTLY_DENIED && !canOpenAppSettings
            val body = when (shown) {
                WaveMessage.MICROPHONE_BLOCKED -> stringResource(
                    if (isBlockedInBrowser) Res.string.microphone_blocked_browser_message
                    else Res.string.microphone_blocked_message
                )
                WaveMessage.STOPPED -> stringResource(Res.string.stopped_listening_message)
                WaveMessage.TONE_QUIET -> stringResource(Res.string.cant_hear_tone_message)
                WaveMessage.CALIBRATING -> stringResource(
                    if (isScreenUpright) Res.string.hold_still_message_upright else Res.string.hold_still_message
                )
                WaveMessage.SENSING -> state.demo.getHint(isScreenUpright)
            }

            title?.let {
                NimbatText(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = textAlign
                )
            }
            NimbatText(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign
            )

            val buttonModifier = Modifier
                .padding(top = 8.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
            when (shown) {
                WaveMessage.MICROPHONE_BLOCKED if !isBlockedInBrowser -> NimbatButton(
                    text = stringResource(
                        if (permission == PermissionState.PERMANENTLY_DENIED) Res.string.open_settings
                        else Res.string.allow_microphone
                    ),
                    onClick = { onAction(WaveAction.OnAllowMicrophoneClick) },
                    isLoading = state.isRequestingMicrophone,
                    modifier = buttonModifier
                )

                WaveMessage.STOPPED -> NimbatButton(
                    text = stringResource(Res.string.try_again),
                    onClick = { onAction(WaveAction.OnTryAgainClick) },
                    style = NimbatButtonStyle.SECONDARY,
                    modifier = buttonModifier
                )

                else -> Unit
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun WaveStatusPreview() {
    NimbatTheme {
        WaveStatus(
            state = WaveState(microphonePermission = PermissionState.DENIED),
            canOpenAppSettings = true,
            isScreenUpright = false,
            onAction = {},
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)
        )
    }
}
