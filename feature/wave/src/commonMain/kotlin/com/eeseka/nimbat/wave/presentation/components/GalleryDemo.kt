package com.eeseka.nimbat.wave.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.eeseka.nimbat.core.design_system.components.layouts.NimbatCard
import com.eeseka.nimbat.core.design_system.components.textfields.NimbatText
import com.eeseka.nimbat.core.design_system.theme.NimbatTheme
import com.eeseka.nimbat.wave.presentation.mappers.getCaption
import com.eeseka.nimbat.wave.presentation.mappers.getName
import com.eeseka.nimbat.wave.presentation.mappers.getPhoto
import com.eeseka.nimbat.wave.presentation.model.GalleryAnimal
import com.eeseka.nimbat.wave.presentation.util.pageDepth
import com.eeseka.nimbat.wave.presentation.util.rememberSyncedPagerState
import org.jetbrains.compose.resources.painterResource

@Composable
fun GalleryDemo(
    animalIndex: Int,
    isCompactHeight: Boolean,
    onAnimalSettled: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animals = GalleryAnimal.entries
    val pagerState = rememberSyncedPagerState(page = animalIndex, pageCount = animals.size, onPageSettled = onAnimalSettled)

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 24.dp),
        pageSpacing = 12.dp,
        modifier = modifier
    ) { page ->
        AnimalCard(
            animal = animals[page],
            isCompactHeight = isCompactHeight,
            modifier = Modifier
                .fillMaxSize()
                .pageDepth(pagerState, page)
        )
    }
}

@Composable
private fun AnimalCard(
    animal: GalleryAnimal,
    isCompactHeight: Boolean,
    modifier: Modifier = Modifier
) {
    val name = animal.getName()

    NimbatCard(modifier = modifier) {
        if (isCompactHeight) {
            Row(modifier = Modifier.fillMaxSize()) {
                AnimalPhoto(animal = animal, name = name, modifier = Modifier.weight(1f).fillMaxHeight())
                AnimalCaption(
                    name = name,
                    caption = animal.getCaption(),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                )
            }
        } else {
            AnimalPhoto(animal = animal, name = name, modifier = Modifier.weight(1f).fillMaxWidth())
            AnimalCaption(name = name, caption = animal.getCaption())
        }
    }
}

@Composable
private fun AnimalPhoto(
    animal: GalleryAnimal,
    name: String,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(animal.getPhoto()),
        contentDescription = name,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
private fun AnimalCaption(
    name: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        NimbatText(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        NimbatText(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@PreviewLightDark
@Composable
private fun GalleryDemoPreview() {
    NimbatTheme {
        GalleryDemo(
            animalIndex = 0,
            isCompactHeight = false,
            onAnimalSettled = {},
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .height(520.dp)
        )
    }
}
