package ai.ritav.app.core.security

/** Trust level assigned to content before it is exposed to AI reasoning. */
enum class ContentTrustLevel {
    USER_COMMAND,
    APP_CONTENT,
    EXTERNAL_CONTENT
}

/**
 * Untrusted content is data, never authority. Its text may be analysed, but it
 * cannot grant permissions, alter policy, or authorize an action.
 */
data class UntrustedContent(
    val text: String,
    val source: String,
    val trustLevel: ContentTrustLevel
)

class PromptInjectionBoundary {
    fun sanitize(content: UntrustedContent): UntrustedContent =
        content.copy(text = content.text.trim())

    /** Security policy always outranks app/web/message/document instructions. */
    fun canOverrideSecurityPolicy(content: UntrustedContent): Boolean = false

    /** Content cannot create or elevate a permission grant. */
    fun canAuthorizeAction(content: UntrustedContent): Boolean = false

    /** User commands are authority only when received through the trusted input path. */
    fun isTrustedCommand(content: UntrustedContent): Boolean =
        content.trustLevel == ContentTrustLevel.USER_COMMAND
}
