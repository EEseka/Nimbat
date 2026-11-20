package com.eeseka.nimbat.onboarding.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButton
import com.eeseka.nimbat.core.design_system.components.buttons.NimbatButtonStyle
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme

@Composable
fun OnboardingControls(
    pageCount: Int,
    currentPage: Int,
    pagerPosition: () -> Float,
    primaryText: String,
    onPrimaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimaryLoading: Boolean = false,
    primaryLoadingText: String? = null,
    secondaryText: String? = null,
    onSecondaryClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OnboardingPageIndicator(
            pageCount = pageCount,
            currentPage = currentPage,
            pagerPosition = pagerPosition
        )

        Spacer(modifier = Modifier.height(32.dp))

        NimbatButton(
            text = primaryText,
            onClick = onPrimaryClick,
            isLoading = isPrimaryLoading,
            loadingText = primaryLoadingText
        )

        AnimatedVisibility(
            visible = secondaryText != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            NimbatButton(
                text = secondaryText.orEmpty(),
                onClick = onSecondaryClick,
                style = NimbatButtonStyle.TEXT,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingControlsPreview() {
    NimbatTheme {
        OnboardingControls(
            pageCount = 6,
            currentPage = 4,
            pagerPosition = { 4f },
            primaryText = "Next",
            onPrimaryClick = {},
            secondaryText = "Try again",
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        )
    }
}
