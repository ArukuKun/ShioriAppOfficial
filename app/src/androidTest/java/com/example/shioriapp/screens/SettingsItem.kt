package com.example.shioriapp.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class SettingsItemTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun settingsItem_MuestraTituloYSubtituloCorrectamente() {
        composeTestRule.setContent {
            SettingsItem(
                icon = Icons.Default.Language,
                title = "Idioma",
                subtitle = "Español"
            )
        }

        composeTestRule.onNodeWithText("Idioma").assertIsDisplayed()
        composeTestRule.onNodeWithText("Español").assertIsDisplayed()
    }
}