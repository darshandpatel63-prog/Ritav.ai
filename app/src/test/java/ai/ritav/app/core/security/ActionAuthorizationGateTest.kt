package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionAuthorizationGateTest {
    private val plan = ActionPlan(
        appId = "com.example.app",
        capability = Capability.SEND_MESSAGE,
        action = "send",
        riskTier = RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE,
        sessionId = "session-1"
    )

    @Test
    fun tokenCanBeConsumedOnlyOnce() {
        val gate = ActionAuthorizationGate()
        val token = gate.issue(
            plan = plan,
            requiredLevel = AuthorizationLevel.USER_CONFIRMATION,
            nowEpochMillis = 1_000L
        )

        assertTrue(gate.consume(token, plan, AuthorizationLevel.USER_CONFIRMATION, 1_001L))
        assertFalse(gate.consume(token, plan, AuthorizationLevel.USER_CONFIRMATION, 1_002L))
    }

    @Test
    fun tokenCannotBeReusedForDifferentAction() {
        val gate = ActionAuthorizationGate()
        val token = gate.issue(
            plan = plan,
            requiredLevel = AuthorizationLevel.USER_CONFIRMATION,
            nowEpochMillis = 1_000L
        )

        assertFalse(
            gate.consume(
                token,
                plan.copy(action = "delete"),
                AuthorizationLevel.USER_CONFIRMATION,
                1_001L
            )
        )
    }

    @Test
    fun expiredTokenIsRejected() {
        val gate = ActionAuthorizationGate()
        val token = gate.issue(
            plan = plan,
            requiredLevel = AuthorizationLevel.USER_CONFIRMATION,
            nowEpochMillis = 1_000L,
            ttlMillis = 1_000L
        )

        assertFalse(gate.consume(token, plan, AuthorizationLevel.USER_CONFIRMATION, 2_001L))
    }

    @Test
    fun weakerAuthorizationCannotSatisfyStrongerToken() {
        val gate = ActionAuthorizationGate()
        val token = gate.issue(
            plan = plan,
            requiredLevel = AuthorizationLevel.DEVICE_AUTHENTICATION,
            nowEpochMillis = 1_000L
        )

        assertFalse(gate.consume(token, plan, AuthorizationLevel.USER_CONFIRMATION, 1_001L))
    }
}
