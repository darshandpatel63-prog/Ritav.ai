package ai.ritav.app

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun permissionCenterStartsWithEmptyTrustedRegistry() {
        composeRule.onNodeWithText("Permission Center").assertExists()
        composeRule
            .onNodeWithText("No trusted external applications are currently configured.")
            .assertExists()
        composeRule
            .onNodeWithText("External actions remain blocked.")
            .assertExists()
    }
}
