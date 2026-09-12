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
            return ExecutionResult(true, false, "adapter called")
        }
    }

    private fun registryFor(plan: ActionPlan) = AppCapabilityRegistry(
        listOf(AppCapabilitySpec(plan.appId, plan.capability, setOf(plan.action), plan.riskTier))
    )

    private fun pipelineFor(policy: PolicyEngine, gate: ActionAuthorizationGate = ActionAuthorizationGate()) =
        SecurityExecutionPipeline(policy, ExecutionPolicyGate(policy), gate)

    @Test fun unregisteredCapabilityNeverReachesAdapter() {
        val adapter = RecordingAdapter()
        val policy = PolicyEngine()
        val bridge = ExecutionBridge(CapabilityPolicyGate(AppCapabilityRegistry()), pipelineFor(policy), adapter)
        val result = bridge.execute(ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE), true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun registeredCapabilityReachesApprovedAdapter() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE)
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
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE)
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action)))
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy), adapter)
        val result = bridge.execute(plan, true, containsSensitiveData = true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun financialCapabilityCannotReachAdapterEvenWhenRegistered() {
        val plan = ActionPlan("bank.app", Capability.FINANCIAL_ACTION, "transfer", RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED)
        val adapter = RecordingAdapter()
        val registry = AppCapabilityRegistry(listOf(AppCapabilitySpec(plan.appId, plan.capability, setOf(plan.action), plan.riskTier, financialCategory = true)))
        val policy = PolicyEngine()
        val bridge = ExecutionBridge(CapabilityPolicyGate(registry), pipelineFor(policy), adapter)
        val result = bridge.execute(plan, true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun tierTwoCannotBypassPipelineWithAuthorizationEnumAlone() {
        val plan = ActionPlan("demo.app", Capability.UI_AUTOMATION, "edit", RiskTier.TIER_2_CONTENT_MUTATION, "s1")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, "s1")))
        val policy = PolicyEngine(permissions)
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy), adapter)
        val identity = SecuritySession("s1", IdentityLevel.OWNER_SIGNAL, 1000L, 61000L)
        val result = bridge.execute(plan, true, AuthorizationLevel.USER_CONFIRMATION, identitySession = identity)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun tierTwoRequiresMatchingOneTimeTokenBeforeAdapter() {
        val plan = ActionPlan("demo.app", Capability.UI_AUTOMATION, "edit", RiskTier.TIER_2_CONTENT_MUTATION, "s1")
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant(plan.appId, plan.capability, plan.action, "s1")))
        val policy = PolicyEngine(permissions)
        val authGate = ActionAuthorizationGate()
        val bridge = ExecutionBridge(CapabilityPolicyGate(registryFor(plan)), pipelineFor(policy, authGate), adapter)
        val identity = SecuritySession("s1", IdentityLevel.OWNER_SIGNAL, 1000L, 61000L)
        val token = authGate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, 1000L)
        val result = bridge.execute(plan, true, AuthorizationLevel.USER_CONFIRMATION, authorizationToken = token, identitySession = identity)
        assertTrue(result.success)
        assertEquals(1, adapter.calls)
    }
}
