package com.agent.s25.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Pure AMOLED Black theme optimized for Samsung Galaxy S25 Ultra Dynamic AMOLED 2X
val AmoledBackground = Color(0xFF000000)
val AmoledSurface = Color(0xFF121218)
val AmoledSurfaceVariant = Color(0xFF1A1A24)
val AmoledSurfaceElevated = Color(0xFF232332)
val AmoledGlassBorder = Color(0x26FFFFFF)
val AmoledGlassBorderActive = Color(0x556366F1)
val AmoledGlassBorderCyan = Color(0x5538BDF8)

// Meta Muse Signature Accents (Alex Cornell Spec)
val ElectricIndigo = Color(0xFF6366F1)
val ElectricIndigoDark = Color(0xFF4F46E5)
val ElectricIndigoGlow = Color(0x406366F1)

val SoftCyan = Color(0xFF38BDF8)
val SoftCyanDark = Color(0xFF0284C7)
val SoftCyanGlow = Color(0x3338BDF8)

val AmberIndicGold = Color(0xFFF59E0B)
val AmberIndicGoldGlow = Color(0x33F59E0B)

val EmeraldSafeGreen = Color(0xFF10B981)
val EmeraldSafeGreenGlow = Color(0x3310B981)

val DangerRed = Color(0xFFEF4444)
val DangerRedGlow = Color(0x33EF4444)

// Text Hierarchy
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF94A3B8)
val TextMuted = Color(0xFF64748B)

// Backward compatibility mappings
val PrimaryAccent = ElectricIndigo
val PrimaryAccentVariant = SoftCyan
val IndicGold = AmberIndicGold
val SafeGreen = EmeraldSafeGreen
val WarningRed = DangerRed

// Meta Muse Gradients
val MuseElectricGradient = Brush.horizontalGradient(
    colors = listOf(ElectricIndigo, SoftCyan)
)

val MuseHaloGradient = Brush.radialGradient(
    colors = listOf(
        ElectricIndigo.copy(alpha = 0.5f),
        SoftCyan.copy(alpha = 0.25f),
        Color.Transparent
    )
)

val MuseCardGlassGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0x2AFFFFFF),
        Color(0x0AFFFFFF)
    )
)
