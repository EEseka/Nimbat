package com.eeseka.nimbat.wave.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSegmentedControl
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSegmentedItem
import com.eeseka.nimbat.core.design_system.components.toggles_and_control.NimbatSegmentedStyle
import com.eeseka.nimbat.core.design_system.components.util.AppHaptic
import com.eeseka.nimbat.core.design_system.components.util.rememberAppHaptic
import com.eeseka.nimbat.core.presentation.permissions.PermissionState
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import com.eeseka.nimbat.wave.presentation.WaveAction
import com.eeseka.nimbat.wave.presentation.WaveState
import com.eeseka.nimbat.wave.presentation.mappers.getTitle
import com.eeseka.nimbat.wave.presentation.model.WaveDemo
import com.eeseka.nimbat.wave.presentation.model.WaveSensorStatus
import kotlinx.collections.immutable.toImmutableList

@Composable
fun SinglePaneWave(
    state: WaveState,
    configuration: DeviceConfiguration,
    canOpenAppSettings: Boolean,
    isScreenUpright: Boolean,
    topInset: Dp,
    bottomInset: Dp,
    onAction: (WaveAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 600.dp)
            .fillMaxSize()
            .padding(top = topInset, bottom = bottomInset)
    ) {
        DemoPicker(
            demo = state.demo,
            onAction = onAction,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        DemoContent(
            state = state,
            isCompactHeight = false,
            isScreenUpright = isScreenUpright,
            onAction = onAction,
            modifier = Modifier.weight(1f).fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Stage(state = state, configuration = configuration, modifier = Modifier.size(112.dp))
            WaveStatus(
                state = state,
                canOpenAppSettings = canOpenAppSettings,
                isScreenUpright = isScreenUpright,
                onAction = onAction,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TwoPaneWave(
    state: WaveState,
    configuration: DeviceConfiguration,
    canOpenAppSettings: Boolean,
    isScreenUpright: Boolean,
    topInset: Dp,
    bottomInset: Dp,
    onAction: (WaveAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompactHeight = configuration == DeviceConfiguration.MOBILE_LANDSCAPE

    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = topInset, end = 24.dp, bottom = bottomInset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Stage(
                state = state,
                configuration = configuration,
                modifier = Modifier.size(if (isCompactHeight) 144.dp else 256.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            WaveStatus(
                state = state,
                canOpenAppSettings = canOpenAppSettings,
                isScreenUpright = isScreenUpright,
                onAction = onAction,
                isCentered = true
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Column(
            modifier = Modifier
                .weight(1.4f)
                .fillMaxHeight()
                .padding(top = topInset, bottom = bottomInset)
        ) {
            DemoPicker(
                demo = state.demo,
                onAction = onAction,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            DemoContent(
                state = state,
                isCompactHeight = isCompactHeight,
                isScreenUpright = isScreenUpright,
                onAction = onAction,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DemoPicker(
    demo: WaveDemo,
    onAction: (WaveAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = rememberAppHaptic()
    val items = WaveDemo.entries.map { NimbatSegmentedItem(title = it.getTitle()) }.toImmutableList()

    NimbatSegmentedControl(
        items = items,
        selectedIndex = demo.ordinal,
        onItemSelected = { index ->
            hapticFeedback(AppHaptic.Selection)
            onAction(WaveAction.OnDemoSelected(WaveDemo.entries[index]))
        },
        style = NimbatSegmentedStyle.FIXED_BAR,
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
    )
}

@Composable
private fun DemoContent(
    state: WaveState,
    isCompactHeight: Boolean,
    isScreenUpright: Boolean,
    onAction: (WaveAction) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = state.demo,
        transitionSpec = {
            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
            (slideInHorizontally { width -> direction * width / 4 } + fadeIn()) togetherWith
                (slideOutHorizontally { width -> -direction * width / 4 } + fadeOut())
        },
        label = "wave_demo",
        modifier = modifier
    ) { demo ->
        when (demo) {
            WaveDemo.SLIDES -> SlidesDemo(
                slideIndex = state.slideIndex,
                isCompactHeight = isCompactHeight,
                onSlideSettled = { onAction(WaveAction.OnSlideSettled(it)) },
                modifier = Modifier.fillMaxSize()
            )

            WaveDemo.GALLERY -> GalleryDemo(
                animalIndex = state.animalIndex,
                isCompactHeight = isCompactHeight,
                onAnimalSettled = { onAction(WaveAction.OnAnimalSettled(it)) },
                modifier = Modifier.fillMaxSize()
            )

            WaveDemo.BAT_DASH -> BatDashDemo(
                game = state.batDash,
                isScreenUpright = isScreenUpright,
                onCaveTap = { onAction(WaveAction.OnCaveTap(it)) },
                onFrame = { onAction(WaveAction.OnBatDashFrame(it)) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun Stage(
    state: WaveState,
    configuration: DeviceConfiguration,
    modifier: Modifier = Modifier
) {
    WaveStage(
        configuration = configuration,
        isListening = state.microphonePermission == PermissionState.GRANTED && state.sensorStatus != WaveSensorStatus.FAILED,
        calibrationProgress = if (state.sensorStatus == WaveSensorStatus.CALIBRATING) state.calibrationProgress else 1f,
        towardLevel = state.towardLevel,
        awayLevel = state.awayLevel,
        lastGesture = state.lastGesture,
        gestureCount = state.gestureCount,
        modifier = modifier
    )
}
