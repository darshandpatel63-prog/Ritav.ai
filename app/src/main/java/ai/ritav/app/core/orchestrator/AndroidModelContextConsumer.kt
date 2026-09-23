package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.TrustedUserCommand
import ai.ritav.app.core.security.UntrustedContent

/**
 * Android-side model-context consumer.
 *
 * Accessibility context is consumed once, retains its explicit APP_CONTENT
 * provenance, and is then passed through the same AgentRequest factory that
 * protects every other model ingress.
 */
internal class AndroidModelContextConsumer(
    private val accessibilityContextProvider: AccessibilityContextProvider =
        AndroidAccessibilityContextProvider()
) {
    fun createRequest(
        taskId: String,
        userCommand: TrustedUserCommand,
        scope: AgentCapabilityScope,
        targetPackage: String? = null
    ): AgentRequest? {
        val context = targetPackage
            ?.let { accessibilityContextProvider.consume(it) }
            ?.let(::listOf)
            .orEmpty()

        return AgentRequest.createFromContext(
            taskId = taskId,
            userCommand = userCommand,
            context = context,
            scope = scope
        )
    }
}

internal fun interface AccessibilityContextProvider {
    fun consume(packageName: String): UntrustedContent?
}

private class AndroidAccessibilityContextProvider : AccessibilityContextProvider {
    override fun consume(packageName: String): UntrustedContent? =
        ai.ritav.app.platform.AndroidAccessibilityEvidenceBroker.consumeModelContext(packageName)
}
