package com.shop.billing.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    background = Color(0xFFfdfcff),
    onBackground = Color(0xFF1a1c1e),
    surface = Color(0xFFfdfcff),
    onSurface = Color(0xFF1a1c1e),
    surfaceVariant = Color(0xFFdfe2eb),
    onSurfaceVariant = Color(0xFF43474e),
    outline = Color(0xFF73777f),
    outlineVariant = Color(0xFFc3c7cf),
    inverseSurface = Color(0xFF2f3033),
    inverseOnSurface = Color(0xFFf1f0f4),
    inversePrimary = BlueLight
)

@Composable
fun BillingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
