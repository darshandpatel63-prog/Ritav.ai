package ai.ritav.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
    fun navigation_opens_and_selection_tracks_current_destination() {
        // Complete onboarding only when this test environment has not completed it already.
        // Onboarding completion is intentionally persisted across launches.
        val continueNode = composeRule.onAllNodesWithText("Continue to Ritav").fetchSemanticsNodes()
        if (continueNode.isNotEmpty()) {
            composeRule.onNodeWithText("Continue to Ritav").performClick()
        }

        val toggle = composeRule.onNodeWithContentDescription("Ritav global navigation, movable")

        toggle.assertIsDisplayed()
        toggle.performClick()
        composeRule.onNodeWithContentDescription("Home, selected").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Security").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Security").performClick()
        composeRule.onNodeWithText("Permission Center").assertIsDisplayed()

        toggle.performClick()
        composeRule.onNodeWithContentDescription("Security, selected").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Settings").performClick()
        composeRule.onNodeWithText("UI & Appearance").assertIsDisplayed()

        toggle.performClick()
        composeRule.onNodeWithContentDescription("Settings, selected").assertIsDisplayed()
    }
}
