package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyEngineTest {
    @Test fun deniesTier4() {
        val result = PolicyEngine().evaluate(ActionRequest("bank", "pay", RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED, permissionGranted = true, userExplicitlyRequested = true, authorizationLevel = AuthorizationLevel.DEVICE_AUTHENTICATION))
        assertFalse(result.allowed)
    }

    @Test fun deniesWithoutPermission() {
        val result = PolicyEngine().evaluate(ActionRequest("demo", "open", RiskTier.TIER_1_REVERSIBLE))
        assertFalse(result.allowed)
    }

    @Test fun requiresUserConfirmationForMutation() {
        val result = PolicyEngine().evaluate(ActionRequest("chat", "send", RiskTier.TIER_2_CONTENT_MUTATION, permissionGranted = true, userExplicitlyRequested = true))
        assertFalse(result.allowed)
        assertTrue(result.requiresConfirmation)
        assertEquals(AuthorizationLevel.USER_CONFIRMATION, result.requiredAuthorization)
    }

    @Test fun mutationAllowedAfterConfirmation() {
        val result = PolicyEngine().evaluate(ActionRequest("chat", "send", RiskTier.TIER_2_CONTENT_MUTATION, permissionGranted = true, userExplicitlyRequested = true, authorizationLevel = AuthorizationLevel.USER_CONFIRMATION))
        assertTrue(result.allowed)
    }

    @Test fun tier3RequiresDeviceAuthentication() {
        val result = PolicyEngine().evaluate(ActionRequest("chat", "send", RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE, permissionGranted = true, userExplicitlyRequested = true, authorizationLevel = AuthorizationLevel.USER_CONFIRMATION))
        assertFalse(result.allowed)
        assertEquals(AuthorizationLevel.DEVICE_AUTHENTICATION, result.requiredAuthorization)
    }

    @Test fun emergencyStopBlocks() {
        val engine = PolicyEngine()
        engine.stop()
        val result = engine.evaluate(ActionRequest("demo", "open", RiskTier.TIER_1_REVERSIBLE, permissionGranted = true))
        assertFalse(result.allowed)
    }

    @Test fun blankAppOrActionFailsClosed() {
        val result = PolicyEngine().evaluate(ActionRequest("", "", RiskTier.TIER_0_INFORMATIONAL, permissionGranted = true))
        assertFalse(result.allowed)
    }

    @Test fun firewallFlagsSecretTerms() {
        assertTrue(SensitiveDataFirewall.containsSecretLikeContent("enter OTP 123456"))
        assertTrue(SensitiveDataFirewall.containsSecretLikeContent("my password"))
        assertFalse(SensitiveDataFirewall.containsSecretLikeContent("open settings"))
    }
}
