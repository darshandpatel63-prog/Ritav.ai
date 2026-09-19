package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityExecutionPipelineTest {
    private data class ProtectedFixture(
        val plan: ActionPlan,
        val action: ActionRequest,
        val session: SecuritySession
    )

    private fun protectedFixture(now: Long): ProtectedFixture {
        val session = IdentitySessionManager().createSession(
            identity = IdentityLevel.TRUSTED_SIGNAL,
            nowEpochMillis = now,
            ttlMillis = 60_000L
        )
        val plan = ActionPlan(
            appId = "demo",
            capability = Capability.UI_AUTOMATION,
            action = "edit",
            riskTier = RiskTier.TIER_2_CONTENT_MUTATION,
            expectedState = "EDITED",
            sessionId = session.id
        )
        val action = ActionRequest(
            appId = "demo",
            action = "edit",
            riskTier = RiskTier.TIER_2_CONTENT_MUTATION,
            capability = Capability.UI_AUTOMATION,
            sessionId = session.id,
            userExplicitlyRequested = true,
            authorizationLevel = AuthorizationLevel.USER_CONFIRMATION
        )
        return ProtectedFixture(plan, action, session)
    }

    @Test fun malformedPlanIsDeniedBeforeSecurityProcessing() {
        val fixture = protectedFixture(1_000L)
        val malformedPlan = fixture.plan.copy(expectedState = "")
        val engine = PolicyEngine(
            InMemoryPermissionStore(
                setOf(
                    CapabilityGrant(
                        fixture.plan.appId,
                        fixture.plan.capability,
                        fixture.plan.action,
                        fixture.plan.sessionId
                    )
                )
            )
        )
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), ActionAuthorizationGate())
        val result = pipeline.authorize(
            SecurityExecutionRequest(
                fixture.action,
                malformedPlan,
                null,
                fixture.session,
                1_000L
            )
        )
        assertFalse(result.allowed)
    }

    @Test fun malformedExecutionRequestIsDeniedBeforeAuthorization() {
        val fixture = protectedFixture(1_000L)
        val engine = PolicyEngine(
            InMemoryPermissionStore(
                setOf(
                    CapabilityGrant(
                        fixture.plan.appId,
                        fixture.plan.capability,
                        fixture.plan.action,
                        fixture.plan.sessionId
                    )
                )
            )
        )
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(
            engine,
            ExecutionPolicyGate(engine),
            gate,
            identitySessionManager = fixture.identityManager
        )
        val token = gate.issue(fixture.plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)
        val malformed = fixture.action.copy(action = "x".repeat(4_097))
        val result = pipeline.authorize(
            SecurityExecutionRequest(
                malformed,
                fixture.plan,
                token,
                fixture.session,
                1_000L
            )
        )
        assertFalse(result.allowed)
        assertTrue(gate.consume(token, fixture.plan, AuthorizationLevel.USER_CONFIRMATION, 1_001L))
    }

    @Test fun protectedActionWithoutSessionBindingIsDenied() {
        val plan = ActionPlan(
            appId = "demo",
            capability = Capability.UI_AUTOMATION,
            action = "edit",
            riskTier = RiskTier.TIER_2_CONTENT_MUTATION,
            expectedState = "EDITED",
            sessionId = null
        )
        val action = ActionRequest(
            appId = "demo",
            action = "edit",
            riskTier = RiskTier.TIER_2_CONTENT_MUTATION,
            capability = Capability.UI_AUTOMATION,
            sessionId = null
        )
        val engine = PolicyEngine(
            InMemoryPermissionStore(
                setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit", null))
            )
        )
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), ActionAuthorizationGate())
        val result = pipeline.authorize(SecurityExecutionRequest(action, plan, null, null, 1_000L))
        assertFalse(result.allowed)
    }

    @Test fun mismatchedPlanIsDenied() {
        val fixture = protectedFixture(1_000L)
        val engine = PolicyEngine(
            InMemoryPermissionStore(
                setOf(
                    CapabilityGrant(
                        fixture.plan.appId,
                        fixture.plan.capability,
                        fixture.plan.action,
                        fixture.plan.sessionId
                    )
                )
            )
        )
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), ActionAuthorizationGate())
        val result = pipeline.authorize(
            SecurityExecutionRequest(
                fixture.action,
                fixture.plan.copy(action = "other"),
                null,
                fixture.session,
                1_000L
            )
        )
        assertFalse(result.allowed)
    }

    @Test fun protectedActionNeedsTrustedActiveSession() {
        val fixture = protectedFixture(1_000L)
        val engine = PolicyEngine(
            InMemoryPermissionStore(
                setOf(
                    CapabilityGrant(
                        fixture.plan.appId,
                        fixture.plan.capability,
                        fixture.plan.action,
                        fixture.plan.sessionId
                    )
                )
            )
        )
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), ActionAuthorizationGate())
        val result = pipeline.authorize(
            SecurityExecutionRequest(
                fixture.action,
                fixture.plan,
                null,
                null,
                1_000L
            )
        )
        assertFalse(result.allowed)
    }

    @Test fun validOneTimeAuthorizationAllowsTier2Action() {
        val fixture = protectedFixture(1_000L)
        val engine = PolicyEngine(
            InMemoryPermissionStore(
                setOf(
                    CapabilityGrant(
                        fixture.plan.appId,
                        fixture.plan.capability,
                        fixture.plan.action,
                        fixture.plan.sessionId
                    )
                )
            )
        )
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(
            engine,
            ExecutionPolicyGate(engine),
            gate,
            identitySessionManager = fixture.identityManager
        )
        val token = gate.issue(fixture.plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)
        val result = pipeline.authorize(
            SecurityExecutionRequest(
                fixture.action,
                fixture.plan,
                token,
                fixture.session,
                1_000L
            )
        )
        assertTrue(result.allowed)

        val replay = pipeline.authorize(
            SecurityExecutionRequest(
                fixture.action,
                fixture.plan,
                token,
                fixture.session,
                1_000L
            )
        )
        assertFalse(replay.allowed)
    }

    @Test fun sensitiveInputIsBlockedBeforeAuthorizationAndExecution() {
        val fixture = protectedFixture(1_000L)
        val engine = PolicyEngine(
            InMemoryPermissionStore(
                setOf(
                    CapabilityGrant(
                        fixture.plan.appId,
                        fixture.plan.capability,
                        fixture.plan.action,
                        fixture.plan.sessionId
                    )
                )
            )
        )
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(
            engine,
            ExecutionPolicyGate(engine),
            gate,
            identitySessionManager = fixture.identityManager
        )

        val token = gate.issue(fixture.plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)
        val result = pipeline.authorize(
            SecurityExecutionRequest(
                action = fixture.action,
                plan = fixture.plan,
                authorizationToken = token,
                identitySession = fixture.session,
                nowEpochMillis = 1_000L,
                inputText = "Please use OTP 123456"
            )
        )

        assertFalse(result.allowed)
        assertTrue(result.sanitizedInput?.contains("[REDACTED:OTP]") == true)
        assertTrue(pipeline.audit().none { it.reason.contains("123456") })
    }

    @Test fun oversizedInputIsBlockedBeforeAuthorizationAndExecution() {
        val fixture = protectedFixture(1_000L)
        val engine = PolicyEngine(
            InMemoryPermissionStore(
                setOf(
                    CapabilityGrant(
                        fixture.plan.appId,
                        fixture.plan.capability,
                        fixture.plan.action,
                        fixture.plan.sessionId
                    )
                )
            )
        )
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(
            engine,
            ExecutionPolicyGate(engine),
            gate,
            identitySessionManager = fixture.identityManager
        )
        val token = gate.issue(fixture.plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        val result = pipeline.authorize(
            SecurityExecutionRequest(
                action = fixture.action,
                plan = fixture.plan,
                authorizationToken = token,
                identitySession = fixture.session,
                nowEpochMillis = 1_000L,
                inputText = "x".repeat(16_385)
            )
        )

        assertFalse(result.allowed)
        assertTrue(result.sanitizedInput == null)

        val allowedAfterBlockedAttempt = pipeline.authorize(
            SecurityExecutionRequest(
                action = fixture.action,
                plan = fixture.plan,
                authorizationToken = token,
                identitySession = fixture.session,
                nowEpochMillis = 1_000L,
                inputText = "benign input"
            )
        )
        assertTrue(allowedAfterBlockedAttempt.allowed)
    }

    @Test fun normalizedSensitiveInputIsBlockedBeforeAuthorization() {
        val fixture = protectedFixture(1_000L)
        val engine = PolicyEngine(
            InMemoryPermissionStore(
                setOf(
                    CapabilityGrant(
                        fixture.plan.appId,
                        fixture.plan.capability,
                        fixture.plan.action,
                        fixture.plan.sessionId
                    )
                )
            )
        )
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(
            engine,
            ExecutionPolicyGate(engine),
            gate,
            identitySessionManager = fixture.identityManager
        )
        val token = gate.issue(fixture.plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        val result = pipeline.authorize(
            SecurityExecutionRequest(
                action = fixture.action,
                plan = fixture.plan,
                authorizationToken = token,
                identitySession = fixture.session,
                nowEpochMillis = 1_000L,
                inputText = "OTP 123456"
            )
        )

        assertFalse(result.allowed)
        assertTrue(result.sanitizedInput == null)
    }

    @Test fun obfuscatedSensitiveInputDoesNotConsumeAuthorizationToken() {
        val fixture = protectedFixture(1_000L)
        val engine = PolicyEngine(
            InMemoryPermissionStore(
                setOf(
                    CapabilityGrant(
                        fixture.plan.appId,
                        fixture.plan.capability,
                        fixture.plan.action,
                        fixture.plan.sessionId
                    )
                )
            )
        )
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(
            engine,
            ExecutionPolicyGate(engine),
            gate,
            identitySessionManager = fixture.identityManager
        )
        val token = gate.issue(fixture.plan, AuthorizationLevel.USER_CONFIRMATION, 1_000L)

        val blocked = pipeline.authorize(
            SecurityExecutionRequest(
                action = fixture.action,
                plan = fixture.plan,
                authorizationToken = token,
                identitySession = fixture.session,
                nowEpochMillis = 1_000L,
                inputText = "O​ T P 12​ 34 56"
            )
        )
        assertFalse(blocked.allowed)

        val allowed = pipeline.authorize(
            SecurityExecutionRequest(
                action = fixture.action,
                plan = fixture.plan,
                authorizationToken = token,
                identitySession = fixture.session,
                nowEpochMillis = 1_000L,
                inputText = "safe input"
            )
        )
        assertTrue(allowed.allowed)
    }

    @Test fun explicitSensitiveFlagIsStillBlockedWithoutRawInput() {
        val fixture = protectedFixture(1_000L)
        val sensitiveAction = fixture.action.copy(containsSensitiveData = true)
        val engine = PolicyEngine(
            InMemoryPermissionStore(
                setOf(
                    CapabilityGrant(
                        fixture.plan.appId,
                        fixture.plan.capability,
                        fixture.plan.action,
                        fixture.plan.sessionId
                    )
                )
            )
        )
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), ActionAuthorizationGate())

        val result = pipeline.authorize(
            SecurityExecutionRequest(
                action = sensitiveAction,
                plan = fixture.plan,
                identitySession = fixture.session,
                nowEpochMillis = 1_000L
            )
        )

        assertFalse(result.allowed)
    }

    @Test fun financeFirewallDeniesBeforeAuthorizationAndReturnsNoAuthorizationPath() {
        val plan = ActionPlan(
            appId = "bank.app",
            capability = Capability.FINANCIAL_ACTION,
            action = "transfer",
            riskTier = RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE,
            expectedState = "TRANSFERRED",
            sessionId = "finance-session"
        )
        val action = ActionRequest(
            appId = plan.appId,
            action = plan.action,
            riskTier = plan.riskTier,
            capability = plan.capability,
            sessionId = plan.sessionId,
            userExplicitlyRequested = true,
            authorizationLevel = AuthorizationLevel.DEVICE_AUTHENTICATION
        )
        val engine = PolicyEngine()
        val gate = ActionAuthorizationGate()
        val pipeline = SecurityExecutionPipeline(engine, ExecutionPolicyGate(engine), gate)
        val token = gate.issue(plan, AuthorizationLevel.DEVICE_AUTHENTICATION, 1_000L)

        val result = pipeline.authorize(
            SecurityExecutionRequest(
                action = action,
                plan = plan,
                authorizationToken = token,
                identitySession = null,
                nowEpochMillis = 1_000L
            )
        )

        assertFalse(result.allowed)
        assertTrue(result.authorizationRequired == AuthorizationLevel.NONE)
        assertTrue(result.reason.contains("finance firewall"))

        val secondAttempt = pipeline.authorize(
            SecurityExecutionRequest(
                action = action,
                plan = plan,
                authorizationToken = token,
                identitySession = null,
                nowEpochMillis = 1_000L
            )
        )
        assertFalse(secondAttempt.allowed)
        assertTrue(secondAttempt.reason.contains("finance firewall"))
    }
}
