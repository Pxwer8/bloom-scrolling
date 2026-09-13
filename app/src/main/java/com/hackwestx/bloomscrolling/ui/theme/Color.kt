package com.hackwestx.bloomscrolling.ui.theme

import androidx.compose.ui.graphics.Color

// Primitives — periwinkle ramp
val Periwinkle100 = Color(0xFFF4F8FF)
val Periwinkle200 = Color(0xFFDCEAFD)
val Periwinkle300 = Color(0xFFAFCBF8)
val Periwinkle400 = Color(0xFF8CA6F2)
val Periwinkle500 = Color(0xFF7B84EF)

// Neutrals
val Neutral0   = Color(0xFFFFFFFF)
val Neutral100 = Color(0xFFF6F7FB)
val Neutral300 = Color(0xFFE3E6EF)
val Neutral500 = Color(0xFF8A8FA3)
val Neutral700 = Color(0xFF4B4F63)
val Neutral900 = Color(0xFF1C1E2A)

// Status
val StatusGreen = Color(0xFF3FB88A)  // time saved / positive
val StatusRed   = Color(0xFFE2685E)  // relapse / negative

// Semantic roles (use these names in the app, not the primitives directly)
val ColorBg            = Neutral100
val ColorSurface       = Neutral0
val ColorSurfaceAlt    = Periwinkle100
val ColorBorder        = Neutral300
val ColorTextPrimary   = Neutral900
val ColorTextSecondary = Neutral500
val ColorTextOnAccent  = Neutral0
val ColorAccent        = Periwinkle500
val ColorAccentSoft    = Periwinkle300
val ColorSplashBg      = Periwinkle400
val ColorPositive      = StatusGreen
val ColorNegative      = StatusRed
