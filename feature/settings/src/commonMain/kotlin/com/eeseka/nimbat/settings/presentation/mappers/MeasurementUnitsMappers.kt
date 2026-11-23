package com.eeseka.nimbat.settings.presentation.mappers

import androidx.compose.runtime.Composable
import com.eeseka.nimbat.core.domain.settings.MeasurementUnits
import nimbat.feature.settings.generated.resources.Res
import nimbat.feature.settings.generated.resources.units_imperial
import nimbat.feature.settings.generated.resources.units_metric
import org.jetbrains.compose.resources.stringResource

@Composable
fun MeasurementUnits.getTitle(): String = stringResource(
    when (this) {
        MeasurementUnits.METRIC -> Res.string.units_metric
        MeasurementUnits.IMPERIAL -> Res.string.units_imperial
    }
)
