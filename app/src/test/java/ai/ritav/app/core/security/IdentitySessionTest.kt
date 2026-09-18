package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentitySessionTest {
    @Test fun managerCreatesActiveSessionWithinItsLifetime() {
        val manager = IdentitySessionManager()
        val session = manager.createSession(IdentityLevel.OWNER_SIGNAL, 1_000L, 60_000L)

        assertTrue(manager.permitsProtectedCapability(session, 1_000L))
        assertTrue(manager.permitsProtectedCapability(session, 61_000L))
        assertFalse(manager.permitsProtectedCapability(session, 61_001L))
    }

    @Test fun sessionIsNotActiveBeforeAuthenticationTimestamp() {
        val manager = IdentitySessionManager()
        val session = manager.createSession(IdentityLevel.OWNER_SIGNAL, 1_000L, 60_000L)

        assertFalse(manager.permitsProtectedCapability(session, 999L))
    }

    @Test fun unknownIdentityCannotCreateProtectedSession() {
        val manager = IdentitySessionManager()

        var rejected = false
        try {
            manager.createSession(IdentityLevel.UNKNOWN, 1_000L, 60_000L)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }

        assertTrue(rejected)
    }

    @Test fun negativeClockAndOverflowAreRejected() {
        val manager = IdentitySessionManager()

        var negativeRejected = false
        try {
            manager.createSession(IdentityLevel.OWNER_SIGNAL, -1L, 60_000L)
        } catch (_: IllegalArgumentException) {
            negativeRejected = true
        }

        var overflowRejected = false
        try {
            manager.createSession(IdentityLevel.OWNER_SIGNAL, Long.MAX_VALUE, 1L)
        } catch (_: IllegalArgumentException) {
            overflowRejected = true
        }

        assertTrue(negativeRejected)
        assertTrue(overflowRejected)
    }
}
