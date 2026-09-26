package ai.ritav.core.security.macos

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
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

internal const val MAX_MACOS_KEYCHAIN_VALUE_BYTES = 131_072
internal const val MAX_MACOS_KEYCHAIN_KEY_LENGTH = 128

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class MacosSecureLocalStore(
    private val service: String = DEFAULT_SERVICE
) : PlatformSecureLocalStore {
    init { require(service.isNotBlank() && service.length <= MAX_MACOS_KEYCHAIN_KEY_LENGTH) }

    override fun putString(name: String, value: String) {
        validateName(name)
        val bytes = value.encodeToByteArray()
        require(bytes.size <= MAX_MACOS_KEYCHAIN_VALUE_BYTES)
        val lookup = buildQuery(name)
        val update = buildDataAttribute(bytes)
        val updateStatus = SecItemUpdate(lookup.dictionary, update.dictionary)
        lookup.release()
        update.release()
        when (updateStatus) {
            errSecSuccess -> Unit
            errSecItemNotFound -> {
                val add = buildQuery(name, bytes)
                val status = SecItemAdd(add.dictionary, null)
                add.release()
                check(status == errSecSuccess) { "macOS secure local write failed: status=$status" }
            }
            else -> error("macOS secure local update failed: status=$updateStatus")
        }
    }

    override fun getString(name: String): String? {
        validateName(name)
        val query = buildQuery(name, returnData = true, matchLimitOne = true)
        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query.dictionary, result.ptr)
            query.release()
            when (status) {
                errSecItemNotFound -> null
                errSecSuccess -> {
                    val data = result.value as? NSData ?: error("macOS secure local read returned invalid data")
                    require(data.length.toLong() <= MAX_MACOS_KEYCHAIN_VALUE_BYTES)
                    ByteArray(data.length.toInt()).also { bytes ->
                        if (bytes.isNotEmpty()) {
                            bytes.usePinned { pinned ->
                                platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
                            }
                        }
                    }.decodeToString()
                }
                else -> error("macOS secure local read failed: status=$status")
            }
        }
    }

    override fun remove(name: String) {
        validateName(name)
        val query = buildQuery(name)
        val status = SecItemDelete(query.dictionary)
        query.release()
        check(status == errSecSuccess || status == errSecItemNotFound)
    }

    private fun validateName(name: String) {
        require(name.isNotBlank() && name.length <= MAX_MACOS_KEYCHAIN_KEY_LENGTH)
    }

    private fun buildDataAttribute(valueBytes: ByteArray): KeychainQuery {
        val dictionary = CFDictionaryCreateMutable(null, 0, null, null) ?: error("dictionary creation failed")
        val unsignedBytes = UByteArray(valueBytes.size) { valueBytes[it].toUByte() }
        val data = if (unsignedBytes.isEmpty()) CFDataCreate(null, null, 0) else unsignedBytes.usePinned {
            CFDataCreate(null, it.addressOf(0), valueBytes.size.toLong())
        } ?: error("data creation failed")
        CFDictionaryAddValue(dictionary, kSecValueData!!, data)
        return KeychainQuery(dictionary, listOf(data))
    }

    private fun buildQuery(name: String, valueBytes: ByteArray? = null, returnData: Boolean = false, matchLimitOne: Boolean = false): KeychainQuery {
        val dictionary = CFDictionaryCreateMutable(null, 0, null, null) ?: error("dictionary creation failed")
        val ownedValues = mutableListOf<CFTypeRef>()
        fun addString(key: CFStringRef, value: String) {
            val cfValue = CFStringCreateWithCString(null, value, kCFStringEncodingUTF8) ?: error("string creation failed")
            CFDictionaryAddValue(dictionary, key, cfValue)
            ownedValues += cfValue
        }
        CFDictionaryAddValue(dictionary, kSecClass!!, kSecClassGenericPassword!!)
        addString(kSecAttrService!!, service)
        addString(kSecAttrAccount!!, name)
        if (valueBytes != null) {
            val unsignedBytes = UByteArray(valueBytes.size) { valueBytes[it].toUByte() }
            val data = if (unsignedBytes.isEmpty()) CFDataCreate(null, null, 0) else unsignedBytes.usePinned {
                CFDataCreate(null, it.addressOf(0), valueBytes.size.toLong())
            } ?: error("data creation failed")
            CFDictionaryAddValue(dictionary, kSecValueData!!, data)
            ownedValues += data
        }
        if (returnData) CFDictionaryAddValue(dictionary, kSecReturnData!!, kCFBooleanTrue!!)
        if (matchLimitOne) CFDictionaryAddValue(dictionary, kSecMatchLimit!!, kSecMatchLimitOne!!)
        return KeychainQuery(dictionary, ownedValues)
    }

    private class KeychainQuery(val dictionary: CFDictionaryRef, private val ownedValues: List<CFTypeRef>) {
        fun release() {
            ownedValues.forEach { value -> CFRelease(value) }
            CFRelease(dictionary)
        }
    }

    private companion object { const val DEFAULT_SERVICE = "ai.ritav.core.secure-state" }
}
