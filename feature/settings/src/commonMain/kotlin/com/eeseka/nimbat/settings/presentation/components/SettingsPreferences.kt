package com.eeseka.nimbat.settings.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.CodeXml
import com.composables.icons.lucide.Contrast
import com.composables.icons.lucide.Crosshair
import com.composables.icons.lucide.Ear
import com.composables.icons.lucide.EarOff
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.MicOff
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Ruler
import com.composables.icons.lucide.Thermometer
import com.composables.icons.lucide.Vibrate
import com.composables.icons.lucide.Volume2
import com.eeseka.nimbat.core.design_system.components.progress_indicator.NimbatProgressIndicator
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSegmentedControl
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSegmentedItem
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSegmentedStyle
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSlider
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSwitch
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.design_system.theme.extended
import com.eeseka.nimbat.core.domain.settings.AppTheme
import com.eeseka.nimbat.core.domain.settings.MeasurementUnits
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.settings.presentation.SettingsAction
import com.eeseka.nimbat.settings.presentation.SettingsState
import com.eeseka.nimbat.settings.presentation.mappers.getIcon
import com.eeseka.nimbat.settings.presentation.mappers.getTitle
import com.eeseka.nimbat.settings.presentation.util.formatDistanceOffset
import com.eeseka.nimbat.settings.presentation.util.formatPercent
import com.eeseka.nimbat.settings.presentation.util.formatTemperature
import com.eeseka.nimbat.settings.presentation.util.speedInUnits
import com.eeseka.nimbat.sound.speedOfSound
import kotlinx.collections.immutable.toImmutableList
import nimbat.feature.settings.generated.resources.Res
import nimbat.feature.settings.generated.resources.about
import nimbat.feature.settings.generated.resources.allow
import nimbat.feature.settings.generated.resources.appearance
import nimbat.feature.settings.generated.resources.distance_offset
import nimbat.feature.settings.generated.resources.distance_offset_message
import nimbat.feature.settings.generated.resources.feel
import nimbat.feature.settings.generated.resources.haptics
import nimbat.feature.settings.generated.resources.haptics_message
import nimbat.feature.settings.generated.resources.lights_out
import nimbat.feature.settings.generated.resources.lights_out_message
import nimbat.feature.settings.generated.resources.microphone_blocked
import nimbat.feature.settings.generated.resources.microphone_blocked_browser_message
import nimbat.feature.settings.generated.resources.microphone_blocked_message
import nimbat.feature.settings.generated.resources.open_settings
import nimbat.feature.settings.generated.resources.room_temperature
import nimbat.feature.settings.generated.resources.room_temperature_message
import nimbat.feature.settings.generated.resources.room_temperature_message_imperial
import nimbat.feature.settings.generated.resources.run
import nimbat.feature.settings.generated.resources.sound
import nimbat.feature.settings.generated.resources.sound_check
import nimbat.feature.settings.generated.resources.sound_check_failed
import nimbat.feature.settings.generated.resources.sound_check_listening
import nimbat.feature.settings.generated.resources.sound_check_not_run
import nimbat.feature.settings.generated.resources.sound_check_passed
import nimbat.feature.settings.generated.resources.source_code
import nimbat.feature.settings.generated.resources.source_code_message
import nimbat.feature.settings.generated.resources.theme
import nimbat.feature.settings.generated.resources.units
import nimbat.feature.settings.generated.resources.volume
import nimbat.feature.settings.generated.resources.volume_message
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private const val MIN_ROOM_TEMPERATURE_C = -10f
private const val MAX_ROOM_TEMPERATURE_C = 45f
private const val MIN_VOLUME = 0.1f

private enum class SoundCheckAction { RUN, LISTENING, HIDDEN }

@Composable
fun SettingsPreferences(
    state: SettingsState,
    canOpenAppSettings: Boolean,
    onAction: (SettingsAction) -> Unit,
    onSourceCodeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        SoundSection(state = state, canOpenAppSettings = canOpenAppSettings, onAction = onAction)
        AppearanceSection(appTheme = state.appTheme, isPureBlackEnabled = state.isPureBlackEnabled, onAction = onAction)
        if (state.canUseHaptics) {
            FeelSection(areHapticsEnabled = state.areHapticsEnabled, onAction = onAction)
        }
        AboutSection(onSourceCodeClick = onSourceCodeClick)
    }
}

