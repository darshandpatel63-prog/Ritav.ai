package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.RiskTier
import ai.ritav.app.core.security.TrustedUserCommand
import ai.ritav.app.core.security.UntrustedContent

data class AgentCapabilityScope(
    val allowedCapabilities: Set<Capability>
)

class AgentRequest private constructor(
    val taskId: String,
    val input: String,
    val scope: AgentCapabilityScope,
    /** Security-filtered context with provenance preserved for model reasoning. */
    val context: List<UntrustedContent>
) {
    companion object {
        /**
         * Legacy/simple ingress: the supplied string is explicitly represented as
         * the single trusted user command and still passes the same model-context
         * security boundary as richer context.
         */
        fun create(
            taskId: String,
            input: String,
            scope: AgentCapabilityScope
        ): AgentRequest? {
            val command = TrustedUserCommand.create(input, "direct-user-input") ?: return null
            return createFromContext(
                taskId = taskId,
                userCommand = command,
                context = emptyList(),
                scope = scope
            )
        }

        /**
         * Primary model/agent ingress. Security filtering happens before the
         * request becomes visible to any SpecialistAgent.
         */
        fun createFromContext(
            taskId: String,
            userCommand: TrustedUserCommand,
            context: List<UntrustedContent>,
            scope: AgentCapabilityScope
        ): AgentRequest? {
            if (Capability.FINANCIAL_ACTION in scope.allowedCapabilities) return null

            val prepared = ModelContextBoundary().prepare(
                taskId = taskId,
                userCommand = userCommand,
                context = context
            ) ?: return null

            val safeScope = AgentCapabilityScope(scope.allowedCapabilities.toSet())
            return AgentRequest(
                taskId = prepared.taskId,
                input = prepared.userCommand.text,
                scope = safeScope,
                context = prepared.context
            )
        }
    }
}

data class AgentProposal(
    val taskId: String,
    val agentId: String,
    val proposedAction: String,
    val capability: Capability,
    val riskTier: RiskTier,
    val rationale: String
)

interface SpecialistAgent {
    val id: String
    fun propose(request: AgentRequest): AgentProposal?
}

interface MasterOrchestrator {
    fun plan(request: AgentRequest): List<AgentProposal>
}
