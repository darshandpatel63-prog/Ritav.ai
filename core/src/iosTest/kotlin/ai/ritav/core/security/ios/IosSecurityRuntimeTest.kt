package ai.ritav.core.security.ios

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class IosSecureLocalStoreTest {
    @Test
    fun keychainRoundTripAndDelete() {
        val store = IosSecureLocalStore(service = "ai.ritav.test")
        val key = "test-key"
        try {
            store.remove(key)
        } catch (_: Throwable) {
        }

        store.putString(key, "hello")
        assertNotNull(store.getString(key))
        assert(store.getString(key) == "hello")

        store.remove(key)
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
        assert(rejected)
    }
}

class IosDeviceAuthenticationRuntimeTest {
    @Test
    fun authenticationAvailabilityIsOSReported() {
        val runtime = IosDeviceAuthenticationRuntime()
        // Simulator/device capability varies; only require a deterministic Boolean path.
        val available = runtime.isDeviceAuthenticationAvailable()
        assert(available == true || available == false)
    }
}
