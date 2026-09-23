package ai.ritav.app.platform

import ai.ritav.app.core.orchestrator.AgentCapabilityScope
import ai.ritav.app.core.orchestrator.AgentRequest
import ai.ritav.app.core.orchestrator.ScreenContextSnapshot
import ai.ritav.app.core.orchestrator.ScreenContentSource
import ai.ritav.app.core.security.Capability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAccessibilityContextGateTest {
    private fun request(scope: Set<Capability> = setOf(Capability.READ_ALLOWED_CONTENT)): AgentRequest =
        AgentRequest.create(
            taskId = "task-1",
            input = "open",
            scope = AgentCapabilityScope(scope)
        )!!

    private fun snapshot(
        taskId: String = "task-1",
        packageName: String = "com.example.safe",
        text: String = "Welcome"
    ) = ScreenContextSnapshot(
        taskId = taskId,
        packageName = packageName,
        source = ScreenContentSource.ACCESSIBILITY,
        text = text,
        observedAtMillis = 100L
    )

    @Test fun readingContentRequiresExplicitScopedCapability() {
        val gate = AndroidAccessibilityContextGate()
        assertFalse(
            gate.arm(
                request = request(emptySet()),
                packageName = "com.example.safe",
                agentId = "android-model",
                expiresAtElapsedRealtime = 10_000L,
                nowElapsedRealtime = 1_000L,
                stopGeneration = 1L
            )
        )
    }

    @Test fun matchingTaskPackageAndGenerationProducesFilteredContext() {
        val gate = AndroidAccessibilityContextGate()
        assertTrue(
            gate.arm(
                request = request(),
                packageName = "com.example.safe",
                agentId = "android-model",
                expiresAtElapsedRealtime = 10_000L,
                nowElapsedRealtime = 1_000L,
                stopGeneration = 1L
            )
        )

        val prepared = gate.prepare(snapshot(), 1_500L, 1L)

        assertNotNull(prepared)
        assertEquals("task-1", prepared?.request?.taskId)
    }

    @Test fun wrongPackageCannotConsumeArmedContext() {
        val gate = AndroidAccessibilityContextGate()
        gate.arm(request(), "com.example.safe", "android-model", 10_000L, 1_000L, 1L)

        assertNull(gate.prepare(snapshot(packageName = "com.example.other"), 1_500L, 1L))
    }

    @Test fun wrongTaskCannotConsumeArmedContext() {
        val gate = AndroidAccessibilityContextGate()
        gate.arm(request(), "com.example.safe", "android-model", 10_000L, 1_000L, 1L)

        assertNull(gate.prepare(snapshot(taskId = "other-task"), 1_500L, 1L))
    }

    @Test fun stopGenerationInvalidatesArmedContext() {
        val gate = AndroidAccessibilityContextGate()
        gate.arm(request(), "com.example.safe", "android-model", 10_000L, 1_000L, 3L)

        assertNull(gate.prepare(snapshot(), 1_500L, 4L))
    }

    @Test fun expiryInvalidatesArmedContext() {
        val gate = AndroidAccessibilityContextGate()
        gate.arm(request(), "com.example.safe", "android-model", 2_000L, 1_000L, 1L)

        assertNull(gate.prepare(snapshot(), 2_000L, 1L))
    }

    @Test fun sensitiveAccessibilityTextIsBlocked() {
        val gate = AndroidAccessibilityContextGate()
        gate.arm(request(), "com.example.safe", "android-model", 10_000L, 1_000L, 1L)

        assertNull(gate.prepare(snapshot(text = "Password: hunter2"), 1_500L, 1L))
    }

    @Test fun staleObservationCannotReachModelContext() {
        val gate = AndroidAccessibilityContextGate()
        gate.arm(request(), "com.example.safe", "android-model", 10_000L, 1_000L, 1L)

        assertNull(gate.prepare(snapshot(observedAt = 2_000L), 4_001L, 1L))
    }

    @Test fun futureObservationCannotReachModelContext() {
        val gate = AndroidAccessibilityContextGate()
        gate.arm(request(), "com.example.safe", "android-model", 10_000L, 1_000L, 1L)

        assertNull(gate.prepare(snapshot(observedAt = 1_501L), 1_500L, 1L))
    }

    @Test fun sessionDurationIsBounded() {
        val gate = AndroidAccessibilityContextGate()
        assertFalse(
            gate.arm(
                request = request(),
                packageName = "com.example.safe",
                agentId = "android-model",
                expiresAtElapsedRealtime = 31_001L,
                nowElapsedRealtime = 1_000L,
                stopGeneration = 1L
            )
        )
    }
}
