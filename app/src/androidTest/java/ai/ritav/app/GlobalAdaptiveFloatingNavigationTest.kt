package ai.ritav.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        completeOnboardingIfRequired()

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
    @Test
    fun emergency_stop_recovery_requires_explicit_resume_and_fresh_authentication() {
        completeOnboardingIfRequired()

        composeRule.onNodeWithText("Emergency Stop").performClick()
        composeRule.onNodeWithText("Protected actions are blocked.").assertIsDisplayed()

        composeRule.onNodeWithText("Security").performClick()
        composeRule.onNodeWithText("Resume Ritav").assertIsDisplayed()
        composeRule.onNodeWithText("Resume Ritav").performClick()

        composeRule.onNodeWithText(
            "Ritav resumed. Protected actions require fresh authentication."
        ).assertIsDisplayed()
        composeRule.onNodeWithText("Authenticate protected actions").assertIsDisplayed()
    }

    private fun completeOnboardingIfRequired() {
        if (composeRule.onAllNodesWithText("Continue to Ritav").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Continue to Ritav")
                .performScrollTo()
                .performClick()
        }
    }
}
