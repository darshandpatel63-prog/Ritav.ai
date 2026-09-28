package ai.ritav.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class GlobalAdaptiveFloatingNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun navigation_opens_with_active_item_and_circular_selection() {
        val toggle = composeRule.onNodeWithContentDescription("Ritav global navigation, movable")

        toggle.assertIsDisplayed()
        toggle.performClick()
        composeRule.onNodeWithContentDescription("Security").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Security").performClick()
        toggle.performClick()
        composeRule.onNodeWithContentDescription("Security, selected").assertIsDisplayed()

        toggle.performClick()
        composeRule.onNodeWithContentDescription("Home").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        toggle.performClick()
        composeRule.onNodeWithContentDescription("Settings, selected").assertIsDisplayed()
    }
}
