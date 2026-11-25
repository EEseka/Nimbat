package com.eeseka.nimbat.home.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.EarOff
import com.composables.icons.lucide.Lucide
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButton
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatCard
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.design_system.theme.extended
import nimbat.feature.home.generated.resources.Res
import nimbat.feature.home.generated.resources.check_again
import nimbat.feature.home.generated.resources.hearing_trouble
import nimbat.feature.home.generated.resources.hearing_trouble_message
import nimbat.feature.home.generated.resources.listening
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SoundCheckHint(
    isRunningSoundCheck: Boolean,
    onCheckAgainClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = rememberAppHaptic()
    val extended = MaterialTheme.colorScheme.extended

    NimbatCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(MaterialShapes.Cookie9Sided.toShape())
                        .background(extended.warningContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.EarOff,
                        contentDescription = null,
                        tint = extended.onWarningContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    NimbatText(
                        text = stringResource(Res.string.hearing_trouble),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    NimbatText(
                        text = stringResource(Res.string.hearing_trouble_message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            NimbatButton(
                text = stringResource(Res.string.check_again),
                loadingText = stringResource(Res.string.listening),
                isLoading = isRunningSoundCheck,
                onClick = {
                    hapticFeedback(AppHaptic.ImpactMedium)
                    onCheckAgainClick()
                },
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SoundCheckHintPreview() {
    NimbatTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            SoundCheckHint(isRunningSoundCheck = false, onCheckAgainClick = {})
        }
    }
}
