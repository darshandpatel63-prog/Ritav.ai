package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.RiskTier

data class AgentCapabilityScope(
    val allowedCapabilities: Set<Capability>
)

data class AgentRequest(
    val taskId: String,
    val input: String,
    val scope: AgentCapabilityScope
)

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
    fun plan(input: String, taskId: String): List<AgentProposal>
}
