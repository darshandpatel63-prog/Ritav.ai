package ai.ritav.app.core.security

/** Trust level assigned only to data that is never user authority. */
enum class ContentTrustLevel {
    APP_CONTENT,
    EXTERNAL_CONTENT
}

/**
 * Data received from an app, website, notification, document or other
 * non-authoritative producer. It can be analyzed but can never become a
 * security authority object through a mutable trust-level field.
 */
data class UntrustedContent(
    val text: String,
    val source: String,
    val trustLevel: ContentTrustLevel
)

/**
 * User authority is represented by a distinct type so untrusted context cannot
 * be relabeled as a user command by copying or changing an enum value.
 *
 * Creation stays internal to the application module; real user-input adapters
 * must use this constructor boundary rather than accepting external content as
 * authority.
 */
class TrustedUserCommand private constructor(
    val text: String,
    val source: String
) {
    internal companion object {
        private const val MAX_TEXT_LENGTH = 16_384
        private const val MAX_SOURCE_LENGTH = 128

        fun create(text: String, source: String): TrustedUserCommand? {
            if (source.isBlank() || source.length > MAX_SOURCE_LENGTH) return null
            if (text.length > MAX_TEXT_LENGTH) return null
            if (text.trim().isBlank()) return null
            return TrustedUserCommand(text.trim(), source)
        }
    }
}

class PromptInjectionBoundary {
    fun sanitize(content: UntrustedContent): UntrustedContent =
        content.copy(text = content.text.trim())

    fun canOverrideSecurityPolicy(content: UntrustedContent): Boolean = false

    fun canAuthorizeAction(content: UntrustedContent): Boolean = false
}
