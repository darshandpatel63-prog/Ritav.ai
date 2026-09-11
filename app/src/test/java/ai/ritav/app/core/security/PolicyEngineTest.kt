package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyEngineTest {
    private fun store() = InMemoryPermissionStore(
        setOf(CapabilityGrant("demo", Capability.APP_LAUNCH, "open", sessionId = null))
    )

    @Test fun deniesTier4() {
        val result = PolicyEngine(store()).evaluate(ActionRequest("demo", "open", RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED, capability = Capability.APP_LAUNCH))
        assertFalse(result.allowed)
    }

    @Test fun deniesWithoutScopedPermission() {
        val result = PolicyEngine().evaluate(ActionRequest("demo", "open", RiskTier.TIER_1_REVERSIBLE, capability = Capability.APP_LAUNCH))
        assertFalse(result.allowed)
    }

    @Test fun deniesWrongAppEvenWhenActionMatches() {
        val result = PolicyEngine(store()).evaluate(ActionRequest("other", "open", RiskTier.TIER_1_REVERSIBLE, capability = Capability.APP_LAUNCH))
        assertFalse(result.allowed)
    }

    @Test fun tier2RequiresUserConfirmation() {
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.UI_AUTOMATION, "edit")))
        val withoutAuth = PolicyEngine(permissions).evaluate(ActionRequest("demo", "edit", RiskTier.TIER_2_CONTENT_MUTATION, capability = Capability.UI_AUTOMATION, userExplicitlyRequested = true))
        assertFalse(withoutAuth.allowed)
        val confirmed = PolicyEngine(permissions).evaluate(ActionRequest("demo", "edit", RiskTier.TIER_2_CONTENT_MUTATION, capability = Capability.UI_AUTOMATION, userExplicitlyRequested = true, authorizationLevel = AuthorizationLevel.USER_CONFIRMATION))
        assertTrue(confirmed.allowed)
        assertTrue(confirmed.requiresConfirmation.not())
    }

    @Test fun tier3RequiresDeviceAuthentication() {
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant("demo", Capability.SEND_MESSAGE, "send")))
        val result = PolicyEngine(permissions).evaluate(ActionRequest("demo", "send", RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE, capability = Capability.SEND_MESSAGE, userExplicitlyRequested = true, authorizationLevel = AuthorizationLevel.USER_CONFIRMATION))
        assertFalse(result.allowed)
    }

    @Test fun financialCapabilityIsBlockedByDefault() {
        val permissions = InMemoryPermissionStore(setOf(CapabilityGrant("bank", Capability.FINANCIAL_ACTION, "pay")))
        val result = PolicyEngine(permissions).evaluate(ActionRequest("bank", "pay", RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE, capability = Capability.FINANCIAL_ACTION, userExplicitlyRequested = true, authorizationLevel = AuthorizationLevel.DEVICE_AUTHENTICATION))
        assertFalse(result.allowed)
    }

    @Test fun emergencyStopBlocks() {
        val engine = PolicyEngine(store())
        engine.stop()
        val result = engine.evaluate(ActionRequest("demo", "open", RiskTier.TIER_1_REVERSIBLE, capability = Capability.APP_LAUNCH))
        assertFalse(result.allowed)
    }

    @Test fun firewallUsesContextNotAnyFourDigitNumber() {
        assertTrue(SensitiveDataFirewall.containsSecretLikeContent("enter OTP 123456"))
        assertTrue(SensitiveDataFirewall.containsSecretLikeContent("password: secret-value"))
        assertFalse(SensitiveDataFirewall.containsSecretLikeContent("open room 1234"))
    }
}
