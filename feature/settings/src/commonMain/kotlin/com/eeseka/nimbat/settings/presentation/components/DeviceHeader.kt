package com.eeseka.nimbat.settings.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.illustrations.drawDevice
import com.eeseka.nimbat.core.design_system.components.illustrations.drawWave
import com.eeseka.nimbat.core.design_system.components.illustrations.rememberLoopProgress
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import nimbat.feature.settings.generated.resources.Res
import nimbat.feature.settings.generated.resources.rename
import org.jetbrains.compose.resources.stringResource

private const val WAVE_COUNT = 2

@Composable
fun DeviceHeader(
    deviceName: String,
    configuration: DeviceConfiguration,
    onRenameClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = rememberAppHaptic()
    val renameLabel = stringResource(Res.string.rename)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DeviceSonar(
            configuration = configuration,
            modifier = Modifier.size(144.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        NimbatText(
            text = deviceName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable(
                interactionSource = null,
                indication = null,
                onClickLabel = renameLabel,
                role = Role.Button
            ) {
                hapticFeedback(AppHaptic.ImpactLight)
                onRenameClick()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DeviceSonar(
    configuration: DeviceConfiguration,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val backdropShape = MaterialShapes.Cookie12Sided.toShape()
    val turn by rememberLoopProgress(durationMillis = 40_000, label = "device_backdrop")
    val pulse by rememberLoopProgress(durationMillis = 2_400, label = "device_waves")

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = turn * 360f }
                .clip(backdropShape)
                .background(scheme.primaryContainer.copy(alpha = 0.22f))
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = size.minDimension
            val center = Offset(size.width / 2, size.height / 2)

            repeat(WAVE_COUNT) { i ->
                val progress = (pulse + i.toFloat() / WAVE_COUNT) % 1f
                val color = scheme.tertiary.copy(alpha = 1f - progress)
                val radius = side * (0.3f + progress * 0.18f)
                drawWave(center, radius, facingDegrees = 0f, color = color, strokeWidth = side * 0.016f)
                drawWave(center, radius, facingDegrees = 180f, color = color, strokeWidth = side * 0.016f)
            }

            drawDevice(
                configuration = configuration,
                center = center,
                height = side * 0.46f,
                body = scheme.primary,
                screen = scheme.surface
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun DeviceHeaderPreview() {
    NimbatTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            DeviceHeader(
                deviceName = "Emmanuel's iPhone",
                configuration = DeviceConfiguration.MOBILE_PORTRAIT,
                onRenameClick = {}
            )
        }
    }
}