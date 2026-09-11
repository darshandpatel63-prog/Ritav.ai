package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.RiskTier
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class ScopedAgentInvokerTest {
    private val agent = object : SpecialistAgent {
        override val id = "test-agent"
        override fun propose(request: AgentRequest) = AgentProposal(request.taskId, id, "open", Capability.APP_LAUNCH, RiskTier.TIER_1_REVERSIBLE, "test")
    }

    @Test fun rejectsCapabilityOutsideScope() {
        val request = AgentRequest("task-1", "open", AgentCapabilityScope(emptySet()))
        assertNull(ScopedAgentInvoker().invoke(agent, request))
    }

    @Test fun acceptsCapabilityInsideScope() {
        val request = AgentRequest("task-1", "open", AgentCapabilityScope(setOf(Capability.APP_LAUNCH)))
        assertNotNull(ScopedAgentInvoker().invoke(agent, request))
    }
}
