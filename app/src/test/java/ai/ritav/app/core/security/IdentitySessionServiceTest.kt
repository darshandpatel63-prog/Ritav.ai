package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentitySessionServiceTest {
    @Test
    fun successfulDeviceAuthenticationCreatesTrustedSession() {
        val gateway = RecordingGateway(available = true, result = true)
        val service = IdentitySessionService(
            sessionManager = IdentitySessionManager(),
            authenticationGateway = gateway,
            clockEpochMillis = { 1_000L }
        )

        var session: SecuritySession? = null
        service.authenticate("Authorize protected Ritav actions") { session = it }

        assertNotNull(session)
        assertEquals(IdentityLevel.TRUSTED_SIGNAL, session!!.identity)
        assertTrue(session!!.isActive(1_000L))
    }

    @Test
    fun failedAuthenticationCannotCreateSession() {
        val gateway = RecordingGateway(available = true, result = false)
        val service = IdentitySessionService(IdentitySessionManager(), gateway)

        val existingManager = IdentitySessionManager()
        var session: SecuritySession? =
            existingManager.createSession(IdentityLevel.OWNER_SIGNAL, 0L, 1_000L)
        service.authenticate("Authorize protected Ritav actions") { session = it }

        assertNull(session)
    }

    @Test
    fun unavailableAuthenticationCannotCreateSession() {
        val gateway = RecordingGateway(available = false, result = true)
        val service = IdentitySessionService(IdentitySessionManager(), gateway)

        var session: SecuritySession? =
            IdentitySessionManager().createSession(IdentityLevel.OWNER_SIGNAL, 0L, 1_000L)
        service.authenticate("Authorize protected Ritav actions") { session = it }

        assertNull(session)
        assertEquals(false, gateway.authenticateCalled)
    }

    @Test
    fun malformedOrSensitiveReasonFailsClosed() {
        val gateway = RecordingGateway(available = true, result = true)
        val service = IdentitySessionService(IdentitySessionManager(), gateway)

        var session: SecuritySession? =
            IdentitySessionManager().createSession(IdentityLevel.OWNER_SIGNAL, 0L, 1_000L)
        service.authenticate("password: secret1234") { session = it }

        assertNull(session)
        assertEquals(false, gateway.authenticateCalled)
    }

    @Test
    fun negativeClockFailsClosedAfterAuthentication() {
        val gateway = RecordingGateway(available = true, result = true)
        val service = IdentitySessionService(
            sessionManager = IdentitySessionManager(),
            authenticationGateway = gateway,
            clockEpochMillis = { -1L }
        )

        var session: SecuritySession? =
            IdentitySessionManager().createSession(IdentityLevel.OWNER_SIGNAL, 0L, 1_000L)
        service.authenticate("Authorize protected Ritav actions") { session = it }

        assertNull(session)
    }

    @Test
    fun callbackIsDeliveredOnlyOnce() {
        val gateway = object : DeviceAuthorizationGateway {
            override fun isDeviceAuthenticationAvailable(): Boolean = true

            override fun authenticate(
                reason: String,
                callback: (success: Boolean) -> Unit
            ) {
                callback(true)
                callback(true)
                callback(false)
            }
        }
        val service = IdentitySessionService(
            sessionManager = IdentitySessionManager(),
            authenticationGateway = gateway,
            clockEpochMillis = { 1_000L }
        )

        val sessions = mutableListOf<SecuritySession?>()
        service.authenticate("Authorize protected Ritav actions") { sessions += it }

        assertEquals(1, sessions.size)
        assertNotNull(sessions.single())
    }

    @Test
    fun authenticationGatewayFailureFailsClosed() {
        val gateway = object : DeviceAuthorizationGateway {
            override fun isDeviceAuthenticationAvailable(): Boolean = true

            override fun authenticate(
                reason: String,
                callback: (success: Boolean) -> Unit
            ) {
                error("platform failure")
            }
        }
        val service = IdentitySessionService(IdentitySessionManager(), gateway)

        var session: SecuritySession? =
            IdentitySessionManager().createSession(IdentityLevel.OWNER_SIGNAL, 0L, 1_000L)
        service.authenticate("Authorize protected Ritav actions") { session = it }

        assertNull(session)
    }


    private class RecordingGateway(
        private val available: Boolean,
        private val result: Boolean
    ) : DeviceAuthorizationGateway {
        var authenticateCalled = false
            private set

        override fun isDeviceAuthenticationAvailable(): Boolean = available

        override fun authenticate(
            reason: String,
            callback: (success: Boolean) -> Unit
        ) {
            authenticateCalled = true
            callback(result)
        }
    }
}
