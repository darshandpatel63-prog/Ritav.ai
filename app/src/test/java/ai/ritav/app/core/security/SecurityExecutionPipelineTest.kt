package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityExecutionPipelineTest {
    private fun plan() = ActionPlan(
        appId = "demo",
        capability = Capability.UI_AUTOMATION,
        action = "edit",
        riskTier = RiskTier.TIER_2_CONTENT_MUTATION,
        sessionId = "session-1"
    )

    private fun action() = ActionRequest(
        appId = "demo",
        action = "edit",
        riskTier = RiskTier.TIER_2_CONTENT_MUTATION,
        capability = Capability.UI_AUTOMATION,
        sessionId = "session-1",
        userExplicitlyRequested = true,
        authorizationLevel = AuthorizationLevel.USER_CONFIRMATION
    )

    @Test fun mismatchedPlanIsDenied() {
        val p = plan()
        val engine = PolicyEngine(InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", "session-1"))))
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), ActionAuthorizationGate())
        val result = pipeline.authorize(SecurityExecutionRequest(action(), p.copy(action = "other"), null, trustedSession(1_000), 1_000))
        assertFalse(result.allowed)
    }

    @Test fun protectedActionNeedsTrustedActiveSession() {
        val p = plan()
        val engine = PolicyEngine(InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", "session-1"))))
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), ActionAuthorizationGate())
        val result = pipeline.authorize(SecurityExecutionRequest(action(), p, null, null, 1_000))
        assertFalse(result.allowed)
    }

    @Test fun validOneTimeAuthorizationAllowsTier2Action() {
        val p = plan()
        val engine = PolicyEngine(InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", "session-1"))))
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), gate)
        val token = gate.issue(p, AuthorizationLevel.USER_CONFIRMATION, 1_000)
        val result = pipeline.authorize(SecurityExecutionRequest(action(), p, token, trustedSession(1_000), 1_000))
        assertTrue(result.allowed)

        val replay = pipeline.authorize(SecurityExecutionRequest(action(), p, token, trustedSession(1_000), 1_000))
        assertFalse(replay.allowed)
    }

    private fun trustedSession(now: Long) = SecuritySession(
        id = "session-1",
        identity = IdentityLevel.OWNER_SIGNAL,
        authenticatedAtEpochMillis = now,
        expiresAtEpochMillis = now + 60_000
    )
}
