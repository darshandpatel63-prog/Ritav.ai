package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionPlanAuthorizationTest {
    private fun plan(action: String = "send") = ActionPlan(
        appId = "com.example.app",
        capability = Capability.SEND_MESSAGE,
        action = action,
        riskTier = RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE,
        expectedState = "SENT",
        sessionId = "session-1"
    )

    @Test
    fun tokenIsBoundToExactPlan() {
        val gate = testAuthorizationService()
        val token = gate.issue(plan(), AuthorizationLevel.DEVICE_AUTHENTICATION, 1_000L)

        assertFalse(gate.consume(token, plan("delete"), AuthorizationLevel.DEVICE_AUTHENTICATION, 1_001L))
    }

    @Test
    fun matchingPlanCanConsumeOnce() {
        val gate = testAuthorizationService()
        val token = gate.issue(plan(), AuthorizationLevel.DEVICE_AUTHENTICATION, 1_000L)

        assertTrue(gate.consume(token, plan(), AuthorizationLevel.DEVICE_AUTHENTICATION, 1_001L))
        assertFalse(gate.consume(token, plan(), AuthorizationLevel.DEVICE_AUTHENTICATION, 1_002L))
    }

    @Test
    fun lowerAuthorizationLevelCannotSatisfyGrant() {
        val gate = testAuthorizationService()
        val token = gate.issue(plan(), AuthorizationLevel.DEVICE_AUTHENTICATION, 1_000L)

        assertFalse(gate.consume(token, plan(), AuthorizationLevel.USER_CONFIRMATION, 1_001L))
    }
}
