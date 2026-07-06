package com.shop.billing.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val LightColorScheme = lightColorScheme(
    primary = Blue227ed4,
    onPrimary = Color.White,
    primaryContainer = BlueLight,
    onPrimaryContainer = Color(0xFF001d36),
    secondary = Color(0xFF535f70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFd7e3f7),
    onSecondaryContainer = Color(0xFF101c2b),
    tertiary = Color(0xFF6b5778),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFf2daff),
    onTertiaryContainer = Color(0xFF251431),
    error = Color(0xFFba1a1a),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color.White,
    onBackground = TextPrimary,
    surface = Color.White,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceGray,
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF73777f),
    outlineVariant = Divider,
    inverseSurface = Color(0xFF2f3033),
    inverseOnSurface = Color(0xFFf1f0f4),
    inversePrimary = BlueLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4B9EFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF004A9E),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFFBBC7DB),
    onSecondary = Color(0xFF253140),
    secondaryContainer = Color(0xFF3B4858),
    onSecondaryContainer = Color(0xFFD7E3F7),
    tertiary = Color(0xFFD6BEE3),
    onTertiary = Color(0xFF3B2948),
    tertiaryContainer = Color(0xFF523F5F),
    onTertiaryContainer = Color(0xFFF2DAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = SurfaceDark,
    onBackground = TextOnDark,
    surface = CardDark,
    onSurface = TextOnDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = Color(0xFF5A5A5A),
    outlineVariant = DividerDark,
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF1A1C1E),
    inversePrimary = Blue227ed4
)

@Composable
fun BillingTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val activity = androidx.compose.ui.platform.LocalContext.current as? Activity
    if (activity != null) {
        SideEffect {
            val window = activity.window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
                !darkTheme
            window.navigationBarColor = colorScheme.surface.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
