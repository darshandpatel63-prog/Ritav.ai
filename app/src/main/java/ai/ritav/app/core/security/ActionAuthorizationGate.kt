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
        require(plan.isValid())
        require(requiredLevel == requiredAuthorizationFor(plan))
        require(ttlMillis in 1..MAX_TTL_MILLIS)
        require(nowEpochMillis >= 0)
        require(nowEpochMillis <= Long.MAX_VALUE - ttlMillis)

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
        if (token.isBlank() || token.length > MAX_TOKEN_LENGTH) return false
        if (nowEpochMillis < 0) return false
        if (!plan.isValid()) return false
        if (providedLevel != requiredAuthorizationFor(plan)) return false

        val grant = grants.remove(token) ?: return false
        if (nowEpochMillis > grant.expiresAtEpochMillis) return false
        if (grant.planHash != plan.stableHash()) return false
        return authorizationRank(providedLevel) >= authorizationRank(grant.requiredLevel)
    }

    fun purgeExpired(nowEpochMillis: Long) {
        grants.entries.removeIf { nowEpochMillis > it.value.expiresAtEpochMillis }
    }

    private fun requiredAuthorizationFor(plan: ActionPlan): AuthorizationLevel = when (plan.riskTier) {
        RiskTier.TIER_0_INFORMATIONAL, RiskTier.TIER_1_REVERSIBLE -> AuthorizationLevel.NONE
        RiskTier.TIER_2_CONTENT_MUTATION -> AuthorizationLevel.USER_CONFIRMATION
        RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE -> AuthorizationLevel.DEVICE_AUTHENTICATION
        RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED -> AuthorizationLevel.NONE
    }

    private fun authorizationRank(level: AuthorizationLevel): Int = when (level) {
        AuthorizationLevel.NONE -> 0
        AuthorizationLevel.USER_CONFIRMATION -> 1
        AuthorizationLevel.DEVICE_AUTHENTICATION -> 2
    }

    private companion object {
        const val DEFAULT_TTL_MILLIS = 60_000L
        const val MAX_TTL_MILLIS = 5 * 60_000L
        const val MAX_TOKEN_LENGTH = 128
    }
}
