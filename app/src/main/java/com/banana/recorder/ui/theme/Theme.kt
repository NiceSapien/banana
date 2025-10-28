package com.banana.recorder.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = BananaPrimary,
    onPrimary = BananaOnPrimary,
    primaryContainer = BananaPrimaryContainer,
    onPrimaryContainer = BananaOnPrimaryContainer,
    secondary = BananaSecondary,
    onSecondary = BananaOnSecondary,
    secondaryContainer = BananaSecondaryContainer,
    onSecondaryContainer = BananaOnSecondaryContainer,
    tertiary = BananaTertiary,
    onTertiary = BananaOnTertiary,
    tertiaryContainer = BananaTertiaryContainer,
    onTertiaryContainer = BananaOnTertiaryContainer,
    error = BananaError,
    onError = BananaOnError,
    errorContainer = BananaErrorContainer,
    onErrorContainer = BananaOnErrorContainer,
    background = BananaBackground,
    onBackground = BananaOnBackground,
    surface = BananaSurface,
    onSurface = BananaOnSurface,
    surfaceVariant = BananaSurfaceVariant,
    onSurfaceVariant = BananaOnSurfaceVariant,
    outline = BananaOutline,
    outlineVariant = BananaOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = BananaPrimary,
    onPrimary = BananaOnPrimary,
    primaryContainer = BananaOnPrimaryContainer,
    onPrimaryContainer = BananaPrimaryContainer,
    secondary = BananaSecondary,
    onSecondary = BananaOnSecondary,
    secondaryContainer = BananaOnSecondaryContainer,
    onSecondaryContainer = BananaSecondaryContainer,
    tertiary = BananaTertiary,
    onTertiary = BananaOnTertiary,
    tertiaryContainer = BananaOnTertiaryContainer,
    onTertiaryContainer = BananaTertiaryContainer,
    error = BananaError,
    onError = BananaOnError,
    errorContainer = BananaOnErrorContainer,
    onErrorContainer = BananaErrorContainer,
    background = BananaOnBackground,
    onBackground = BananaBackground,
    surface = BananaOnSurface,
    onSurface = BananaSurface,
    surfaceVariant = BananaOnSurfaceVariant,
    onSurfaceVariant = BananaSurfaceVariant,
    outline = BananaOutline,
    outlineVariant = BananaOutlineVariant
)

@Composable
fun BananaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
