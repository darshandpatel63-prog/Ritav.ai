package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionAuthorizationGateTest {
    private val plan = ActionPlan(
        appId = "com.example.app",
        capability = Capability.SEND_MESSAGE,
        action = "send",
        riskTier = RiskTier.TIER_2_CONTENT_MUTATION,
        expectedState = "SENT",
        sessionId = "session-1"
    )

    @Test
    fun emergencyStopBlocksGateLevelTokenMinting() {
        val emergencyStop = EmergencyStopController().apply { activate() }
        val gate = ActionAuthorizationGate(emergencyStop)

        try {
            gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)
            assertFalse(true)
        } catch (_: IllegalStateException) {
            assertTrue(true)
        }
    }

    @Test
    fun authorizationServiceDefaultsToGateEmergencyStop() {
        val emergencyStop = EmergencyStopController().apply { activate() }
        val gate = ActionAuthorizationGate(emergencyStop)
        val service = ActionAuthorizationService(gate, StubDeviceAuthorizationGateway())

        assertTrue(
            service.issueUserConfirmationToken(
                plan,
                plan.stableHash()
            ) == null
        )
    }

    @Test
    fun malformedPlanCannotMintAuthorizationToken() {
        val gate = ActionAuthorizationGate()
        val malformed = plan.copy(expectedState = "")
        try {
            gate.issue(malformed, AuthorizationLevel.USER_CONFIRMATION, 1_000L)
            assertFalse(true)
        } catch (_: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    @Test
    fun authorizationTtlCannotOverflowClock() {
        val gate = ActionAuthorizationGate()
        try {
            gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, Long.MAX_VALUE, 1L)
            assertFalse(true)
        } catch (_: IllegalArgumentException) {
            assertTrue(true)
        }
    }


    @Test
    fun negativeAuthorizationClockCannotMintAuthorizationToken() {
        val gate = ActionAuthorizationGate()
        try {
            gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, -1L)
            assertFalse(true)
        } catch (_: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    @Test
    fun oversizedTokenIsRejectedWithoutChangingValidTokenState() {
        val gate = ActionAuthorizationGate()
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)
        assertFalse(gate.consume("x".repeat(129), plan, AuthorizationLevel.USER_CONFIRMATION, 1_001L))
        assertTrue(gate.consume(token, plan, AuthorizationLevel.USER_CONFIRMATION, 1_001L))
    }

    @Test
    fun negativeAuthorizationClockCannotConsumeAuthorizationToken() {
        val gate = ActionAuthorizationGate()
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        assertFalse(
            gate.consume(
                token,
                plan,
                AuthorizationLevel.USER_CONFIRMATION,
                -1L
            )
        )
    }

    @Test
    fun tokenIssuedBeforeEmergencyStopCannotBeConsumedAfterResume() {
        val emergencyStop = EmergencyStopController()
        val gate = ActionAuthorizationGate(emergencyStop)
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        emergencyStop.activate()
        assertFalse(gate.consume(token, plan, AuthorizationLevel.USER_CONFIRMATION, 1_001L))

        emergencyStop.resetAfterExplicitUserConfirmation(true)
        assertFalse(gate.consume(token, plan, AuthorizationLevel.USER_CONFIRMATION, 1_002L))
    }

    @Test
    fun emergencyStopBlocksTokenConsumption() {
        val emergencyStop = EmergencyStopController()
        val gate = ActionAuthorizationGate(emergencyStop)
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)
        emergencyStop.activate()

        assertFalse(gate.consume(token, plan, AuthorizationLevel.USER_CONFIRMATION, 1_001L))
    }

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
    fun rejectedWrongPlanAttemptDoesNotBurnValidToken() {
        val gate = ActionAuthorizationGate()
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        assertFalse(
            gate.consume(
                token,
                plan.copy(action = "delete"),
                AuthorizationLevel.USER_CONFIRMATION,
                1_001L
            )
        )
        assertTrue(gate.consume(token, plan, AuthorizationLevel.USER_CONFIRMATION, 1_002L))
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
    fun lowerAuthorizationTokenCannotBeMintedForHigherRiskPlan() {
        val gate = ActionAuthorizationGate()
        try {
            gate.issue(plan.copy(riskTier = RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE), AuthorizationLevel.USER_CONFIRMATION, 1_000L)
            assertFalse(true)
        } catch (_: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    @Test
    fun weakerAuthorizationCannotSatisfyStrongerToken() {
        val gate = ActionAuthorizationGate()
        val token = gate.issue(
            plan = plan.copy(riskTier = RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE),
            requiredLevel = AuthorizationLevel.DEVICE_AUTHENTICATION,
            nowEpochMillis = 1_000L
        )

        assertFalse(gate.consume(token, plan, AuthorizationLevel.USER_CONFIRMATION, 1_001L))
    }
}
