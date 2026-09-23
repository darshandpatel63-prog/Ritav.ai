package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.ContentTrustLevel
import ai.ritav.app.core.security.PromptInjectionBoundary
import ai.ritav.app.core.security.SensitiveInformationFirewall
import ai.ritav.app.core.security.UntrustedContent

enum class ScreenContentSource {
    ACCESSIBILITY,
    OCR
}

data class ScreenContextSnapshot(
    val taskId: String,
    val packageName: String,
    val source: ScreenContentSource,
    val text: String,
    val observedAtMillis: Long
)

/**
 * Deterministic boundary for content extracted from a screen/UI.
 *
 * A screen snapshot is never user authority. It is represented as APP_CONTENT,
 * is bounded, secret-inspected and provenance-preserving before it can reach
 * the model-context boundary.
 */
class ScreenContextSecurityFilter(
    private val promptInjectionBoundary: PromptInjectionBoundary = PromptInjectionBoundary(),
    private val sensitiveInformationFirewall: SensitiveInformationFirewall = SensitiveInformationFirewall()
) {
    fun filter(snapshot: ScreenContextSnapshot): ScreenContextSnapshot? {
        if (snapshot.taskId.isBlank() || snapshot.taskId.length > MAX_TASK_ID_LENGTH) return null
        if (!isValidPackageName(snapshot.packageName)) return null
        if (snapshot.text.length > MAX_TEXT_LENGTH) return null
        if (snapshot.observedAtMillis < 0L) return null

        val content = UntrustedContent(
            text = snapshot.text,
            source = snapshot.source.name.lowercase() + ":" + snapshot.packageName,
            trustLevel = ContentTrustLevel.APP_CONTENT
        )

        val sanitized = runCatching {
            promptInjectionBoundary.sanitize(content)
        }.getOrNull() ?: return null
        if (sanitized.text.isBlank() || sanitized.text.length > MAX_TEXT_LENGTH) return null

        val inspection = runCatching {
            sensitiveInformationFirewall.inspect(sanitized.text)
        }.getOrNull() ?: return null
        if (!inspection.allowed) return null

        return snapshot.copy(text = inspection.redactedText)
    }

    private fun isValidPackageName(packageName: String): Boolean =
        packageName.length in 1..MAX_PACKAGE_NAME_LENGTH &&
            packageName.matches(PACKAGE_NAME_REGEX)

    private companion object {
        const val MAX_TASK_ID_LENGTH = 256
        const val MAX_PACKAGE_NAME_LENGTH = 256
        const val MAX_TEXT_LENGTH = 16_384
        val PACKAGE_NAME_REGEX = Regex("""^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$""")
    }
}