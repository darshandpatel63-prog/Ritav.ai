package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityPolicyGateTest {
    private val registry = AppCapabilityRegistry(
        listOf(
            AppCapabilitySpec(
                packageName = "com.example.safe",
                capability = Capability.APP_LAUNCH,
                actions = setOf("open"),
                riskTier = RiskTier.TIER_1_REVERSIBLE
            )
        )
    )
    private val gate = CapabilityPolicyGate(registry)

    @Test fun unknownAppIsDenied() {
        assertFalse(gate.evaluate(ActionRequest("com.example.unknown", "open", RiskTier.TIER_1_REVERSIBLE, Capability.APP_LAUNCH)).allowed)
    }

    @Test fun unregisteredActionIsDenied() {
        assertFalse(gate.evaluate(ActionRequest("com.example.safe", "delete", RiskTier.TIER_1_REVERSIBLE, Capability.APP_LAUNCH)).allowed)
    }

    @Test fun wrongRiskTierIsDenied() {
        assertFalse(gate.evaluate(ActionRequest("com.example.safe", "open", RiskTier.TIER_2_CONTENT_MUTATION, Capability.APP_LAUNCH)).allowed)
    }

    @Test fun registeredCapabilityIsAllowed() {
        assertTrue(gate.evaluate(ActionRequest("com.example.safe", "open", RiskTier.TIER_1_REVERSIBLE, Capability.APP_LAUNCH)).allowed)
    }

    @Test fun financialCapabilityIsDenied() {
        assertFalse(gate.evaluate(ActionRequest("com.example.safe", "pay", RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED, Capability.FINANCIAL_ACTION)).allowed)
    }
}
