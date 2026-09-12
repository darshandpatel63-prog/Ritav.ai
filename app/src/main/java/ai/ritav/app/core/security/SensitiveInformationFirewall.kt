package ai.ritav.app.core.security

import java.text.Normalizer

/**
 * Deterministic pre-AI/pre-execution boundary for high-risk secrets.
 *
 * The original matched value is never returned in a match object and is not
 * persisted by this firewall. Inputs that cannot be inspected safely are
 * blocked rather than forwarded to AI reasoning.
 */
enum class SensitiveDataType {
    OTP,
    UPI_PIN,
    PASSWORD,
    CVV,
    RECOVERY_CODE,
    PRIVATE_KEY,
    API_KEY,
    UNKNOWN_SECRET
}

enum class FirewallBlockReason {
    SENSITIVE_DATA_DETECTED,
    INPUT_TOO_LARGE,
    NORMALIZATION_INSPECTION_FAILED
}

data class SensitiveMatch(
    val type: SensitiveDataType,
    val start: Int,
    val end: Int
)

data class FirewallResult(
    val allowed: Boolean,
    val redactedText: String,
    val matches: List<SensitiveMatch>,
    val blockReason: FirewallBlockReason? = null
)

class SensitiveInformationFirewall {
    fun inspect(text: String): FirewallResult {
        if (text.length > MAX_INPUT_LENGTH) {
            return FirewallResult(
                allowed = false,
                redactedText = "",
                matches = emptyList(),
                blockReason = FirewallBlockReason.INPUT_TOO_LARGE
            )
        }
        if (text.isEmpty()) return FirewallResult(true, text, emptyList())

        val matches = findDirectMatches(text)
        if (matches.isNotEmpty()) {
            // Replace from right to left so original offsets remain valid.
            var redacted = text
            matches.sortedByDescending { it.start }.forEach { match ->
                redacted = redacted.substring(0, match.start) +
                    "[REDACTED:${match.type.name}]" +
                    redacted.substring(match.end)
            }
            return FirewallResult(
                allowed = false,
                redactedText = redacted,
                matches = matches,
                blockReason = FirewallBlockReason.SENSITIVE_DATA_DETECTED
            )
        }

        // Normalized/compact inspection is detection-only. We deliberately do
        // not reuse transformed offsets for redaction because Unicode
        // normalization/compaction can change UTF-16 offsets. If a transformed
        // representation reveals a sensitive pattern, block conservatively.
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
        val compact = normalized.filterNot { it.isWhitespace() }
        val representationChanged = normalized != text || compact != normalized
        if (representationChanged && containsSensitivePattern(normalized, compact)) {
            return FirewallResult(
                allowed = false,
                redactedText = "",
                matches = emptyList(),
                blockReason = FirewallBlockReason.NORMALIZATION_INSPECTION_FAILED
            )
        }

        return FirewallResult(true, text, emptyList())
    }

    private fun findDirectMatches(text: String): List<SensitiveMatch> = buildList {
        OTP.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.OTP, it.range.first, it.range.last + 1)) }
        UPI_PIN.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.UPI_PIN, it.range.first, it.range.last + 1)) }
        CVV.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.CVV, it.range.first, it.range.last + 1)) }
        PRIVATE_KEY.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.PRIVATE_KEY, it.range.first, it.range.last + 1)) }
        API_KEY.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.API_KEY, it.range.first, it.range.last + 1)) }
        RECOVERY_CODE.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.RECOVERY_CODE, it.range.first, it.range.last + 1)) }
        PASSWORD_CONTEXT.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.PASSWORD, it.range.first, it.range.last + 1)) }
    }
        .distinctBy { Triple(it.type, it.start, it.end) }
        .sortedWith(compareBy<SensitiveMatch> { it.start }.thenByDescending { it.end - it.start })
        .let(::removeOverlappingMatches)

    private fun containsSensitivePattern(normalized: String, compact: String): Boolean =
        listOf(
            OTP,
            UPI_PIN,
            CVV,
            PRIVATE_KEY,
            API_KEY,
            RECOVERY_CODE,
            PASSWORD_CONTEXT
        ).any { regex -> regex.containsMatchIn(normalized) || regex.containsMatchIn(compact) }

    private fun removeOverlappingMatches(matches: List<SensitiveMatch>): List<SensitiveMatch> {
        val selected = mutableListOf<SensitiveMatch>()
        for (candidate in matches) {
            if (selected.none { candidate.start < it.end && candidate.end > it.start }) {
                selected += candidate
            }
        }
        return selected
    }

    companion object {
        private const val MAX_INPUT_LENGTH = 16_384
        private val OTP = Regex("(?i)(?:otp|one[- ]time password|verification code|security code)\\s*(?:is|:|=)?\\s*\\b\\d{4,8}\\b")
        private val UPI_PIN = Regex("(?i)(?:upi\\s*pin|pin for upi)\\s*(?:is|:|=)?\\s*\\b\\d{4,6}\\b")
        private val CVV = Regex("(?i)(?:cvv|cvc|security code)\\s*(?:is|:|=)?\\s*\\b\\d{3,4}\\b")
        private val PRIVATE_KEY = Regex("-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----[\\s\\S]*?-----END (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----")
        private val API_KEY = Regex("(?i)\\b(?:api[_ -]?key|access[_ -]?token|secret[_ -]?key)\\s*[:=]\\s*[A-Za-z0-9_./+=-]{12,}\\b")
        private val RECOVERY_CODE = Regex("(?i)(?:recovery|backup|emergency)\\s+code(?:s)?\\s*(?:are|is|:|=)?\\s*\\b[A-Za-z0-9-]{6,32}(?:\\s*,\\s*[A-Za-z0-9-]{6,32})*\\b")
        private val PASSWORD_CONTEXT = Regex("(?i)(?:password|passcode|login password)\\s*(?:is|:|=)\\s*[^\\s,;]{4,}")
    }
}
