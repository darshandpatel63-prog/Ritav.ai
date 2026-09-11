package ai.ritav.app.core.security

/**
 * Platform authentication abstraction. The core security layer never implements
 * or guesses biometric/device credentials itself; Android provides the secure UI.
 */
interface DeviceAuthorizationGateway {
    fun isDeviceAuthenticationAvailable(): Boolean

    /** Starts platform-controlled authentication and invokes exactly one callback. */
    fun authenticate(reason: String, callback: (success: Boolean) -> Unit)
}

/** Test-only implementation. Never use this as a production authenticator. */
class StubDeviceAuthorizationGateway(
    private val available: Boolean = false,
    private val result: Boolean = false
) : DeviceAuthorizationGateway {
    override fun isDeviceAuthenticationAvailable(): Boolean = available

    override fun authenticate(reason: String, callback: (success: Boolean) -> Unit) {
        callback(available && result)
    }
}
