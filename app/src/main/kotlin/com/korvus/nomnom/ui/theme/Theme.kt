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
    primary = TerracottaPrimary,
    onPrimary = Color(0xFFFFF6E8),
    primaryContainer = TerracottaSoft,
    onPrimaryContainer = Color(0xFF3A1505),

    secondary = MustardGold,
    onSecondary = Color(0xFFFFF6E8),
    secondaryContainer = HoneyContainer,
    onSecondaryContainer = Color(0xFF3D2A0F),

    tertiary = AubergineTertiary,
    onTertiary = Color(0xFFFFF6E8),

    background = IvoryBg,
    onBackground = CoffeeOnBg,

    surface = IvorySurface,
    onSurface = CoffeeOnBg,
    surfaceVariant = SandVariant,
    onSurfaceVariant = TaupeMuted,

    outline = ClayOutline,
    outlineVariant = Color(0xFFD8C9B2),

    error = BurntError,
    onError = OnError,
    errorContainer = RoseErrorContainer,
    onErrorContainer = Color(0xFF3A1505),

    scrim = Color(0xFF1F1308),
    inverseSurface = Color(0xFF2A1F15),
    inverseOnSurface = Color(0xFFF5EBDA),
)

private val DarkScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = CoffeeOnPrimaryDark,
    primaryContainer = BurntOrangeContainer,
    onPrimaryContainer = Color(0xFFFFD9B8),

    secondary = HoneyDark,
    onSecondary = CoffeeOnPrimaryDark,
    secondaryContainer = HoneyContainerDark,
    onSecondaryContainer = Color(0xFFF0DCAE),

    tertiary = Color(0xFFE6A8A8),
    onTertiary = Color(0xFF3A1515),

    background = EspressoBg,
    onBackground = CreamOnBg,

    surface = EspressoSurface,
    onSurface = CreamOnBg,
    surfaceVariant = EspressoSurfaceVar,
    onSurfaceVariant = SandOnVariant,

    outline = Color(0xFF6E5B43),
    outlineVariant = Color(0xFF3D2D20),

    error = Color(0xFFFFB59A),
    onError = Color(0xFF3A1505),
    errorContainer = Color(0xFF6B2A12),
    onErrorContainer = Color(0xFFFFD9C8),

    scrim = Color.Black,
    inverseSurface = Color(0xFFF0E4D2),
    inverseOnSurface = Color(0xFF1A120B),
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
