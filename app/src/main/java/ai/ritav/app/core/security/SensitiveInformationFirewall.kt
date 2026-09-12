package ai.ritav.app.core.security

/**
 * Deterministic pre-AI/pre-execution boundary for high-risk secrets.
 * The original matched value is never returned in a match object and is not persisted.
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

data class SensitiveMatch(
    val type: SensitiveDataType,
    val start: Int,
    val end: Int
)

data class FirewallResult(
    val allowed: Boolean,
    val redactedText: String,
    val matches: List<SensitiveMatch>
)

class SensitiveInformationFirewall {
    fun inspect(text: String): FirewallResult {
        require(text.length <= MAX_INPUT_LENGTH) { "Sensitive input exceeds inspection limit" }
        if (text.isEmpty()) return FirewallResult(true, text, emptyList())

        val matches = buildList {
            OTP.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.OTP, it.range.first, it.range.last + 1)) }
            UPI_PIN.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.UPI_PIN, it.range.first, it.range.last + 1)) }
            CVV.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.CVV, it.range.first, it.range.last + 1)) }
            PRIVATE_KEY.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.PRIVATE_KEY, it.range.first, it.range.last + 1)) }
            API_KEY.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.API_KEY, it.range.first, it.range.last + 1)) }
            RECOVERY_CODE.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.RECOVERY_CODE, it.range.first, it.range.last + 1)) }
            PASSWORD_CONTEXT.findAll(text).forEach { add(SensitiveMatch(SensitiveDataType.PASSWORD, it.range.first, it.range.last + 1)) }
        }.distinctBy { Triple(it.type, it.start, it.end) }
            .sortedByDescending { it.start }

        if (matches.isEmpty()) return FirewallResult(true, text, emptyList())

        var redacted = text
        matches.forEach { match ->
            val original = redacted
            redacted = original.substring(0, match.start) +
                "[REDACTED:${match.type.name}]" +
                original.substring(match.end)
        }
        return FirewallResult(false, redacted, matches)
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
