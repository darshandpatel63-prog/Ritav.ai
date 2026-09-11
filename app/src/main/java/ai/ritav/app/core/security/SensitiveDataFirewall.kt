package ai.ritav.app.core.security

object SensitiveDataFirewall {
    private val contextualSecretPatterns = listOf(
        Regex("(?i)\\b(?:otp|one[- ]time password)\\b\\s*(?:is|:|=)?\\s*\\d{4,8}\\b"),
        Regex("(?i)\\b(?:upi\\s*pin|pin)\\b\\s*(?:is|:|=)?\\s*\\d{4,6}\\b"),
        Regex("(?i)\\b(?:cvv|cvc)\\b\\s*(?:is|:|=)?\\s*\\d{3,4}\\b"),
        Regex("(?i)\\b(?:password|passcode|recovery code|security code)\\b\\s*(?:is|:|=)\\s*\\S+")
    )

    fun containsSecretLikeContent(text: String): Boolean =
        contextualSecretPatterns.any { it.containsMatchIn(text) }

    fun redactSecrets(text: String): String =
        contextualSecretPatterns.fold(text) { current, pattern ->
            pattern.replace(current) { match ->
                match.value.replace(Regex("\\d{3,8}|(?<=[:=]\\s)\\S+$"), "[REDACTED]")
            }
        }
}
