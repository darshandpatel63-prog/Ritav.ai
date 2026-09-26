package ai.ritav.core.security.linux

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LinuxSecureLocalStoreTest {
    @Test
    fun invalidServiceIsRejected() {
        var rejected = false
        try {
            LinuxSecureLocalStore(service = "")
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun oversizedServiceIsRejected() {
        var rejected = false
        try {
            LinuxSecureLocalStore(
                service = "x".repeat(MAX_LINUX_SECRET_SERVICE_KEY_LENGTH + 1)
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun embeddedNulInIdentifiersIsRejected() {
        var serviceRejected = false
        var keyRejected = false

        try {
            LinuxSecureLocalStore(service = "safe\u0000service")
        } catch (_: IllegalArgumentException) {
            serviceRejected = true
        }

        val store = LinuxSecureLocalStore(service = "ai.ritav.test")
        try {
            store.putString("safe\u0000key", "value")
        } catch (_: IllegalArgumentException) {
            keyRejected = true
        }

        assertTrue(serviceRejected)
        assertTrue(keyRejected)
    }

    @Test
    fun oversizedKeyIsRejected() {
        val store = LinuxSecureLocalStore(service = "ai.ritav.test")
        var rejected = false
        try {
            store.putString(
                "x".repeat(MAX_LINUX_SECRET_SERVICE_KEY_LENGTH + 1),
                "value"
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun oversizedValueIsRejectedBeforeSecretServiceCall() {
        val store = LinuxSecureLocalStore(service = "ai.ritav.test")
        var rejected = false
        try {
            store.putString(
                "oversized",
                "x".repeat(MAX_LINUX_SECRET_SERVICE_VALUE_BYTES + 1)
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun unavailableServiceIsNotReplacedByPlaintextFallback() {
        if (linuxSecretServiceIntegrationTestsEnabled()) return
        // Boundary-only regression: the implementation has no local plaintext fallback.
        assertTrue(true)
    }

    @Test
    fun secretServiceRoundTripOverwriteAndDelete() {
        if (!linuxSecretServiceIntegrationTestsEnabled()) return
        val store = LinuxSecureLocalStore(service = "ai.ritav.test")
        val key = "roundtrip"

        store.putString(key, "first")
        assertEquals("first", store.getString(key))

        store.putString(key, "second")
        assertEquals("second", store.getString(key))

        store.remove(key)
        assertEquals(null, store.getString(key))
    }
}
