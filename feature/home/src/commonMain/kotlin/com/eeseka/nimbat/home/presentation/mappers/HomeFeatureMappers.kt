package com.eeseka.nimbat.home.presentation.mappers

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.ArrowLeftRight
import com.composables.icons.lucide.DoorOpen
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPinned
import com.composables.icons.lucide.MonitorSmartphone
import com.composables.icons.lucide.Radar
import com.composables.icons.lucide.RulerDimensionLine
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Wind
import com.eeseka.nimbat.core.design_system.theme.extended
import com.eeseka.nimbat.home.presentation.model.HomeFeature
import nimbat.feature.home.generated.resources.Res
import nimbat.feature.home.generated.resources.beam
import nimbat.feature.home.generated.resources.beam_description
import nimbat.feature.home.generated.resources.breathe
import nimbat.feature.home.generated.resources.breathe_description
import nimbat.feature.home.generated.resources.find_it
import nimbat.feature.home.generated.resources.find_it_description
import nimbat.feature.home.generated.resources.guard
import nimbat.feature.home.generated.resources.guard_description
import nimbat.feature.home.generated.resources.how_far
import nimbat.feature.home.generated.resources.how_far_description
import nimbat.feature.home.generated.resources.join
import nimbat.feature.home.generated.resources.join_description
import nimbat.feature.home.generated.resources.measure
import nimbat.feature.home.generated.resources.measure_description
import nimbat.feature.home.generated.resources.room_map
import nimbat.feature.home.generated.resources.room_map_description
import nimbat.feature.home.generated.resources.tripwire
import nimbat.feature.home.generated.resources.tripwire_description
import nimbat.feature.home.generated.resources.wave
import nimbat.feature.home.generated.resources.wave_description
import org.jetbrains.compose.resources.stringResource

@Composable
fun HomeFeature.getTitle(): String = stringResource(
    when (this) {
        HomeFeature.WAVE -> Res.string.wave
        HomeFeature.GUARD -> Res.string.guard
        HomeFeature.BREATHE -> Res.string.breathe
        HomeFeature.JOIN -> Res.string.join
        HomeFeature.HOW_FAR -> Res.string.how_far
        HomeFeature.FIND_IT -> Res.string.find_it
        HomeFeature.MEASURE -> Res.string.measure
        HomeFeature.BEAM -> Res.string.beam
        HomeFeature.ROOM_MAP -> Res.string.room_map
        HomeFeature.TRIPWIRE -> Res.string.tripwire
    }
)

@Composable
fun HomeFeature.getDescription(): String = stringResource(
    when (this) {
        HomeFeature.WAVE -> Res.string.wave_description
        HomeFeature.GUARD -> Res.string.guard_description
        HomeFeature.BREATHE -> Res.string.breathe_description
        HomeFeature.JOIN -> Res.string.join_description
        HomeFeature.HOW_FAR -> Res.string.how_far_description
        HomeFeature.FIND_IT -> Res.string.find_it_description
        HomeFeature.MEASURE -> Res.string.measure_description
        HomeFeature.BEAM -> Res.string.beam_description
        HomeFeature.ROOM_MAP -> Res.string.room_map_description
        HomeFeature.TRIPWIRE -> Res.string.tripwire_description
    }
)

fun HomeFeature.getIcon(): ImageVector = when (this) {
    HomeFeature.WAVE -> Lucide.Hand
    HomeFeature.GUARD -> Lucide.ShieldCheck
    HomeFeature.BREATHE -> Lucide.Wind
    HomeFeature.JOIN -> Lucide.MonitorSmartphone
    HomeFeature.HOW_FAR -> Lucide.ArrowLeftRight
    HomeFeature.FIND_IT -> Lucide.Radar
    HomeFeature.MEASURE -> Lucide.RulerDimensionLine
    HomeFeature.BEAM -> Lucide.Send
    HomeFeature.ROOM_MAP -> Lucide.MapPinned
    HomeFeature.TRIPWIRE -> Lucide.DoorOpen
}

fun HomeFeature.getPulseMillis(): Int = 2_400 + ordinal * 180

@Composable
fun HomeFeature.getAccentColor(): Color {
    val scheme = MaterialTheme.colorScheme

    return when (this) {
        HomeFeature.WAVE -> scheme.extended.skyContainer
        HomeFeature.GUARD -> scheme.extended.coralContainer
        HomeFeature.BREATHE -> scheme.extended.greenContainer
        HomeFeature.JOIN -> scheme.primaryContainer
        HomeFeature.HOW_FAR -> scheme.extended.tealContainer
        HomeFeature.FIND_IT -> scheme.extended.amberContainer
        HomeFeature.MEASURE -> scheme.extended.goldContainer
        HomeFeature.ROOM_MAP -> scheme.extended.indigoContainer
        HomeFeature.BEAM -> scheme.tertiaryContainer
        HomeFeature.TRIPWIRE -> scheme.extended.limeContainer
    }
}

@Composable
fun HomeFeature.getOnAccentColor(): Color {
    val scheme = MaterialTheme.colorScheme

    return when (this) {
        HomeFeature.WAVE -> scheme.extended.onSkyContainer
        HomeFeature.GUARD -> scheme.extended.onCoralContainer
        HomeFeature.BREATHE -> scheme.extended.onGreenContainer
        HomeFeature.JOIN -> scheme.onPrimaryContainer
        HomeFeature.HOW_FAR -> scheme.extended.onTealContainer
        HomeFeature.FIND_IT -> scheme.extended.onAmberContainer
        HomeFeature.MEASURE -> scheme.extended.onGoldContainer
        HomeFeature.ROOM_MAP -> scheme.extended.onIndigoContainer
        HomeFeature.BEAM -> scheme.onTertiaryContainer
        HomeFeature.TRIPWIRE -> scheme.extended.onLimeContainer
    }
}
