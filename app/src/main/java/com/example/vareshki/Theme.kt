package com.example.vareshki

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC6),
    background = Color(0xFFF2F2F2),
    surface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFFE6E0E9), // Светло-серый для светлой темы
    onPrimary = Color(0xFFF2F2F2),
    onSecondary = Color(0xFFF2F2F2),
    onBackground = Color(0xFF121212),
    onSurface = Color(0xFF121212)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC6),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF2C2C2C), // Тёмно-серый для тёмной темы
    onPrimary = Color(0xFF121212),
    onSecondary = Color(0xFF121212),
    onBackground = Color(0xFFF2F2F2),
    onSurface = Color(0xFFF2F2F2)
)

@Composable
fun VareshkiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}