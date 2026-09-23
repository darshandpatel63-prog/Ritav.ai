package ai.ritav.app.core.security

import ai.ritav.app.core.orchestrator.AgentActionPlanFactory
import ai.ritav.app.core.orchestrator.AgentProposal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class AgentActionPlanFactoryTest {
    private fun registry(): AppCapabilityRegistry = AppCapabilityRegistry(
        listOf(
            AppCapabilitySpec(
                packageName = "com.example.safe",
                capability = Capability.APP_LAUNCH,
                actions = setOf("open"),
                riskTier = RiskTier.TIER_1_REVERSIBLE,
                trustedCertificateSha256 = "a".repeat(64)
            )
        )
    )

    private fun proposal(
        riskTier: RiskTier = RiskTier.TIER_1_REVERSIBLE,
        capability: Capability = Capability.APP_LAUNCH,
        action: String = "open"
    ) = AgentProposal(
        taskId = "task-1",
        agentId = "planner",
        proposedAction = action,
        capability = capability,
        riskTier = riskTier,
        rationale = "plan"
    )

    @Test fun derivesExactPlanFromRegistryMetadata() {
        val plan = AgentActionPlanFactory(registry()).create(proposal(), "task-1", "com.example.safe", "session-1")
        assertNotNull(plan)
        assertEquals(RiskTier.TIER_1_REVERSIBLE, plan!!.riskTier)
        assertEquals("LAUNCH_DISPATCHED", plan.expectedState)
        assertEquals("session-1", plan.sessionId)
    }

    @Test fun rejectsTaskMismatchBeforePlanCreation() {
        assertNull(
            AgentActionPlanFactory(registry()).create(
                proposal(),
                "different-task",
                "com.example.safe"
            )
        )
    }

    @Test fun rejectsAgentSuppliedRiskThatDoesNotMatchRegistry() {
        val plan = AgentActionPlanFactory(registry()).create(
            proposal(riskTier = RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE),
            "task-1",
            "com.example.safe"
        )
        assertNull(plan)
    }

    @Test fun rejectsUnregisteredTargetOrAction() {
        assertNull(AgentActionPlanFactory(registry()).create(proposal(), "task-1", "com.example.unknown"))
        assertNull(
            AgentActionPlanFactory(registry()).create(
                proposal(action = "send"),
                "task-1",
                "com.example.safe"
            )
        )
    }

    @Test fun refusesFinancialCapabilityEvenWhenRegistered() {
        val financialRegistry = AppCapabilityRegistry(
            listOf(
                AppCapabilitySpec(
                    packageName = "com.example.safe",
                    capability = Capability.FINANCIAL_ACTION,
                    actions = setOf("pay"),
                    riskTier = RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED,
                    financialCategory = true,
                    trustedCertificateSha256 = "b".repeat(64)
                )
            )
        )
        assertNull(
            AgentActionPlanFactory(financialRegistry).create(
                proposal(capability = Capability.FINANCIAL_ACTION, action = "pay", riskTier = RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED),
                "task-1",
                "com.example.safe"
            )
        )
    }

    @Test fun refusesUnsupportedVerificationSemantics() {
        val extendedRegistry = AppCapabilityRegistry(
            listOf(
                AppCapabilitySpec(
                    packageName = "com.example.safe",
                    capability = Capability.TYPE_TEXT,
                    actions = setOf("type"),
                    riskTier = RiskTier.TIER_2_CONTENT_MUTATION,
                    trustedCertificateSha256 = "c".repeat(64)
                )
            )
        )
        assertNull(
            AgentActionPlanFactory(extendedRegistry).create(
                proposal(capability = Capability.TYPE_TEXT, action = "type", riskTier = RiskTier.TIER_2_CONTENT_MUTATION),
                "task-1",
                "com.example.safe"
            )
        )
    }
}
