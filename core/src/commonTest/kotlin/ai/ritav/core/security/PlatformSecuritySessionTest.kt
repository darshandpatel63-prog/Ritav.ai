package ai.ritav.core.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlatformSecuritySessionTest {
    @Test
    fun successfulAuthenticationCreatesShortLivedGenerationBoundSession() {
        val store = FakeSecureStore()
        val authenticator = FakeAuthenticator(available = true, result = true)
        var now = 1_000L
        val service = PlatformSecuritySessionService(store, authenticator) { now }

        var session: PlatformSecuritySession? = null
        service.authenticate("Authorize protected Ritav action") { session = it }

        assertNotNull(session)
        assertEquals(1, authenticator.authenticateCalls)
        assertTrue(service.isValid(session, now))

        now += PLATFORM_SECURITY_SESSION_TTL_MILLIS
        assertFalse(service.isValid(session, now))
    }

    @Test
    fun unavailableAuthenticationFailsClosedWithoutPrompt() {
        val store = FakeSecureStore()
        val authenticator = FakeAuthenticator(available = false, result = true)
        val service = PlatformSecuritySessionService(store, authenticator) { 1_000L }

        var session: PlatformSecuritySession? = null
        service.authenticate("Authorize protected Ritav action") { session = it }

        assertNull(session)
        assertEquals(0, authenticator.authenticateCalls)
    }

    @Test
    fun authenticationFailureDoesNotCreateSession() {
        val store = FakeSecureStore()
        val authenticator = FakeAuthenticator(available = true, result = false)
        val service = PlatformSecuritySessionService(store, authenticator) { 1_000L }

        var session: PlatformSecuritySession? = null
        service.authenticate("Authorize protected Ritav action") { session = it }

        assertNull(session)
        assertEquals(1, authenticator.authenticateCalls)
    }

    @Test
    fun malformedReasonFailsClosedBeforePrompt() {
        val store = FakeSecureStore()
        val authenticator = FakeAuthenticator(available = true, result = true)
        val service = PlatformSecuritySessionService(store, authenticator) { 1_000L }

        var callbackCalled = false
        service.authenticate("x".repeat(MAX_PLATFORM_AUTH_REASON_LENGTH + 1)) {
            callbackCalled = true
            assertNull(it)
        }

        assertTrue(callbackCalled)
        assertEquals(0, authenticator.authenticateCalls)
    }

    @Test
    fun invalidateSessionsRejectsPreviouslyIssuedSession() {
        val store = FakeSecureStore()
        val authenticator = FakeAuthenticator(available = true, result = true)
        val service = PlatformSecuritySessionService(store, authenticator) { 2_000L }

        var session: PlatformSecuritySession? = null
        service.authenticate("Authorize protected Ritav action") { session = it }
        assertTrue(service.isValid(session, 2_000L))

        assertTrue(service.invalidateSessions())
        assertFalse(service.isValid(session, 2_000L))
    }

    @Test
    fun storageFailureFailsClosed() {
        val store = FakeSecureStore(failReads = true)
        val authenticator = FakeAuthenticator(available = true, result = true)
        val service = PlatformSecuritySessionService(store, authenticator) { 3_000L }

        var session: PlatformSecuritySession? = null
        service.authenticate("Authorize protected Ritav action") { session = it }

        assertNull(session)
        assertEquals(0, authenticator.authenticateCalls)
    }

    @Test
    fun malformedStoredGenerationFailsClosedWithoutReplacement() {
        val store = FakeSecureStore(initialGeneration = "")
        val authenticator = FakeAuthenticator(available = true, result = true)
        val service = PlatformSecuritySessionService(store, authenticator) { 3_500L }

        var session: PlatformSecuritySession? = null
        service.authenticate("Authorize protected Ritav action") { session = it }

        assertNull(session)
        assertEquals(0, authenticator.authenticateCalls)
        assertEquals("", store.peekGeneration())
    }

    @Test
    fun secureStoreWriteFailureFailsClosedAfterAuthentication() {
        val store = FakeSecureStore(failWrites = true)
        val authenticator = FakeAuthenticator(available = true, result = true)
        val service = PlatformSecuritySessionService(store, authenticator) { 3_750L }

        var session: PlatformSecuritySession? = null
        service.authenticate("Authorize protected Ritav action") { session = it }

        assertNull(session)
        assertEquals(1, authenticator.authenticateCalls)
    }

    @Test
    fun generationChangeDuringAuthenticationInvalidatesTheAttempt() {
        val store = FakeSecureStore()
        lateinit var service: PlatformSecuritySessionService
        val authenticator = FakeAuthenticator(
            available = true,
            result = true,
            beforeCallback = { assertTrue(service.invalidateSessions()) }
        )
        service = PlatformSecuritySessionService(store, authenticator) { 4_000L }

        var session: PlatformSecuritySession? = null
        service.authenticate("Authorize protected Ritav action") { session = it }

        assertNull(session)
    }

    private class FakeSecureStore(
        private val failReads: Boolean = false,
        private val failWrites: Boolean = false,
        initialGeneration: String? = null
    ) : PlatformSecureLocalStore {
        private val values = mutableMapOf<String, String>()

        init {
            if (initialGeneration != null) {
                values["ritav_platform_security_generation_v1"] = initialGeneration
            }
        }

        override fun putString(name: String, value: String) {
            if (failWrites) error("secure store unavailable")
            values[name] = value
        }

        override fun getString(name: String): String? {
            if (failReads) error("secure store unavailable")
            return values[name]
        }

        override fun remove(name: String) {
            values.remove(name)
        }

        fun peekGeneration(): String? = values["ritav_platform_security_generation_v1"]
    }

    private class FakeAuthenticator(
        private val available: Boolean,
        private val result: Boolean,
        private val beforeCallback: (() -> Unit)? = null
    ) : PlatformDeviceAuthenticator {
        var authenticateCalls: Int = 0
            private set

        override fun isDeviceAuthenticationAvailable(): Boolean = available

        override fun authenticate(
            reason: String,
            callback: (success: Boolean) -> Unit
        ) {
            authenticateCalls += 1
            beforeCallback?.invoke()
            callback(result)
        }
    }
}
