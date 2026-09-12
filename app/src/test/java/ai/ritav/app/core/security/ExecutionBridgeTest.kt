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
        listOf(
            AppCapabilitySpec(
                packageName = plan.appId,
                capability = plan.capability,
                actions = setOf(plan.action),
                riskTier = plan.riskTier
            )
        )
    )

    @Test fun unregisteredCapabilityNeverReachesAdapter() {
        val adapter = RecordingAdapter()
        val bridge = ExecutionBridge(
            PolicyEngine(),
            CapabilityPolicyGate(AppCapabilityRegistry()),
            adapter
        )
        val result = bridge.execute(
            ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE),
            userExplicitlyRequested = true
        )
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun registeredCapabilityReachesApprovedAdapter() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE)
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(
            setOf(CapabilityGrant(plan.appId, plan.capability, plan.action))
        )
        val bridge = ExecutionBridge(
            PolicyEngine(permissions),
            CapabilityPolicyGate(registryFor(plan)),
            adapter
        )
        val result = bridge.execute(plan, userExplicitlyRequested = true)
        assertTrue(result.success)
        assertEquals(1, adapter.calls)
    }

    @Test fun wrongRiskRegistrationNeverReachesAdapter() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_2_CONTENT_MUTATION)
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(
            setOf(CapabilityGrant(plan.appId, plan.capability, plan.action))
        )
        val registry = AppCapabilityRegistry(
            listOf(AppCapabilitySpec(plan.appId, plan.capability, setOf(plan.action), RiskTier.TIER_1_REVERSIBLE))
        )
        val bridge = ExecutionBridge(PolicyEngine(permissions), CapabilityPolicyGate(registry), adapter)
        val result = bridge.execute(plan, userExplicitlyRequested = true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun sensitiveExecutionIsBlockedAtFinalBoundary() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE)
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(
            setOf(CapabilityGrant(plan.appId, plan.capability, plan.action))
        )
        val bridge = ExecutionBridge(
            PolicyEngine(permissions),
            CapabilityPolicyGate(registryFor(plan)),
            adapter
        )
        val result = bridge.execute(
            plan = plan,
            userExplicitlyRequested = true,
            containsSensitiveData = true
        )
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun financialCapabilityCannotReachAdapterEvenWhenRegistered() {
        val plan = ActionPlan("bank.app", Capability.FINANCIAL_ACTION, "transfer", RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED)
        val adapter = RecordingAdapter()
        val registry = AppCapabilityRegistry(
            listOf(AppCapabilitySpec(plan.appId, plan.capability, setOf(plan.action), plan.riskTier, financialCategory = true))
        )
        val bridge = ExecutionBridge(
            PolicyEngine(),
            CapabilityPolicyGate(registry),
            adapter
        )
        val result = bridge.execute(plan, userExplicitlyRequested = true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun policyStillRunsAfterCapabilityGatePasses() {
        val plan = ActionPlan("demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE)
        val adapter = RecordingAdapter()
        val bridge = ExecutionBridge(
            PolicyEngine(),
            CapabilityPolicyGate(registryFor(plan)),
            adapter
        )
        val result = bridge.execute(plan, userExplicitlyRequested = true)
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }
}
