package ai.ritav.core.security.ios

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.kCFBooleanTrue
import platform.Foundation.NSData
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
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
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess

@OptIn(ExperimentalForeignApi::class)
@OptIn(ExperimentalForeignApi::class)
internal const val MAX_IOS_KEYCHAIN_VALUE_BYTES = 131_072
internal const val MAX_IOS_KEYCHAIN_KEY_LENGTH = 128

/**
 * Bounded Keychain-backed store for iOS/iPadOS security state.
 *
 * The implementation uses the app's default Keychain access group and
 * kSecAttrAccessibleWhenUnlockedThisDeviceOnly so state is not migratable
 * through the device backup/restore path. No raw secrets are logged or
 * returned through errors.
 */
class IosSecureLocalStore(
    private val service: String = DEFAULT_SERVICE
) {
    init {
        require(service.isNotBlank() && service.length <= MAX_IOS_KEYCHAIN_KEY_LENGTH)
    }

    fun putString(name: String, value: String) {
        validateName(name)
        val data = requireUtf8(value)

        val query = itemQuery(name).toMutableMap<Any?, Any?>()
        SecItemDelete(query as CFDictionaryRef)

        query[kSecValueData] = data
        query[kSecAttrAccessible] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly

        check(SecItemAdd(query as CFDictionaryRef, null) == errSecSuccess) {
            "iOS secure local write failed"
        }
    }

    fun getString(name: String): String? {
        validateName(name)

        val query = itemQuery(name).toMutableMap<Any?, Any?>()
        query[kSecReturnData] = kCFBooleanTrue
        query[kSecMatchLimit] = kSecMatchLimitOne

        return memScoped {
            val result = null
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
            when (status) {
                errSecItemNotFound -> null
                errSecSuccess -> {
                    val data = result as? NSData ?: return@memScoped null
                    require(data.length.toLong() <= MAX_IOS_KEYCHAIN_VALUE_BYTES)
                    NSString.create(data, NSUTF8StringEncoding)?.toString()
                }
                else -> error("iOS secure local read failed")
            }
        }
    }

    fun remove(name: String) {
        validateName(name)
        check(SecItemDelete(itemQuery(name) as CFDictionary) == errSecSuccess ||
            SecItemCopyMatching(
                itemQuery(name).toMutableMap<Any?, Any?>()
                    .apply {
                        this[kSecReturnData] = kCFBooleanTrue
                        this[kSecMatchLimit] = kSecMatchLimitOne
                    } as CFDictionary,
                null
            ) == errSecItemNotFound
        ) {
            "iOS secure local delete failed"
        }
    }

    private fun requireUtf8(value: String): NSData {
        val bytes = value.encodeToByteArray()
        require(bytes.size <= MAX_IOS_KEYCHAIN_VALUE_BYTES) {
            "iOS secure value is too large"
        }
        return bytes.usePinned { pinned ->
            NSData.create(
                bytes = pinned.addressOf(0),
                length = bytes.size.toULong()
            )
        }
    }

    private fun validateName(name: String) {
        require(name.isNotBlank() && name.length <= MAX_IOS_KEYCHAIN_KEY_LENGTH)
    }

    private fun itemQuery(name: String): MutableMap<Any?, Any?> = mutableMapOf(
        kSecClass to kSecClassGenericPassword,
        kSecAttrService to service,
        kSecAttrAccount to name
    )

    private companion object {
        const val DEFAULT_SERVICE = "ai.ritav.core.secure-state"
    }
}
