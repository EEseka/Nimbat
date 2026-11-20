package com.eeseka.nimbat.onboarding.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import com.eeseka.nimbat.core.presentation.util.currentDeviceConfiguration
import com.eeseka.nimbat.onboarding.presentation.components.illustrations.SonarIllustration
import kotlin.math.abs

@Composable
fun OnboardingPageContent(
    pageIndex: Int,
    title: String,
    description: String,
    pageOffset: () -> Float,
    illustration: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    extraContent: (@Composable () -> Unit)? = null
) {
    val illustrationSize = when (currentDeviceConfiguration()) {
        DeviceConfiguration.MOBILE_LANDSCAPE -> 220.dp
        DeviceConfiguration.TABLET_PORTRAIT -> 400.dp
        else -> 320.dp
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(illustrationSize)
                .aspectRatio(1f)
                .graphicsLayer {
                    val away = abs(pageOffset()).coerceAtMost(1f)
                    scaleX = 1f - 0.3f * away
                    scaleY = 1f - 0.3f * away
                    alpha = 1f - 0.7f * away
                },
            contentAlignment = Alignment.Center
        ) {
            MorphingBackdrop(
                pageIndex = pageIndex,
                pageOffset = pageOffset,
                modifier = Modifier.fillMaxSize()
            )
            illustration(Modifier.fillMaxSize(0.86f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        NimbatText(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.graphicsLayer { leaveOutward(pageOffset(), distance = 0.15f) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedContent(
            targetState = description,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "description",
            modifier = Modifier.graphicsLayer { leaveOutward(pageOffset(), distance = 0.3f) }
        ) { text ->
            NimbatText(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (extraContent != null) {
            Spacer(modifier = Modifier.height(24.dp))
            extraContent()
        }
    }
}

private fun GraphicsLayerScope.leaveOutward(pageOffset: Float, distance: Float) {
    translationX = -pageOffset * size.width * distance
    alpha = 1f - (abs(pageOffset) * 1.6f).coerceAtMost(1f)
}

@PreviewLightDark
@Composable
private fun OnboardingPageContentPreview() {
    NimbatTheme {
        OnboardingPageContent(
            pageIndex = 0,
            title = "Sonar for your devices",
            description = "Your phone, tablet and laptop sense each other and your hand, using sound too high for you to hear.",
            pageOffset = { 0f },
            illustration = { SonarIllustration(it) },
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        )
    }
}
