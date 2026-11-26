package com.eeseka.nimbat.wave.presentation.mappers

import androidx.compose.runtime.Composable
import com.eeseka.nimbat.wave.presentation.model.WaveDemo
import nimbat.feature.wave.generated.resources.Res
import nimbat.feature.wave.generated.resources.bat_dash
import nimbat.feature.wave.generated.resources.bat_dash_hint
import nimbat.feature.wave.generated.resources.bat_dash_hint_upright
import nimbat.feature.wave.generated.resources.gallery
import nimbat.feature.wave.generated.resources.gallery_hint
import nimbat.feature.wave.generated.resources.gallery_hint_upright
import nimbat.feature.wave.generated.resources.slides
import nimbat.feature.wave.generated.resources.slides_hint
import nimbat.feature.wave.generated.resources.slides_hint_upright
import org.jetbrains.compose.resources.stringResource

@Composable
fun WaveDemo.getTitle(): String = stringResource(
    when (this) {
        WaveDemo.SLIDES -> Res.string.slides
        WaveDemo.GALLERY -> Res.string.gallery
        WaveDemo.BAT_DASH -> Res.string.bat_dash
    }
)

@Composable
fun WaveDemo.getHint(isScreenUpright: Boolean): String = stringResource(
    when (this) {
        WaveDemo.SLIDES -> if (isScreenUpright) Res.string.slides_hint_upright else Res.string.slides_hint
        WaveDemo.GALLERY -> if (isScreenUpright) Res.string.gallery_hint_upright else Res.string.gallery_hint
        WaveDemo.BAT_DASH -> if (isScreenUpright) Res.string.bat_dash_hint_upright else Res.string.bat_dash_hint
    }
)
