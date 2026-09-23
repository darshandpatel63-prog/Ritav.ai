package ai.ritav.app.platform

import ai.ritav.app.core.orchestrator.AgentRequest
import ai.ritav.app.core.orchestrator.AgentCapabilityScope
import ai.ritav.app.core.orchestrator.ModelBackedSpecialistAgent
import ai.ritav.app.core.orchestrator.ModelProposalDraft
import ai.ritav.app.core.orchestrator.ModelRuntime
import ai.ritav.app.core.orchestrator.SecureModelRuntimeGateway
import ai.ritav.app.core.orchestrator.ScreenContextSnapshot
import ai.ritav.app.core.orchestrator.ScreenContentSource
import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.ContentTrustLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAccessibilityModelContextBridgeTest {
    private class RecordingRuntime : ModelRuntime {
        var lastContext = emptyList<ai.ritav.app.core.security.UntrustedContent>()

        override fun generate(request: ai.ritav.app.core.orchestrator.ModelRuntimeRequest): ModelProposalDraft? {
            lastContext = request.context
            return ModelProposalDraft(
                taskId = request.taskId,
                proposedAction = "open",
                capability = Capability.APP_LAUNCH,
                riskTier = ai.ritav.app.core.security.RiskTier.TIER_1_REVERSIBLE,
                rationale = "open approved app"
            )
        }
    }

    @Test fun filteredAccessibilityContentFlowsIntoSecureModelGateway() {
        val request = AgentRequest.create(
            taskId = "task-1",
            input = "open",
            scope = AgentCapabilityScope(setOf(Capability.READ_ALLOWED_CONTENT, Capability.APP_LAUNCH))
        )!!
        val runtime = RecordingRuntime()
        val gateway = SecureModelRuntimeGateway(runtime)
        val agent = ModelBackedSpecialistAgent("android-model", gateway)
        val gate = AndroidAccessibilityContextGate()
        val bridge = AndroidAccessibilityModelContextBridge(agent, gate)

        assertTrue(
            gate.arm(
                request = request,
                packageName = "com.example.safe",
                agentId = agent.id,
                expiresAtElapsedRealtime = 10_000L,
                nowElapsedRealtime = 1_000L,
                stopGeneration = 1L
            )
        )

        val proposal = bridge.consume(
            snapshot = ScreenContextSnapshot(
                taskId = "task-1",
                packageName = "com.example.safe",
                source = ScreenContentSource.ACCESSIBILITY,
                text = "Welcome home",
                observedAtMillis = 100L
            ),
            nowElapsedRealtime = 1_500L,
            currentStopGeneration = 1L
        )

        assertNotNull(proposal)
        assertEquals(1, runtime.lastContext.size)
        assertEquals(ContentTrustLevel.APP_CONTENT, runtime.lastContext.single().trustLevel)
        assertEquals("Welcome home", runtime.lastContext.single().text)
    }
}
