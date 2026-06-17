package com.example.shioriapp.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsScreen_muestraSeccionesYReaccionaAApariencia() {
        var navegóAApariencia = false

        composeTestRule.setContent {
            MaterialTheme {
                // 🔥 SOLUCIÓN: Obligamos al lienzo de prueba a expandirse a toda la pantalla
                Surface(modifier = Modifier.fillMaxSize()) {
                    SettingsScreen(
                        onBack = {},
                        onNavigateToAppearance = { navegóAApariencia = true }
                    )
                }
            }
        }

        // 🔥 SOLUCIÓN: Usamos "onAllNodesWithText().onFirst()"
        // Esto es a prueba de balas contra fusiones de texto en Compose.
        composeTestRule.onAllNodesWithText("General", substring = true, ignoreCase = true)
            .onFirst()
            .assertExists()

        composeTestRule.onAllNodesWithText("Lector", substring = true, ignoreCase = true)
            .onFirst()
            .performScrollTo()
            .assertExists()

        composeTestRule.onAllNodesWithText("Avanzado", substring = true, ignoreCase = true)
            .onFirst()
            .performScrollTo()
            .assertExists()

        // Interactuar con el botón "Apariencia"
        composeTestRule.onAllNodesWithText("Apariencia", substring = true, ignoreCase = true)
            .onFirst()
            .performScrollTo()
            .performClick()

        // Verificamos que el clic fue exitoso
        assertTrue(navegóAApariencia)
    }
}