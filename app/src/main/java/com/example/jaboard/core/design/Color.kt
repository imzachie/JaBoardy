package com.example.jaboard.core.design

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Pixel Cobalt Material 3 Expressive Fallback Palette
val CobaltPrimaryLight = Color(0xFF1A56DB)
val CobaltOnPrimaryLight = Color(0xFFFFFFFF)
val CobaltPrimaryContainerLight = Color(0xFFD8E2FF)
val CobaltOnPrimaryContainerLight = Color(0xFF001A43)

val SlateSecondaryLight = Color(0xFF4B5E82)
val SlateOnSecondaryLight = Color(0xFFFFFFFF)
val SlateSecondaryContainerLight = Color(0xFFDCE4F9)
val SlateOnSecondaryContainerLight = Color(0xFF0B1C38)

val TealTertiaryLight = Color(0xFF0F766E)
val TealOnTertiaryLight = Color(0xFFFFFFFF)
val TealTertiaryContainerLight = Color(0xFFCCFBF1)
val TealOnTertiaryContainerLight = Color(0xFF00201D)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight = Color(0xFFF6F8FC)
val OnBackgroundLight = Color(0xFF171B24)
val SurfaceLight = Color(0xFFF6F8FC)
val OnSurfaceLight = Color(0xFF171B24)
val SurfaceVariantLight = Color(0xFFE0E5F0)
val OnSurfaceVariantLight = Color(0xFF434857)
val OutlineLight = Color(0xFF737887)
val OutlineVariantLight = Color(0xFFC4C9D6)

val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF0F4FA)
val SurfaceContainerLight = Color(0xFFE9EEF7)
val SurfaceContainerHighLight = Color(0xFFE2E8F2)
val SurfaceContainerHighestLight = Color(0xFFDCE2ED)

// Dark Theme Tokens
val CobaltPrimaryDark = Color(0xFFA8C7FA)
val CobaltOnPrimaryDark = Color(0xFF002F6C)
val CobaltPrimaryContainerDark = Color(0xFF0842A0)
val CobaltOnPrimaryContainerDark = Color(0xFFD8E2FF)

val SlateSecondaryDark = Color(0xFFBEC6DC)
val SlateOnSecondaryDark = Color(0xFF263147)
val SlateSecondaryContainerDark = Color(0xFF3C475F)
val SlateOnSecondaryContainerDark = Color(0xFFDCE4F9)

val TealTertiaryDark = Color(0xFF7DD3C8)
val TealOnTertiaryDark = Color(0xFF003733)
val TealTertiaryContainerDark = Color(0xFF00504A)
val TealOnTertiaryContainerDark = Color(0xFFCCFBF1)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark = Color(0xFF10131A)
val OnBackgroundDark = Color(0xFFE2E6F0)
val SurfaceDark = Color(0xFF10131A)
val OnSurfaceDark = Color(0xFFE2E6F0)
val SurfaceVariantDark = Color(0xFF434857)
val OnSurfaceVariantDark = Color(0xFFC4C9D6)
val OutlineDark = Color(0xFF8D92A2)
val OutlineVariantDark = Color(0xFF434857)

val SurfaceContainerLowestDark = Color(0xFF0B0E14)
val SurfaceContainerLowDark = Color(0xFF161A23)
val SurfaceContainerDark = Color(0xFF1B202B)
val SurfaceContainerHighDark = Color(0xFF252B38)
val SurfaceContainerHighestDark = Color(0xFF303746)

// Semantic Status Colors
object JaBoardStatusColors {
  val HealthyGreen = Color(0xFF15803D)
  val HealthyGreenContainer = Color(0xFFDCFCE7)
  val WarningAmber = Color(0xFFB45309)
  val WarningAmberContainer = Color(0xFFFEF3C7)
  val CriticalRed = Color(0xFFB91C1C)
  val CriticalRedContainer = Color(0xFFFEE2E2)
}

val JaBoardLightColorScheme =
  lightColorScheme(
    primary = CobaltPrimaryLight,
    onPrimary = CobaltOnPrimaryLight,
    primaryContainer = CobaltPrimaryContainerLight,
    onPrimaryContainer = CobaltOnPrimaryContainerLight,
    secondary = SlateSecondaryLight,
    onSecondary = SlateOnSecondaryLight,
    secondaryContainer = SlateSecondaryContainerLight,
    onSecondaryContainer = SlateOnSecondaryContainerLight,
    tertiary = TealTertiaryLight,
    onTertiary = TealOnTertiaryLight,
    tertiaryContainer = TealTertiaryContainerLight,
    onTertiaryContainer = TealOnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
  )

val JaBoardDarkColorScheme =
  darkColorScheme(
    primary = CobaltPrimaryDark,
    onPrimary = CobaltOnPrimaryDark,
    primaryContainer = CobaltPrimaryContainerDark,
    onPrimaryContainer = CobaltOnPrimaryContainerDark,
    secondary = SlateSecondaryDark,
    onSecondary = SlateOnSecondaryDark,
    secondaryContainer = SlateSecondaryContainerDark,
    onSecondaryContainer = SlateOnSecondaryContainerDark,
    tertiary = TealTertiaryDark,
    onTertiary = TealOnTertiaryDark,
    tertiaryContainer = TealTertiaryContainerDark,
    onTertiaryContainer = TealOnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
  )
