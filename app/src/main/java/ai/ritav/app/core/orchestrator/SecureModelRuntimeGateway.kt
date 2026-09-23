package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.ContentTrustLevel
import ai.ritav.app.core.security.UntrustedContent

data class ModelInferenceRequest internal constructor(
    val taskId: String,
    val userCommand: UntrustedContent,
    val context: List<UntrustedContent>
) {
    init {
        require(userCommand.trustLevel == ContentTrustLevel.USER_COMMAND)
        require(userCommand.text.isNotBlank())
        require(userCommand.source.isNotBlank())
        require(context.none { it.trustLevel == ContentTrustLevel.USER_COMMAND })
    }
}

data class ModelInferenceResult(
    val taskId: String,
    val proposal: AgentProposal?
)

interface ModelRuntime {
    fun infer(request: ModelInferenceRequest): ModelInferenceResult?
}

/**
 * Security gateway between filtered Ritav context and an actual model runtime.
 * No provider SDK, network access, credentials or authorization authority is
 * introduced by this layer.
 */
class SecureModelRuntimeGateway(
    private val modelRuntime: ModelRuntime
) {
    fun infer(request: AgentRequest): ModelInferenceResult? {
        val prepared = ModelContextBoundary().prepare(
            taskId = request.taskId,
            userCommand = UntrustedContent(
                request.input,
                "agent-request-user-command",
                ContentTrustLevel.USER_COMMAND
            ),
            context = request.context
        ) ?: return null

        val inferenceRequest = runCatching {
            ModelInferenceRequest(
                taskId = prepared.taskId,
                userCommand = prepared.userCommand,
                context = prepared.context
            )
        }.getOrNull() ?: return null

        val result = runCatching { modelRuntime.infer(inferenceRequest) }.getOrNull() ?: return null
        return result.takeIf { it.taskId == request.taskId }
    }
}
