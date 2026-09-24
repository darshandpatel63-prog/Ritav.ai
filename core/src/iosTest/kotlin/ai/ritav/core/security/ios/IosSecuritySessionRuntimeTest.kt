package ai.ritav.core.security.ios

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosSecuritySessionRuntimeTest {
    @Test
    fun concreteAppleSecurityRuntimeWiresNativePrimitivesIntoSessionPath() {
        val runtime = IosSecuritySessionRuntime()
        assertNotNull(runtime.secureStore)
        assertNotNull(runtime.deviceAuthenticator)
        assertNotNull(runtime.sessionService)

        var callbackCalled = false
        runtime.sessionService.authenticate("x".repeat(513)) {
            callbackCalled = true
            assertNull(it)
        }

        assertTrue(callbackCalled)
    }
}
