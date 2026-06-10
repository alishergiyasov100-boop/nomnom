package com.korvus.nomnom.ui.theme

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

private val LightScheme = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = White,
    primaryContainer = MintContainer,
    onPrimaryContainer = MintOn,

    secondary = OliveSecondary,
    onSecondary = White,
    secondaryContainer = OliveContainer,
    onSecondaryContainer = Color(0xFF1F2D11),

    tertiary = GoldenAccent,
    onTertiary = Color(0xFF2A1F00),

    background = White,
    onBackground = Charcoal,

    surface = White,
    onSurface = Charcoal,
    surfaceVariant = OffWhite,
    onSurfaceVariant = MutedText,

    outline = Hairline,
    outlineVariant = Color(0xFFEDEDEA),

    error = CrimsonError,
    onError = White,
    errorContainer = RoseErrorContainer,
    onErrorContainer = Color(0xFF410E0B),

    scrim = Color(0xFF000000),
    inverseSurface = Charcoal,
    inverseOnSurface = White,
)

private val DarkScheme = darkColorScheme(
    primary = MintPrimaryDark,
    onPrimary = Color(0xFF0E1410),
    primaryContainer = ForestContDark,
    onPrimaryContainer = Color(0xFFC8E6D5),

    secondary = Color(0xFFAFC79B),
    onSecondary = Color(0xFF0E1410),
    secondaryContainer = Color(0xFF2D3D24),
    onSecondaryContainer = Color(0xFFD7E5C8),

    tertiary = GoldenAccent,
    onTertiary = Color(0xFF2A1F00),

    background = InkBg,
    onBackground = CreamOn,

    surface = InkSurface,
    onSurface = CreamOn,
    surfaceVariant = InkSurfaceVar,
    onSurfaceVariant = MutedTextDark,

    outline = Color(0xFF2E3A33),
    outlineVariant = Color(0xFF1F2820),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    scrim = Color.Black,
    inverseSurface = CreamOn,
    inverseOnSurface = InkBg,
)

@Composable
fun NomNomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = scheme.background.toArgb()
            window.navigationBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
