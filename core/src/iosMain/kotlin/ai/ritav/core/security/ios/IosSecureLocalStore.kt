package ai.ritav.core.security.ios

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CFBridgingRelease
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSDictionary
import platform.Foundation.create
import platform.Foundation.dictionaryWithObjects
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleWhenUnlockedThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

internal const val MAX_IOS_KEYCHAIN_VALUE_BYTES = 131_072
internal const val MAX_IOS_KEYCHAIN_KEY_LENGTH = 128

/**
 * Bounded Keychain-backed store for iOS/iPadOS security state.
 *
 * Items use kSecAttrAccessibleWhenUnlockedThisDeviceOnly, keeping them tied
 * to the current device and unavailable while the device is locked.
 */
@Suppress("CAST_NEVER_SUCCEEDS")
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
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

        val data = if (bytes.isEmpty()) {
            NSData.create(bytes = null, length = 0uL)
        } else {
            bytes.usePinned { pinned ->
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = bytes.size.toULong()
                )
            }
        }

        val query = buildQuery(
            account = name,
            extras = arrayOf(
                kSecValueData to data,
                kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly
            )
        )
        val existing = SecItemDelete(buildQuery(account = name))
        check(existing == errSecSuccess || existing == errSecItemNotFound) {
            "iOS secure local replacement preparation failed"
        }

        val status = SecItemAdd(query, null)
        check(status == errSecSuccess) {
            "iOS secure local write failed"
        }
    }

    fun getString(name: String): String? {
        validateName(name)
        val query = buildQuery(
            account = name,
            extras = arrayOf(
                kSecReturnData to true,
                kSecMatchLimit to kSecMatchLimitOne
            )
        )

        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)

            when (status) {
                errSecItemNotFound -> null
                errSecSuccess -> {
                    val data = CFBridgingRelease(result.value) as? NSData ?: return@memScoped null
                    require(data.length.toLong() <= MAX_IOS_KEYCHAIN_VALUE_BYTES)

                    ByteArray(data.length.toInt()).also { bytes ->
                        if (bytes.isNotEmpty()) {
                            bytes.usePinned { pinned ->
                                platform.posix.memcpy(
                                    pinned.addressOf(0),
                                    data.bytes,
                                    data.length
                                )
                            }
                        }
                    }.decodeToString()
                }
                else -> error("iOS secure local read failed")
            }
        }
    }

    fun remove(name: String) {
        validateName(name)
        val status = SecItemDelete(buildQuery(account = name))
        check(status == errSecSuccess || status == errSecItemNotFound) {
            "iOS secure local delete failed"
        }
    }

    private fun validateName(name: String) {
        require(name.isNotBlank() && name.length <= MAX_IOS_KEYCHAIN_KEY_LENGTH)
    }

    private fun buildQuery(
        account: String,
        extras: Array<out Pair<Any?, Any?>> = emptyArray()
    ): CFDictionaryRef {
        val keys = mutableListOf<Any?>(
            kSecClass,
            kSecAttrService,
            kSecAttrAccount
        )
        val values = mutableListOf<Any?>(
            kSecClassGenericPassword,
            service,
            account
        )
        extras.forEach { (key, value) ->
            keys.add(key)
            values.add(value)
        }
        return NSDictionary.dictionaryWithObjects(
            objects = values,
            forKeys = keys
        ) as CFDictionaryRef
    }

    private companion object {
        const val DEFAULT_SERVICE = "ai.ritav.core.secure-state"
    }
}
