package com.eeseka.nimbat.guard.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButton
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButtonStyle
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.extended
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.domain.guard.model.GuardPhase
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import com.eeseka.nimbat.guard.presentation.GuardState
import nimbat.feature.guard.generated.resources.Res
import nimbat.feature.guard.generated.resources.cant_hear_tone
import nimbat.feature.guard.generated.resources.cant_hear_tone_message
import nimbat.feature.guard.generated.resources.curtain_ready
import nimbat.feature.guard.generated.resources.curtain_ready_message
import nimbat.feature.guard.generated.resources.hold_still
import nimbat.feature.guard.generated.resources.hold_still_message
import nimbat.feature.guard.generated.resources.movement_count
import nimbat.feature.guard.generated.resources.stop
import nimbat.feature.guard.generated.resources.walk_away
import nimbat.feature.guard.generated.resources.walk_away_message
import nimbat.feature.guard.generated.resources.watching
import nimbat.feature.guard.generated.resources.watching_message
import nimbat.feature.guard.generated.resources.watching_quietly
import nimbat.feature.guard.generated.resources.watching_quietly_message
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val COUNTDOWN_SIZE_FRACTION = 0.8f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private val COUNTDOWN_SHAPES = listOf(
    MaterialShapes.Sunny,
    MaterialShapes.Cookie9Sided,
    MaterialShapes.SoftBurst,
    MaterialShapes.Cookie12Sided,
    MaterialShapes.Burst,
    MaterialShapes.Clover8Leaf
)

@Composable
fun WatchingOverlay(
    state: GuardState,
    configuration: DeviceConfiguration,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GuardOverlayLayout(
        configuration = configuration,
        visual = { size ->
            if (state.phase == GuardPhase.COUNTDOWN) {
                Countdown(secondsLeft = state.countdownSeconds, size = size * COUNTDOWN_SIZE_FRACTION)
            } else {
                GuardScene(
                    configuration = configuration,
                    color = MaterialTheme.colorScheme.extended.coralContainer,
                    screenColor = MaterialTheme.colorScheme.background,
                    movementLevel = state.movementLevel,
                    movementCount = state.movementCount,
                    modifier = Modifier.size(size)
                )
            }
        },
        words = { WatchingMessage(state = state) },
        button = {
            NimbatButton(
                text = stringResource(Res.string.stop),
                onClick = onStopClick,
                style = NimbatButtonStyle.SECONDARY
            )
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun Countdown(secondsLeft: Int, size: Dp) {
    val coral = MaterialTheme.colorScheme.extended.coralContainer
    val onCoral = MaterialTheme.colorScheme.extended.onCoralContainer

    AnimatedContent(
        targetState = secondsLeft,
        transitionSpec = {
            (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith
                (scaleOut(targetScale = 1.3f) + fadeOut())
        },
        label = "guard_countdown"
    ) { seconds ->
        Box(
            modifier = Modifier
                .size(size)
                .clip(COUNTDOWN_SHAPES[seconds % COUNTDOWN_SHAPES.size].toShape())
                .background(coral),
            contentAlignment = Alignment.Center
        ) {
            NimbatText(
                text = seconds.toString(),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = onCoral
            )
        }
    }
}

@Composable
private fun WatchingMessage(state: GuardState) {
    val (title, body) = when {
        state.phase == GuardPhase.COUNTDOWN -> stringResource(Res.string.walk_away) to
            stringResource(Res.string.walk_away_message)
        !state.isToneClear -> stringResource(Res.string.cant_hear_tone) to
            stringResource(Res.string.cant_hear_tone_message)
        state.phase == GuardPhase.CALIBRATING -> stringResource(Res.string.hold_still) to
            stringResource(Res.string.hold_still_message)
        state.mode == GuardMode.CURTAIN -> stringResource(Res.string.curtain_ready) to
            stringResource(Res.string.curtain_ready_message)
        state.mode == GuardMode.SILENT -> stringResource(Res.string.watching_quietly) to
            stringResource(Res.string.watching_quietly_message)
        else -> stringResource(Res.string.watching) to stringResource(Res.string.watching_message)
    }
    val showsCount = state.phase == GuardPhase.WATCHING && state.mode == GuardMode.SILENT

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NimbatText(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        NimbatText(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (showsCount) {
            NimbatText(
                text = pluralStringResource(Res.plurals.movement_count, state.movementCount, state.movementCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.extended.coralContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 860)
@Composable
private fun WatchingOverlayPreview() {
    WatchingOverlay(
        state = GuardState(phase = GuardPhase.COUNTDOWN, countdownSeconds = 7),
        configuration = DeviceConfiguration.MOBILE_PORTRAIT,
        onStopClick = {}
    )
}
