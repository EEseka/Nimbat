package com.eeseka.nimbat.sound_check

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButton
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButtonStyle
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatCard
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatScaffold
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.presentation.permissions.MicrophonePermissionEffect
import com.eeseka.nimbat.core.presentation.util.UiText
import org.koin.compose.viewmodel.koinViewModel

// Temporary screen for trying platform audio on real devices; deleted once Onboarding's sound check exists
@Composable
fun SoundCheckRoot(
    viewModel: SoundCheckViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    MicrophonePermissionEffect(
        isEnabled = state.pendingCheck != null,
        onPermissionResolved = { viewModel.onAction(SoundCheckAction.OnMicrophonePermissionResolved(it)) }
    )

    SoundCheckScreen(
        state = state,
        onAction = viewModel::onAction
    )
}

@Composable
fun SoundCheckScreen(
    state: SoundCheckState,
    onAction: (SoundCheckAction) -> Unit
) {
    NimbatScaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    top = paddingValues.calculateTopPadding() + 16.dp,
                    bottom = paddingValues.calculateBottomPadding() + 16.dp
                )
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NimbatCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NimbatText(text = "Sound check", style = MaterialTheme.typography.headlineMedium)
                        NimbatText(
                            text = state.status.asString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        state.motionWord?.let { word ->
                            NimbatText(
                                text = word,
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        state.motionReadout?.let { readout ->
                            NimbatText(text = readout, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                NimbatButton(
                    text = "Chirp check",
                    isLoading = state.runningCheck == SoundCheck.CHIRP,
                    enabled = state.runningCheck == null,
                    onClick = { onAction(SoundCheckAction.OnChirpCheckClick) }
                )
                NimbatButton(
                    text = "Message check",
                    style = NimbatButtonStyle.SECONDARY,
                    isLoading = state.runningCheck == SoundCheck.MESSAGE,
                    enabled = state.runningCheck == null,
                    onClick = { onAction(SoundCheckAction.OnMessageCheckClick) }
                )
                NimbatButton(
                    text = if (state.runningCheck == SoundCheck.MOTION) "Stop motion check" else "Motion check",
                    style = NimbatButtonStyle.SECONDARY,
                    enabled = state.runningCheck == null || state.runningCheck == SoundCheck.MOTION,
                    onClick = { onAction(SoundCheckAction.OnMotionCheckClick) }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SoundCheckScreenPreview() {
    NimbatTheme {
        SoundCheckScreen(
            state = SoundCheckState(
                status = UiText.DynamicString("Wave your hand toward and away from the device."),
                runningCheck = SoundCheck.MOTION,
                motionWord = "Toward",
                motionReadout = "Toward −45 dB (still −62) · Away −60 dB (still −63)"
            ),
            onAction = {}
        )
    }
}
