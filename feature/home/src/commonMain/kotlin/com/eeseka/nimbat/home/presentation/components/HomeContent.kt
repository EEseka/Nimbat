package com.eeseka.nimbat.home.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.presentation.util.DeviceConfiguration
import com.eeseka.nimbat.home.presentation.HomeAction
import com.eeseka.nimbat.home.presentation.HomeState
import com.eeseka.nimbat.home.presentation.model.HomeFeature
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import nimbat.feature.home.generated.resources.Res
import nimbat.feature.home.generated.resources.just_this_device
import nimbat.feature.home.generated.resources.with_another_device
import org.jetbrains.compose.resources.stringResource

private const val ENTER_STAGGER_MILLIS = 50L

private val soloFeatures = persistentListOf(
    HomeFeature.WAVE,
    HomeFeature.GUARD,
    HomeFeature.BREATHE
)
private val partnerFeatures = persistentListOf(
    HomeFeature.HOW_FAR,
    HomeFeature.FIND_IT,
    HomeFeature.MEASURE,
    HomeFeature.ROOM_MAP,
    HomeFeature.BEAM
)

@Composable
fun HomeContent(
    state: HomeState,
    configuration: DeviceConfiguration,
    topInset: Dp,
    bottomInset: Dp,
    onAction: (HomeAction) -> Unit,
    onFeatureClick: (HomeFeature) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = if (configuration == DeviceConfiguration.MOBILE_PORTRAIT) 2 else 3
    val horizontalPadding = if (configuration.isMobile) 16.dp else 24.dp

    var hasPlayedEntrance by rememberSaveable { mutableStateOf(false) }
    val playEntrance = remember { !hasPlayedEntrance }
    LaunchedEffect(Unit) { hasPlayedEntrance = true }

    Column(
        modifier = modifier
            .widthIn(max = 1080.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = topInset,
                bottom = bottomInset,
                start = horizontalPadding,
                end = horizontalPadding
            ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AnimatedVisibility(
            visible = state.isSoundCheckPassed == false,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            SoundCheckHint(
                isRunningSoundCheck = state.isRunningSoundCheck,
                onCheckAgainClick = { onAction(HomeAction.OnRunSoundCheckClick) }
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(text = stringResource(Res.string.just_this_device))

            FeatureGrid(
                features = soloFeatures,
                columns = columns,
                playEntrance = playEntrance,
                firstEnterIndex = 0,
                onFeatureClick = onFeatureClick
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(text = stringResource(Res.string.with_another_device))

            JoinCard(
                onClick = { onFeatureClick(HomeFeature.JOIN) },
                playEntrance = playEntrance,
                enterDelayMillis = soloFeatures.size * ENTER_STAGGER_MILLIS,
                modifier = Modifier.fillMaxWidth()
            )

            FeatureGrid(
                features = partnerFeatures,
                columns = columns,
                playEntrance = playEntrance,
                firstEnterIndex = soloFeatures.size + 1,
                onFeatureClick = onFeatureClick
            )
        }
    }
}

@Composable
private fun FeatureGrid(
    features: ImmutableList<HomeFeature>,
    columns: Int,
    playEntrance: Boolean,
    firstEnterIndex: Int,
    onFeatureClick: (HomeFeature) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        features.chunked(columns).forEachIndexed { rowIndex, rowFeatures ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowFeatures.forEachIndexed { index, feature ->
                    FeatureCard(
                        feature = feature,
                        onClick = { onFeatureClick(feature) },
                        playEntrance = playEntrance,
                        enterDelayMillis = (firstEnterIndex + rowIndex * columns + index) * ENTER_STAGGER_MILLIS,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    NimbatText(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = 4.dp)
    )
}
