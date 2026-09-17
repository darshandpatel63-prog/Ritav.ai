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
         * Creates an agent request only after deterministic security checks.
         * Sensitive or uninspectable input is rejected so it cannot reach an agent.
         * Financial execution is never a valid agent capability, even if a caller
         * attempts to place it into the scoped capability set.
         */
        fun create(
            taskId: String,
            input: String,
            scope: AgentCapabilityScope
        ): AgentRequest? {
            if (Capability.FINANCIAL_ACTION in scope.allowedCapabilities) return null
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
