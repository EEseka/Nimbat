package com.eeseka.nimbat.guard.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatCard
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.design_system.theme.extended
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import nimbat.feature.guard.generated.resources.Res
import nimbat.feature.guard.generated.resources.alarm_body
import nimbat.feature.guard.generated.resources.alarm_body_upright
import nimbat.feature.guard.generated.resources.alarm_title
import nimbat.feature.guard.generated.resources.curtain_body
import nimbat.feature.guard.generated.resources.curtain_body_web
import nimbat.feature.guard.generated.resources.curtain_title
import nimbat.feature.guard.generated.resources.silent_body
import nimbat.feature.guard.generated.resources.silent_body_upright
import nimbat.feature.guard.generated.resources.silent_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun GuardHero(
    mode: GuardMode,
    configuration: DeviceConfiguration,
    isScreenUpright: Boolean,
    isWeb: Boolean,
    modifier: Modifier = Modifier
) {
    val coral = MaterialTheme.colorScheme.extended.coralContainer
    val onCoral = MaterialTheme.colorScheme.extended.onCoralContainer

    NimbatCard(
        containerColor = coral,
        contentColor = onCoral,
        modifier = modifier
    ) {
        if (configuration == DeviceConfiguration.MOBILE_LANDSCAPE) {
            Row(modifier = Modifier.fillMaxSize()) {
                GuardScene(
                    configuration = configuration,
                    color = onCoral,
                    screenColor = coral,
                    modifier = Modifier.weight(0.6f).fillMaxHeight()
                )
                HeroWords(
                    mode = mode,
                    isScreenUpright = isScreenUpright,
                    isWeb = isWeb,
                    color = onCoral,
                    isCompact = true,
                    modifier = Modifier
                        .weight(1.4f)
                        .fillMaxHeight()
                        .padding(top = 24.dp, end = 24.dp, bottom = 24.dp)
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                GuardScene(
                    configuration = configuration,
                    color = onCoral,
                    screenColor = coral,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
                HeroWords(
                    mode = mode,
                    isScreenUpright = isScreenUpright,
                    isWeb = isWeb,
                    color = onCoral,
                    isCompact = false
                )
            }
        }
    }
}

@Composable
private fun HeroWords(
    mode: GuardMode,
    isScreenUpright: Boolean,
    isWeb: Boolean,
    color: Color,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = mode,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (slideInHorizontally { width -> direction * width / 4 } + fadeIn()) togetherWith
                (slideOutHorizontally { width -> -direction * width / 4 } + fadeOut())
        },
        contentAlignment = Alignment.CenterStart,
        label = "guard_hero_words",
        modifier = modifier
    ) { shown ->
        val title = stringResource(
            when (shown) {
                GuardMode.ALARM -> Res.string.alarm_title
                GuardMode.SILENT -> Res.string.silent_title
                GuardMode.CURTAIN -> Res.string.curtain_title
            }
        )
        val body = stringResource(
            when (shown) {
                GuardMode.ALARM -> if (isScreenUpright) Res.string.alarm_body_upright else Res.string.alarm_body
                GuardMode.SILENT -> if (isScreenUpright) Res.string.silent_body_upright else Res.string.silent_body
                GuardMode.CURTAIN -> if (isWeb) Res.string.curtain_body_web else Res.string.curtain_body
            }
        )

        Column(
            modifier = if (isCompact) Modifier.verticalScroll(rememberScrollState()) else Modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            NimbatText(
                text = title,
                style = if (isCompact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            NimbatText(
                text = body,
                style = if (isCompact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                color = color
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun GuardHeroPreview() {
    NimbatTheme {
        GuardHero(
            mode = GuardMode.ALARM,
            configuration = DeviceConfiguration.MOBILE_PORTRAIT,
            isScreenUpright = false,
            isWeb = false,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .height(420.dp)
        )
    }
}
