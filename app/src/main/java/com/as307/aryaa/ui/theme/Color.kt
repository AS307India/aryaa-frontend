package com.as307.aryaa.ui.theme

import androidx.compose.ui.graphics.Color

object AryaaColors {
    val Navy = Color(0xFF0A0F1E)         // background base
    val NavyMid = Color(0xFF111827)      // secondary background
    val NavyCard = Color(0xFF161D2F)     // card surface
    val NavyBorder = Color(0xFF1E2A42)   // dividers/borders
    val Saffron = Color(0xFFFF6B1A)      // primary accent — SOS, CTAs
    val Emerald = Color(0xFF10B981)      // success/safe states
    val Crimson = Color(0xFFEF4444)      // danger/SOS active
    val Amber = Color(0xFFF59E0B)        // warning
    val Blue = Color(0xFF3B82F6)         // info
    val White = Color(0xFFFFFFFF)
    val Slate = Color(0xFF8892A4)        // muted text
    val SlateLight = Color(0xFFC4CAD6)   // secondary text

    // Extension properties on Color to get custom dim variants programmatically
    val Color.dim: Color get() = this.copy(alpha = 0.12f)
    val Color.dim15: Color get() = this.copy(alpha = 0.15f)

    // Explicitly named colors as requested
    val SaffronDim: Color get() = Saffron.dim15
    val EmeraldDim: Color get() = Emerald.dim
    val CrimsonDim: Color get() = Crimson.dim
    val AmberDim: Color get() = Amber.dim
    val BlueDim: Color get() = Blue.dim
}
