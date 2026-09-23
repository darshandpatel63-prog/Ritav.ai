package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.SensitiveInformationFirewall
import ai.ritav.app.core.security.RiskTier
import ai.ritav.app.core.security.UntrustedContent

/**
 * The only request shape exposed to a model runtime.
 *
 * It is constructed by SecureModelRuntimeGateway from an already security-
 * filtered AgentRequest. The raw caller cannot construct a model request with
 * arbitrary content through the public factory.
 */
data class ModelRuntimeRequest private constructor(
    val taskId: String,
    val userCommand: String,
    val context: List<UntrustedContent>,
    val allowedCapabilities: Set<Capability>
) {
    internal companion object {
        fun from(request: AgentRequest): ModelRuntimeRequest =
            ModelRuntimeRequest(
                taskId = request.taskId,
                userCommand = request.input,
                context = request.context.toList(),
                allowedCapabilities = request.scope.allowedCapabilities.toSet()
            )
    }
}

/**
 * Provider-neutral model runtime adapter.
 *
 * The provider implementation is deliberately kept outside the security
 * boundary. It receives only ModelRuntimeRequest produced by the secure
 * gateway and its response is treated as untrusted model output.
 */
fun interface ModelRuntime {
    fun generate(request: ModelRuntimeRequest): ModelProposalDraft?
}

/** Untrusted, provider-produced candidate action. */
data class ModelProposalDraft(
    val taskId: String,
    val proposedAction: String,
    val capability: Capability,
    val riskTier: RiskTier,
    val rationale: String
)

/**
 * Deterministic output filter for model-generated proposal data.
 *
 * Model output may be malformed, out of scope, financially sensitive or secret-
 * bearing even when the input context was safely filtered. Such output is
 * rejected rather than repaired into an executable instruction.
 */
class ModelOutputBoundary(
    private val sensitiveInformationFirewall: SensitiveInformationFirewall =
        SensitiveInformationFirewall()
) {
    fun validate(
        draft: ModelProposalDraft,
        request: AgentRequest
    ): ModelProposalDraft? {
        if (draft.taskId != request.taskId) return null
        if (draft.taskId.isBlank() || draft.taskId.length > MAX_ID_LENGTH) return null
        if (draft.proposedAction.isBlank() || draft.proposedAction.length > MAX_ACTION_LENGTH) return null
        if (draft.rationale.length > MAX_RATIONALE_LENGTH) return null
        if (draft.capability == Capability.FINANCIAL_ACTION) return null
        if (draft.capability !in request.scope.allowedCapabilities) return null

        val safeAction = inspectText(draft.proposedAction) ?: return null
        val safeRationale = inspectText(draft.rationale) ?: return null

        return draft.copy(
            proposedAction = safeAction,
            rationale = safeRationale
        )
    }

    private fun inspectText(value: String): String? {
        val normalized = value.trim()
        if (normalized.isBlank()) return null
        if (normalized.length > MAX_ACTION_LENGTH) return null
        val inspection = runCatching {
            sensitiveInformationFirewall.inspect(normalized)
        }.getOrNull() ?: return null
        if (!inspection.allowed) return null
        return inspection.redactedText
    }

    private companion object {
        const val MAX_ID_LENGTH = 256
        const val MAX_ACTION_LENGTH = 4_096
        const val MAX_RATIONALE_LENGTH = 4_096
    }
}

/**
 * Security ingress around a real model-runtime adapter.
 *
 * Raw user/external content first enters AgentRequest.createFromContext(), so
 * the model runtime itself never receives the unfiltered source material.
 * Model failures are contained and fail closed.
 */
class SecureModelRuntimeGateway(
    private val runtime: ModelRuntime,
    private val outputBoundary: ModelOutputBoundary = ModelOutputBoundary()
) {
    fun propose(
        taskId: String,
        userCommand: UntrustedContent,
        context: List<UntrustedContent>,
        scope: AgentCapabilityScope,
        agentId: String
    ): AgentProposal? {
        if (agentId.isBlank() || agentId.length > MAX_AGENT_ID_LENGTH) return null

        val request = AgentRequest.createFromContext(
            taskId = taskId,
            userCommand = userCommand,
            context = context,
            scope = scope
        ) ?: return null

        return propose(request, agentId)
    }

    fun propose(
        request: AgentRequest,
        agentId: String
    ): AgentProposal? {
        if (agentId.isBlank() || agentId.length > MAX_AGENT_ID_LENGTH) return null

        val modelRequest = ModelRuntimeRequest.from(request)
        val draft = runCatching {
            runtime.generate(modelRequest)
        }.getOrNull() ?: return null

        val safeDraft = outputBoundary.validate(draft, request) ?: return null

        return AgentProposal(
            taskId = safeDraft.taskId,
            agentId = agentId,
            proposedAction = safeDraft.proposedAction,
            capability = safeDraft.capability,
            riskTier = safeDraft.riskTier,
            rationale = safeDraft.rationale
        )
    }

    private companion object {
        const val MAX_AGENT_ID_LENGTH = 256
    }
}

/**
 * Model-backed SpecialistAgent. Its output is still untrusted and must pass
 * ScopedAgentInvoker before conversion to ActionPlan.
 */
class ModelBackedSpecialistAgent(
    override val id: String,
    private val gateway: SecureModelRuntimeGateway
) : SpecialistAgent {
    override fun propose(request: AgentRequest): AgentProposal? =
        gateway.propose(request, id)
}
