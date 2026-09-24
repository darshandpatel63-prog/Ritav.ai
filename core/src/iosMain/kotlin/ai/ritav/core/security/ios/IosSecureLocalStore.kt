package ai.ritav.core.security.ios

import ai.ritav.core.security.PlatformSecureLocalStore
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.alloc
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Foundation.NSData
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
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
 * Uses CoreFoundation dictionaries directly at the Security.framework boundary.
 * This avoids relying on toll-free NSDictionary/CFDictionary casts in Kotlin/Native.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosSecureLocalStore(
    private val service: String = DEFAULT_SERVICE
) : PlatformSecureLocalStore {

    init {
        require(service.isNotBlank() && service.length <= MAX_IOS_KEYCHAIN_KEY_LENGTH)
    }

    override fun putString(name: String, value: String) {
        validateName(name)
        val bytes = value.encodeToByteArray()
        require(bytes.size <= MAX_IOS_KEYCHAIN_VALUE_BYTES) {
            "iOS secure value is too large"
        }

        val lookupQuery = buildKeychainQuery(name)
        val updateAttributes = buildKeychainQuery(
            name = name,
            valueBytes = bytes,
            includeAccessible = true
        )

        val lookupRef = lookupQuery
        val updateRef = updateAttributes
        val updateStatus = SecItemUpdate(lookupRef, updateRef)
        CFRelease(lookupRef)
        CFRelease(updateRef)

        when (updateStatus) {
            errSecSuccess -> Unit
            errSecItemNotFound -> {
                val addQuery = buildKeychainQuery(
                    name = name,
                    valueBytes = bytes,
                    includeAccessible = true
                )
                val addStatus = SecItemAdd(addQuery, null)
                CFRelease(addQuery)
                check(addStatus == errSecSuccess) {
                    "iOS secure local write failed"
                }
            }
            else -> error("iOS secure local update failed")
        }
    }

    override fun getString(name: String): String? {
        validateName(name)

        val query = buildKeychainQuery(
            name = name,
            returnData = true,
            matchLimitOne = true
        )

        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            CFRelease(query)

            when (status) {
                errSecItemNotFound -> null
                errSecSuccess -> {
                    val data = result.value as? NSData ?: return@memScoped null
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

    override fun remove(name: String) {
        validateName(name)

        val query = buildKeychainQuery(name)
        val status = SecItemDelete(query)
        CFRelease(query)

        check(status == errSecSuccess || status == errSecItemNotFound) {
            "iOS secure local delete failed"
        }
    }

    private fun validateName(name: String) {
        require(name.isNotBlank() && name.length <= MAX_IOS_KEYCHAIN_KEY_LENGTH)
    }

    private fun buildKeychainQuery(
        name: String,
        valueBytes: ByteArray? = null,
        includeAccessible: Boolean = false,
        returnData: Boolean = false,
        matchLimitOne: Boolean = false
    ): CFDictionaryRef {
        val dictionary = CFDictionaryCreateMutable(null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
            ?: error("iOS secure local dictionary creation failed")

        fun addString(key: platform.CoreFoundation.CFTypeRef, value: String) {
            val cfValue = CFStringCreateWithCString(null, value, kCFStringEncodingUTF8)
                ?: error("iOS secure local string creation failed")
            CFDictionaryAddValue(dictionary, key, cfValue)
            CFRelease(cfValue)
        }

        CFDictionaryAddValue(dictionary, kSecClass!!, kSecClassGenericPassword!!)
        addString(kSecAttrService!!, service)
        addString(kSecAttrAccount!!, name)

        if (valueBytes != null) {
            val unsignedBytes = UByteArray(valueBytes.size) { index -> valueBytes[index].toUByte() }
            val cfData = unsignedBytes.usePinned { pinned ->
                CFDataCreate(null, pinned.addressOf(0), valueBytes.size.toLong())
            } ?: error("iOS secure local data creation failed")
            CFDictionaryAddValue(dictionary, kSecValueData!!, cfData!!)
            CFRelease(cfData)
        }

        if (includeAccessible) {
            CFDictionaryAddValue(
                dictionary,
                kSecAttrAccessible!!,
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly!!
            )
        }

        if (returnData) {
            CFDictionaryAddValue(dictionary, kSecReturnData!!, platform.CoreFoundation.kCFBooleanTrue!!)
        }

        if (matchLimitOne) {
            CFDictionaryAddValue(dictionary, kSecMatchLimit!!, kSecMatchLimitOne!!)
        }

        return dictionary
    }

    private companion object {
        const val DEFAULT_SERVICE = "ai.ritav.core.secure-state"
    }
}
