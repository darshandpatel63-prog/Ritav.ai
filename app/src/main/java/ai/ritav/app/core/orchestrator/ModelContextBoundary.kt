package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.ContentTrustLevel
import ai.ritav.app.core.security.PromptInjectionBoundary
import ai.ritav.app.core.security.SensitiveInformationFirewall
import ai.ritav.app.core.security.UntrustedContent

/**
 * Deterministic choke point for every piece of content that may be exposed to
 * model/agent reasoning.
 *
 * User commands are the only content class that can represent user authority.
 * App/external content remains data with explicit provenance and can never be
 * promoted to a user command by this boundary.
 */
data class PreparedModelContext(
    val taskId: String,
    val userCommand: UntrustedContent,
    val context: List<UntrustedContent>
)

class ModelContextBoundary(
    private val promptInjectionBoundary: PromptInjectionBoundary = PromptInjectionBoundary(),
    private val sensitiveInformationFirewall: SensitiveInformationFirewall = SensitiveInformationFirewall()
) {
    fun prepare(
        taskId: String,
        userCommand: UntrustedContent,
        context: List<UntrustedContent> = emptyList()
    ): PreparedModelContext? {
        if (taskId.isBlank() || taskId.length > MAX_TASK_ID_LENGTH) return null
        if (context.size > MAX_CONTEXT_ITEMS) return null
        if (userCommand.trustLevel != ContentTrustLevel.USER_COMMAND) return null

        val safeUserCommand = sanitizeForModel(userCommand) ?: return null
        if (safeUserCommand.text.isBlank()) return null

        val safeContext = ArrayList<UntrustedContent>(context.size)
        var totalCharacters = safeUserCommand.text.length

        for (item in context) {
            if (item.trustLevel == ContentTrustLevel.USER_COMMAND) return null
            val safeItem = sanitizeForModel(item) ?: return null
            totalCharacters += safeItem.text.length
            if (totalCharacters > MAX_TOTAL_CONTEXT_CHARACTERS) return null
            safeContext += safeItem
        }

        return PreparedModelContext(
            taskId = taskId,
            userCommand = safeUserCommand,
            context = safeContext.toList()
        )
    }

    private fun sanitizeForModel(content: UntrustedContent): UntrustedContent? {
        if (content.source.isBlank() || content.source.length > MAX_SOURCE_LENGTH) return null
        if (content.text.length > MAX_ITEM_TEXT_LENGTH) return null

        val sanitized = runCatching { promptInjectionBoundary.sanitize(content) }.getOrNull() ?: return null
        if (sanitized.text.isBlank()) return null
        if (sanitized.text.length > MAX_ITEM_TEXT_LENGTH) return null

        val inspection = runCatching {
            sensitiveInformationFirewall.inspect(sanitized.text)
        }.getOrNull() ?: return null

        // Never pass detected or uninspectable secrets downstream, even in
        // redacted form. The model must not receive the original secret.
        if (!inspection.allowed) return null

        return sanitized.copy(text = inspection.redactedText)
    }

    private companion object {
        const val MAX_TASK_ID_LENGTH = 256
        const val MAX_SOURCE_LENGTH = 128
        const val MAX_ITEM_TEXT_LENGTH = 16_384
        const val MAX_CONTEXT_ITEMS = 16
        const val MAX_TOTAL_CONTEXT_CHARACTERS = 32_768
    }
}
