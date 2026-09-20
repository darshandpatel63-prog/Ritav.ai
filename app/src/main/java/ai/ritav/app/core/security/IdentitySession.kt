package ai.ritav.app.core.security

import java.util.UUID

/** Local session identity signal. It is a signal, not an absolute security proof. */
enum class IdentityLevel {
    UNKNOWN,
    OWNER_SIGNAL,
    TRUSTED_SIGNAL
}

class SecuritySession private constructor(
    val id: String,
    val identity: IdentityLevel,
    val authenticatedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long,
    private val emergencyStopGeneration: Long,
    private val issuanceBinding: Any
) {
    fun isActive(nowEpochMillis: Long): Boolean =
        nowEpochMillis >= authenticatedAtEpochMillis && nowEpochMillis <= expiresAtEpochMillis

    internal fun isIssuedBy(binding: Any): Boolean = issuanceBinding === binding

    companion object {
        internal fun createForManager(
            issuanceBinding: Any,
            identity: IdentityLevel,
            authenticatedAtEpochMillis: Long,
            expiresAtEpochMillis: Long,
            emergencyStopGeneration: Long
        ): SecuritySession {
            require(authenticatedAtEpochMillis >= 0)
            require(expiresAtEpochMillis >= authenticatedAtEpochMillis)
            return SecuritySession(
                id = UUID.randomUUID().toString(),
                identity = identity,
                authenticatedAtEpochMillis = authenticatedAtEpochMillis,
                expiresAtEpochMillis = expiresAtEpochMillis,
                emergencyStopGeneration = emergencyStopGeneration,
                issuanceBinding = issuanceBinding
            )
        }

        private const val MAX_ID_LENGTH = 256
    }
}

class IdentitySessionManager(
    private val emergencyStop: EmergencyStopController = EmergencyStopController()
) {
    private val issuanceBinding = Any()

    /**
     * Internal issuance boundary. Production callers must obtain the identity
     * level from a trusted authentication gateway before calling this method.
     */
    internal fun createSession(
        identity: IdentityLevel,
        nowEpochMillis: Long,
        ttlMillis: Long = DEFAULT_TTL_MILLIS
    ): SecuritySession {
        require(identity != IdentityLevel.UNKNOWN)
        require(nowEpochMillis >= 0)
        require(ttlMillis in 1..MAX_TTL_MILLIS)
        require(nowEpochMillis <= Long.MAX_VALUE - ttlMillis)

        return SecuritySession.createForManager(
            issuanceBinding = issuanceBinding,
            identity = identity,
            authenticatedAtEpochMillis = nowEpochMillis,
            expiresAtEpochMillis = nowEpochMillis + ttlMillis,
            emergencyStopGeneration = emergencyStop.generation()
        )
    }

    fun permitsProtectedCapability(session: SecuritySession?, nowEpochMillis: Long): Boolean =
        session != null &&
            nowEpochMillis >= 0 &&
            session.isIssuedBy(issuanceBinding) &&
            session.isActive(nowEpochMillis) &&
            sessionGenerationMatches(session) &&
            session.identity != IdentityLevel.UNKNOWN

    private fun sessionGenerationMatches(session: SecuritySession): Boolean =
        session.emergencyStopGeneration == emergencyStop.generation()

    private companion object {
        const val DEFAULT_TTL_MILLIS = 5 * 60_000L
        const val MAX_TTL_MILLIS = 15 * 60_000L
    }
}
