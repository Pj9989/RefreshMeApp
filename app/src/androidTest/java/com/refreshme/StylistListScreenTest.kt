package com.refreshme

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.refreshme.data.Stylist
import com.refreshme.ui.theme.RefreshMeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StylistListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeStylists = listOf(
        Stylist(id = "1", name = "Alice"),
        Stylist(id = "2", name = "Bob")
    )

    @Test
    fun testStylistListScreen_displaysStylists() {
        composeTestRule.setContent {
            RefreshMeTheme {
                StylistListScreen(
                    stylists = fakeStylists,
                    isLoading = false,
                    error = null,
                    searchQuery = "",
                    currentRating = 0f,
                    currentPrice = 1000f,
                    isMobileOnly = false,
                    userLocation = null,
                    onSearchQueryChange = {},
                    onStylistClick = {},
                    onRefresh = {},
                    onRequestLocation = {},
                    onApplyFilters = { _, _, _ -> }
                )
            }
        }

        // Assert that the stylists are displayed
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test
    fun testStylistListScreen_searchFunctionality() {
        composeTestRule.setContent {
            RefreshMeTheme {
                val searchQuery = remember { mutableStateOf("") }
                val stylists = if (searchQuery.value.isBlank()) {
                    fakeStylists
                } else {
                    fakeStylists.filter { it.name.contains(searchQuery.value, ignoreCase = true) }
                }

                StylistListScreen(
                    stylists = stylists,
                    isLoading = false,
                    error = null,
                    searchQuery = searchQuery.value,
                    currentRating = 0f,
                    currentPrice = 1000f,
                    isMobileOnly = false,
                    userLocation = null,
                    onSearchQueryChange = { searchQuery.value = it },
                    onStylistClick = {},
                    onRefresh = {},
                    onRequestLocation = {},
                    onApplyFilters = { _, _, _ -> }
                )
            }
        }

        // Perform a search
        composeTestRule.onNodeWithText("Search by name or specialty").performTextInput("Alice")

        // Assert that only the searched stylist is displayed
        composeTestRule.onNodeWithText("Alice").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob").assertDoesNotExist()
    }
}