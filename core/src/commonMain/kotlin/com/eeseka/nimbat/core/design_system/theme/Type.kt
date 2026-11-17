package com.eeseka.nimbat.core.design_system.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import nimbat.core.generated.resources.Res
import nimbat.core.generated.resources.bricolage_grotesque_bold
import nimbat.core.generated.resources.bricolage_grotesque_medium
import nimbat.core.generated.resources.bricolage_grotesque_regular
import nimbat.core.generated.resources.figtree_bold
import nimbat.core.generated.resources.figtree_medium
import nimbat.core.generated.resources.figtree_regular
import nimbat.core.generated.resources.jetbrains_mono_bold
import nimbat.core.generated.resources.jetbrains_mono_medium
import org.jetbrains.compose.resources.Font

@Composable
fun getAppTypography(): Typography {
    val bricolageGrotesque = FontFamily(
        Font(Res.font.bricolage_grotesque_regular, FontWeight.Normal),
        Font(Res.font.bricolage_grotesque_medium, FontWeight.Medium),
        Font(Res.font.bricolage_grotesque_bold, FontWeight.Bold)
    )
    val figtree = FontFamily(
        Font(Res.font.figtree_regular, FontWeight.Normal),
        Font(Res.font.figtree_medium, FontWeight.Medium),
        Font(Res.font.figtree_bold, FontWeight.Bold)
    )
    val jetBrainsMono = FontFamily(
        Font(Res.font.jetbrains_mono_medium, FontWeight.Medium),
        Font(Res.font.jetbrains_mono_bold, FontWeight.Bold)
    )

    val baseline = Typography()

    return Typography(
        displayLarge = baseline.displayLarge.copy(
            fontFamily = bricolageGrotesque,
            fontWeight = FontWeight.Bold
        ),
        displayMedium = baseline.displayMedium.copy(
            fontFamily = bricolageGrotesque,
            fontWeight = FontWeight.Medium
        ),
        displaySmall = baseline.displaySmall.copy(
            fontFamily = bricolageGrotesque,
            fontWeight = FontWeight.Normal
        ),
        headlineLarge = baseline.headlineLarge.copy(
            fontFamily = bricolageGrotesque,
            fontWeight = FontWeight.Bold
        ),
        headlineMedium = baseline.headlineMedium.copy(
            fontFamily = bricolageGrotesque,
            fontWeight = FontWeight.Medium
        ),
        headlineSmall = baseline.headlineSmall.copy(
            fontFamily = bricolageGrotesque,
            fontWeight = FontWeight.Normal
        ),
        titleLarge = baseline.titleLarge.copy(
            fontFamily = figtree,
            fontWeight = FontWeight.Bold
        ),
        titleMedium = baseline.titleMedium.copy(
            fontFamily = figtree,
            fontWeight = FontWeight.Bold
        ),
        titleSmall = baseline.titleSmall.copy(
            fontFamily = figtree,
            fontWeight = FontWeight.Bold
        ),
        bodyLarge = baseline.bodyLarge.copy(
            fontFamily = figtree,
            fontWeight = FontWeight.Normal
        ),
        bodyMedium = baseline.bodyMedium.copy(
            fontFamily = figtree,
            fontWeight = FontWeight.Normal
        ),
        bodySmall = baseline.bodySmall.copy(
            fontFamily = figtree,
            fontWeight = FontWeight.Normal
        ),
        labelLarge = baseline.labelLarge.copy(
            fontFamily = figtree,
            fontWeight = FontWeight.Medium
        ),
        labelMedium = baseline.labelMedium.copy(
            fontFamily = jetBrainsMono,
            fontWeight = FontWeight.Medium
        ),
        labelSmall = baseline.labelSmall.copy(
            fontFamily = jetBrainsMono,
            fontWeight = FontWeight.Medium
        )
    )
}
