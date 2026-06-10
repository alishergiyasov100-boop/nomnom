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
    primaryContainer = MintTint,
    onPrimaryContainer = MintOn,

    secondary = FatButter,
    onSecondary = White,
    secondaryContainer = FatTint,
    onSecondaryContainer = Color(0xFF332300),

    tertiary = ProteinCoral,
    onTertiary = White,
    tertiaryContainer = ProteinTint,
    onTertiaryContainer = Color(0xFF3E1109),

    background = White,
    onBackground = Charcoal,

    surface = White,
    onSurface = Charcoal,
    surfaceVariant = Cloud,
    onSurfaceVariant = MutedText,

    outline = Hairline,
    outlineVariant = Color(0xFFEDEDEA),

    error = CrimsonError,
    onError = White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    scrim = Color(0xFF000000),
    inverseSurface = Charcoal,
    inverseOnSurface = White,
)

private val DarkScheme = darkColorScheme(
    primary = MintPrimaryDark,
    onPrimary = InkBg,
    primaryContainer = MintContDark,
    onPrimaryContainer = Color(0xFFCDE7D8),

    secondary = Color(0xFFE5C481),
    onSecondary = InkBg,
    secondaryContainer = Color(0xFF4A3818),
    onSecondaryContainer = Color(0xFFF6E1B5),

    tertiary = Color(0xFFFF9F87),
    onTertiary = InkBg,
    tertiaryContainer = Color(0xFF5C291D),
    onTertiaryContainer = Color(0xFFFFD7CB),

    background = InkBg,
    onBackground = CreamOn,

    surface = InkSurface,
    onSurface = CreamOn,
    surfaceVariant = InkSurfaceVar,
    onSurfaceVariant = MutedTextDark,

    outline = Color(0xFF2E3431),
    outlineVariant = Color(0xFF1F2421),

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
