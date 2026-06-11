package com.example.shioriapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta Monocromática Ultra-Limpia
 * Inspirada en estética minimalista, blanco sobre negro absoluto.
 */

// Colores Base de Sistema
val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF000000)

val DarkBackground = Color(0xFF000000) // Negro puro (OLED)
val DarkSurface = Color(0xFF000000)
val DarkSurfaceVariant = Color(0xFF121212)
val DarkOnSurface = Color(0xFFFFFFFF)

// Variaciones de Gris para Jerarquía Visual
val NeutralGris = Color(0xFF9E9E9E)
val LightGris = Color(0xFFE0E0E0)
val DarkGris = Color(0xFF424242)

// Translucidez Estándar (Glassmorphism)
val GlassWhite = Color.White.copy(alpha = 0.7f)
val GlassBlack = Color.Black.copy(alpha = 0.7f)
val UltraGlassWhite = Color.White.copy(alpha = 0.1f)
val UltraGlassBlack = Color.Black.copy(alpha = 0.1f)
