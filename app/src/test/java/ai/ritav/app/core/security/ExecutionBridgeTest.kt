package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionBridgeTest {
    private class RecordingAdapter : AndroidActionAdapter {
        var calls = 0
        override fun execute(plan: ActionPlan): ExecutionResult {
            calls++
            return ExecutionResult(true, false, "adapter called", observedState = plan.expectedState)
        }
    }

    private class ThrowingAdapter : AndroidActionAdapter {
        var calls = 0
        override fun execute(plan: ActionPlan): ExecutionResult {
            calls++
            error("simulated adapter failure")
        }
    }

    private data class ProtectedFixture(
        val identityManager: IdentitySessionManager,
        val plan: ActionPlan,
        val session: SecuritySession
    )

    private fun protectedFixture(): ProtectedFixture {
        val now = System.currentTimeMillis()
        val identityManager = IdentitySessionManager()
        val session = identityManager.createSession(
            identity = IdentityLevel.TRUSTED_SIGNAL,
            nowEpochMillis = now - 1_000L,
            ttlMillis = 60_000L
        )
        return ProtectedFixture(
            identityManager = identityManager,
            plan = ActionPlan(
                "demo.app",
                Capability.UI_AUTOMATION,
                "edit",
                RiskTier.TIER_2_CONTENT_MUTATION,
                expectedState = "EDITED",
                sessionId = session.id
            ),
            session = session
        )
    }

    private fun registryFor(plan: ActionPlan) = AppCapabilityRegistry(
        listOf(AppCapabilitySpec(plan.appId, plan.capability, setOf(plan.action), plan.riskTier))
    )

    private fun pipelineFor(
        policy: PolicyEngine,
        identityManager: IdentitySessionManager,
        gate: ActionAuthorizationGate = ActionAuthorizationGate()
    ) = SecurityExecutionPipeline(
        policy,
        ExecutionPolicyGate(policy),
        gate,
        identitySessionManager = identityManager
    )

    @Test fun unregisteredCapabilityNeverReachesAdapter() {
        val adapter = RecordingAdapter()
        val policy = PolicyEngine()
        val bridge = ExecutionBridge(CapabilityPolicyGate(AppCapabilityRegistry()), pipelineFor(policy, IdentitySessionManager()), adapter)
        val result = bridge.execute(
            ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED"),
            true
        )
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun registeredCapabilityReachesApprovedAdapter() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registryFor(plan)),
            pipelineFor(policy, IdentitySessionManager()),
            adapter
        )
        val result = bridge.execute(plan, userExplicitlyRequested = true)
        assertTrue(result.success)
        assertEquals(1, adapter.calls)
    }

    @Test fun wrongRiskRegistrationNeverReachesAdapter() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_2_CONTENT_MUTATION, expectedState = "OPENED")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val registry = AppCapabilityRegistry(
            listOf(
                AppCapabilitySpec(
                    plan.appId,
                    plan.capability,
                    setOf(plan.action),
                    RiskTier.TIER_1_REVERSIBLE
                )
            )
        )
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registry),
            pipelineFor(policy, IdentitySessionManager()),
            adapter
        )
        val result = bridge.execute(plan, userExplicitlyRequested = true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun sensitiveExecutionIsBlockedAtFinalBoundary() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registryFor(plan)),
            pipelineFor(policy, IdentitySessionManager()),
            adapter
        )
        val result = bridge.execute(plan, true, containsSensitiveData = true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun sensitiveInputIsBlockedAtBridgeAndAuthorizationTokenRemainsUsable() {
        val fixture = protectedFixture()
        val plan = fixture.plan
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(
            setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, plan.sessionId))
        )
        val policy = PolicyEngine(permissions)
        val authGate = ActionAuthorizationGate()
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registryFor(plan)),
            pipelineFor(policy, fixture.identityManager, authGate),
            adapter
        )
        val token = authGate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, System.currentTimeMillis())

        val blocked = bridge.execute(
            plan,
            userExplicitlyRequested = true,
            authorizationLevel = AuthorizationLevel.USER_CONFIRMATION,
            authorizationToken = token,
            identitySession = fixture.session,
            inputText = "OTP: 123456"
        )
        assertFalse(blocked.success)
        assertEquals(0, adapter.calls)

        val allowed = bridge.execute(
            plan,
            userExplicitlyRequested = true,
            authorizationLevel = AuthorizationLevel.USER_CONFIRMATION,
            authorizationToken = token,
            identitySession = fixture.session,
            inputText = "open the editor"
        )
        assertTrue(allowed.success)
        assertEquals(1, adapter.calls)
    }

    @Test fun financialCapabilityCannotReachAdapterEvenWhenRegistered() {
        val plan = ActionPlan(
            "bank.app",
            Capability.FINANCIAL_ACTION,
            "transfer",
            RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED,
            expectedState = "TRANSFERRED"
        )
        val adapter = RecordingAdapter()
        val registry = AppCapabilityRegistry(
            listOf(
                AppCapabilitySpec(
                    plan.appId,
                    plan.capability,
                    setOf(plan.action),
                    plan.riskTier,
                    financialCategory = true
                )
            )
        )
        val policy = PolicyEngine()
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registry),
            pipelineFor(policy, IdentitySessionManager()),
            adapter
        )
        val result = bridge.execute(plan, true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun mismatchedIdentitySessionCannotAuthorizeProtectedAction() {
        val fixture = protectedFixture()
        val plan = fixture.plan
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(
            setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, plan.sessionId))
        )
        val policy = PolicyEngine(permissions)
        val gate = ActionAuthorizationGate()
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registryFor(plan)),
            pipelineFor(policy, fixture.identityManager, gate),
            adapter
        )
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, System.currentTimeMillis())
        val mismatchedSession = IdentitySessionManager().createSession(
            identity = IdentityLevel.OWNER_SIGNAL,
            nowEpochMillis = System.currentTimeMillis() - 1_000L,
            ttlMillis = 60_000L
        )

        val result = bridge.execute(
            plan,
            userExplicitlyRequested = true,
            authorizationLevel = AuthorizationLevel.USER_CONFIRMATION,
            authorizationToken = token,
            identitySession = mismatchedSession
        )

        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun tierTwoCannotBypassPipelineWithAuthorizationEnumAlone() {
        val fixture = protectedFixture()
        val plan = fixture.plan
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(
            setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, plan.sessionId))
        )
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registryFor(plan)),
            pipelineFor(policy, fixture.identityManager),
            adapter
        )
        val result = bridge.execute(
            plan,
            true,
            AuthorizationLevel.USER_CONFIRMATION,
            identitySession = fixture.session
        )
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun tierTwoRequiresMatchingOneTimeTokenBeforeAdapter() {
        val fixture = protectedFixture()
        val plan = fixture.plan
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(
            setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, plan.sessionId))
        )
        val policy = PolicyEngine(permissions)
        val authGate = ActionAuthorizationGate()
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registryFor(plan)),
            pipelineFor(policy, fixture.identityManager, authGate),
            adapter
        )
        val token = authGate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, System.currentTimeMillis())
        val result = bridge.execute(
            plan,
            true,
            AuthorizationLevel.USER_CONFIRMATION,
            authorizationToken = token,
            identitySession = fixture.session
        )
        assertTrue(result.success)
        assertEquals(1, adapter.calls)
    }

    @Test fun wrongPlanTokenCannotAuthorizeExecution() {
        val fixture = protectedFixture()
        val plan = fixture.plan
        val otherPlan = plan.copy(action = "delete")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(
            setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, plan.sessionId))
        )
        val policy = PolicyEngine(permissions)
        val gate = ActionAuthorizationGate()
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registryFor(plan)),
            pipelineFor(policy, fixture.identityManager, gate),
            adapter
        )
        val token = gate.issue(otherPlan, AuthorizationLevel.USER_CONFIRMATION, System.currentTimeMillis())
        val result = bridge.execute(
            plan,
            true,
            AuthorizationLevel.USER_CONFIRMATION,
            authorizationToken = token,
            identitySession = fixture.session
        )
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun authorizationTokenCannotBeReplayed() {
        val fixture = protectedFixture()
        val plan = fixture.plan
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(
            setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, plan.sessionId))
        )
        val policy = PolicyEngine(permissions)
        val gate = ActionAuthorizationGate()
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registryFor(plan)),
            pipelineFor(policy, fixture.identityManager, gate),
            adapter
        )
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, System.currentTimeMillis())
        val first = bridge.execute(
            plan,
            true,
            AuthorizationLevel.USER_CONFIRMATION,
            authorizationToken = token,
            identitySession = fixture.session
        )
        val second = bridge.execute(
            plan,
            true,
            AuthorizationLevel.USER_CONFIRMATION,
            authorizationToken = token,
            identitySession = fixture.session
        )
        assertTrue(first.success)
        assertFalse(second.success)
        assertEquals(1, adapter.calls)
    }

    @Test fun adapterFailureIsContainedAndDoesNotReportSuccess() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED")
        val adapter = ThrowingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registryFor(plan)),
            pipelineFor(policy, IdentitySessionManager()),
            adapter
        )
        val result = bridge.execute(plan, true)
        assertFalse(result.success)
        assertFalse(result.verified)
        assertEquals(1, adapter.calls)
    }

    @Test fun emergencyStopBlocksExecution() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED")
        val adapter = RecordingAdapter()
        val stop = EmergencyStopController()
        stop.activate()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions, stop)
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registryFor(plan)),
            pipelineFor(policy, IdentitySessionManager()),
            adapter
        )
        val result = bridge.execute(plan, true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun bridgeAndPipelineShareTheSameAuditSinkByDefault() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions)
        val pipeline = pipelineFor(policy, IdentitySessionManager())
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipeline, adapter)

        val result = bridge.execute(plan, true)

        assertTrue(result.success)
        assertTrue(pipeline.audit().any { it.eventType == AuditEventType.POLICY_DECISION && it.allowed })
        assertTrue(pipeline.audit().any { it.eventType == AuditEventType.EXECUTION && it.allowed })
        assertTrue(pipeline.audit().any { it.eventType == AuditEventType.VERIFICATION && it.verified })
    }

    @Test fun bridgeAuditTimestampsAreSampledAtEachEmission() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions)
        val pipeline = pipelineFor(policy, IdentitySessionManager())
        var now = 1000L
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registryFor(plan)),
            pipeline,
            adapter,
            clock = { now += 1000L; now }
        )

        val result = bridge.execute(plan, true)
        assertTrue(result.success)

        val execution = pipeline.audit().single { it.eventType == AuditEventType.EXECUTION }
        val verification = pipeline.audit().single { it.eventType == AuditEventType.VERIFICATION }
        assertTrue(execution.timestampEpochMillis < verification.timestampEpochMillis)
        assertEquals(4000L, execution.timestampEpochMillis)
        assertEquals(5000L, verification.timestampEpochMillis)
    }

    @Test fun mismatchedObservedStateCannotBeReportedAsVerified() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED")
        val adapter = object : AndroidActionAdapter {
            override fun execute(plan: ActionPlan) =
                ExecutionResult(true, false, "wrong state", observedState = "CLOSED")
        }
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(
            CapabilityPolicyGate(registryFor(plan)),
            pipelineFor(policy, IdentitySessionManager()),
            adapter
        )
        val result = bridge.execute(plan, userExplicitlyRequested = true)
        assertFalse(result.success)
        assertFalse(result.verified)
    }
}
