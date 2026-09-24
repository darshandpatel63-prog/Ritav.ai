package ai.ritav.core.security.windows

import ai.ritav.core.security.PlatformSecureLocalStore
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import platform.posix.getenv
import platform.windows.CRYPTPROTECT_UI_FORBIDDEN
import platform.windows.CryptProtectData
import platform.windows.CryptUnprotectData
import platform.windows.DATA_BLOB
import platform.windows.GetLastError
import platform.windows.LocalFree
import java.io.File

internal const val MAX_WINDOWS_SECURE_VALUE_BYTES = 131_072
internal const val MAX_WINDOWS_SECURE_KEY_LENGTH = 128

/**
 * Windows DPAPI-backed local store.
 *
 * DPAPI user scope is intentionally used: protected bytes are normally
 * decryptable only by the same Windows user context on the same machine.
 * The encrypted blob is local application state; no network/provider is used.
 *
 * This is a storage primitive only. It grants no authorization or execution
 * authority.
 */
@OptIn(ExperimentalForeignApi::class)
class WindowsSecureLocalStore(
    private val rootDirectory: File = defaultRootDirectory()
) : PlatformSecureLocalStore {

    override fun putString(name: String, value: String) {
        validateName(name)
        val plaintext = value.encodeToByteArray()
        require(plaintext.size <= MAX_WINDOWS_SECURE_VALUE_BYTES) {
            "Windows secure value is too large"
        }

        val protected = protect(plaintext)
        val target = fileFor(name)
        val temp = File(target.parentFile, target.name + ".tmp")

        temp.writeBytes(protected)
        if (!temp.renameTo(target)) {
            temp.delete()
            error("Windows secure local atomic replace failed")
        }
    }

    override fun getString(name: String): String? {
        validateName(name)
        val target = fileFor(name)
        if (!target.exists()) return null

        val protected = target.readBytes()
        require(protected.size <= MAX_WINDOWS_SECURE_VALUE_BYTES * 2) {
            "Windows secure blob is too large"
        }

        return unprotect(protected).decodeToString()
    }

    override fun remove(name: String) {
        validateName(name)
        val target = fileFor(name)
        if (!target.exists()) return
        check(target.delete()) { "Windows secure local delete failed" }
    }

    private fun protect(plaintext: ByteArray): ByteArray = memScoped {
        val input = alloc<DATA_BLOB>()
        val output = alloc<DATA_BLOB>()

        plaintext.usePinned { pinned ->
            input.cbData = plaintext.size.toUInt()
            input.pbData = pinned.addressOf(0)
            check(
                CryptProtectData(
                    input.ptr,
                    null,
                    null,
                    null,
                    null,
                    CRYPTPROTECT_UI_FORBIDDEN,
                    output.ptr
                )
            ) { "Windows DPAPI protect failed: error=" + GetLastError() }
        }

        try {
            require(output.cbData.toLong() <= MAX_WINDOWS_SECURE_VALUE_BYTES * 2L) {
                "Windows DPAPI output is too large"
            }
            output.copyBytes()
        } finally {
            output.pbData?.let { LocalFree(it) }
        }
    }

    private fun unprotect(protectedBytes: ByteArray): ByteArray = memScoped {
        val input = alloc<DATA_BLOB>()
        val output = alloc<DATA_BLOB>()

        protectedBytes.usePinned { pinned ->
            input.cbData = protectedBytes.size.toUInt()
            input.pbData = pinned.addressOf(0)
            check(
                CryptUnprotectData(
                    input.ptr,
                    null,
                    null,
                    null,
                    null,
                    CRYPTPROTECT_UI_FORBIDDEN,
                    output.ptr
                )
            ) { "Windows DPAPI unprotect failed: error=" + GetLastError() }
        }

        try {
            require(output.cbData.toLong() <= MAX_WINDOWS_SECURE_VALUE_BYTES) {
                "Windows DPAPI plaintext is too large"
            }
            output.copyBytes()
        } finally {
            output.pbData?.let { LocalFree(it) }
        }
    }

    private fun fileFor(name: String): File {
        val directory = rootDirectory
        if (!directory.exists()) {
            check(directory.mkdirs() || directory.exists()) {
                "Windows secure local directory creation failed"
            }
        }
        return File(directory, encodeFileName(name))
    }

    private fun validateName(name: String) {
        require(name.isNotBlank() && name.length <= MAX_WINDOWS_SECURE_KEY_LENGTH)
        require(name.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }) {
            "Windows secure key contains unsupported characters"
        }
    }

    private fun encodeFileName(name: String): String =
        name.encodeToByteArray().joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        } + ".bin"

    private fun DATA_BLOB.copyBytes(): ByteArray {
        val size = cbData.toInt()
        if (size == 0) return ByteArray(0)
        val source = pbData ?: error("Windows DPAPI returned null data")
        return ByteArray(size) { index ->
            source[index].toInt().and(0xff).toByte()
        }
    }

    private companion object {
        fun defaultRootDirectory(): File {
            val localAppData = getenv("LOCALAPPDATA")?.toKString()
                ?: error("Windows LOCALAPPDATA is unavailable")
            require(localAppData.isNotBlank() && localAppData.length <= 512)
            return File(localAppData, "Ritav.ai/secure-state-v1")
        }
    }
}
