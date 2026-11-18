package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.domain.usecase.AppAccentPalette

private val LightColorScheme = lightColorScheme(
    primary = IndigoPrimaryLight,
    onPrimary = IndigoOnPrimaryLight,
    primaryContainer = IndigoPrimaryContainerLight,
    onPrimaryContainer = IndigoOnPrimaryContainerLight,
    secondary = VioletSecondaryLight,
    onSecondary = VioletOnSecondaryLight,
    secondaryContainer = VioletSecondaryContainerLight,
    onSecondaryContainer = VioletOnSecondaryContainerLight,
    tertiary = AzureTertiaryLight,
    onTertiary = AzureOnTertiaryLight,
    tertiaryContainer = AzureTertiaryContainerLight,
    onTertiaryContainer = AzureOnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    error = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = IndigoOnPrimaryDark,
    primaryContainer = IndigoPrimaryContainerDark,
    onPrimaryContainer = IndigoOnPrimaryContainerDark,
    secondary = VioletSecondaryDark,
    onSecondary = VioletOnSecondaryDark,
    secondaryContainer = VioletSecondaryContainerDark,
    onSecondaryContainer = VioletOnSecondaryContainerDark,
    tertiary = AzureTertiaryDark,
    onTertiary = AzureOnTertiaryDark,
    tertiaryContainer = AzureTertiaryContainerDark,
    onTertiaryContainer = AzureOnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    error = ErrorRedDark
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

@Composable
fun HabitFlowTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentPalette: AppAccentPalette = AppAccentPalette.ELECTRIC_INDIGO,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = when (accentPalette) {
        AppAccentPalette.ELECTRIC_INDIGO -> baseScheme
        AppAccentPalette.AURORA_EMERALD -> if (darkTheme) {
            baseScheme.copy(
                primary = EmeraldPrimaryDark,
                onPrimary = EmeraldOnPrimaryDark,
                primaryContainer = EmeraldPrimaryContainerDark,
                onPrimaryContainer = EmeraldOnPrimaryContainerDark,
                secondary = EmeraldSecondaryDark,
                secondaryContainer = EmeraldSecondaryContainerDark
            )
        } else {
            baseScheme.copy(
                primary = EmeraldPrimaryLight,
                primaryContainer = EmeraldPrimaryContainerLight,
                onPrimaryContainer = EmeraldOnPrimaryContainerLight,
                secondary = EmeraldSecondaryLight,
                secondaryContainer = EmeraldSecondaryContainerLight,
                onSecondaryContainer = EmeraldOnSecondaryContainerLight
            )
        }
        AppAccentPalette.MIDNIGHT_GOLD -> if (darkTheme) {
            baseScheme.copy(
                primary = GoldPrimaryDark,
                onPrimary = GoldOnPrimaryDark,
                primaryContainer = GoldPrimaryContainerDark,
                onPrimaryContainer = GoldOnPrimaryContainerDark,
                secondary = GoldSecondaryDark,
                secondaryContainer = GoldSecondaryContainerDark
            )
        } else {
            baseScheme.copy(
                primary = GoldPrimaryLight,
                primaryContainer = GoldPrimaryContainerLight,
                onPrimaryContainer = GoldOnPrimaryContainerLight,
                secondary = GoldSecondaryLight,
                secondaryContainer = GoldSecondaryContainerLight,
                onSecondaryContainer = GoldOnSecondaryContainerLight
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HabitFlowTypography,
        shapes = HabitFlowShapes,
        content = content
    )
}
