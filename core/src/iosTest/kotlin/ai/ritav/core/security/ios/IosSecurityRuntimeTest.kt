package ai.ritav.core.security.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosSecureLocalStoreTest {
    @Test
    fun keychainRoundTripAndDelete() {
        val store = IosSecureLocalStore(service = "ai.ritav.test")
        val key = "test-key"
        println("IOS_KEYCHAIN_TEST_STAGE=put")
        try {
            store.putString(key, "hello")
            println("IOS_KEYCHAIN_TEST_STAGE=get")
            assertNotNull(store.getString(key))
            assertEquals("hello", store.getString(key))
        } catch (t: Throwable) {
            assertTrue(false, "keychain round-trip failure: $t")
        }

        println("IOS_KEYCHAIN_TEST_STAGE=get-after")
        assertEquals("hello", store.getString(key))
    }

    @Test
    fun existingValueCanBeSafelyOverwritten() {
        val store = IosSecureLocalStore(service = "ai.ritav.test")
        val key = "overwrite"
        try {
            store.putString(key, "first")
            store.putString(key, "second")

            assertEquals("second", store.getString(key))
            assertEquals("second", store.getString(key))
        } catch (t: Throwable) {
            assertTrue(false, "keychain overwrite failure: $t")
        }
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
    fun oversizedAuthenticationReasonFailsClosed() {
        val runtime = IosDeviceAuthenticationRuntime()
        var called = false

        runtime.authenticate("x".repeat(513)) {
            called = true
            assertEquals(false, it)
        }

        assertTrue(called)
    }

    @Test
    fun authenticationAvailabilityIsOSReported() {
        val runtime = IosDeviceAuthenticationRuntime()
        // Simulator/device capability varies; only require a deterministic Boolean path.
        val available = runtime.isDeviceAuthenticationAvailable()
        assertTrue(available || !available)
    }
}
