package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val PitchPulseColorScheme = darkColorScheme(
    primary = PitchEmerald,
    onPrimary = PitchObsidian,
    primaryContainer = PitchEmeraldContainer,
    onPrimaryContainer = PitchEmerald,
    secondary = MatchdayGold,
    onSecondary = PitchObsidian,
    secondaryContainer = MatchdayGoldContainer,
    onSecondaryContainer = MatchdayGold,
    tertiary = InfoCyan,
    onTertiary = PitchObsidian,
    background = PitchObsidian,
    onBackground = ChalkWhite,
    surface = PitchSurface,
    onSurface = ChalkWhite,
    surfaceVariant = PitchSurfaceElevated,
    onSurfaceVariant = SlateMuted,
    surfaceContainerLowest = PitchObsidian,
    surfaceContainerLow = PitchSurface,
    surfaceContainer = PitchSurfaceElevated,
    surfaceContainerHigh = PitchSurfaceHighlight,
    outline = PitchBorder,
    outlineVariant = PitchBorder,
    error = LiveCrimson,
    onError = ChalkWhite,
    errorContainer = LiveCrimsonContainer,
    onErrorContainer = LiveCrimson
)

private val PitchPulseShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun PitchPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PitchPulseColorScheme,
        typography = PitchPulseTypography,
        shapes = PitchPulseShapes,
        content = content
    )
}
