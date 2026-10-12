package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MedicalPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = MedicalDarkBlue,
    onPrimaryContainer = Color.White,
    secondary = MedicalGreen,
    onSecondary = Color.White,
    background = MedicalDarkBackground,
    onBackground = MedicalDarkTextPrimary,
    surface = MedicalDarkSurface,
    onSurface = MedicalDarkTextPrimary,
    surfaceVariant = Color(0xFF1E2D3D),
    onSurfaceVariant = MedicalDarkTextSecondary,
    outline = Color(0xFF334155),
    error = MedicalError
)

private val LightColorScheme = lightColorScheme(
    primary = MedicalPrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = MedicalLightBlue,
    onPrimaryContainer = MedicalDarkBlue,
    secondary = MedicalGreen,
    onSecondary = Color.White,
    secondaryContainer = MedicalLightGreen,
    onSecondaryContainer = MedicalDarkBlue,
    tertiary = MedicalDarkBlue,
    onTertiary = Color.White,
    background = MedicalBackground,
    onBackground = MedicalTextPrimary,
    surface = MedicalSurface,
    onSurface = MedicalTextPrimary,
    surfaceVariant = Color(0xFFEDF2F7),
    onSurfaceVariant = MedicalTextSecondary,
    outline = MedicalBorder,
    error = MedicalError
)

@Composable
fun DalilikAlTibbiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
