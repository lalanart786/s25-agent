package com.agent.s25.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    onPrimary = TextPrimary,
    primaryContainer = ElectricIndigoDark,
    onPrimaryContainer = TextPrimary,
    secondary = SoftCyan,
    onSecondary = AmoledBackground,
    secondaryContainer = AmoledSurfaceVariant,
    onSecondaryContainer = SoftCyan,
    tertiary = AmberIndicGold,
    onTertiary = AmoledBackground,
    background = AmoledBackground,
    onBackground = TextPrimary,
    surface = AmoledSurface,
    onSurface = TextPrimary,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = AmoledGlassBorder,
    outlineVariant = AmoledGlassBorderActive,
    error = DangerRed,
    onError = TextPrimary
)

val MuseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun S25AgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        shapes = MuseShapes,
        content = content
    )
}
