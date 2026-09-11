package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyEngineTest {
    @Test fun deniesTier4() {
        val result = PolicyEngine().evaluate(ActionRequest("bank", "pay", RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED, permissionGranted = true, userExplicitlyRequested = true))
        assertFalse(result.allowed)
    }

    @Test fun deniesWithoutPermission() {
        val result = PolicyEngine().evaluate(ActionRequest("demo", "open", RiskTier.TIER_1_REVERSIBLE))
        assertFalse(result.allowed)
    }

    @Test fun requiresConfirmationForMutation() {
        val result = PolicyEngine().evaluate(ActionRequest("chat", "send", RiskTier.TIER_2_CONTENT_MUTATION, permissionGranted = true, userExplicitlyRequested = true))
        assertTrue(result.allowed)
        assertTrue(result.requiresConfirmation)
    }

    @Test fun emergencyStopBlocks() {
        val engine = PolicyEngine()
        engine.stop()
        val result = engine.evaluate(ActionRequest("demo", "open", RiskTier.TIER_1_REVERSIBLE, permissionGranted = true))
        assertFalse(result.allowed)
    }

    @Test fun firewallFlagsSecretTerms() {
        assertTrue(SensitiveDataFirewall.containsSecretLikeContent("enter OTP 123456"))
        assertTrue(SensitiveDataFirewall.containsSecretLikeContent("my password"))
        assertFalse(SensitiveDataFirewall.containsSecretLikeContent("open settings"))
    }
}
