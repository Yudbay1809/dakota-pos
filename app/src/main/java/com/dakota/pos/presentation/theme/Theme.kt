package com.dakota.pos.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CoffeeCaramel,
    onPrimary = CoffeeEspressoDark,
    primaryContainer = CoffeeWarmBrown,
    onPrimaryContainer = CoffeeCream,
    secondary = CoffeeLatte,
    onSecondary = CoffeeEspressoDark,
    background = PosBackgroundDark,
    onBackground = PosTextPrimaryDark,
    surface = PosSurfaceDark,
    onSurface = PosTextPrimaryDark,
    surfaceVariant = PosSurfaceElevatedDark,
    onSurfaceVariant = PosTextSecondaryDark,
    outline = PosBorderDark,
    error = StatusError
)

private val LightColorScheme = lightColorScheme(
    primary = CoffeeWarmBrown,
    onPrimary = CoffeeCream,
    primaryContainer = CoffeeLatte,
    onPrimaryContainer = CoffeeEspressoDark,
    secondary = CoffeeCaramel,
    onSecondary = CoffeeEspressoDark,
    background = CoffeeCream,
    onBackground = CoffeeEspressoDark,
    surface = Color(0xFFFAF6F0),
    onSurface = CoffeeEspressoDark,
    outline = CoffeeLatte,
    error = StatusError
)

@Composable
fun DakotaTheme(
    darkTheme: Boolean = true, // Default to sleek dark mode for POS devices
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
