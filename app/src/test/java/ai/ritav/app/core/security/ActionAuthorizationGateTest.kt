package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionAuthorizationGateTest {
    @Test
    fun tokenCanBeConsumedOnlyOnce() {
        val gate = ActionAuthorizationGate()
        val token = gate.issue(
            appId = "com.example.app",
            capability = Capability.SEND_MESSAGE,
            action = "send",
            sessionId = "session-1",
            requiredLevel = AuthorizationLevel.USER_CONFIRMATION,
            nowEpochMillis = 1_000L
        )

        assertTrue(gate.consume(token, "com.example.app", Capability.SEND_MESSAGE, "send", "session-1", AuthorizationLevel.USER_CONFIRMATION, 1_001L))
        assertFalse(gate.consume(token, "com.example.app", Capability.SEND_MESSAGE, "send", "session-1", AuthorizationLevel.USER_CONFIRMATION, 1_002L))
    }

    @Test
    fun tokenCannotBeReusedForDifferentAction() {
        val gate = ActionAuthorizationGate()
        val token = gate.issue(
            appId = "com.example.app",
            capability = Capability.SEND_MESSAGE,
            action = "send",
            sessionId = "session-1",
            requiredLevel = AuthorizationLevel.USER_CONFIRMATION,
            nowEpochMillis = 1_000L
        )

        assertFalse(gate.consume(token, "com.example.app", Capability.SEND_MESSAGE, "delete", "session-1", AuthorizationLevel.USER_CONFIRMATION, 1_001L))
    }

    @Test
    fun expiredTokenIsRejected() {
        val gate = ActionAuthorizationGate()
        val token = gate.issue(
            appId = "com.example.app",
            capability = Capability.SEND_MESSAGE,
            action = "send",
            sessionId = "session-1",
            requiredLevel = AuthorizationLevel.USER_CONFIRMATION,
            nowEpochMillis = 1_000L,
            ttlMillis = 1_000L
        )

        assertFalse(gate.consume(token, "com.example.app", Capability.SEND_MESSAGE, "send", "session-1", AuthorizationLevel.USER_CONFIRMATION, 2_001L))
    }
}
