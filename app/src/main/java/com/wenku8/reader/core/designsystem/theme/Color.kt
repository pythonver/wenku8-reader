package com.wenku8.reader.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ===== Brand — cool blue / cyan (evolved from the original #4284F5) =====
val BrandBlue = Color(0xFF3B6EF6)
val BrandBlueDark = Color(0xFF7C9CFF)
val BrandCyan = Color(0xFF00B8D4)

// ===== Neutrals (light) =====
val NeutralBgLight = Color(0xFFFAFAFA)
val NeutralSurfaceLight = Color(0xFFFFFFFF)
val NeutralTextLight = Color(0xFF17181A)
val NeutralTextSecondaryLight = Color(0xFF5A5D63)
val NeutralTextDisabledLight = Color(0xFF9A9DA3)

// ===== Neutrals (dark) =====
val NeutralBgDark = Color(0xFF0F1115)
val NeutralSurfaceDark = Color(0xFF17191D)
val NeutralTextDark = Color(0xFFE8EAED)
val NeutralTextSecondaryDark = Color(0xFFA2A6AD)
val NeutralTextDisabledDark = Color(0xFF5F6368)

// ===== Semantic =====
val SuccessColor = Color(0xFF2E9E5B)
val WarningColor = Color(0xFFE6A23C)
val ErrorColor = Color(0xFFE05252)
val InfoColor = Color(0xFF3B6EF6)

// ===== Reader four-color mode (independent of app theme) =====
val ReaderDayBg = Color(0xFFF7F6F2)
val ReaderDayText = Color(0xFF202124)
val ReaderNightBg = Color(0xFF121212)
val ReaderNightText = Color(0xFFB9B9B9)
val ReaderSepiaBg = Color(0xFFCFBEB6)
val ReaderSepiaText = Color(0xFF4A4038)
val ReaderGreenBg = Color(0xFFC7EDCC)
val ReaderGreenText = Color(0xFF2D3B2F)

// ===== Material 3 schemes (dynamic color intentionally off — brand stays ours) =====
val LightColors = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE4FF),
    onPrimaryContainer = Color(0xFF10224A),
    secondary = BrandCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2EBF2),
    onSecondaryContainer = Color(0xFF00363F),
    tertiary = Color(0xFF5C8A7E),
    onTertiary = Color.White,
    background = NeutralBgLight,
    onBackground = NeutralTextLight,
    surface = NeutralSurfaceLight,
    onSurface = NeutralTextLight,
    surfaceVariant = Color(0xFFF0F1F4),
    onSurfaceVariant = NeutralTextSecondaryLight,
    surfaceTint = BrandBlue,
    error = ErrorColor,
    onError = Color.White,
    outline = Color(0xFFDADCE0),
    outlineVariant = Color(0xFFE8E9EC),
)

val DarkColors = darkColorScheme(
    primary = BrandBlueDark,
    onPrimary = Color(0xFF0A1B3E),
    primaryContainer = Color(0xFF27458F),
    onPrimaryContainer = Color(0xFFDCE4FF),
    secondary = BrandCyan,
    onSecondary = Color(0xFF00363F),
    secondaryContainer = Color(0xFF00505C),
    onSecondaryContainer = Color(0xFFB2EBF2),
    tertiary = Color(0xFF8BBDAF),
    onTertiary = Color(0xFF07372C),
    background = NeutralBgDark,
    onBackground = NeutralTextDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralTextDark,
    surfaceVariant = Color(0xFF22252B),
    onSurfaceVariant = NeutralTextSecondaryDark,
    surfaceTint = BrandBlueDark,
    error = Color(0xFFF07171),
    onError = Color(0xFF3B0A0A),
    outline = Color(0xFF3A3D42),
    outlineVariant = Color(0xFF2A2D32),
)
