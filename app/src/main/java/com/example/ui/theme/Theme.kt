package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GasCyanLight,
    onPrimary = EnergyNavyDark,
    primaryContainer = GasCyanDark,
    onPrimaryContainer = Color.White,
    secondary = ElectricityAmberLight,
    onSecondary = EnergyNavyDark,
    secondaryContainer = ElectricityAmberDark,
    onSecondaryContainer = Color.White,
    tertiary = CostGreen,
    background = EnergyNavyDark,
    surface = EnergySlateDark,
    surfaceVariant = EnergyCardDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = GasCyan,
    onPrimary = Color.White,
    primaryContainer = GasCyanContainer,
    onPrimaryContainer = GasCyanDark,
    secondary = ElectricityAmberDark,
    onSecondary = Color.White,
    secondaryContainer = ElectricityAmberContainer,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = CostGreen,
    background = SurfaceLight,
    surface = SurfaceCardLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our branded theme for consistent energy identity
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
