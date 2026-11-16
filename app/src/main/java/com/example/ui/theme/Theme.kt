package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalAppDarkTheme = staticCompositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = EduTealLight,
    onPrimary = Color(0xFF091B36),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF042F2E),
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = Color(0xFF818CF8),
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = Color(0xFF312E81),
    onTertiaryContainer = Color(0xFFE0E7FF),
    background = EduDarkBg,
    onBackground = Color(0xFFF1F5F9),
    surface = EduDarkSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = EduDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1),
    surfaceTint = EduTealLight,
    surfaceContainerLowest = Color(0xFF090E1A),
    surfaceContainerLow = Color(0xFF131C2E),
    surfaceContainer = EduDarkSurface,
    surfaceContainerHigh = Color(0xFF1E293B),
    surfaceContainerHighest = Color(0xFF273549),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFF334155),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
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
    onBackground = Color(0xFF0F172A),
    surface = EduLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE8EEF5),
    onSurfaceVariant = Color(0xFF475569),
    surfaceTint = EduTealPrimary,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFC),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    surfaceContainerHighest = Color(0xFFCBD5E1),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFCBD5E1),
    error = EduErrorRed,
    onError = Color.White,
    errorContainer = EduErrorContainer,
    onErrorContainer = Color(0xFF991B1B)
)

@Composable
@ReadOnlyComposable
fun eduSuccessContainerColor(): Color =
    if (LocalAppDarkTheme.current) EduSuccessGreen.copy(alpha = 0.22f) else EduSuccessContainer

@Composable
@ReadOnlyComposable
fun eduSuccessContentColor(): Color =
    if (LocalAppDarkTheme.current) Color(0xFF4ADE80) else EduSuccessGreen

@Composable
@ReadOnlyComposable
fun eduWarningContainerColor(): Color =
    if (LocalAppDarkTheme.current) EduWarningAmber.copy(alpha = 0.22f) else EduWarningContainer

@Composable
@ReadOnlyComposable
fun eduWarningContentColor(): Color =
    if (LocalAppDarkTheme.current) Color(0xFFFBBF24) else EduWarningAmber

@Composable
@ReadOnlyComposable
fun eduErrorContainerColor(): Color =
    if (LocalAppDarkTheme.current) EduErrorRed.copy(alpha = 0.24f) else EduErrorContainer

@Composable
@ReadOnlyComposable
fun eduErrorContentColor(): Color =
    if (LocalAppDarkTheme.current) Color(0xFFF87171) else EduErrorRed

@Composable
@ReadOnlyComposable
fun eduSkyContainerColor(): Color =
    if (LocalAppDarkTheme.current) EduSkyTertiary.copy(alpha = 0.24f) else EduSkyContainer

@Composable
@ReadOnlyComposable
fun eduWhatsAppTextColor(): Color =
    if (LocalAppDarkTheme.current) Color(0xFF4ADE80) else Color(0xFF15803D)

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

    CompositionLocalProvider(LocalAppDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