@Composable
private fun SoundSection(
    state: SettingsState,
    canOpenAppSettings: Boolean,
    onAction: (SettingsAction) -> Unit
) {
    val hapticFeedback = rememberAppHaptic()
    val extended = MaterialTheme.colorScheme.extended
    val units = state.measurementUnits
    val speed = speedInUnits(metersPerSecond = speedOfSound(state.roomTemperatureCelsius), units = units)
    val unitItems = MeasurementUnits.entries.map { NimbatSegmentedItem(title = it.getTitle()) }.toImmutableList()
    val isMicrophoneBlocked = state.microphonePermission != PermissionState.GRANTED
    val firstIndex = if (isMicrophoneBlocked) 1 else 0
    val count = firstIndex + 5

    val soundCheckAction = when {
        state.isRunningSoundCheck -> SoundCheckAction.LISTENING
        isMicrophoneBlocked -> SoundCheckAction.HIDDEN
        else -> SoundCheckAction.RUN
    }

    SettingsSection(title = stringResource(Res.string.sound)) {
        AnimatedVisibility(
            visible = isMicrophoneBlocked,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val isBlockedInBrowser = state.microphonePermission == PermissionState.PERMANENTLY_DENIED && !canOpenAppSettings

            SettingsRow(
                icon = Lucide.MicOff,
                badgeColor = MaterialTheme.colorScheme.errorContainer,
                onBadgeColor = MaterialTheme.colorScheme.onErrorContainer,
                title = stringResource(Res.string.microphone_blocked),
                subtitle = stringResource(
                    if (isBlockedInBrowser) Res.string.microphone_blocked_browser_message
                    else Res.string.microphone_blocked_message
                ),
                index = 0,
                count = count,
                trailing = if (isBlockedInBrowser) {
                    null
                } else {
                    {
                        SettingsTextAction(
                            text = stringResource(
                                if (state.microphonePermission == PermissionState.PERMANENTLY_DENIED) Res.string.open_settings
                                else Res.string.allow
                            ),
                            icon = Lucide.Mic,
                            color = MaterialTheme.colorScheme.error,
                            onClick = { onAction(SettingsAction.OnAllowMicrophoneClick) }
                        )
                    }
                }
            )
        }

        SettingsRow(
            icon = if (state.isSoundCheckPassed == false) Lucide.EarOff else Lucide.Ear,
            badgeColor = when (state.isSoundCheckPassed) {
                true -> extended.successContainer
                false -> extended.warningContainer
                null -> MaterialTheme.colorScheme.primaryContainer
            },
            onBadgeColor = when (state.isSoundCheckPassed) {
                true -> extended.onSuccessContainer
                false -> extended.onWarningContainer
                null -> MaterialTheme.colorScheme.onPrimaryContainer
            },
            title = stringResource(Res.string.sound_check),
            subtitle = stringResource(
                when {
                    state.isRunningSoundCheck -> Res.string.sound_check_listening
                    state.isSoundCheckPassed == true -> Res.string.sound_check_passed
                    state.isSoundCheckPassed == false -> Res.string.sound_check_failed
                    else -> Res.string.sound_check_not_run
                }
            ),
            index = firstIndex,
            count = count,
            trailing = {
                AnimatedContent(
                    targetState = soundCheckAction,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    contentAlignment = Alignment.CenterEnd,
                    label = "sound_check_action"
                ) { action ->
                    when (action) {
                        SoundCheckAction.RUN -> SettingsTextAction(
                            text = stringResource(Res.string.run),
                            icon = Lucide.Play,
                            color = MaterialTheme.colorScheme.primary,
                            onClick = { onAction(SettingsAction.OnRunSoundCheckClick) }
                        )

                        SoundCheckAction.LISTENING -> NimbatProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )

                        SoundCheckAction.HIDDEN -> Unit
                    }
                }
            }
        )

        SettingsRow(
            icon = Lucide.Thermometer,
            badgeColor = extended.amberContainer,
            onBadgeColor = extended.onAmberContainer,
            title = stringResource(Res.string.room_temperature),
            subtitle = when (units) {
                MeasurementUnits.METRIC -> stringResource(Res.string.room_temperature_message, speed)
                MeasurementUnits.IMPERIAL -> stringResource(Res.string.room_temperature_message_imperial, speed)
            },
            index = firstIndex + 1,
            count = count,
            trailing = { SettingsValue(text = formatTemperature(state.roomTemperatureCelsius, units)) },
            control = {
                NimbatSlider(
                    value = state.roomTemperatureCelsius.toFloat(),
                    onValueChange = { value ->
                        val celsius = value.roundToInt().toDouble()
                        if (celsius != state.roomTemperatureCelsius) {
                            hapticFeedback(AppHaptic.Selection)
                            onAction(SettingsAction.OnRoomTemperatureChange(celsius))
                        }
                    },
                    valueRange = MIN_ROOM_TEMPERATURE_C..MAX_ROOM_TEMPERATURE_C,
                    steps = (MAX_ROOM_TEMPERATURE_C - MIN_ROOM_TEMPERATURE_C).toInt() - 1,
                    onValueChangeFinished = { onAction(SettingsAction.OnRoomTemperatureChangeFinished) }
                )
            }
        )

        SettingsRow(
            icon = Lucide.Volume2,
            badgeColor = extended.skyContainer,
            onBadgeColor = extended.onSkyContainer,
            title = stringResource(Res.string.volume),
            subtitle = stringResource(Res.string.volume_message),
            index = firstIndex + 2,
            count = count,
            trailing = { SettingsValue(text = formatPercent(state.volume)) },
            control = {
                NimbatSlider(
                    value = state.volume,
                    onValueChange = { value ->
                        val volume = (value * 10).roundToInt() / 10f
                        if (volume != state.volume) {
                            hapticFeedback(AppHaptic.Selection)
                            onAction(SettingsAction.OnVolumeChange(volume))
                        }
                    },
                    valueRange = MIN_VOLUME..1f,
                    steps = 8,
                    onValueChangeFinished = { onAction(SettingsAction.OnVolumeChangeFinished) }
                )
            }
        )

        SettingsRow(
            icon = Lucide.Crosshair,
            badgeColor = extended.tealContainer,
            onBadgeColor = extended.onTealContainer,
            title = stringResource(Res.string.distance_offset),
            subtitle = stringResource(Res.string.distance_offset_message),
            index = firstIndex + 3,
            count = count,
            trailing = { SettingsValue(text = formatDistanceOffset(state.distanceOffsetMeters, units)) }
        )

        SettingsRow(
            icon = Lucide.Ruler,
            badgeColor = extended.goldContainer,
            onBadgeColor = extended.onGoldContainer,
            title = stringResource(Res.string.units),
            index = firstIndex + 4,
            count = count,
            control = {
                NimbatSegmentedControl(
                    items = unitItems,
                    selectedIndex = units.ordinal,
                    onItemSelected = { index ->
                        hapticFeedback(AppHaptic.Selection)
                        onAction(SettingsAction.OnMeasurementUnitsSelected(MeasurementUnits.entries[index]))
                    },
                    style = NimbatSegmentedStyle.FIXED_BAR,
                    contentPadding = PaddingValues(0.dp)
                )
            }
        )
    }
}

