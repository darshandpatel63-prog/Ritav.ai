package ai.ritav.app

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ConversationalHomeInputTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun ime_send_uses_the_fail_closed_send_path_and_keeps_the_composer_available() {
        enterHomeIfOnboardingIsPresent()

        val input = composeRule.onNodeWithContentDescription("Message Ritav input")
        input.performTextInput("hello from the keyboard")
        input.assertIsFocused()
        input.performImeAction()

        composeRule.onNodeWithText("hello from the keyboard").assertIsDisplayed()
        composeRule.onNodeWithText(
            "The conversational UI received your message, but the production model runtime is currently unavailable. No model/provider bypass was attempted."
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Message length 0 of 4096 characters").assertIsDisplayed()
    }

    @Test
    fun input_is_accessibly_bounded_to_the_configured_character_limit() {
        enterHomeIfOnboardingIsPresent()

        val input = composeRule.onNodeWithContentDescription("Message Ritav input")
        input.performTextInput("x".repeat(MAX_CONVERSATION_INPUT_LENGTH + 100))

        composeRule.onNodeWithContentDescription(
            "Message length 4096 of 4096 characters"
        ).assertIsDisplayed()
        input.assertIsFocused()
    }

    private fun enterHomeIfOnboardingIsPresent() {
        if (composeRule.onAllNodesWithText("Continue to Ritav").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("Continue to Ritav")
                .performScrollTo()
                .performClick()
        }
    }
}
