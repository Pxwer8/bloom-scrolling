package com.hackwestx.bloomscrolling.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Named scale — reference these (or MaterialTheme.typography.* below) from
// composables, never a raw sp value.
val Display = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp)
val HeadingH1 = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp)
val HeadingH2 = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp)
val HeadingH3 = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
val BodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp)
val BodyRegular = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp)
val BodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)
val Label = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp)
val Caption = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)

// Mapped into Material3's slots so MaterialTheme.typography.* also works.
val Typography = Typography(
    displayLarge = Display,
    displayMedium = Display,
    displaySmall = HeadingH1,
    headlineLarge = HeadingH1,
    headlineMedium = HeadingH1,
    headlineSmall = HeadingH2,
    titleLarge = HeadingH2,
    titleMedium = HeadingH3,
    titleSmall = HeadingH3,
    bodyLarge = BodyLarge,
    bodyMedium = BodyRegular,
    bodySmall = BodySmall,
    labelLarge = Label,
    labelMedium = Label,
    labelSmall = Caption
)
