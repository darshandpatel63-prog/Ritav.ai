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
        expectedState = "EDITED",
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

    @Test fun malformedPlanIsDeniedBeforeSecurityProcessing() {
        val p = plan().copy(expectedState = "")
        val engine = PolicyEngine(InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", "session-1"))))
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), ActionAuthorizationGate())
        val result = pipeline.authorize(SecurityExecutionRequest(action(), p, null, trustedSession(1_000), 1_000))
        assertFalse(result.allowed)
    }

    @Test fun malformedExecutionRequestIsDeniedBeforeAuthorization() {
        val p = plan()
        val engine = PolicyEngine(InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", "session-1"))))
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), gate)
        val token = gate.issue(p, AuthorizationLevel.USER_CONFIRMATION, 1_000)
        val malformed = action().copy(action = "x".repeat(4_097))
        val result = pipeline.authorize(SecurityExecutionRequest(malformed, p, token, trustedSession(1_000), 1_000))
        assertFalse(result.allowed)
        assertTrue(gate.consume(token, p, AuthorizationLevel.USER_CONFIRMATION, 1_001))
    }

    @Test fun protectedActionWithoutSessionBindingIsDenied() {
        val p = plan().copy(sessionId = null)
        val action = action().copy(sessionId = null)
        val engine = PolicyEngine(InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", null))))
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), ActionAuthorizationGate())
        val result = pipeline.authorize(SecurityExecutionRequest(action, p, null, null, 1_000))
        assertFalse(result.allowed)
    }

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

    @Test fun sensitiveInputIsBlockedBeforeAuthorizationAndExecution() {
        val p = plan()
        val engine = PolicyEngine(InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", "session-1"))))
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), gate)

        val token = gate.issue(p, AuthorizationLevel.USER_CONFIRMATION, 1_000)
        val result = pipeline.authorize(
            SecurityExecutionRequest(
                action = action(), plan = p, authorizationToken = token,
                identitySession = trustedSession(1_000), nowEpochMillis = 1_000,
                inputText = "Please use OTP 123456"
            )
        )

        assertFalse(result.allowed)
        assertTrue(result.sanitizedInput?.contains("[REDACTED:OTP]") == true)
        assertTrue(pipeline.audit().none { it.reason.contains("123456") })
    }

    @Test fun oversizedInputIsBlockedBeforeAuthorizationAndExecution() {
        val p = plan()
        val engine = PolicyEngine(InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", "session-1"))))
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), gate)
        val token = gate.issue(p, AuthorizationLevel.USER_CONFIRMATION, 1_000)

        val result = pipeline.authorize(
            SecurityExecutionRequest(
                action = action(), plan = p, authorizationToken = token,
                identitySession = trustedSession(1_000), nowEpochMillis = 1_000,
                inputText = "x".repeat(16_385)
            )
        )

        assertFalse(result.allowed)
        assertTrue(result.sanitizedInput == null)

        val allowedAfterBlockedAttempt = pipeline.authorize(
            SecurityExecutionRequest(
                action = action(), plan = p, authorizationToken = token,
                identitySession = trustedSession(1_000), nowEpochMillis = 1_000,
                inputText = "benign input"
            )
        )
        assertTrue(allowedAfterBlockedAttempt.allowed)
    }

    @Test fun normalizedSensitiveInputIsBlockedBeforeAuthorization() {
        val p = plan()
        val engine = PolicyEngine(InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", "session-1"))))
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), gate)
        val token = gate.issue(p, AuthorizationLevel.USER_CONFIRMATION, 1_000)

        val result = pipeline.authorize(
            SecurityExecutionRequest(
                action = action(), plan = p, authorizationToken = token,
                identitySession = trustedSession(1_000), nowEpochMillis = 1_000,
                inputText = "OTP\u00a0123456"
            )
        )

        assertFalse(result.allowed)
        assertTrue(result.sanitizedInput == null)
    }

    @Test fun obfuscatedSensitiveInputDoesNotConsumeAuthorizationToken() {
        val p = plan()
        val engine = PolicyEngine(InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", "session-1"))))
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), gate)
        val token = gate.issue(p, AuthorizationLevel.USER_CONFIRMATION, 1_000)

        val blocked = pipeline.authorize(
            SecurityExecutionRequest(
                action = action(), plan = p, authorizationToken = token,
                identitySession = trustedSession(1_000), nowEpochMillis = 1_000,
                inputText = "O\u200b T P 12\u200b 34 56"
            )
        )
        assertFalse(blocked.allowed)

        val allowed = pipeline.authorize(
            SecurityExecutionRequest(
                action = action(), plan = p, authorizationToken = token,
                identitySession = trustedSession(1_000), nowEpochMillis = 1_000,
                inputText = "safe input"
            )
        )
        assertTrue(allowed.allowed)
    }

    @Test fun explicitSensitiveFlagIsStillBlockedWithoutRawInput() {
        val p = plan()
        val sensitiveAction = action().copy(containsSensitiveData = true)
        val engine = PolicyEngine(InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", "session-1"))))
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), ActionAuthorizationGate())

        val result = pipeline.authorize(
            SecurityExecutionRequest(
                action = sensitiveAction, plan = p,
                identitySession = trustedSession(1_000), nowEpochMillis = 1_000
            )
        )

        assertFalse(result.allowed)
    }

    @Test fun financeFirewallDeniesBeforeAuthorizationAndReturnsNoAuthorizationPath() {
        val p = ActionPlan(
            appId = "bank.app",
            capability = Capability.FINANCIAL_ACTION,
            action = "transfer",
            riskTier = RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE,
            expectedState = "TRANSFERRED",
            sessionId = "finance-session"
        )
        val action = ActionRequest(
            appId = p.appId,
            action = p.action,
            riskTier = p.riskTier,
            capability = p.capability,
            sessionId = p.sessionId,
            userExplicitlyRequested = true,
            authorizationLevel = AuthorizationLevel.DEVICE_AUTHENTICATION
        )
        val engine = PolicyEngine()
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), gate)
        val token = gate.issue(p, AuthorizationLevel.DEVICE_AUTHENTICATION, 1_000)

        val result = pipeline.authorize(
            SecurityExecutionRequest(
                action = action,
                plan = p,
                authorizationToken = token,
                identitySession = trustedSession(1_000),
                nowEpochMillis = 1_000
            )
        )

        assertFalse(result.allowed)
        assertTrue(result.authorizationRequired == AuthorizationLevel.NONE)
        assertTrue(result.reason.contains("finance firewall"))

        // The finance denial must not consume a token that is never a valid bypass.
        val secondAttempt = pipeline.authorize(
            SecurityExecutionRequest(
                action = action,
                plan = p,
                authorizationToken = token,
                identitySession = trustedSession(1_000),
                nowEpochMillis = 1_000
            )
        )
        assertFalse(secondAttempt.allowed)
        assertTrue(secondAttempt.reason.contains("finance firewall"))
    }

    private fun trustedSession(now: Long) = SecuritySession.create(
        id = "session-1",
        identity = IdentityLevel.OWNER_SIGNAL,
        authenticatedAtEpochMillis = now,
        expiresAtEpochMillis = now + 60_000
    )
}
