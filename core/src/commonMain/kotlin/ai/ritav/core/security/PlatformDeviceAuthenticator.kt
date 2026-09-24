package ai.ritav.core.security

/**
 * Platform-neutral device-authentication primitive.
 *
 * The implementation delegates the authentication decision to the host OS.
 * A successful result is only an authentication signal; it is not an
 * authorization token and cannot bypass deterministic policy.
 */
interface PlatformDeviceAuthenticator {
    fun isDeviceAuthenticationAvailable(): Boolean

    /** Must invoke the callback at most once. */
    fun authenticate(reason: String, callback: (success: Boolean) -> Unit)
}
