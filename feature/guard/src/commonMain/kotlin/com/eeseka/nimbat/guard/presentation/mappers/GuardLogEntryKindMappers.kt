package com.eeseka.nimbat.guard.presentation.mappers

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.BellRing
import com.composables.icons.lucide.Footprints
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MicOff
import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntryKind
import nimbat.feature.guard.generated.resources.Res
import nimbat.feature.guard.generated.resources.alarm_went_off
import nimbat.feature.guard.generated.resources.movement
import nimbat.feature.guard.generated.resources.tone_lost
import nimbat.feature.guard.generated.resources.walked_away
import org.jetbrains.compose.resources.stringResource

@Composable
fun GuardLogEntryKind.getTitle(): String = stringResource(
    when (this) {
        GuardLogEntryKind.MOVEMENT -> Res.string.movement
        GuardLogEntryKind.ALARM -> Res.string.alarm_went_off
        GuardLogEntryKind.TONE_LOST -> Res.string.tone_lost
        GuardLogEntryKind.WALKED_AWAY -> Res.string.walked_away
    }
)

fun GuardLogEntryKind.getIcon(): ImageVector = when (this) {
    GuardLogEntryKind.MOVEMENT -> Lucide.Footprints
    GuardLogEntryKind.ALARM -> Lucide.BellRing
    GuardLogEntryKind.TONE_LOST -> Lucide.MicOff
    GuardLogEntryKind.WALKED_AWAY -> Lucide.LogOut
}
