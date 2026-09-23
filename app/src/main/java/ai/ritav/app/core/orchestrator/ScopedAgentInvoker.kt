package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.orchestrator.AgentActionPlanFactory
import ai.ritav.app.core.security.ActionPlan
import ai.ritav.app.core.security.AppCapabilityRegistry
import ai.ritav.app.core.security.Capability

class ScopedAgentInvoker {
    fun invoke(agent: SpecialistAgent, request: AgentRequest): AgentProposal? {
        val proposal = agent.propose(request) ?: return null
        if (proposal.taskId != request.taskId) return null
        if (proposal.agentId != agent.id) return null
        if (proposal.agentId.isBlank() || proposal.agentId.length > MAX_ID_LENGTH) return null
        if (proposal.proposedAction.isBlank() || proposal.proposedAction.length > MAX_TEXT_LENGTH) return null
        if (proposal.rationale.length > MAX_TEXT_LENGTH) return null
        if (proposal.capability == Capability.FINANCIAL_ACTION) return null
        if (proposal.capability !in request.scope.allowedCapabilities) return null
        return proposal
    }

    /**
     * Converts an accepted agent proposal into an execution-shaped plan only
     * through deterministic registry/verification policy. No authorization is
     * issued or consumed here.
     */
    fun invokeAsActionPlan(
        agent: SpecialistAgent,
        request: AgentRequest,
        appId: String,
        registry: AppCapabilityRegistry,
        sessionId: String? = null
    ): ActionPlan? {
        val proposal = invoke(agent, request) ?: return null
        return AgentActionPlanFactory(registry).create(
            proposal = proposal,
            appId = appId,
            sessionId = sessionId
        )
    }

    private companion object {
        const val MAX_ID_LENGTH = 256
        const val MAX_TEXT_LENGTH = 4096
    }
}
