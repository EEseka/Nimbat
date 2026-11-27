package com.eeseka.nimbat.guard.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatCard
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.core.design_system.theme.extended
import com.eeseka.nimbat.core.domain.guard.GuardStorage
import com.eeseka.nimbat.core.domain.guard.model.GuardLogEntryKind
import com.eeseka.nimbat.core.domain.guard.model.GuardMode
import com.eeseka.nimbat.guard.presentation.mappers.getIcon
import com.eeseka.nimbat.guard.presentation.mappers.getTitle
import com.eeseka.nimbat.guard.presentation.model.GuardLogEntryUi
import com.eeseka.nimbat.guard.presentation.model.GuardSessionUi
import com.eeseka.nimbat.guard.presentation.util.toDurationLabel
import com.eeseka.nimbat.guard.presentation.util.toShortDateTimeLabel
import com.eeseka.nimbat.guard.presentation.util.toTimeLabel
import com.eeseka.nimbat.sound.GestureDirection
import kotlinx.collections.immutable.persistentListOf
import nimbat.feature.guard.generated.resources.Res
import nimbat.feature.guard.generated.resources.all_quiet
import nimbat.feature.guard.generated.resources.came_closer
import nimbat.feature.guard.generated.resources.clear
import nimbat.feature.guard.generated.resources.log
import nimbat.feature.guard.generated.resources.log_empty
import nimbat.feature.guard.generated.resources.log_kept
import nimbat.feature.guard.generated.resources.moved_away
import nimbat.feature.guard.generated.resources.session_running
import nimbat.feature.guard.generated.resources.session_time_range
import nimbat.feature.guard.generated.resources.tone_lost_details
import nimbat.feature.guard.generated.resources.walked_away_details
import org.jetbrains.compose.resources.stringResource

@Composable
fun GuardLogHeader(
    hasSessions: Boolean,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            NimbatText(
                text = stringResource(Res.string.log),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (hasSessions) {
                NimbatText(
                    text = stringResource(Res.string.log_kept, GuardStorage.SESSIONS_KEPT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (hasSessions) {
            ClearAction(onClick = onClearClick)
        }
    }
}

@Composable
private fun ClearAction(onClick: () -> Unit) {
    val hapticFeedback = rememberAppHaptic()

    NimbatText(
        text = stringResource(Res.string.clear),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable {
                hapticFeedback(AppHaptic.ImpactLight)
                onClick()
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

@Composable
fun GuardLogEmpty(modifier: Modifier = Modifier) {
    NimbatText(
        text = stringResource(Res.string.log_empty),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
fun GuardSessionCard(
    session: GuardSessionUi,
    modifier: Modifier = Modifier
) {
    val startLabel = session.startedAtMillis.toShortDateTimeLabel()
    val timeLabel = session.endedAtMillis?.let {
        stringResource(Res.string.session_time_range, startLabel, it.toTimeLabel())
    } ?: stringResource(Res.string.session_running, startLabel)

    NimbatCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                NimbatText(
                    text = session.mode.getTitle(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                NimbatText(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (session.entries.isEmpty()) {
                NimbatText(
                    text = stringResource(Res.string.all_quiet),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                SessionTimeline(session = session)
                session.entries.forEach { entry -> EntryRow(entry = entry) }
            }
        }
    }
}

@Composable
private fun SessionTimeline(session: GuardSessionUi) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val coral = MaterialTheme.colorScheme.extended.coralContainer
    val colors = mapOf(
        GuardLogEntryKind.MOVEMENT to coral,
        GuardLogEntryKind.ALARM to coral,
        GuardLogEntryKind.TONE_LOST to MaterialTheme.colorScheme.outline,
        GuardLogEntryKind.WALKED_AWAY to coral.copy(alpha = 0.4f)
    )
    val endMillis = session.endedAtMillis ?: session.entries.maxOf { it.atMillis + (it.durationMillis ?: 0L) }
    val spanMillis = (endMillis - session.startedAtMillis).coerceAtLeast(1L).toFloat()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val midY = size.height / 2
        val tickWidth = 4.dp.toPx()
        drawLine(
            color = track,
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        session.entries.forEach { entry ->
            val color = colors.getValue(entry.kind)
            val startX = (entry.atMillis - session.startedAtMillis) / spanMillis * size.width
            if (entry.kind == GuardLogEntryKind.WALKED_AWAY) {
                val endX = startX + (entry.durationMillis ?: 0L) / spanMillis * size.width
                drawRoundRect(
                    color = color,
                    topLeft = Offset(startX, midY - 4.dp.toPx()),
                    size = Size((endX - startX).coerceAtLeast(tickWidth), 8.dp.toPx()),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            } else {
                val tickHeight = size.height * (0.4f + 0.6f * (entry.strength ?: 0.5f))
                val tickLeft = (startX - tickWidth / 2).coerceIn(0f, size.width - tickWidth)
                drawRoundRect(
                    color = color,
                    topLeft = Offset(tickLeft, midY - tickHeight / 2),
                    size = Size(tickWidth, tickHeight),
                    cornerRadius = CornerRadius(tickWidth / 2)
                )
            }
        }
    }
}

@Composable
private fun EntryRow(entry: GuardLogEntryUi) {
    val details = when (entry.kind) {
        GuardLogEntryKind.MOVEMENT -> listOfNotNull(
            entry.durationMillis?.toDurationLabel(),
            when (entry.direction) {
                GestureDirection.TOWARD -> stringResource(Res.string.came_closer)
                GestureDirection.AWAY -> stringResource(Res.string.moved_away)
                null -> null
            }
        ).joinToString(separator = " · ")
        GuardLogEntryKind.ALARM -> ""
        GuardLogEntryKind.TONE_LOST -> stringResource(Res.string.tone_lost_details)
        GuardLogEntryKind.WALKED_AWAY -> stringResource(
            Res.string.walked_away_details,
            (entry.durationMillis ?: 0L).toDurationLabel()
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = entry.kind.getIcon(),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            NimbatText(
                text = entry.kind.getTitle(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (details.isNotEmpty()) {
                NimbatText(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        NimbatText(
            text = entry.atMillis.toTimeLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@PreviewLightDark
@Composable
private fun GuardSessionCardPreview() {
    NimbatTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            GuardSessionCard(
                session = GuardSessionUi(
                    id = 1,
                    mode = GuardMode.ALARM,
                    startedAtMillis = 0L,
                    endedAtMillis = 1_800_000L,
                    entries = persistentListOf(
                        GuardLogEntryUi(GuardLogEntryKind.MOVEMENT, 300_000L, 2_400L, 0.8f, GestureDirection.TOWARD),
                        GuardLogEntryUi(GuardLogEntryKind.TONE_LOST, 900_000L, null, null, null),
                        GuardLogEntryUi(GuardLogEntryKind.MOVEMENT, 1_500_000L, 6_100L, 0.3f, null)
                    )
                )
            )
        }
    }
}
