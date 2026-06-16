package com.example.shioriapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color.White.copy(alpha = 0.1f),
    onPrimaryContainer = Color.White,
    
    secondary = Color.White,
    onSecondary = Color.Black,
    
    background = DarkBackground,
    onBackground = Color.White,
    
    surface = DarkSurface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF121212),
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    
    outline = Color(0xFF222222),
    outlineVariant = Color(0xFF333333),
    error = Color(0xFFCF6679)
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color.Black.copy(alpha = 0.05f),
    onPrimaryContainer = Color.Black,
    
    secondary = Color.Black,
    onSecondary = Color.White,
    
    background = Color(0xFFFFFFFF),
    onBackground = Color.Black,
    
    surface = Color(0xFFFFFFFF),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color.Black.copy(alpha = 0.7f),
    
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFF5F5F5),
    error = Color(0xFFB00020)
)

@Composable
fun ShioriAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
