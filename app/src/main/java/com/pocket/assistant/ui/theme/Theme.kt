package com.pocket.assistant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF1B6CFF)
private val BlueDark = Color(0xFF8AB4FF)
private val Teal = Color(0xFF00A37A)

private val LightColors = lightColorScheme(
    primary = Blue,
    secondary = Teal,
    surfaceVariant = Color(0xFFEFF2F7),
)

private val DarkColors = darkColorScheme(
    primary = BlueDark,
    secondary = Teal,
)

@Composable
fun PocketAssistantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
