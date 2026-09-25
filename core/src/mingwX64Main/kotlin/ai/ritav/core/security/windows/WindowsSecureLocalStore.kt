package ai.ritav.core.security.windows

import ai.ritav.core.security.PlatformSecureLocalStore
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fwrite
import platform.posix.getenv
import platform.posix.mkdir
import platform.posix.remove
import platform.posix.rename
import platform.windows.CRYPTPROTECT_UI_FORBIDDEN
import platform.windows.CryptProtectData
import platform.windows.CryptUnprotectData
import platform.windows.DATA_BLOB
import platform.windows.GetLastError
import platform.windows.LocalFree

internal const val MAX_WINDOWS_SECURE_VALUE_BYTES = 131_072
internal const val MAX_WINDOWS_SECURE_KEY_LENGTH = 128
private const val MAX_WINDOWS_SECURE_BLOB_BYTES = MAX_WINDOWS_SECURE_VALUE_BYTES * 2

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
    private val rootDirectory: String = defaultRootDirectory()
) : PlatformSecureLocalStore {

    init {
        require(rootDirectory.isNotBlank() && rootDirectory.length <= 1024)
        ensureDirectory(rootDirectory)
    }

    override fun putString(name: String, value: String) {
        validateName(name)
        val plaintext = value.encodeToByteArray()
        require(plaintext.size <= MAX_WINDOWS_SECURE_VALUE_BYTES) {
            "Windows secure value is too large"
        }

        val protected = protect(plaintext)
        val target = fileFor(name)
        val temp = target + ".tmp"

        writeFile(temp, protected)
        platform.posix.remove(target)
        if (rename(temp, target) != 0) {
            platform.posix.remove(temp)
            error("Windows secure local replace failed")
        }
    }

    override fun getString(name: String): String? {
        validateName(name)
        val target = fileFor(name)
        val protected = readFile(target) ?: return null
        require(protected.size <= MAX_WINDOWS_SECURE_BLOB_BYTES) {
            "Windows secure blob is too large"
        }
        return unprotect(protected).decodeToString()
    }

    override fun remove(name: String) {
        validateName(name)
        val target = fileFor(name)
        if (platform.posix.remove(target) != 0) {
            // POSIX remove returns non-zero for a missing path as well; verify
            // existence through a read so failure is not silently ignored.
            if (readFile(target) != null) {
                error("Windows secure local delete failed")
            }
        }
    }

    private fun protect(plaintext: ByteArray): ByteArray = memScoped {
        val input = alloc<DATA_BLOB>()
        val output = alloc<DATA_BLOB>()

        plaintext.usePinned { pinned ->
            input.cbData = plaintext.size.toUInt()
            input.pbData = pinned.addressOf(0).reinterpret()
            check(
                CryptProtectData(
                    input.ptr,
                    null,
                    null,
                    null,
                    null,
                    CRYPTPROTECT_UI_FORBIDDEN.toUInt(),
                    output.ptr
                ) != 0
            ) { "Windows DPAPI protect failed: error=" + GetLastError() }
        }

        try {
            require(output.cbData.toLong() <= MAX_WINDOWS_SECURE_BLOB_BYTES.toLong()) {
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
            require(output.cbData.toLong() <= MAX_WINDOWS_SECURE_VALUE_BYTES.toLong()) {
                "Windows DPAPI plaintext is too large"
            }
            output.copyBytes()
        } finally {
            output.pbData?.let { LocalFree(it) }
        }
    }

    private fun writeFile(path: String, bytes: ByteArray) {
        val mode = "wb"
        memScoped {
            val file = fopen(path, mode)
                    ?: error("Windows secure local open-for-write failed")
                try {
                    if (bytes.isNotEmpty()) {
                        bytes.usePinned { bytesPinned ->
                            val written = fwrite(
                                bytesPinned.addressOf(0),
                                1u,
                                bytes.size.toULong(),
                                file
                            )
                            check(written == bytes.size.toULong()) {
                                "Windows secure local write failed"
                            }
                        }
                    }
                } finally {
                    check(fclose(file) == 0) {
                        "Windows secure local close failed"
                    }
                }
        }
    }

    private fun readFile(path: String): ByteArray? {
        memScoped {
            val file = fopen(path, "rb") ?: return null
            try {
                    val result = ArrayList<Byte>(MAX_WINDOWS_SECURE_BLOB_BYTES)
                    val buffer = ByteArray(4096)
                    while (true) {
                        val count = buffer.usePinned { pinned ->
                            fread(
                                pinned.addressOf(0),
                                1u,
                                buffer.size.toULong(),
                                file
                            ).toInt()
                        }
                        if (count == 0) break
                        result.addAll(buffer.take(count))
                        require(result.size <= MAX_WINDOWS_SECURE_BLOB_BYTES) {
                            "Windows secure blob is too large"
                        }
                    }
                    return result.toByteArray()
            } finally {
                check(fclose(file) == 0) {
                    "Windows secure local close failed"
                }
            }
        }
    }

    private fun ensureDirectory(path: String) {
        val normalized = path.replace('\\', '/').trimEnd('/')
        val parts = normalized.split('/').filter { it.isNotEmpty() }
        var current = if (normalized.startsWith('/')) "/" else ""
        for ((index, part) in parts.withIndex()) {
            current = if (current.isEmpty() || current == "/") current + part else current + "/" + part
            // A Windows drive prefix such as "C:" is not itself a directory
            // path and must not be passed to mkdir.
            if (index == 0 && part.endsWith(":")) continue
            if (mkdir(current) != 0) {
                // The directory may already exist; verify by attempting to create
                // the next level. A later file operation will fail closed otherwise.
            }
        }
    }

    private fun fileFor(name: String): String = rootDirectory + "/" + encodeFileName(name)

    private fun validateName(name: String) {
        require(name.isNotBlank() && name.length <= MAX_WINDOWS_SECURE_KEY_LENGTH)
        require(name.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }) {
            "Windows secure key contains unsupported characters"
        }
    }

    private fun encodeFileName(name: String): String =
        name.encodeToByteArray().joinToString("") { byte ->
            val value = byte.toInt() and 0xff
            "0123456789abcdef"[value ushr 4].toString() +
                "0123456789abcdef"[value and 0x0f].toString()
        } + ".bin"

    private fun DATA_BLOB.copyBytes(): ByteArray {
        val size = cbData.toInt()
        if (size == 0) return ByteArray(0)
        val source = pbData ?: error("Windows DPAPI returned null data")
        val bytes = source.reinterpret<ByteVar>()
        return ByteArray(size) { index ->
            (bytes[index].toInt() and 0xff).toByte()
        }
    }

    private companion object {
        fun defaultRootDirectory(): String {
            val localAppData = getenv("LOCALAPPDATA")?.toKString()
                ?: error("Windows LOCALAPPDATA is unavailable")
            require(localAppData.isNotBlank() && localAppData.length <= 512)
            return localAppData + "/Ritav.ai/secure-state-v1"
        }
    }
}
