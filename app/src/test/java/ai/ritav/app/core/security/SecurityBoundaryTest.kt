package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityBoundaryTest {
    @Test fun invalidRequestIsDenied() {
        val result = PolicyEngine().evaluate(ActionRequest("", "", RiskTier.TIER_0_INFORMATIONAL))
        assertFalse(result.allowed)
    }

    @Test fun contextualSecretDetectionAvoidsGenericFourDigits() {
        assertTrue(SensitiveDataFirewall.containsSecretLikeContent("OTP: 123456"))
        assertTrue(SensitiveDataFirewall.containsSecretLikeContent("CVV=123"))
        assertFalse(SensitiveDataFirewall.containsSecretLikeContent("room 1234"))
    }

    @Test fun redactionRemovesDetectedSecretValue() {
        val redacted = SensitiveDataFirewall.redactSecrets("OTP: 123456")
        assertFalse(redacted.contains("123456"))
    }

    @Test fun auditLogDoesNotPersistActionPayload() {
        val log = InMemoryAuditLog()
        log.append(AuditEvent(1L, "POLICY", "demo", "secret payload", Capability.TYPE_TEXT, RiskTier.TIER_2_CONTENT_MUTATION, AuthorizationLevel.USER_CONFIRMATION, false, "denied"))
        assertEquals("[ACTION_ID]", log.snapshot().single().action)
    }
}
