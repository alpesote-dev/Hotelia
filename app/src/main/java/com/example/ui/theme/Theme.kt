package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = BrassSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrassContainer,
    onSecondaryContainer = OnBrassContainer,
    tertiary = TerracottaTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracottaContainer,
    background = IvoryBackground,
    onBackground = CharcoalInk,
    surface = AlabasterSurface,
    onSurface = CharcoalInk,
    surfaceVariant = TravertineSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLowest = LinenCard,
    surfaceContainerLow = Color(0xFFF6F2EA),
    surfaceContainer = Color(0xFFF0EBE0),
    surfaceContainerHigh = Color(0xFFEAE3D5),
    outline = SandstoneOutline,
    outlineVariant = Color(0xFFE5DEC9),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkOnEmerald,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = DarkOnEmeraldContainer,
    secondary = DarkBrassSecondary,
    onSecondary = DarkOnBrass,
    secondaryContainer = DarkBrassContainer,
    onSecondaryContainer = DarkOnBrassContainer,
    tertiary = DarkTerracotta,
    onTertiary = DarkOnTerracotta,
    tertiaryContainer = DarkTerracottaContainer,
    onTertiaryContainer = DarkOnTerracottaContainer,
    background = ObsidianBackground,
    onBackground = IvoryText,
    surface = ObsidianSurface,
    onSurface = IvoryText,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = MutedStone,
    surfaceContainerLowest = Color(0xFF131917),
    surfaceContainerLow = Color(0xFF1B2220),
    surfaceContainer = Color(0xFF202826),
    surfaceContainerHigh = Color(0xFF2A3330),
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF2E3734),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

val HoteliaShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HoteliaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HoteliaTypography,
        shapes = HoteliaShapes,
        content = content
    )
}
