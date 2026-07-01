package com.termuxagent.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import android.os.Build

enum class ThemeMode { System, Light, Dark }

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.System }
val LocalDynamicColor = staticCompositionLocalOf { true }

private val DarkColors = darkColorScheme(
    primary = Cyan400,
    onPrimary = Ink900,
    primaryContainer = Ink600,
    onPrimaryContainer = Cyan300,
    secondary = Violet400,
    onSecondary = Ink900,
    secondaryContainer = Ink700,
    onSecondaryContainer = Violet400,
    tertiary = Amber400,
    onTertiary = Ink900,
    tertiaryContainer = Ink700,
    onTertiaryContainer = Amber300,
    background = Ink850,
    onBackground = Paper100,
    surface = Ink800,
    onSurface = Paper100,
    surfaceVariant = Ink700,
    onSurfaceVariant = Paper300,
    surfaceTint = Cyan400,
    inverseSurface = Paper200,
    inverseOnSurface = Ink900,
    error = Rose400,
    onError = Ink900,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Rose400,
    outline = Paper400,
    outlineVariant = Ink500,
    scrim = Ink900,
)

private val LightColors = lightColorScheme(
    primary = Cyan600,
    onPrimary = Color.White,
    primaryContainer = Paper200,
    onPrimaryContainer = Cyan600,
    secondary = Violet400,
    onSecondary = Color.White,
    secondaryContainer = Paper200,
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = Amber500,
    onTertiary = Color.White,
    tertiaryContainer = Paper200,
    onTertiaryContainer = Color(0xFF92400E),
    background = Paper50,
    onBackground = InkTextDark,
    surface = Color.White,
    onSurface = InkTextDark,
    surfaceVariant = Paper200,
    onSurfaceVariant = InkTextSecondaryDark,
    surfaceTint = Cyan600,
    inverseSurface = Ink800,
    inverseOnSurface = Paper100,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    outline = Paper400,
    outlineVariant = Paper300,
    scrim = Ink900,
)

@Composable
fun TermuXagentTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        isDark -> DarkColors
        else -> LightColors
    }

    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalDynamicColor provides dynamicColor
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TermuXagentTypography,
            content = content
        )
    }
}
