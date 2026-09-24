package ai.ritav.core.security

import kotlin.uuid.Uuid

internal const val PLATFORM_SECURITY_SESSION_TTL_MILLIS = 30_000L
internal const val MAX_PLATFORM_AUTH_REASON_LENGTH = 512
internal const val MAX_PLATFORM_SECURITY_GENERATION_LENGTH = 128
private const val SECURITY_GENERATION_STORAGE_KEY = "ritav_platform_security_generation_v1"

/**
 * Opaque short-lived authentication session used by deterministic security code.
 *
 * The session is not an authorization token. It is bound to a generation stored
 * in the platform secure store and is invalidated whenever that generation changes.
 */
data class PlatformSecuritySession internal constructor(
    val id: String,
    val generation: String,
    val issuedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long
)

/**
 * Platform-neutral deterministic authentication/session path.
 *
 * OS authentication provides only an authentication signal. This service turns
 * that signal into a short-lived, generation-bound session. It never grants
 * capabilities or execution authority.
 */
class PlatformSecuritySessionService(
    private val secureStore: PlatformSecureLocalStore,
    private val deviceAuthenticator: PlatformDeviceAuthenticator,
    private val nowEpochMillis: () -> Long
) {
    /**
     * Starts OS-controlled device authentication and returns one short-lived
     * session on success. A platform authentication implementation must invoke
     * its callback at most once.
     */
    fun authenticate(
        reason: String,
        callback: (session: PlatformSecuritySession?) -> Unit
    ) {
        if (reason.isBlank() || reason.length > MAX_PLATFORM_AUTH_REASON_LENGTH) {
            callback(null)
            return
        }

        val preAuthenticationGeneration = readOrInitializeGeneration() ?: run {
            callback(null)
            return
        }

        val available = runCatching {
            deviceAuthenticator.isDeviceAuthenticationAvailable()
        }.getOrDefault(false)
        if (!available) {
            callback(null)
            return
        }

        runCatching {
            deviceAuthenticator.authenticate(reason) { success ->
                if (!success) {
                    callback(null)
                    return@authenticate
                }

                val now = runCatching { nowEpochMillis() }.getOrNull()
                    ?: run {
                        callback(null)
                        return@authenticate
                    }
                if (now < 0L) {
                    callback(null)
                    return@authenticate
                }

                // Detect invalidation or another successful authentication while
                // the OS-owned authentication prompt was open.
                val currentGeneration = readGeneration() ?: run {
                    callback(null)
                    return@authenticate
                }
                if (currentGeneration != preAuthenticationGeneration) {
                    callback(null)
                    return@authenticate
                }

                val expiresAt = now + PLATFORM_SECURITY_SESSION_TTL_MILLIS
                if (expiresAt < now) {
                    callback(null)
                    return@authenticate
                }

                // Rotate the generation so previously issued sessions are
                // immediately invalidated and only the newest authenticated
                // session remains valid.
                val sessionGeneration = newGeneration()
                if (!persistGeneration(sessionGeneration)) {
                    callback(null)
                    return@authenticate
                }

                callback(
                    PlatformSecuritySession(
                        id = Uuid.random().toString(),
                        generation = sessionGeneration,
                        issuedAtEpochMillis = now,
                        expiresAtEpochMillis = expiresAt
                    )
                )
            }
        }.onFailure {
            callback(null)
        }
    }

    /**
     * Deterministically validates a session against the current secure-store
     * generation and its bounded lifetime.
     */
    fun isValid(
        session: PlatformSecuritySession?,
        nowEpochMillis: Long
    ): Boolean {
        if (session == null || nowEpochMillis < 0L) return false
        if (session.id.isBlank() || session.id.length > MAX_PLATFORM_SECURITY_GENERATION_LENGTH) return false
        if (session.generation.isBlank() || session.generation.length > MAX_PLATFORM_SECURITY_GENERATION_LENGTH) {
            return false
        }
        if (session.issuedAtEpochMillis < 0L || session.expiresAtEpochMillis <= session.issuedAtEpochMillis) {
            return false
        }
        if (session.expiresAtEpochMillis - session.issuedAtEpochMillis > PLATFORM_SECURITY_SESSION_TTL_MILLIS) {
            return false
        }
        if (nowEpochMillis < session.issuedAtEpochMillis || nowEpochMillis >= session.expiresAtEpochMillis) {
            return false
        }

        val currentGeneration = readGeneration() ?: return false
        return currentGeneration == session.generation
    }

    /**
     * Invalidates all issued sessions by rotating the secure generation.
     * Failure is reported as false because the caller cannot safely assume
     * invalidation succeeded when protected storage is unavailable.
     */
    fun invalidateSessions(): Boolean {
        return persistGeneration(newGeneration())
    }

    private fun readOrInitializeGeneration(): String? {
        return when (val state = readGenerationState()) {
            GenerationRead.Missing -> {
                val created = newGeneration()
                created.takeIf(::persistGeneration)
            }
            is GenerationRead.Present -> state.value
            GenerationRead.Invalid -> null
        }
    }

    private fun readGeneration(): String? {
        return when (val state = readGenerationState()) {
            is GenerationRead.Present -> state.value
            GenerationRead.Missing,
            GenerationRead.Invalid -> null
        }
    }

    private fun readGenerationState(): GenerationRead {
        val result = runCatching { secureStore.getString(SECURITY_GENERATION_STORAGE_KEY) }
        if (result.isFailure) return GenerationRead.Invalid

        val value = result.getOrNull() ?: return GenerationRead.Missing
        if (value.isBlank() || value.length > MAX_PLATFORM_SECURITY_GENERATION_LENGTH) {
            return GenerationRead.Invalid
        }
        return GenerationRead.Present(value)
    }

    private sealed interface GenerationRead {
        data object Missing : GenerationRead
        data object Invalid : GenerationRead
        data class Present(val value: String) : GenerationRead
    }

    private fun persistGeneration(value: String): Boolean {
        if (value.isBlank() || value.length > MAX_PLATFORM_SECURITY_GENERATION_LENGTH) {
            return false
        }
        return runCatching {
            secureStore.putString(SECURITY_GENERATION_STORAGE_KEY, value)
        }.isSuccess
    }

    private fun newGeneration(): String = Uuid.random().toString()
}
