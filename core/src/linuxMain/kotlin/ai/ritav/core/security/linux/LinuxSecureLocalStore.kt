package ai.ritav.core.security.linux

import ai.ritav.core.security.PlatformSecureLocalStore
import ai.ritav.core.security.linux.libsecret.ritav_secret_clear
import ai.ritav.core.security.linux.libsecret.ritav_secret_free
import ai.ritav.core.security.linux.libsecret.ritav_secret_lookup
import ai.ritav.core.security.linux.libsecret.ritav_secret_store
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.pointed
import platform.posix.getenv
import kotlinx.cinterop.toKString

internal const val MAX_LINUX_SECRET_SERVICE_VALUE_BYTES = 131_072
internal const val MAX_LINUX_SECRET_SERVICE_KEY_LENGTH = 128

/**
 * Bounded Linux Secret Service-backed local store.
 *
 * The Linux runtime delegates protected-value storage to the user's Secret
 * Service (for example GNOME Keyring or KWallet-compatible services) through
 * libsecret. Lookup attributes are non-secret metadata; the stored value is
 * the protected secret.
 *
 * This primitive grants storage authority only. Missing/unavailable Secret
 * Service access fails closed and is never replaced with plaintext storage.
 */
@OptIn(ExperimentalForeignApi::class)
class LinuxSecureLocalStore(
    private val service: String = DEFAULT_SERVICE
) : PlatformSecureLocalStore {

    init {
        validateIdentifier(service)
    }

    override fun putString(name: String, value: String) {
        validateIdentifier(name)
        validateValue(value)

        val status = ritav_secret_store(service, name, value)
        check(status == STATUS_SUCCESS) {
            "Linux secure local write failed: status=$status"
        }
    }

    override fun getString(name: String): String? {
        validateIdentifier(name)

        return memScoped {
            val outValue = alloc<CPointerVar<ByteVar>>()
            val status = ritav_secret_lookup(service, name, outValue.ptr)

            when (status) {
                STATUS_NOT_FOUND -> null
                STATUS_SUCCESS -> {
                    val value = outValue.value
                        ?: error("Linux secure local read returned no value")
                    try {
                        value.toKString()
                    } finally {
                        ritav_secret_free(value)
                    }
                }
                else -> error("Linux secure local read failed: status=$status")
            }
        }
    }

    override fun remove(name: String) {
        validateIdentifier(name)

        val status = ritav_secret_clear(service, name)
        check(status == STATUS_SUCCESS || status == STATUS_NOT_FOUND) {
            "Linux secure local delete failed: status=$status"
        }
    }

    private fun validateIdentifier(value: String) {
        require(
            value.isNotBlank() &&
                value.length <= MAX_LINUX_SECRET_SERVICE_KEY_LENGTH &&
                '\u0000' !in value
        )
    }

    private fun validateValue(value: String) {
        require(
            '\u0000' !in value &&
                value.encodeToByteArray().size <= MAX_LINUX_SECRET_SERVICE_VALUE_BYTES
        ) {
            "Linux secure value is too large or contains an embedded NUL"
        }
    }

    private companion object {
        const val STATUS_NOT_FOUND = 0
        const val STATUS_SUCCESS = 1
        const val DEFAULT_SERVICE = "ai.ritav.core.secure-state"
    }
}

internal fun linuxSecretServiceIntegrationTestsEnabled(): Boolean =
    getenv("RITAV_ENABLE_SECRET_SERVICE_INTEGRATION_TESTS")?.toKString() == "1"
