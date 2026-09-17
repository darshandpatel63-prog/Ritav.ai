package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FinanceExecutionFirewallTest {
    private val firewall = FinanceExecutionFirewall()

    @Test fun financialCapabilityIsAlwaysDenied() {
        val request = ActionRequest(
            appId = "bank.app",
            action = "transfer",
            riskTier = RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE,
            capability = Capability.FINANCIAL_ACTION,
            userExplicitlyRequested = true,
            authorizationLevel = AuthorizationLevel.DEVICE_AUTHENTICATION
        )

        val result = firewall.inspect(request)

        assertFalse(result.allowed)
        assertEquals("Financial execution is blocked by the dedicated finance firewall", result.reason)
    }

    @Test fun authorizationAndExplicitIntentCannotOverrideFinanceDeny() {
        val request = ActionRequest(
            appId = "upi.app",
            action = "pay",
            riskTier = RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE,
            capability = Capability.FINANCIAL_ACTION,
            userExplicitlyRequested = true,
            authorizationLevel = AuthorizationLevel.DEVICE_AUTHENTICATION
        )

        assertFalse(firewall.inspect(request).allowed)
    }

    @Test fun nonFinancialCapabilityIsOutsideThisBoundary() {
        val request = ActionRequest(
            appId = "demo.app",
            action = "open",
            riskTier = RiskTier.TIER_1_REVERSIBLE,
            capability = Capability.APP_LAUNCH
        )

        val result = firewall.inspect(request)

        assertTrue(result.allowed)
    }
}
