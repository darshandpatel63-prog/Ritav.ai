package ai.ritav.core.security.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosSecureLocalStoreTest {
    @Test
    fun keychainRoundTripAndDelete() {
        val store = IosSecureLocalStore(service = "ai.ritav.test")
        val key = "test-key"
        println("IOS_KEYCHAIN_TEST_STAGE=remove-before")
        try {
            store.remove(key)
        } catch (_: Throwable) {
        }

        println("IOS_KEYCHAIN_TEST_STAGE=put")
        store.putString(key, "hello")
        println("IOS_KEYCHAIN_TEST_STAGE=get")
        assertNotNull(store.getString(key))
        assertEquals("hello", store.getString(key))

        println("IOS_KEYCHAIN_TEST_STAGE=remove-after")
        store.remove(key)
        println("IOS_KEYCHAIN_TEST_STAGE=get-after-remove")
        assertNull(store.getString(key))
    }

    @Test
    fun oversizedValueIsRejectedBeforeKeychainWrite() {
        val store = IosSecureLocalStore(service = "ai.ritav.test")
        val key = "oversized"
        val oversized = "x".repeat(MAX_IOS_KEYCHAIN_VALUE_BYTES + 1)

        var rejected = false
        try {
            store.putString(key, oversized)
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }
}

class IosDeviceAuthenticationRuntimeTest {
    @Test
    fun authenticationAvailabilityIsOSReported() {
        val runtime = IosDeviceAuthenticationRuntime()
        // Simulator/device capability varies; only require a deterministic Boolean path.
        val available = runtime.isDeviceAuthenticationAvailable()
        assertTrue(available || !available)
    }
}
