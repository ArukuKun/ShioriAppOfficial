package com.example.shioriapp.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material3.MaterialTheme
import org.junit.Rule
import org.junit.Test

class AppearanceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun appearanceScreen_muestraOpcionesDeTemaYAlternaAmoled() {
        composeTestRule.setContent {
            MaterialTheme {
                AppearanceScreen(onBack = {})
            }
        }

        composeTestRule.onNodeWithText("Predeterminado del sistema").assertIsDisplayed()
        composeTestRule.onNodeWithText("Claro").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oscuro").assertIsDisplayed()

        composeTestRule.onNodeWithText("Negro puro (AMOLED)").assertIsDisplayed()

        composeTestRule.onNodeWithText("Claro").performClick()
        composeTestRule.onNodeWithText("Claro").assertIsSelected()

        composeTestRule.onNodeWithText("Negro puro (AMOLED)").performClick()
    }
}