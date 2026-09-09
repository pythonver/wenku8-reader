package com.wenku8.reader.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Reader background modes — independent of the app-level light/dark theme. */
enum class ReaderThemeMode { DAY, NIGHT, SEPIA, GREEN }

/** Colors for the reading surface. The reading body NEVER uses brand colors. */
data class ReaderColors(
    val background: Color,
    val text: Color,
    val secondaryText: Color,
    val divider: Color,
)

val ReaderDayColors = ReaderColors(
    background = ReaderDayBg,
    text = ReaderDayText,
    secondaryText = Color(0xFF5F6368),
    divider = Color(0x14000000),
)

val ReaderNightColors = ReaderColors(
    background = ReaderNightBg,
    text = ReaderNightText,
    secondaryText = Color(0xFF7A7E85),
    divider = Color(0x1FFFFFFF),
)

val ReaderSepiaColors = ReaderColors(
    background = ReaderSepiaBg,
    text = ReaderSepiaText,
    secondaryText = Color(0xFF8A7568),
    divider = Color(0x14000000),
)

val ReaderGreenColors = ReaderColors(
    background = ReaderGreenBg,
    text = ReaderGreenText,
    secondaryText = Color(0xFF5C7A60),
    divider = Color(0x14000000),
)

val LocalReaderColors = staticCompositionLocalOf { ReaderDayColors }

fun readerColorsFor(mode: ReaderThemeMode): ReaderColors = when (mode) {
    ReaderThemeMode.DAY -> ReaderDayColors
    ReaderThemeMode.NIGHT -> ReaderNightColors
    ReaderThemeMode.SEPIA -> ReaderSepiaColors
    ReaderThemeMode.GREEN -> ReaderGreenColors
}
