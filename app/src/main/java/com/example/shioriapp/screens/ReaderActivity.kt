package com.example.shioriapp.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class ReaderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Volvemos inmersiva SOLO esta ventana
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        // Recibimos el nombre de la fuente
        val sourceName = intent.getStringExtra("sourceName") ?: ""

        setContent {
            // Llamamos a tu lector. El botón onBack simplemente destruye esta ventana.
            ReaderScreen(
                sourceName = sourceName,
                onBack = { finish() }
            )
        }
    }
}