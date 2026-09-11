package ai.ritav.app.core.security

object SensitiveDataFirewall {
    private val secretPatterns = listOf(
        Regex("\\b\\d{4,8}\\b"),
        Regex("(?i)\\b(otp|one[- ]time password|upi pin|password|passcode|cvv|cvc|recovery code|security code)\\b")
    )

    fun containsSecretLikeContent(text: String): Boolean =
        secretPatterns.any { it.containsMatchIn(text) }
}
