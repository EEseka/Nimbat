package com.eeseka.nimbat.wave.presentation.mappers

import androidx.compose.runtime.Composable
import com.eeseka.nimbat.wave.presentation.model.GalleryAnimal
import nimbat.feature.wave.generated.resources.Res
import nimbat.feature.wave.generated.resources.animal_bat
import nimbat.feature.wave.generated.resources.animal_bat_caption
import nimbat.feature.wave.generated.resources.animal_cat
import nimbat.feature.wave.generated.resources.animal_cat_caption
import nimbat.feature.wave.generated.resources.animal_dog
import nimbat.feature.wave.generated.resources.animal_dog_caption
import nimbat.feature.wave.generated.resources.animal_dolphin
import nimbat.feature.wave.generated.resources.animal_dolphin_caption
import nimbat.feature.wave.generated.resources.animal_elephant
import nimbat.feature.wave.generated.resources.animal_elephant_caption
import nimbat.feature.wave.generated.resources.animal_moth
import nimbat.feature.wave.generated.resources.animal_moth_caption
import nimbat.feature.wave.generated.resources.animal_owl
import nimbat.feature.wave.generated.resources.animal_owl_caption
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GalleryAnimal.getName(): String = stringResource(
    when (this) {
        GalleryAnimal.BAT -> Res.string.animal_bat
        GalleryAnimal.DOLPHIN -> Res.string.animal_dolphin
        GalleryAnimal.MOTH -> Res.string.animal_moth
        GalleryAnimal.OWL -> Res.string.animal_owl
        GalleryAnimal.DOG -> Res.string.animal_dog
        GalleryAnimal.CAT -> Res.string.animal_cat
        GalleryAnimal.ELEPHANT -> Res.string.animal_elephant
    }
)

@Composable
fun GalleryAnimal.getCaption(): String = stringResource(
    when (this) {
        GalleryAnimal.BAT -> Res.string.animal_bat_caption
        GalleryAnimal.DOLPHIN -> Res.string.animal_dolphin_caption
        GalleryAnimal.MOTH -> Res.string.animal_moth_caption
        GalleryAnimal.OWL -> Res.string.animal_owl_caption
        GalleryAnimal.DOG -> Res.string.animal_dog_caption
        GalleryAnimal.CAT -> Res.string.animal_cat_caption
        GalleryAnimal.ELEPHANT -> Res.string.animal_elephant_caption
    }
)

fun GalleryAnimal.getPhoto(): DrawableResource = when (this) {
    GalleryAnimal.BAT -> Res.drawable.animal_bat
    GalleryAnimal.DOLPHIN -> Res.drawable.animal_dolphin
    GalleryAnimal.MOTH -> Res.drawable.animal_moth
    GalleryAnimal.OWL -> Res.drawable.animal_owl
    GalleryAnimal.DOG -> Res.drawable.animal_dog
    GalleryAnimal.CAT -> Res.drawable.animal_cat
    GalleryAnimal.ELEPHANT -> Res.drawable.animal_elephant
}
