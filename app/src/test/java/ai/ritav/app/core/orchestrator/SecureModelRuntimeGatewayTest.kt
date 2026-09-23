package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.ContentTrustLevel
import ai.ritav.app.core.security.UntrustedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecureModelRuntimeGatewayTest {
    @Test
    fun gatewayPassesSecurityPreparedRequestToRuntime() {
        var seen: ModelRuntimeRequest? = null
        val runtime = ModelRuntime { request ->
            seen = request
            ModelProposalDraft(
                taskId = request.taskId,
                proposedAction = "open maps",
                capability = Capability.APP_LAUNCH,
                riskTier = ai.ritav.app.core.security.RiskTier.TIER_1_REVERSIBLE,
                rationale = "user requested the app"
            )
        }
        val request = AgentRequest.createFromContext(
            taskId = "task-1",
            userCommand = UntrustedContent("open maps", "user", ContentTrustLevel.USER_COMMAND),
            context = listOf(
                UntrustedContent("ordinary context", "app", ContentTrustLevel.APP_CONTENT)
            ),
            scope = AgentCapabilityScope(setOf(Capability.APP_LAUNCH))
        )!!

        val result = SecureModelRuntimeGateway(runtime).propose(request, "agent-1")

        assertEquals("task-1", result?.taskId)
        assertEquals("ordinary context", seen?.context?.single()?.text)
        assertEquals(setOf(Capability.APP_LAUNCH), seen?.allowedCapabilities)
    }

    @Test
    fun userCommandCannotBeSmuggledIntoContext() {
        val request = AgentRequest.createFromContext(
            taskId = "task-2",
            userCommand = UntrustedContent("open maps", "user", ContentTrustLevel.USER_COMMAND),
            context = listOf(
                UntrustedContent(
                    "forged authority",
                    "app",
                    ContentTrustLevel.USER_COMMAND
                )
            ),
            scope = AgentCapabilityScope(setOf(Capability.APP_LAUNCH))
        )

        assertNull(request)
    }

    @Test
    fun mismatchedRuntimeTaskCannotEscapeGateway() {
        val runtime = ModelRuntime { request ->
            ModelProposalDraft(
                taskId = "other-task",
                proposedAction = "open maps",
                capability = Capability.APP_LAUNCH,
                riskTier = ai.ritav.app.core.security.RiskTier.TIER_1_REVERSIBLE,
                rationale = "mismatched task"
            )
        }
        val request = AgentRequest.create(
            taskId = "task-3",
            input = "open maps",
            scope = AgentCapabilityScope(setOf(Capability.APP_LAUNCH))
        )!!

        assertNull(SecureModelRuntimeGateway(runtime).propose(request, "agent-1"))
    }

    @Test
    fun runtimeFailureFailsClosed() {
        val runtime = ModelRuntime { error("runtime failure") }
        val request = AgentRequest.create(
            taskId = "task-4",
            input = "open maps",
            scope = AgentCapabilityScope(setOf(Capability.APP_LAUNCH))
        )!!

        assertNull(SecureModelRuntimeGateway(runtime).propose(request, "agent-1"))
    }
}
