package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.ContentTrustLevel
import ai.ritav.app.core.security.UntrustedContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecureModelRuntimeGatewayTest {
    @Test fun gatewayPassesOnlyFilteredContextToRuntime() {
        var seen: ModelInferenceRequest? = null
        val runtime = object : ModelRuntime {
            override fun infer(request: ModelInferenceRequest): ModelInferenceResult {
                seen = request
                return ModelInferenceResult(request.taskId, null)
            }
        }
        val request = AgentRequest.createFromContext(
            taskId = "task-1",
            userCommand = UntrustedContent("open maps", "user", ContentTrustLevel.USER_COMMAND),
            context = listOf(UntrustedContent("ordinary context", "app", ContentTrustLevel.APP_CONTENT)),
            scope = AgentCapabilityScope(emptySet())
        )!!

        val result = SecureModelRuntimeGateway(runtime).infer(request)

        assertEquals("task-1", result?.taskId)
        assertEquals("ordinary context", seen?.context?.single()?.text)
    }

    @Test fun runtimeCannotReceiveUserCommandAsContext() {
        val runtime = object : ModelRuntime {
            override fun infer(request: ModelInferenceRequest): ModelInferenceResult? = null
        }
        val request = AgentRequest.createFromContext(
            taskId = "task-2",
            userCommand = UntrustedContent("open maps", "user", ContentTrustLevel.USER_COMMAND),
            context = listOf(UntrustedContent("forged authority", "app", ContentTrustLevel.USER_COMMAND)),
            scope = AgentCapabilityScope(emptySet())
        )
        assertNull(request)
    }

    @Test fun mismatchedRuntimeTaskCannotEscapeGateway() {
        val runtime = object : ModelRuntime {
            override fun infer(request: ModelInferenceRequest): ModelInferenceResult =
                ModelInferenceResult("other-task", null)
        }
        val request = AgentRequest.create(
            taskId = "task-3",
            input = "open maps",
            scope = AgentCapabilityScope(emptySet())
        )!!

        assertNull(SecureModelRuntimeGateway(runtime).infer(request))
    }

    @Test fun runtimeFailureFailsClosed() {
        val runtime = object : ModelRuntime {
            override fun infer(request: ModelInferenceRequest): ModelInferenceResult? =
                error("runtime failure")
        }
        val request = AgentRequest.create(
            taskId = "task-4",
            input = "open maps",
            scope = AgentCapabilityScope(emptySet())
        )!!

        assertNull(SecureModelRuntimeGateway(runtime).infer(request))
    }
}
