package ai.ritav.core.security.ios

import ai.ritav.core.security.PlatformDeviceAuthenticator

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLock
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication

@OptIn(ExperimentalForeignApi::class)
/**
 * Native iOS/iPadOS device-authentication primitive.
 *
 * This does not issue authorization tokens and has no execution authority.
 * It only asks LocalAuthentication for an OS-owned authentication result.
 */
class IosDeviceAuthenticationRuntime : PlatformDeviceAuthenticator {
    private companion object {
        const val MAX_AUTH_REASON_LENGTH = 512
    }
    override fun isDeviceAuthenticationAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthentication,
            error = null
        )
    }

    override fun authenticate(
        reason: String,
        callback: (success: Boolean) -> Unit
    ) {
        if (reason.isBlank() || reason.length > MAX_AUTH_REASON_LENGTH) {
            callback(false)
            return
        }

        val context = LAContext()
        if (!context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null)) {
            callback(false)
            return
        }

        val lock = NSLock()
        var delivered = false

        fun deliver(success: Boolean) {
            lock.lock()
            try {
                if (delivered) return
                delivered = true
            } finally {
                lock.unlock()
            }
            callback(success)
        }

        context.evaluatePolicy(
            LAPolicyDeviceOwnerAuthentication,
            localizedReason = reason
        ) { success, _: NSError? ->
            deliver(success)
        }
    }
}
