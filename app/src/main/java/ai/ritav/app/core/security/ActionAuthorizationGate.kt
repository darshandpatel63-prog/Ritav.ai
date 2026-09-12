package ai.ritav.app.core.security

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * One-time authorization gate bound to the exact action plan.
 * Token minting is internal; production callers must use the trusted
 * ActionAuthorizationService rather than self-asserting an auth level.
 */
class ActionAuthorizationGate {
    private data class Grant(
        val planHash: String,
        val requiredLevel: AuthorizationLevel,
        val expiresAtEpochMillis: Long
    )

    private val grants = ConcurrentHashMap<String, Grant>()

    internal fun issue(
        plan: ActionPlan,
        requiredLevel: AuthorizationLevel,
        nowEpochMillis: Long,
        ttlMillis: Long = DEFAULT_TTL_MILLIS
    ): String {
        require(requiredLevel != AuthorizationLevel.NONE)
        require(ttlMillis in 1..MAX_TTL_MILLIS)

        val token = UUID.randomUUID().toString()
        grants[token] = Grant(
            planHash = plan.stableHash(),
            requiredLevel = requiredLevel,
            expiresAtEpochMillis = nowEpochMillis + ttlMillis
        )
        return token
    }

    /** Atomically validates and consumes a token. */
    fun consume(
        token: String,
        plan: ActionPlan,
        providedLevel: AuthorizationLevel,
        nowEpochMillis: Long
    ): Boolean {
        if (token.isBlank()) return false

        val grant = grants.remove(token) ?: return false
        if (nowEpochMillis > grant.expiresAtEpochMillis) return false
        if (grant.planHash != plan.stableHash()) return false
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
