package ai.ritav.app.core.orchestrator

class ScopedAgentInvoker {
    fun invoke(agent: SpecialistAgent, request: AgentRequest): AgentProposal? {
        val proposal = agent.propose(request) ?: return null
        if (proposal.taskId != request.taskId) return null
        if (proposal.capability !in request.scope.allowedCapabilities) return null
        return proposal
    }
}
