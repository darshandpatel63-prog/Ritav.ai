package ai.ritav.app

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class GlobalAdaptiveFloatingNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigation_opens_with_active_item_and_circular_overflow_rotation() {
        val items = (0 until 9).map { index ->
            GlobalNavItem(
                label = "Item $index",
                glyph = index.toString(),
                selected = index == 0,
                onClick = {}
            )
        }

        composeRule.setContent {
            RitavTheme(
                preferences = UiPreferences(
                    themeMode = UiThemeMode.LIGHT,
                    themeFamily = UiThemeFamily.RITAV_DEFAULT,
                    motionPreference = UiMotionPreference.OFF
                )
            ) {
                GlobalAdaptiveFloatingNavigation(
                    items = items,
                    initialPosition = NavigationPositionPreference(),
                    motionPreference = UiMotionPreference.OFF,
                    onPositionSettled = { _, _ -> }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Ritav global navigation, movable")
            .assertExists()
            .performClick()

        composeRule.onNodeWithContentDescription("Item 0, selected").assertExists().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Item 7").assertExists().assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Item 0, selected")
            .performTouchInput { swipeLeft() }

        composeRule.onNodeWithContentDescription("Item 8").assertExists().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Item 1").assertExists().assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Item 1")
            .performTouchInput { swipeRight() }

        composeRule.onNodeWithContentDescription("Item 0, selected").assertExists().assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Item 0, selected")
            .performTouchInput { swipeRight() }

        composeRule.onNodeWithContentDescription("Item 8").assertExists().assertIsDisplayed()
    }
    @Test
    fun placement_supports_fractional_circular_rotation() {
        val initial = radialPlacement(
            index = 0f,
            count = 8,
            centerX = 200f,
            centerY = 200f,
            radius = 80f,
            itemPx = 48f
        )
        val halfStep = radialPlacement(
            index = -0.5f,
            count = 8,
            centerX = 200f,
            centerY = 200f,
            radius = 80f,
            itemPx = 48f
        )

        assertNotEquals(initial.x, halfStep.x)
        assertNotEquals(initial.y, halfStep.y)
    }

}
