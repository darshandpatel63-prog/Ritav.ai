package ai.ritav.core.security.ios

import ai.ritav.core.security.PlatformDeviceAuthenticator
import ai.ritav.core.security.PlatformSecureLocalStore
import ai.ritav.core.security.PlatformSecuritySessionService
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * Native iOS/iPadOS security-session composition.
 *
 * The concrete Keychain store and LocalAuthentication runtime are injected into
 * the platform-neutral deterministic session path. This class grants no
 * capabilities and does not execute actions.
 */
class IosSecuritySessionRuntime(
    service: String = DEFAULT_SERVICE
) {
    internal val secureStore: PlatformSecureLocalStore = IosSecureLocalStore(service)
    internal val deviceAuthenticator: PlatformDeviceAuthenticator = IosDeviceAuthenticationRuntime()
    val sessionService = PlatformSecuritySessionService(
        secureStore = secureStore,
        deviceAuthenticator = deviceAuthenticator,
        nowEpochMillis = {
            NSDate().timeIntervalSince1970.toLong() * 1000L
        }
    )

    private companion object {
        const val DEFAULT_SERVICE = "ai.ritav.core.security-session"
    }
}