@Composable
private fun AppearanceSection(
    appTheme: AppTheme,
    isPureBlackEnabled: Boolean,
    onAction: (SettingsAction) -> Unit
) {
    val hapticFeedback = rememberAppHaptic()
    val scheme = MaterialTheme.colorScheme
    val themeItems = AppTheme.entries.map {
        NimbatSegmentedItem(title = it.getTitle(), icon = it.getIcon())
    }.toImmutableList()

    val isDarkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    SettingsSection(title = stringResource(Res.string.appearance)) {
        SettingsRow(
            icon = Lucide.Palette,
            badgeColor = scheme.inverseSurface,
            onBadgeColor = scheme.inverseOnSurface,
            title = stringResource(Res.string.theme),
            index = 0,
            count = if (isDarkTheme) 2 else 1,
            control = {
                NimbatSegmentedControl(
                    items = themeItems,
                    selectedIndex = appTheme.ordinal,
                    onItemSelected = { index ->
                        hapticFeedback(AppHaptic.Selection)
                        onAction(SettingsAction.OnThemeSelected(AppTheme.entries[index]))
                    },
                    style = NimbatSegmentedStyle.FIXED_BAR,
                    contentPadding = PaddingValues(0.dp)
                )
            }
        )

        AnimatedVisibility(
            visible = isDarkTheme,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SettingsRow(
                icon = Lucide.Contrast,
                badgeColor = scheme.extended.indigoContainer,
                onBadgeColor = scheme.extended.onIndigoContainer,
                title = stringResource(Res.string.lights_out),
                subtitle = stringResource(Res.string.lights_out_message),
                index = 1,
                count = 2,
                trailing = {
                    NimbatSwitch(
                        checked = isPureBlackEnabled,
                        onCheckedChange = { isEnabled ->
                            hapticFeedback(AppHaptic.Selection)
                            onAction(SettingsAction.OnPureBlackToggled(isEnabled))
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun FeelSection(
    areHapticsEnabled: Boolean,
    onAction: (SettingsAction) -> Unit
) {
    val hapticFeedback = rememberAppHaptic()
    val scheme = MaterialTheme.colorScheme

    SettingsSection(title = stringResource(Res.string.feel)) {
        SettingsRow(
            icon = Lucide.Vibrate,
            badgeColor = scheme.extended.coralContainer,
            onBadgeColor = scheme.extended.onCoralContainer,
            title = stringResource(Res.string.haptics),
            subtitle = stringResource(Res.string.haptics_message),
            index = 0,
            count = 1,
            trailing = {
                NimbatSwitch(
                    checked = areHapticsEnabled,
                    onCheckedChange = { isEnabled ->
                        hapticFeedback(AppHaptic.Selection)
                        onAction(SettingsAction.OnHapticsToggled(isEnabled))
                    }
                )
            }
        )
    }
}

@Composable
private fun AboutSection(onSourceCodeClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme

    SettingsSection(title = stringResource(Res.string.about)) {
        SettingsRow(
            icon = Lucide.CodeXml,
            badgeColor = scheme.primaryContainer,
            onBadgeColor = scheme.onPrimaryContainer,
            title = stringResource(Res.string.source_code),
            subtitle = stringResource(Res.string.source_code_message),
            index = 0,
            count = 1,
            onClick = onSourceCodeClick,
            trailing = {
                Icon(
                    imageVector = Lucide.ExternalLink,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}

@Composable
private fun SettingsValue(text: String) {
    NimbatText(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary
    )
}
