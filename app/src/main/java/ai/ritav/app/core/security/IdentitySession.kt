package ai.ritav.app.core.security

import java.util.UUID

/** Local session identity signal. It is a signal, not an absolute security proof. */
enum class IdentityLevel {
    UNKNOWN,
    OWNER_SIGNAL,
    TRUSTED_SIGNAL
}

data class SecuritySession(
    val id: String = UUID.randomUUID().toString(),
    val identity: IdentityLevel = IdentityLevel.UNKNOWN,
    val authenticatedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long
) {
    fun isActive(nowEpochMillis: Long): Boolean = nowEpochMillis <= expiresAtEpochMillis
}

class IdentitySessionManager {
    fun createSession(
        identity: IdentityLevel,
        nowEpochMillis: Long,
        ttlMillis: Long = DEFAULT_TTL_MILLIS
    ): SecuritySession {
        require(ttlMillis in 1..MAX_TTL_MILLIS)
        return SecuritySession(
            identity = identity,
            authenticatedAtEpochMillis = nowEpochMillis,
            expiresAtEpochMillis = nowEpochMillis + ttlMillis
        )
    }

    fun permitsProtectedCapability(session: SecuritySession?, nowEpochMillis: Long): Boolean =
        session != null &&
            session.isActive(nowEpochMillis) &&
            session.identity != IdentityLevel.UNKNOWN

    private companion object {
        const val DEFAULT_TTL_MILLIS = 5 * 60_000L
        const val MAX_TTL_MILLIS = 15 * 60_000L
    }
}
