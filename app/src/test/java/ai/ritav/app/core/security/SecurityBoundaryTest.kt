package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityBoundaryTest {
    private val firewall = SensitiveInformationFirewall()

    @Test fun invalidRequestIsDenied() {
        val result = PolicyEngine().evaluate(ActionRequest("", "", RiskTier.TIER_0_INFORMATIONAL))
        assertFalse(result.allowed)
    }

    @Test fun contextualSecretDetectionAvoidsGenericFourDigits() {
        assertFalse(firewall.inspect("OTP: 123456").allowed)
        assertFalse(firewall.inspect("CVV=123").allowed)
        assertTrue(firewall.inspect("room 1234").allowed)
    }

    @Test fun redactionRemovesDetectedSecretValue() {
        val redacted = firewall.inspect("OTP: 123456").redactedText
        assertFalse(redacted.contains("123456"))
    }

    @Test fun auditLogStoresStructuredMetadataWithoutActionPayload() {
        val log = InMemoryAuditLog()
        log.append(
            AuditEvent(
                timestampEpochMillis = 1L,
                sessionId = "session-1",
                actionHash = "hash",
                eventType = AuditEventType.POLICY_DECISION,
                allowed = false,
                verified = false,
                reason = "Denied"
            )
        )

        val event = log.readAll().single()
        assertEquals("hash", event.actionHash)
        assertEquals("Denied", event.reason)
    }
}
