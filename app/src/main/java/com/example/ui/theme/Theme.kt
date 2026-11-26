package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppVisualStyle(val key: String, val label: String) {
    TERRACOTA("terracota", "Terracota"),
    AZUL("azul", "Azul"),
    APPLE("apple", "Estilo Apple");

    companion object {
        fun fromKey(key: String): AppVisualStyle =
            entries.firstOrNull { it.key == key } ?: TERRACOTA
    }
}

val LocalVisualStyle = staticCompositionLocalOf { AppVisualStyle.TERRACOTA }

private val TerracottaLightColors = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = TerracottaOnPrimary,
    primaryContainer = TerracottaContainer,
    onPrimaryContainer = TerracottaOnContainer,
    secondary = SlateSecondary,
    onSecondary = SlateOnSecondary,
    secondaryContainer = SlateContainer,
    onSecondaryContainer = SlateOnContainer,
    tertiary = EmeraldSuccess,
    onTertiary = EmeraldOnSuccess,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldOnContainer,
    error = CrimsonError,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonOnContainer,
    background = LinenBackground,
    onBackground = EspressoOnSurface,
    surface = WarmWhiteSurface,
    onSurface = EspressoOnSurface,
    surfaceVariant = LinenSurfaceVariant,
    onSurfaceVariant = EspressoMuted,
    outline = HairlineOutline,
)

private val TerracottaDarkColors = darkColorScheme(
    primary = DarkTerracotta,
    onPrimary = TerracottaOnContainer,
    primaryContainer = DarkTerracottaContainer,
    onPrimaryContainer = TerracottaContainer,
    secondary = SlateContainer,
    onSecondary = SlateOnContainer,
    secondaryContainer = SlateSecondary,
    onSecondaryContainer = SlateOnSecondary,
    tertiary = DarkEmerald,
    onTertiary = EmeraldOnContainer,
    tertiaryContainer = DarkEmeraldContainer,
    onTertiaryContainer = EmeraldContainer,
    error = CrimsonContainer,
    errorContainer = CrimsonOnContainer,
    onErrorContainer = CrimsonContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkOutline,
)

private val AzulLightColors = lightColorScheme(
    primary = AzulPrimary,
    onPrimary = Color.White,
    primaryContainer = AzulContainer,
    onPrimaryContainer = AzulOnContainer,
    secondary = AzulSecondary,
    onSecondary = Color.White,
    secondaryContainer = AzulSecondaryContainer,
    onSecondaryContainer = AzulOnSecondaryContainer,
    tertiary = EmeraldSuccess,
    onTertiary = EmeraldOnSuccess,
    tertiaryContainer = EmeraldContainer,
    onTertiaryContainer = EmeraldOnContainer,
    error = CrimsonError,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonOnContainer,
    background = AzulBackground,
    onBackground = AzulOnSurface,
    surface = WarmWhiteSurface,
    onSurface = AzulOnSurface,
    surfaceVariant = AzulSurfaceVariant,
    onSurfaceVariant = AzulMuted,
    outline = AzulOutline,
)

private val AzulDarkColors = darkColorScheme(
    primary = AzulDarkPrimary,
    onPrimary = AzulOnContainer,
    primaryContainer = AzulDarkContainer,
    onPrimaryContainer = AzulContainer,
    secondary = AzulSecondaryContainer,
    onSecondary = AzulOnSecondaryContainer,
    secondaryContainer = AzulSecondary,
    onSecondaryContainer = Color.White,
    tertiary = DarkEmerald,
    onTertiary = EmeraldOnContainer,
    tertiaryContainer = DarkEmeraldContainer,
    onTertiaryContainer = EmeraldContainer,
    error = CrimsonContainer,
    errorContainer = CrimsonOnContainer,
    onErrorContainer = CrimsonContainer,
    background = AzulDarkBackground,
    onBackground = DarkOnSurface,
    surface = AzulDarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = AzulDarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = Color(0xFF34455E),
)

private val AppleLightColors = lightColorScheme(
    primary = AppleBlue,
    onPrimary = Color.White,
    primaryContainer = AppleBlueContainer,
    onPrimaryContainer = AppleBlueOnContainer,
    secondary = AppleSlate,
    onSecondary = Color.White,
    secondaryContainer = AppleSlateContainer,
    onSecondaryContainer = AppleText,
    tertiary = AppleGreen,
    onTertiary = Color.White,
    tertiaryContainer = AppleGreenContainer,
    onTertiaryContainer = EmeraldOnContainer,
    error = CrimsonError,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonOnContainer,
    background = AppleBackground,
    onBackground = AppleText,
    surface = WarmWhiteSurface,
    onSurface = AppleText,
    surfaceVariant = AppleSurfaceVariant,
    onSurfaceVariant = AppleMuted,
    outline = AppleOutline,
)

private val AppleDarkColors = darkColorScheme(
    primary = AppleDarkBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003B75),
    onPrimaryContainer = AppleBlueContainer,
    secondary = AppleSlateContainer,
    onSecondary = AppleText,
    secondaryContainer = AppleDarkSurfaceVariant,
    onSecondaryContainer = Color.White,
    tertiary = AppleDarkGreen,
    onTertiary = Color(0xFF00290C),
    tertiaryContainer = DarkEmeraldContainer,
    onTertiaryContainer = EmeraldContainer,
    error = CrimsonContainer,
    errorContainer = CrimsonOnContainer,
    onErrorContainer = CrimsonContainer,
    background = AppleDarkBackground,
    onBackground = Color(0xFFF5F5F7),
    surface = AppleDarkSurface,
    onSurface = Color(0xFFF5F5F7),
    surfaceVariant = AppleDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFAEAEB2),
    outline = AppleDarkOutline,
)

private val DefaultShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
)

private val AppleShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun MisColchasTheme(
    visualStyle: AppVisualStyle = AppVisualStyle.TERRACOTA,
    darkTheme: Boolean = isSystemInDarkTheme(),
    largeText: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (visualStyle) {
        AppVisualStyle.TERRACOTA -> if (darkTheme) TerracottaDarkColors else TerracottaLightColors
        AppVisualStyle.AZUL -> if (darkTheme) AzulDarkColors else AzulLightColors
        AppVisualStyle.APPLE -> if (darkTheme) AppleDarkColors else AppleLightColors
    }
    val shapes = if (visualStyle == AppVisualStyle.APPLE) AppleShapes else DefaultShapes

    CompositionLocalProvider(LocalVisualStyle provides visualStyle) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildAppTypography(largeText = largeText),
            shapes = shapes,
            content = content,
        )
    }
}
