package com.banana.recorder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val BananaColorScheme = lightColorScheme(
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

@Composable
fun BananaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = BananaColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
