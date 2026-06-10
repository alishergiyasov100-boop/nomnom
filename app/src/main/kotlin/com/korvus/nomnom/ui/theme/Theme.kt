package com.korvus.nomnom.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightScheme = lightColorScheme(
    primary = MangoPrimary,
    onPrimary = Cream,
    primaryContainer = CreamSoft,
    onPrimaryContainer = BrownDeep,
    secondary = BasilGreen,
    onSecondary = Cream,
    tertiary = TomatoAccent,
    background = Cream,
    onBackground = BrownDeep,
    surface = Cream,
    onSurface = BrownDeep,
    surfaceVariant = CreamSoft,
    onSurfaceVariant = BrownSoft,
    outline = SmokyGrey,
)

private val DarkScheme = darkColorScheme(
    primary = MangoPrimaryDark,
    onPrimary = BrownDeep,
    primaryContainer = MangoDeep,
    onPrimaryContainer = Cream,
    secondary = BasilGreen,
    onSecondary = BrownDeep,
    tertiary = TomatoAccent,
    background = CreamDark,
    onBackground = Cream,
    surface = SurfaceDark,
    onSurface = Cream,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = CreamSoft,
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
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
