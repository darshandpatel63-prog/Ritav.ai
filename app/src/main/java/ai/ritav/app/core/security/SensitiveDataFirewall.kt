package ai.ritav.app.core.security

object SensitiveDataFirewall {
    private val secretPatterns = listOf(
        Regex("(?i)\\b(otp|one[- ]time password|upi pin|password|passcode|cvv|cvc|recovery code|security code|private key|seed phrase|secret key|auth code)\\b"),
        Regex("(?<!\\d)\\d{4,8}(?!\\d)")
    )

    fun containsSecretLikeContent(text: String): Boolean =
        text.isNotEmpty() && secretPatterns.any { it.containsMatchIn(text) }
}
