package com.eeseka.nimbat.guard.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButton
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButtonStyle
import com.eeseka.nimbat.core.design_system.components.illustrations.rememberLoopProgress
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.extended
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import kotlinx.coroutines.delay
import nimbat.feature.guard.generated.resources.Res
import nimbat.feature.guard.generated.resources.something_moved
import nimbat.feature.guard.generated.resources.turn_off
import org.jetbrains.compose.resources.stringResource

private const val THROB_MILLIS = 450
private const val BUZZ_GAP_MILLIS = 600L

private const val SCENE_INSET_FRACTION = 0.11f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlarmOverlay(
    configuration: DeviceConfiguration,
    onTurnOffClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = rememberAppHaptic()
    val throb by rememberInfiniteTransition(label = "alarm").animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(THROB_MILLIS), RepeatMode.Reverse),
        label = "alarm_throb"
    )
    val turn by rememberLoopProgress(durationMillis = 12_000, label = "alarm_turn")

    LaunchedEffect(Unit) {
        while (true) {
            hapticFeedback(AppHaptic.ImpactHeavy)
            delay(BUZZ_GAP_MILLIS)
        }
    }

    GuardOverlayLayout(
        configuration = configuration,
        visual = { size ->
            val coral = MaterialTheme.colorScheme.extended.coralContainer

            Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = throb
                            scaleY = throb
                            rotationZ = turn * 360f
                        }
                        .clip(MaterialShapes.Cookie12Sided.toShape())
                        .background(coral)
                )
                GuardScene(
                    configuration = configuration,
                    color = MaterialTheme.colorScheme.extended.onCoralContainer,
                    screenColor = coral,
                    pulseMillis = 900,
                    movementLevel = 1f,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(size * SCENE_INSET_FRACTION)
                )
            }
        },
        words = {
            NimbatText(
                text = stringResource(Res.string.something_moved),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        },
        button = {
            NimbatButton(
                text = stringResource(Res.string.turn_off),
                onClick = onTurnOffClick,
                style = NimbatButtonStyle.SECONDARY
            )
        },
        modifier = modifier
    )
}

@Preview(widthDp = 412, heightDp = 860)
@Composable
private fun AlarmOverlayPreview() {
    AlarmOverlay(configuration = DeviceConfiguration.MOBILE_PORTRAIT, onTurnOffClick = {})
}
