package ai.ritav.app

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class RitavOnboardingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onboarding_shows_security_first_controls_before_continue() {
        composeRule.setContent {
            RitavOnboardingScreen(onContinue = {})
        }

        composeRule.onNodeWithContentDescription("Ritav.ai app identity mark").assertExists()
        composeRule.onNodeWithText("Welcome to Ritav.ai").assertExists()
        composeRule.onNodeWithText("Your control comes first.").assertExists()
        composeRule.onNodeWithText("Continue to Ritav").assertExists()
    }
}
