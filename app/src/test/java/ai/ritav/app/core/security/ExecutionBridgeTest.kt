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

    @Test fun deniedActionNeverReachesAdapter() {
        val adapter = RecordingAdapter()
        val bridge = ExecutionBridge(PolicyEngine(), adapter)
        val result = bridge.execute(
            plan = ActionPlan("demo", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE),
            userExplicitlyRequested = true
        )
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }

    @Test fun permittedActionReachesOnlyApprovedAdapter() {
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(
            setOf(CapabilityGrant("demo", Capability.APP_LAUNCH, "open"))
        )
        val bridge = ExecutionBridge(PolicyEngine(permissions), adapter)
        val result = bridge.execute(
            plan = ActionPlan("demo", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE),
            userExplicitlyRequested = true
        )
        assertTrue(result.success)
        assertEquals(1, adapter.calls)
    }

    @Test fun sensitiveExecutionIsBlockedAtFinalBoundary() {
        val adapter = RecordingAdapter()
        val permissions = InMemoryPermissionStore(
            setOf(CapabilityGrant("demo", Capability.APP_LAUNCH, "open"))
        )
        val bridge = ExecutionBridge(PolicyEngine(permissions), adapter)
        val result = bridge.execute(
            plan = ActionPlan("demo", Capability.APP_LAUNCH, "open", RiskTier.TIER_1_REVERSIBLE),
            userExplicitlyRequested = true,
            containsSensitiveData = true
        )
        assertFalse(result.success)
        assertEquals(0, adapter.calls)
    }
}
