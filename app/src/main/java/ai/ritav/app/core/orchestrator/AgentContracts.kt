package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.RiskTier
import ai.ritav.app.core.security.SensitiveInformationFirewall

data class AgentCapabilityScope(
    val allowedCapabilities: Set<Capability>
)

data class AgentRequest private constructor(
    val taskId: String,
    val input: String,
    val scope: AgentCapabilityScope
) {
    companion object {
        private val sensitiveFirewall = SensitiveInformationFirewall()

        /**
         * Creates an agent request only after deterministic sensitive-input inspection.
         * Sensitive or uninspectable input is rejected so it cannot reach an agent.
         */
        fun create(
            taskId: String,
            input: String,
            scope: AgentCapabilityScope
        ): AgentRequest? {
            val inspected = sensitiveFirewall.inspect(input)
            if (!inspected.allowed) return null
            return AgentRequest(taskId, inspected.redactedText, scope)
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
