package ai.ritav.core.security.ios

import platform.Foundation.NSLock
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.darwin.NSError

/**
 * Native iOS/iPadOS device-authentication primitive.
 *
 * This does not issue authorization tokens and has no execution authority.
 * It only asks LocalAuthentication for an OS-owned authentication result.
 */
class IosDeviceAuthenticationRuntime {
    fun isDeviceAuthenticationAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(
            LAPolicyDeviceOwnerAuthentication,
            error = null
        )
    }

    fun authenticate(
        reason: String,
        callback: (success: Boolean) -> Unit
    ) {
        if (reason.isBlank()) {
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
