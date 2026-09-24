package ai.ritav.core.security.ios

import ai.ritav.core.security.ios.keychain.ritav_keychain_delete
import ai.ritav.core.security.ios.keychain.ritav_keychain_get
import ai.ritav.core.security.ios.keychain.ritav_keychain_put
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

internal const val MAX_IOS_KEYCHAIN_VALUE_BYTES = 131_072
internal const val MAX_IOS_KEYCHAIN_KEY_LENGTH = 128

/**
 * Bounded Keychain-backed store for iOS/iPadOS security state.
 *
 * Keychain data is restricted to the current device and is only available
 * while the device is unlocked. The native bridge contains the only Security
 * framework calls; no secrets are logged.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSecureLocalStore(
    private val service: String = DEFAULT_SERVICE
) {
    init {
        require(service.isNotBlank() && service.length <= MAX_IOS_KEYCHAIN_KEY_LENGTH)
    }

    fun putString(name: String, value: String) {
        validateName(name)
        val bytes = value.encodeToByteArray()
        require(bytes.size <= MAX_IOS_KEYCHAIN_VALUE_BYTES) {
            "iOS secure value is too large"
        }

        val status = bytes.usePinned { pinned ->
            ritav_keychain_put(
                service,
                name,
                pinned.addressOf(0),
                bytes.size.toULong()
            )
        }
        check(status == 0) { "iOS secure local write failed" }
    }

    fun getString(name: String): String? {
        validateName(name)
        val buffer = ByteArray(MAX_IOS_KEYCHAIN_VALUE_BYTES)
        val result = buffer.usePinned { pinned ->
            ritav_keychain_get(
                service,
                name,
                pinned.addressOf(0),
                buffer.size.toULong()
            )
        }

        return when {
            result == -1 -> null
            result >= 0 -> buffer.copyOf(result).decodeToString()
            result == -3 -> error("iOS secure local value is too large")
            else -> error("iOS secure local read failed")
        }
    }

    fun remove(name: String) {
        validateName(name)
        val result = ritav_keychain_delete(service, name)
        check(result == 0 || result == -1) { "iOS secure local delete failed" }
    }

    private fun validateName(name: String) {
        require(name.isNotBlank() && name.length <= MAX_IOS_KEYCHAIN_KEY_LENGTH)
    }

    private companion object {
        const val DEFAULT_SERVICE = "ai.ritav.core.secure-state"
    }
}
