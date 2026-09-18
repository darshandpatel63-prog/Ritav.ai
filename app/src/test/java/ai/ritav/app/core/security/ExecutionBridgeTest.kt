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

    private fun registryFor(plan: ActionPlan) = AppCapabilityRegistry(
        listOf(AppCapabilitySpec(plan.appId, plan.capability, setOf(plan.action), plan.riskTier))
    )

    private fun pipelineFor(policy: PolicyEngine, gate: ActionAuthorizationGate = ActionAuthorizationGate()) =
        SecurityExecutionPipeline(policy, ExecutionPolicyGate(policy), gate)

    private fun identity(sessionId: String = "s1"): SecuritySession {
        val now = System.currentTimeMillis()
        return SecuritySession(sessionId, IdentityLevel.OWNER_SIGNAL, now - 1_000L, now + 60_000L)
    }

    @Test fun unregisteredCapabilityNeverReachesAdapter() {
        val adapter = RecordingAdapter()
        val policy = PolicyEngine()
        val bridge = ExecutionBridge(CapabilityPolicyGate(AppCapabilityRegistry()), pipelineFor(policy), adapter)
        val result = bridge.execute(ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED"), true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun registeredCapabilityReachesApprovedAdapter() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy), adapter)
        val result = bridge.execute(plan, userExplicitlyRequested = true)
        assertTrue(result.success)
        assertEquals(1, adapter.calls)
    }

    @Test fun wrongRiskRegistrationNeverReachesAdapter() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_2_CONTENT_MUTATION)
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val registry = AppCapabilityRegistry(listOf(AppCapabilitySpec(plan.appId, plan.capability, setOf(plan.action), RiskTier.TIER_1_REVERSIBLE)))
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(CapabilityPolicyGate(registry), pipelineFor(policy), adapter)
        val result = bridge.execute(plan, userExplicitlyRequested = true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun sensitiveExecutionIsBlockedAtFinalBoundary() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy), adapter)
        val result = bridge.execute(plan, true, containsSensitiveData = true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun sensitiveInputIsBlockedAtBridgeAndAuthorizationTokenRemainsUsable() {
        val plan = ActionPlan("demo.app", Capability.UI_AUTOMATION, "edit", RiskTier.TIER_2_CONTENT_MUTATION, expectedState = "EDITED", sessionId = "s1")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, "s1")))
        val policy = PolicyEngine(permissions)
        val authGate = ActionAuthorizationGate()
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy, authGate), adapter)
        val token = authGate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, System.currentTimeMillis())

        val blocked = bridge.execute(
            plan,
            userExplicitlyRequested = true,
            authorizationLevel = AuthorizationLevel.USER_CONFIRMATION,
            authorizationToken = token,
            identitySession = identity(),
            inputText = "OTP: 123456"
        )
        assertFalse(blocked.success)
        assertEquals(0, adapter.calls)

        val allowed = bridge.execute(
            plan,
            userExplicitlyRequested = true,
            authorizationLevel = AuthorizationLevel.USER_CONFIRMATION,
            authorizationToken = token,
            identitySession = identity(),
            inputText = "open the editor"
        )
        assertTrue(allowed.success)
        assertEquals(1, adapter.calls)
    }

    @Test fun financialCapabilityCannotReachAdapterEvenWhenRegistered() {
        val plan = ActionPlan("bank.app", Capability.FINANCIAL_ACTION, "transfer", RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED, expectedState = "TRANSFERRED")
        val adapter = RecordingAdapter()
        val registry = AppCapabilityRegistry(listOf(AppCapabilitySpec(plan.appId, plan.capability, setOf(plan.action), plan.riskTier, financialCategory = true)))
        val policy = PolicyEngine()
        val bridge = ExecutionBridge(CapabilityPolicyGate(registry), pipelineFor(policy), adapter)
        val result = bridge.execute(plan, true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun tierTwoCannotBypassPipelineWithAuthorizationEnumAlone() {
        val plan = ActionPlan("demo.app", Capability.UI_AUTOMATION, "edit", RiskTier.TIER_2_CONTENT_MUTATION, expectedState = "EDITED", sessionId = "s1")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, "s1")))
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy), adapter)
        val result = bridge.execute(plan, true, AuthorizationLevel.USER_CONFIRMATION, identitySession = identity())
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun tierTwoRequiresMatchingOneTimeTokenBeforeAdapter() {
        val plan = ActionPlan("demo.app", Capability.UI_AUTOMATION, "edit", RiskTier.TIER_2_CONTENT_MUTATION, expectedState = "EDITED", sessionId = "s1")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, "s1")))
        val policy = PolicyEngine(permissions)
        val authGate = ActionAuthorizationGate()
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy, authGate), adapter)
        val token = authGate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, System.currentTimeMillis())
        val result = bridge.execute(plan, true, AuthorizationLevel.USER_CONFIRMATION, authorizationToken = token, identitySession = identity())
        assertTrue(result.success)
        assertEquals(1, adapter.calls)
    }

    @Test fun wrongPlanTokenCannotAuthorizeExecution() {
        val plan = ActionPlan("demo.app", Capability.UI_AUTOMATION, "edit", RiskTier.TIER_2_CONTENT_MUTATION, expectedState = "EDITED", sessionId = "s1")
        val otherPlan = plan.copy(action = "delete")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, "s1")))
        val policy = PolicyEngine(permissions)
        val gate = ActionAuthorizationGate()
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy, gate), adapter)
        val token = gate.issue(otherPlan, AuthorizationLevel.USER_CONFIRMATION, System.currentTimeMillis())
        val result = bridge.execute(plan, true, AuthorizationLevel.USER_CONFIRMATION, authorizationToken = token, identitySession = identity())
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun authorizationTokenCannotBeReplayed() {
        val plan = ActionPlan("demo.app", Capability.UI_AUTOMATION, "edit", RiskTier.TIER_2_CONTENT_MUTATION, expectedState = "EDITED", sessionId = "s1")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, "s1")))
        val policy = PolicyEngine(permissions)
        val gate = ActionAuthorizationGate()
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy, gate), adapter)
        val token = gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, System.currentTimeMillis())
        val first = bridge.execute(plan, true, AuthorizationLevel.USER_CONFIRMATION, authorizationToken = token, identitySession = identity())
        val second = bridge.execute(plan, true, AuthorizationLevel.USER_CONFIRMATION, authorizationToken = token, identitySession = identity())
        assertTrue(first.success)
        assertFalse(second.success)
        assertEquals(1, adapter.calls)
    }

    @Test fun adapterFailureIsContainedAndDoesNotReportSuccess() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED")
        val adapter = ThrowingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy), adapter)
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
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy), adapter)
        val result = bridge.execute(plan, true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun bridgeAndPipelineShareTheSameAuditSinkByDefault() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions)
        val pipeline = pipelineFor(policy)
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
        val pipeline = pipelineFor(policy)
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
}


    @Test fun mismatchedObservedStateCannotBeReportedAsVerified() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE, expectedState = "OPENED")
        val adapter = object : AndroidActionAdapter {
            override fun execute(plan: ActionPlan) =
                ExecutionResult(true, false, "wrong state", observedState = "CLOSED")
        }
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy), adapter)
        val result = bridge.execute(plan, userExplicitlyRequested = true)
        assertFalse(result.success)
        assertFalse(result.verified)
    }
}
