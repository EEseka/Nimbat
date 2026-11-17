package com.eeseka.nimbat.core.design_system.components.toggles_and_control

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.mohamedrejeb.calf.ui.slider.AdaptiveSlider

@Composable
fun NimbatSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    AdaptiveSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished
    )
}

@PreviewLightDark
@Composable
private fun NimbatSliderPreview() {
    NimbatTheme {
        NimbatSlider(
            value = 0.5f,
            onValueChange = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun NimbatDisabledSliderPreview() {
    NimbatTheme {
        NimbatSlider(
            value = 0.5f,
            onValueChange = {},
            enabled = false
        )
    }
}
