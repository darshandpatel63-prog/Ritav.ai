package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.RiskTier
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScopedAgentInvokerTest {
    private val agent = object : SpecialistAgent {
        override val id = "test-agent"
        override fun propose(request: AgentRequest) = AgentProposal(request.taskId, id, "open", Capability.APP_LAUNCH, RiskTier.TIER_1_REVERSIBLE, "test")
    }

    @Test fun rejectsCapabilityOutsideScope() {
        val request = AgentRequest.create("task-1", "open", AgentCapabilityScope(emptySet()))
        assertNotNull(request)
        assertNull(ScopedAgentInvoker().invoke(agent, request!!))
    }

    @Test fun acceptsCapabilityInsideScope() {
        val request = AgentRequest.create("task-1", "open", AgentCapabilityScope(setOf(Capability.APP_LAUNCH)))
        assertNotNull(request)
        assertNotNull(ScopedAgentInvoker().invoke(agent, request!!))
    }

    @Test fun rejectsSensitiveInputBeforeAgentInvocation() {
        val request = AgentRequest.create("task-1", "OTP 123456", AgentCapabilityScope(setOf(Capability.APP_LAUNCH)))
        assertNull(request)
    }

    @Test fun rejectsOversizedInputBeforeAgentInvocation() {
        val request = AgentRequest.create("task-1", "x".repeat(16_385), AgentCapabilityScope(setOf(Capability.APP_LAUNCH)))
        assertNull(request)
    }

    @Test fun rejectsBlankAndOversizedTaskIdsAtAgentIngress() {
        assertNull(AgentRequest.create("", "open", AgentCapabilityScope(setOf(Capability.APP_LAUNCH))))
        assertNull(AgentRequest.create("x".repeat(257), "open", AgentCapabilityScope(setOf(Capability.APP_LAUNCH))))
    }

    @Test fun copiesCapabilityScopeAtAgentIngress() {
        val mutable = mutableSetOf(Capability.APP_LAUNCH)
        val request = AgentRequest.create("task-1", "open", AgentCapabilityScope(mutable))
        mutable.clear()
        assertNotNull(request)
        assertTrue(Capability.APP_LAUNCH in request!!.scope.allowedCapabilities)
    }

    @Test fun rejectsFinancialCapabilityBeforeAgentInvocation() {
        val request = AgentRequest.create(
            "task-1",
            "pay this invoice",
            AgentCapabilityScope(setOf(Capability.FINANCIAL_ACTION))
        )
        assertNull(request)
    }
    @Test fun rejectsMalformedAgentProposalBeforeItLeavesAgentBoundary() {
        val malformed = object : SpecialistAgent {
            override val id = "test-agent"
            override fun propose(request: AgentRequest) = AgentProposal(
                request.taskId, id, "", Capability.APP_LAUNCH, RiskTier.TIER_1_REVERSIBLE, "ok"
            )
        }
        val request = AgentRequest.create("task-1", "open", AgentCapabilityScope(setOf(Capability.APP_LAUNCH)))
        assertNull(ScopedAgentInvoker().invoke(malformed, request!!))
    }

    @Test fun rejectsFinancialProposalEvenWhenScopeIsManipulated() {
        val malicious = object : SpecialistAgent {
            override val id = "test-agent"
            override fun propose(request: AgentRequest) = AgentProposal(
                request.taskId, id, "transfer", Capability.FINANCIAL_ACTION, RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED, "x"
            )
        }
        val request = AgentRequest.create("task-1", "transfer", AgentCapabilityScope(emptySet()))
        assertNotNull(request)
        assertNull(ScopedAgentInvoker().invoke(malicious, request!!))
    }

}
