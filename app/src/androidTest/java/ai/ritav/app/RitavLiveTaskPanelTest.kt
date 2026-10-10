package ai.ritav.app

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RitavLiveTaskPanelTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun idle_state_reports_that_no_authoritative_task_is_active() {
        composeRule.setContent {
            RitavTheme(preferences = UiPreferences()) {
                RitavLiveTaskPanel(state = RitavTaskUiState.IDLE)
            }
        }

        composeRule.onNodeWithText("No active task").assertIsDisplayed()
        composeRule.onNodeWithText(
            "No task is currently being executed. Ritav will only show live progress when an authoritative runtime state exists."
        ).assertIsDisplayed()
    }

    @Test
    fun live_task_status_uses_polite_live_region_semantics() {
        val taskName = "Open approved app"
        val step = "Dispatching approved action"
        val summary = "Approved action is being dispatched through the security-owned adapter."
        val accessibleState =
            "Task executing: $taskName. Current step: $step. $summary"

        composeRule.setContent {
            RitavTheme(preferences = UiPreferences()) {
                RitavLiveTaskPanel(
                    state = RitavTaskUiState.EXECUTING,
                    taskName = taskName,
                    currentStep = step,
                    summary = summary
                )
            }
        }

        val liveStatusNode = SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf(accessibleState)
        ).and(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        )
        composeRule.onNode(liveStatusNode).assertIsDisplayed()
    }

    @Test
    fun executing_state_routes_emergency_stop_to_existing_callback() {
        var stopRequested = false
        composeRule.setContent {
            RitavTheme(preferences = UiPreferences()) {
                RitavLiveTaskPanel(
                    state = RitavTaskUiState.EXECUTING,
                    taskName = "Open approved app",
                    currentStep = "Dispatching approved action",
                    summary = "Approved action is being dispatched through the security-owned adapter.",
                    onEmergencyStop = { stopRequested = true }
                )
            }
        }

        composeRule.onNodeWithText("Task executing").assertIsDisplayed()
        val stopButton = composeRule.onNodeWithText("Emergency Stop")
        stopButton.assertIsDisplayed()
        stopButton.performClick()

        assertTrue(stopRequested)
    }

    @Test
    fun failed_safely_state_displays_failure_without_success_claim() {
        val failureSummary =
            "The action did not complete successfully; verification was not reported as successful."
        composeRule.setContent {
            RitavTheme(preferences = UiPreferences()) {
                RitavLiveTaskPanel(
                    state = RitavTaskUiState.FAILED_SAFELY,
                    taskName = "Open approved app",
                    currentStep = "Verification",
                    summary = failureSummary
                )
            }
        }

        composeRule.onNodeWithText("Task failed safely").assertIsDisplayed()
        composeRule.onNodeWithText(failureSummary).assertIsDisplayed()
    }
    @Test
    fun blocked_state_reports_denial_without_success_claim() {
        val blockedSummary = "Security policy blocked the task before adapter dispatch."
        composeRule.setContent {
            RitavTheme(preferences = UiPreferences()) {
                RitavLiveTaskPanel(
                    state = RitavTaskUiState.BLOCKED,
                    taskName = "Open unapproved app",
                    summary = blockedSummary
                )
            }
        }

        composeRule.onNodeWithText("Task blocked").assertIsDisplayed()
        composeRule.onNodeWithText(blockedSummary).assertIsDisplayed()
    }

    @Test
    fun stopped_state_reports_that_protected_activity_remains_blocked() {
        val stoppedSummary = "Emergency Stop is active. Protected task execution is unavailable."
        composeRule.setContent {
            RitavTheme(preferences = UiPreferences()) {
                RitavLiveTaskPanel(
                    state = RitavTaskUiState.STOPPED,
                    summary = stoppedSummary
                )
            }
        }

        composeRule.onNodeWithText("Task stopped").assertIsDisplayed()
        composeRule.onNodeWithText(stoppedSummary).assertIsDisplayed()
    }
}
