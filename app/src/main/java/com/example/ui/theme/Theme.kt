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
    primary = EduTealLight,
    onPrimary = Color.Black,
    primaryContainer = EduTealDark,
    onPrimaryContainer = EduTealContainer,
    secondary = Color(0xFF90A4AE),
    onSecondary = Color.Black,
    secondaryContainer = EduSlateDark,
    onSecondaryContainer = Color(0xFFECEFF1),
    tertiary = Color(0xFF81D4FA),
    onTertiary = Color.Black,
    background = EduDarkBg,
    onBackground = Color(0xFFE2E8F0),
    surface = EduDarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = EduDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = Color(0xFFEF9A9A),
    onError = Color.Black,
    errorContainer = Color(0xFFB71C1C),
    onErrorContainer = Color(0xFFFFCDD2)
)

private val LightColorScheme = lightColorScheme(
    primary = EduTealPrimary,
    onPrimary = Color.White,
    primaryContainer = EduTealContainer,
    onPrimaryContainer = EduOnTealContainer,
    secondary = EduSlateSecondary,
    onSecondary = Color.White,
    secondaryContainer = EduSlateContainer,
    onSecondaryContainer = EduOnSlateContainer,
    tertiary = EduSkyTertiary,
    onTertiary = Color.White,
    tertiaryContainer = EduSkyContainer,
    onTertiaryContainer = EduOnSkyContainer,
    background = EduLightBg,
    onBackground = Color(0xFF1E293B),
    surface = EduLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    error = EduErrorRed,
    onError = Color.White,
    errorContainer = EduErrorContainer,
    onErrorContainer = EduErrorRed
)

@Composable
fun EduGroupsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep educational brand colors consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
