package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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

    @Test fun privateKeyWithSpecificAlgorithmLabelIsDetected() {
        val key = "-----BEGIN RSA PRIVATE KEY-----\nabc123\n-----END RSA PRIVATE KEY-----"
        val result = firewall.inspect(key)
        assertBlockedWithType(result, SensitiveDataType.PRIVATE_KEY)
        assertFalse(result.redactedText.contains("RSA PRIVATE KEY"))
    }

    @Test fun malformedPrivateKeyEnvelopeIsNotTreatedAsAValidKey() {
        val malformed = "-----BEGIN PRIVATE KEY-----\nabc123\n-----END PUBLIC KEY-----"
        val result = firewall.inspect(malformed)
        assertTrue(result.allowed)
        assertEquals(malformed, result.redactedText)
    }

    @Test fun longNearMissPrivateKeyEnvelopeIsAllowedWithoutMatchingEndMarker() {
        val body = "A".repeat(15_700)
        val nearMiss = "-----BEGIN PRIVATE KEY-----\n$body\n-----END PUBLIC KEY-----"
        assertTrue(nearMiss.length <= 16_384)
        val result = firewall.inspect(nearMiss)
        assertTrue(result.allowed)
        assertEquals(nearMiss, result.redactedText)
    }

    @Test fun longNearMissPrivateKeyEnvelopeDoesNotExhibitUnexpectedWorkFromRepeatedEndMarkerLikeText() {
        val body = ("-----END PUBLIC KEY-----A").repeat(600)
        val nearMiss = "-----BEGIN PRIVATE KEY-----\n$body\n-----END PUBLIC KEY-----"
        assertTrue(nearMiss.length <= 16_384)
        val result = firewall.inspect(nearMiss)
        assertTrue(result.allowed)
        assertEquals(nearMiss, result.redactedText)
    }

    @Test fun apiKeyIsDetectedAndRedactedEvenWhenSecretEndsWithPunctuation() {
        val result = firewall.inspect("api_key=AbCdEfGhIjKlMnOp-")
        assertBlockedWithType(result, SensitiveDataType.API_KEY)
        assertFalse(result.redactedText.contains("AbCdEfGhIjKlMnOp-"))
    }

    @Test fun apiKeyNaturalLanguageAssignmentIsDetected() {
        val result = firewall.inspect("My API key is AbCdEfGhIjKlMnOp1234")
        assertBlockedWithType(result, SensitiveDataType.API_KEY)
        assertFalse(result.redactedText.contains("AbCdEfGhIjKlMnOp1234"))
    }

    @Test fun accessTokenNaturalLanguageAssignmentIsDetected() {
        val result = firewall.inspect("The access token is AbCdEfGhIjKlMnOp1234")
        assertBlockedWithType(result, SensitiveDataType.API_KEY)
        assertFalse(result.redactedText.contains("AbCdEfGhIjKlMnOp1234"))
    }

    @Test fun secretKeyNaturalLanguageAssignmentIsDetected() {
        val result = firewall.inspect("My secret key is AbCdEfGhIjKlMnOp1234")
        assertBlockedWithType(result, SensitiveDataType.API_KEY)
        assertFalse(result.redactedText.contains("AbCdEfGhIjKlMnOp1234"))
    }

    @Test fun commonStandaloneCredentialFormatsAreDetected() {
        val credentials = listOf(
            "sk-12345678901234567890",
            "sk-proj-12345678901234567890",
            "ghp_12345678901234567890",
            "github_pat_12345678901234567890",
            "xoxb-12345678901234567890",
            "xoxp-12345678901234567890",
            "AKIA1234567890ABCD1234",
            "AIza123456789012345678901234567890"
        )
        credentials.forEach { credential ->
            val result = firewall.inspect("credential=$credential")
            assertBlockedWithType(result, SensitiveDataType.API_KEY)
            assertFalse(result.redactedText.contains(credential))
        }
    }

    @Test fun standaloneCredentialPatternDoesNotMatchShortOrEmbeddedIdentifiers() {
        val texts = listOf(
            "sk-1234567890123456789",
            "prefixsk-12345678901234567890suffix",
            "ghp_1234567890123456789",
            "AKIA1234567890ABCD123",
            "AIza12345678901234567890123456789"
        )
        texts.forEach { text ->
            val result = firewall.inspect(text)
            assertTrue("Unexpected block for: $text", result.allowed)
            assertEquals(text, result.redactedText)
        }
    }

    @Test fun benignTextIsAllowedUnchanged() {
        val text = "Please remind me to study anatomy tomorrow."
        val result = firewall.inspect(text)
        assertTrue(result.allowed)
        assertEquals(text, result.redactedText)
        assertTrue(result.matches.isEmpty())
        assertEquals(null, result.blockReason)
    }

    @Test fun genericTextWithoutSecretMarkersIsAllowed() {
        val text = "The product catalog has a security code field and a numeric example 123."
        val result = firewall.inspect(text)
        assertTrue(result.allowed)
        assertEquals(text, result.redactedText)
    }

    @Test fun secretMarkerWithoutASecretValueIsAllowed() {
        val text = "The settings page contains a recovery code field for account recovery."
        val result = firewall.inspect(text)
        assertTrue(result.allowed)
        assertEquals(text, result.redactedText)
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

    @Test fun redactionPreservesUnmatchedText() {
        val text = "Before OTP 123456 after"
        val result = firewall.inspect(text)
        assertFalse(result.allowed)
        assertTrue(result.redactedText.startsWith("Before "))
        assertTrue(result.redactedText.endsWith(" after"))
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

    @Test fun whitespaceChangedRepresentationIsBlockedConservatively() {
        val result = firewall.inspect("O T P 123456")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
        assertTrue(result.matches.isEmpty())
    }

    @Test fun zeroWidthObfuscationIsBlockedConservatively() {
        val result = firewall.inspect("OT\u200bP 123456")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
        assertTrue(result.matches.isEmpty())
    }

    @Test fun zeroWidthObfuscationInsideDigitsIsBlockedConservatively() {
        val result = firewall.inspect("OTP 1\u200b2\u200b3\u200b4\u200b5\u200b6")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun whitespaceInsideSensitiveDigitsIsBlockedConservatively() {
        val result = firewall.inspect("OTP 12 34 56")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun zeroWidthAndWhitespaceCanBeCombinedWithoutBypassingDetection() {
        val result = firewall.inspect("O\u200b T P 12\u200b 34 56")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun spacedUpiPinMarkerIsBlockedConservatively() {
        val result = firewall.inspect("U P I P I N 1234")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun fullWidthUnicodeDigitsAreNormalizedForDetection() {
        val result = firewall.inspect("OTP １２３４５６")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun unicodeNormalizedSecretIsBlockedConservatively() {
        val result = firewall.inspect("OTP\u00a0123456")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
        assertTrue(result.matches.isEmpty())
    }

    @Test fun commonCyrillicAndGreekLookalikesInOtpMarkerAreBlocked() {
        val result = firewall.inspect("ОΤР 123456")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun confusableFoldDoesNotBlockUnrelatedUnicodeText() {
        val text = "Greek ОΤР example without a numeric code."
        val result = firewall.inspect(text)
        assertTrue(result.allowed)
        assertEquals(text, result.redactedText)
    }

    @Test fun otherUnicodeDecimalDigitsAreFoldedForSensitiveDetection() {
        val result = firewall.inspect("OTP ١٢٣٤٥٦")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun supplementaryUnicodeDecimalDigitsAreFoldedForSensitiveDetection() {
        val supplementaryDigits = (0..5).joinToString("") { digit ->
            String(Character.toChars(0x104A0 + digit))
        }
        val result = firewall.inspect("OTP $supplementaryDigits")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun supplementaryPlaneFormatCharactersCannotBypassSensitiveMarkerCompaction() {
        val formatCharacter = String(Character.toChars(0xE0001))
        val result = firewall.inspect("O${formatCharacter}TP 123456")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
        assertTrue(result.matches.isEmpty())
    }

    @Test fun supplementaryPlaneFormatCharactersCannotBypassSensitiveDigitsCompaction() {
        val formatCharacter = String(Character.toChars(0xE0001))
        val result = firewall.inspect("OTP 12${formatCharacter}34${formatCharacter}56")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun punctuationObfuscationOfSensitiveLabelsIsBlockedConservatively() {
        val cases = listOf(
            "O-T-P 123456",
            "U.P.I PIN 1234",
            "verification-code 123456",
            "api-key=AbCdEfGhIjKlMnOp"
        )
        cases.forEach { text ->
            val result = firewall.inspect(text)
            assertFalse("Expected block for: $text", result.allowed)
            assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
            assertEquals("", result.redactedText)
        }
    }

    @Test fun punctuationCompactionDoesNotBlockUnrelatedText() {
        val text = "The api-key documentation explains verification-code formatting without values."
        val result = firewall.inspect(text)
        assertTrue(result.allowed)
        assertEquals(text, result.redactedText)
    }

    @Test fun punctuationObfuscationInsideSensitiveDigitsIsBlockedConservatively() {
        val result = firewall.inspect("OTP 12-34-56")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun unicodeDecimalDigitsRemainBenignWithoutSensitiveMarker() {
        val text = "Invoice number ١٢٣٤٥٦ is ready."
        val result = firewall.inspect(text)
        assertTrue(result.allowed)
        assertEquals(text, result.redactedText)
    }

    @Test fun compactedOneTimePasswordLabelIsBlockedConservatively() {
        val result = firewall.inspect("one\u200btime password 123456")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun compactedRecoveryCodeLabelIsBlockedConservatively() {
        val result = firewall.inspect("recovery\u200b code: ABCD-1234")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun compactedLoginPasswordLabelIsBlockedConservatively() {
        val result = firewall.inspect("login\u200b password: MySecret123!")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun compactedApiKeyLabelIsBlockedConservatively() {
        val result = firewall.inspect("api\u200b key=AbCdEfGhIjKlMnOp")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun compactedAccessTokenLabelIsBlockedConservatively() {
        val result = firewall.inspect("access\u200b token=AbCdEfGhIjKlMnOp")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun compactedSecretKeyLabelIsBlockedConservatively() {
        val result = firewall.inspect("secret\u200b key=AbCdEfGhIjKlMnOp")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun compactedVerificationCodeLabelIsBlockedConservatively() {
        val result = firewall.inspect("verification\u200b code 123456")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun compactedSecurityCodeLabelIsBlockedConservatively() {
        val result = firewall.inspect("security\u200b code 123456")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun compactedUpiPinLabelIsBlockedConservatively() {
        val result = firewall.inspect("upi\u200b pin 1234")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun compactedPinForUpiLabelIsBlockedConservatively() {
        val result = firewall.inspect("pin\u200b for\u200b upi 1234")
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED, result.blockReason)
        assertEquals("", result.redactedText)
    }

    @Test fun normalizedBenignTextRemainsAllowed() {
        val result = firewall.inspect("Cafe\u00a0nearby")
        assertTrue(result.allowed)
        assertEquals("Cafe\u00a0nearby", result.redactedText)
    }

    @Test fun malformedSurrogateInputDoesNotCrashOrExposeInspectionException() {
        val unusual = "\u0000\u0001\u0002\uD800\uFFFF\n\t" + "🙂".repeat(100)
        val result = firewall.inspect(unusual)
        assertNotNull(result)
        assertTrue(result.allowed || result.blockReason != null)
    }

    private fun assertBlockedWithType(result: FirewallResult, type: SensitiveDataType) {
        assertFalse(result.allowed)
        assertEquals(FirewallBlockReason.SENSITIVE_DATA_DETECTED, result.blockReason)
        assertTrue(result.matches.any { it.type == type })
    }
}
