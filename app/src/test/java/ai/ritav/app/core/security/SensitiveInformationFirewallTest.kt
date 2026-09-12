package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensitiveInformationFirewallTest {
    private val firewall = SensitiveInformationFirewall()

    @Test fun otpIsDetectedAndRedacted() {
        val result = firewall.inspect("Use OTP 123456 to continue")
        assertBlockedWithType(result, SensitiveDataType.OTP)
        assertFalse(result.redactedText.contains("123456"))
    }

    @Test fun upiPinIsDetectedAndRedacted() {
        val result = firewall.inspect("UPI PIN is 1234")
        assertBlockedWithType(result, SensitiveDataType.UPI_PIN)
        assertFalse(result.redactedText.contains("1234"))
    }

    @Test fun cvvIsDetectedAndRedacted() {
        val result = firewall.inspect("CVV: 123")
        assertBlockedWithType(result, SensitiveDataType.CVV)
        assertFalse(result.redactedText.contains("123"))
    }

    @Test fun passwordContextIsDetectedAndRedacted() {
        val result = firewall.inspect("password = MySecret123!")
        assertBlockedWithType(result, SensitiveDataType.PASSWORD)
        assertFalse(result.redactedText.contains("MySecret123!"))
    }

    @Test fun recoveryCodeIsDetectedAndRedacted() {
        val result = firewall.inspect("recovery code: ABCD-1234")
        assertBlockedWithType(result, SensitiveDataType.RECOVERY_CODE)
        assertFalse(result.redactedText.contains("ABCD-1234"))
    }

    @Test fun privateKeyIsDetectedAndRedacted() {
        val key = "-----BEGIN PRIVATE KEY-----\nabc123\n-----END PRIVATE KEY-----"
        val result = firewall.inspect("key=$key")
        assertBlockedWithType(result, SensitiveDataType.PRIVATE_KEY)
        assertFalse(result.redactedText.contains("abc123"))
        assertFalse(result.redactedText.contains("BEGIN PRIVATE KEY"))
    }

    @Test fun apiKeyIsDetectedAndRedacted() {
        val result = firewall.inspect("api_key=AbCdEfGhIjKlMnOp")
        assertBlockedWithType(result, SensitiveDataType.API_KEY)
        assertFalse(result.redactedText.contains("AbCdEfGhIjKlMnOp"))
    }

    @Test fun benignTextIsAllowedUnchanged() {
        val text = "Please remind me to study anatomy tomorrow."
        val result = firewall.inspect(text)
        assertTrue(result.allowed)
        assertEquals(text, result.redactedText)
        assertTrue(result.matches.isEmpty())
        assertEquals(null, result.blockReason)
    }

    @Test fun multipleSecretsAreAllDetected() {
        val result = firewall.inspect("OTP 123456 and CVV 987")
        assertFalse(result.allowed)
        assertEquals(2, result.matches.size)
        assertTrue(result.matches.any { it.type == SensitiveDataType.OTP })
        assertTrue(result.matches.any { it.type == SensitiveDataType.CVV })
        assertFalse(result.redactedText.contains("123456"))
        assertFalse(result.redactedText.contains("987"))
    }

    @Test fun overlappingMatchesDoNotCorruptRedaction() {
        val result = firewall.inspect("security code: 123456")
        assertFalse(result.allowed)
        assertTrue(result.matches.size <= 1)
        assertTrue(result.redactedText.contains("[REDACTED:"))
        assertFalse(result.redactedText.contains("123456"))
    }

    @Test fun matchObjectsNeverContainTheSecret() {
        val secret = "123456"
        val result = firewall.inspect("OTP $secret")
        assertFalse(result.allowed)
        result.matches.forEach { match ->
            assertTrue(match.start >= 0)
            assertTrue(match.end <= "OTP $secret".length)
            assertFalse(match.toString().contains(secret))
        }
    }

    @Test fun exactMaximumLengthInputIsInspectable() {
        val text = "a".repeat(16_384)
        val result = firewall.inspect(text)
        assertTrue(result.allowed)
        assertEquals(text, result.redactedText)
    }

    @Test fun oversizedInputFailsClosedWithoutThrowing() {
        val result = firewall.inspect("a".repeat(16_385))
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.INPUT_TOO_LARGE, result.blockReason)
        assertTrue(result.matches.isEmpty())
        assertEquals("", result.redactedText)
    }

    @Test fun oversizedInputContainingSecretIsStillBlockedWithoutInspectingIt() {
        val result = firewall.inspect("x".repeat(16_380) + " OTP 123456")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.INPUT_TOO_LARGE, result.blockReason)
        assertTrue(result.matches.isEmpty())
        assertEquals("", result.redactedText)
    }

    @Test fun unicodeNormalizedSecretIsBlockedConservatively() {
        val result = firewall.inspect("OTP\u00a0123456")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
        assertTrue(result.matches.isEmpty())
    }

    @Test fun unusualInputDoesNotCrash() {
        val unusual = "\u0000\u0001\u0002\uD800\uFFFF\n\t" + "🙂".repeat(100)
        val result = firewall.inspect(unusual)
        assertTrue(result.allowed || !result.allowed)
    }

    private fun assertBlockedWithType(result: FirewallResult, type: SensitiveDataType) {
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.SENSITIVE_DATA_DETECTED, result.blockReason)
        assertTrue(result.matches.any { it.type == type })
        assertTrue(result.redactedText.contains("[REDACTED:$type"))
    }
}
