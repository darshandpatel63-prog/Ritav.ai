package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyEngineTest {
    private fun request(riskTier: RiskTier = RiskTier.TIER_0_INFORMATIONAL, capability: Capability = Capability.READ_ALLOWED_CONTENT, authorization: AuthorizationLevel = AuthorizationLevel.NONE, explicit: Boolean = false, sensitive: Boolean = false) = ActionRequest(appId = "com.example.app", action = "read", riskTier = riskTier, capability = capability, userExplicitlyRequested = explicit, authorizationLevel = authorization, containsSensitiveData = sensitive)

    @Test fun emergencyStopAlwaysDenies() { val stop = EmergencyStopController(); val engine = PolicyEngine(InMemoryPermissionStore(), stop); stop.activate(); assertFalse(engine.evaluate(request()).allowed) }

    @Test fun financialActionIsHardDeniedEvenWithAuthorization() { val engine = PolicyEngine(); val decision = engine.evaluate(request(RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE, Capability.FINANCIAL_ACTION, AuthorizationLevel.DEVICE_AUTHENTICATION, true)); assertFalse(decision.allowed) }

    @Test fun sensitiveDataIsDenied() { assertFalse(PolicyEngine().evaluate(request(sensitive = true)).allowed) }

    @Test fun ungrantedCapabilityIsDenied() { assertFalse(PolicyEngine().evaluate(request()).allowed) }

    @Test fun tierTwoRequiresUserConfirmation() { val store = InMemoryPermissionStore(setOf(CapabilityGrant("com.example.app", Capability.READ_ALLOWED_CONTENT, "read"))); val decision = PolicyEngine(store).evaluate(request(RiskTier.TIER_2_CONTENT_MUTATION)); assertFalse(decision.allowed); assertTrue(decision.requiredAuthorization == AuthorizationLevel.USER_CONFIRMATION) }

    @Test fun tierThreeRequiresExplicitIntentAndDeviceAuth() { val store = InMemoryPermissionStore(setOf(CapabilityGrant("com.example.app", Capability.READ_ALLOWED_CONTENT, "read"))); val engine = PolicyEngine(store); assertFalse(engine.evaluate(request(RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE, authorization = AuthorizationLevel.DEVICE_AUTHENTICATION)).allowed); assertTrue(engine.evaluate(request(RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE, authorization = AuthorizationLevel.DEVICE_AUTHENTICATION, explicit = true)).allowed) }
}
