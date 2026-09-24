package ai.ritav.core.security.ios

import ai.ritav.core.security.PlatformSecureLocalStore
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Foundation.NSData
import platform.Foundation.NSLog
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
 * Security.framework receives CoreFoundation dictionaries whose temporary
 * CFString/CFData values remain owned until the corresponding Security call
 * completes. This avoids unsafe NSDictionary/CFDictionary casts and avoids
 * dangling values when using a non-retaining CFDictionary.
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

        val lookup = buildKeychainQuery(name)
        val update = buildUpdateAttributes(bytes)

        val updateStatus = SecItemUpdate(lookup.dictionary, update.dictionary)
        NSLog("IOS_KEYCHAIN_UPDATE_STATUS=%d", updateStatus)
        lookup.release()
        update.release()

        when (updateStatus) {
            errSecSuccess -> Unit
            errSecItemNotFound -> {
                val add = buildKeychainQuery(
                    name = name,
                    valueBytes = bytes,
                    includeAccessible = true
                )
                val addStatus = SecItemAdd(add.dictionary, null)
                NSLog("IOS_KEYCHAIN_ADD_STATUS=%d", addStatus)
                add.release()
                check(addStatus == errSecSuccess) {
                    "iOS secure local write failed: status=$addStatus"
                }
            }
            else -> error("iOS secure local update failed: status=$updateStatus")
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
            val status = SecItemCopyMatching(query.dictionary, result.ptr)
            NSLog("IOS_KEYCHAIN_GET_STATUS=%d", status)
            query.release()

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
                else -> error("iOS secure local read failed: status=$status")
            }
        }
    }

    override fun remove(name: String) {
        validateName(name)

        val query = buildKeychainQuery(name)
        val status = SecItemDelete(query.dictionary)
        query.release()

        check(status == errSecSuccess || status == errSecItemNotFound) {
            "iOS secure local delete failed: status=$status"
        }
    }

    private fun validateName(name: String) {
        require(name.isNotBlank() && name.length <= MAX_IOS_KEYCHAIN_KEY_LENGTH)
    }

    private fun buildUpdateAttributes(valueBytes: ByteArray): KeychainQuery {
        val dictionary = CFDictionaryCreateMutable(null, 0, null, null)
            ?: error("iOS secure local update dictionary creation failed")
        val unsignedBytes = UByteArray(valueBytes.size) { index -> valueBytes[index].toUByte() }
        val cfData = if (unsignedBytes.isEmpty()) {
            CFDataCreate(null, null, 0)
        } else {
            unsignedBytes.usePinned { pinned ->
                CFDataCreate(null, pinned.addressOf(0), valueBytes.size.toLong())
            }
        } ?: error("iOS secure local update data creation failed")
        CFDictionaryAddValue(dictionary, kSecValueData!!, cfData)
        return KeychainQuery(dictionary, listOf(cfData))
    }

    private fun buildKeychainQuery(
        name: String,
        valueBytes: ByteArray? = null,
        includeAccessible: Boolean = false,
        returnData: Boolean = false,
        matchLimitOne: Boolean = false
    ): KeychainQuery {
        val dictionary = CFDictionaryCreateMutable(null, 0, null, null)
            ?: error("iOS secure local dictionary creation failed")
        val ownedValues = mutableListOf<CFTypeRef>()

        fun addString(key: CFStringRef, value: String) {
            val cfValue = CFStringCreateWithCString(null, value, kCFStringEncodingUTF8)
                ?: error("iOS secure local string creation failed")
            CFDictionaryAddValue(dictionary, key, cfValue)
            ownedValues += cfValue
        }

        CFDictionaryAddValue(dictionary, kSecClass!!, kSecClassGenericPassword!!)
        addString(kSecAttrService!!, service)
        addString(kSecAttrAccount!!, name)

        if (valueBytes != null) {
            val unsignedBytes = UByteArray(valueBytes.size) { index -> valueBytes[index].toUByte() }
            val cfData = if (unsignedBytes.isEmpty()) {
                CFDataCreate(null, null, 0)
            } else {
                unsignedBytes.usePinned { pinned ->
                    CFDataCreate(null, pinned.addressOf(0), valueBytes.size.toLong())
                }
            } ?: error("iOS secure local data creation failed")
            CFDictionaryAddValue(dictionary, kSecValueData!!, cfData)
            ownedValues += cfData
        }

        if (includeAccessible) {
            CFDictionaryAddValue(
                dictionary,
                kSecAttrAccessible!!,
                kSecAttrAccessibleWhenUnlockedThisDeviceOnly!!
            )
        }

        if (returnData) {
            CFDictionaryAddValue(dictionary, kSecReturnData!!, kCFBooleanTrue!!)
        }

        if (matchLimitOne) {
            CFDictionaryAddValue(dictionary, kSecMatchLimit!!, kSecMatchLimitOne!!)
        }

        return KeychainQuery(dictionary, ownedValues)
    }

    private class KeychainQuery(
        val dictionary: CFDictionaryRef,
        private val ownedValues: List<CFTypeRef>
    ) {
        fun release() {
            ownedValues.forEach { value -> CFRelease(value) }
            CFRelease(dictionary)
        }
    }

    private companion object {
        const val DEFAULT_SERVICE = "ai.ritav.core.secure-state"
    }
}
