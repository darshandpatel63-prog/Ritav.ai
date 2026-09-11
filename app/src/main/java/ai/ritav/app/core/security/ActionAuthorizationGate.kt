package ai.ritav.app.core.security

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * One-time authorization gate bound to the exact app/capability/action/session.
 * A confirmation cannot be replayed for another action or reused after consume.
 */
class ActionAuthorizationGate {
    private data class Grant(
        val appId: String,
        val capability: Capability,
        val action: String,
        val sessionId: String?,
        val requiredLevel: AuthorizationLevel,
        val expiresAtEpochMillis: Long
    )

    private val grants = ConcurrentHashMap<String, Grant>()

    fun issue(
        appId: String,
        capability: Capability,
        action: String,
        sessionId: String?,
        requiredLevel: AuthorizationLevel,
        nowEpochMillis: Long,
        ttlMillis: Long = DEFAULT_TTL_MILLIS
    ): String {
        require(appId.isNotBlank())
        require(action.isNotBlank())
        require(ttlMillis in 1..MAX_TTL_MILLIS)
        require(requiredLevel != AuthorizationLevel.NONE)

        val token = UUID.randomUUID().toString()
        grants[token] = Grant(
            appId = appId,
            capability = capability,
            action = action,
            sessionId = sessionId,
            requiredLevel = requiredLevel,
            expiresAtEpochMillis = nowEpochMillis + ttlMillis
        )
        return token
    }

    /** Atomically validates and consumes a token. */
    fun consume(
        token: String,
        appId: String,
        capability: Capability,
        action: String,
        sessionId: String?,
        providedLevel: AuthorizationLevel,
        nowEpochMillis: Long
    ): Boolean {
        if (token.isBlank()) return false

        val grant = grants.remove(token) ?: return false
        if (nowEpochMillis > grant.expiresAtEpochMillis) return false
        if (grant.appId != appId || grant.capability != capability || grant.action != action) return false
        if (grant.sessionId != sessionId) return false
        return authorizationRank(providedLevel) >= authorizationRank(grant.requiredLevel)
    }

    fun purgeExpired(nowEpochMillis: Long) {
        grants.entries.removeIf { nowEpochMillis > it.value.expiresAtEpochMillis }
    }

    private fun authorizationRank(level: AuthorizationLevel): Int = when (level) {
        AuthorizationLevel.NONE -> 0
        AuthorizationLevel.USER_CONFIRMATION -> 1
        AuthorizationLevel.DEVICE_AUTHENTICATION -> 2
    }

    private companion object {
        const val DEFAULT_TTL_MILLIS = 60_000L
        const val MAX_TTL_MILLIS = 5 * 60_000L
    }
}
