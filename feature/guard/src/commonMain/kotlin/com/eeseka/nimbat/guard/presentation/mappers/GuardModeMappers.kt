package com.eeseka.nimbat.guard.presentation.mappers

import androidx.compose.runtime.Composable
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import nimbat.feature.guard.generated.resources.Res
import nimbat.feature.guard.generated.resources.alarm
import nimbat.feature.guard.generated.resources.curtain
import nimbat.feature.guard.generated.resources.silent
import org.jetbrains.compose.resources.stringResource

@Composable
fun GuardMode.getTitle(): String = stringResource(
    when (this) {
        GuardMode.ALARM -> Res.string.alarm
        GuardMode.SILENT -> Res.string.silent
        GuardMode.CURTAIN -> Res.string.curtain
    }
)
