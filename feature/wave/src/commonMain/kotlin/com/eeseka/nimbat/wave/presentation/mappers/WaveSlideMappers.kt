package com.eeseka.nimbat.wave.presentation.mappers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Dog
import com.composables.icons.lucide.Ear
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Radar
import com.composables.icons.lucide.Repeat
import com.composables.icons.lucide.Siren
import com.eeseka.nimbat.wave.presentation.model.WaveSlide
import nimbat.feature.wave.generated.resources.Res
import nimbat.feature.wave.generated.resources.slide_bats_body
import nimbat.feature.wave.generated.resources.slide_bats_title
import nimbat.feature.wave.generated.resources.slide_bend_body
import nimbat.feature.wave.generated.resources.slide_bend_title
import nimbat.feature.wave.generated.resources.slide_echo_body
import nimbat.feature.wave.generated.resources.slide_echo_title
import nimbat.feature.wave.generated.resources.slide_hello_body
import nimbat.feature.wave.generated.resources.slide_hello_title
import nimbat.feature.wave.generated.resources.slide_note_body
import nimbat.feature.wave.generated.resources.slide_note_title
import nimbat.feature.wave.generated.resources.slide_siren_body
import nimbat.feature.wave.generated.resources.slide_siren_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun WaveSlide.getTitle(): String = stringResource(
    when (this) {
        WaveSlide.HELLO -> Res.string.slide_hello_title
        WaveSlide.NOTE -> Res.string.slide_note_title
        WaveSlide.ECHO -> Res.string.slide_echo_title
        WaveSlide.SIREN -> Res.string.slide_siren_title
        WaveSlide.BEND -> Res.string.slide_bend_title
        WaveSlide.BATS -> Res.string.slide_bats_title
    }
)

@Composable
fun WaveSlide.getBody(): String = stringResource(
    when (this) {
        WaveSlide.HELLO -> Res.string.slide_hello_body
        WaveSlide.NOTE -> Res.string.slide_note_body
        WaveSlide.ECHO -> Res.string.slide_echo_body
        WaveSlide.SIREN -> Res.string.slide_siren_body
        WaveSlide.BEND -> Res.string.slide_bend_body
        WaveSlide.BATS -> Res.string.slide_bats_body
    }
)

fun WaveSlide.getIcon(): ImageVector = when (this) {
    WaveSlide.HELLO -> Lucide.Hand
    WaveSlide.NOTE -> Lucide.Dog
    WaveSlide.ECHO -> Lucide.Repeat
    WaveSlide.SIREN -> Lucide.Siren
    WaveSlide.BEND -> Lucide.Ear
    WaveSlide.BATS -> Lucide.Radar
}
