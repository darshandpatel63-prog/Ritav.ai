package ai.ritav.core.security.macos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun integrationEnabled(): Boolean =
    platform.Foundation.NSProcessInfo.processInfo.environment["RITAV_ENABLE_KEYCHAIN_INTEGRATION_TESTS"] == "1"

class MacosSecureLocalStoreTest {
    @Test
    fun invalidServiceIsRejected() {
        var rejected = false
        try {
            MacosSecureLocalStore(service = "")
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun oversizedServiceIsRejected() {
        var rejected = false
        try {
            MacosSecureLocalStore(service = "x".repeat(MAX_MACOS_KEYCHAIN_KEY_LENGTH + 1))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun embeddedNulInServiceIsRejected() {
        var rejected = false
        try {
            MacosSecureLocalStore(service = "safe\u0000service")
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun oversizedKeyIsRejectedBeforeKeychainWrite() {
        val store = MacosSecureLocalStore(service = "ai.ritav.test")
        var rejected = false
        try {
            store.putString("x".repeat(MAX_MACOS_KEYCHAIN_KEY_LENGTH + 1), "value")
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun embeddedNulInKeyIsRejectedBeforeKeychainWrite() {
        val store = MacosSecureLocalStore(service = "ai.ritav.test")
        var rejected = false
        try {
            store.putString("key\u0000suffix", "value")
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun oversizedValueIsRejectedBeforeKeychainWrite() {
        val store = MacosSecureLocalStore(service = "ai.ritav.test")
        var rejected = false
        try {
            store.putString("oversized", "x".repeat(MAX_MACOS_KEYCHAIN_VALUE_BYTES + 1))
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun invalidKeyIsRejected() {
        val store = MacosSecureLocalStore(service = "ai.ritav.test")
        var rejected = false
        try {
            store.putString("", "value")
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun keychainRoundTripOverwriteAndDelete() {
        if (!integrationEnabled()) return
        val store = MacosSecureLocalStore(service = "ai.ritav.test")
        val key = "roundtrip"
        store.putString(key, "first")
        assertEquals("first", store.getString(key))
        store.putString(key, "second")
        assertEquals("second", store.getString(key))
        store.remove(key)
        assertEquals(null, store.getString(key))
    }
}
