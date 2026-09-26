package ai.ritav.app.core.security

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * One-time authorization gate bound to the exact action plan.
 * Token minting is internal; production callers must use the trusted
 * ActionAuthorizationService rather than self-asserting an auth level.
 */
class ActionAuthorizationGate(
    private val emergencyStop: EmergencyStopController = EmergencyStopController(),
    private val clockEpochMillis: () -> Long = System::currentTimeMillis
) {
    private data class Grant(
        val planHash: String,
        val requiredLevel: AuthorizationLevel,
        val expiresAtEpochMillis: Long,
        val emergencyStopGeneration: Long
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

        return emergencyStop.runIfInactive {
            val token = UUID.randomUUID().toString()
            grants[token] = Grant(
                planHash = plan.stableHash(),
                requiredLevel = requiredLevel,
                expiresAtEpochMillis = nowEpochMillis + ttlMillis,
                emergencyStopGeneration = emergencyStop.generation()
            )
            token
        } ?: throw IllegalStateException("Emergency Stop is active")
    }

    internal fun emergencyStopController(): EmergencyStopController = emergencyStop

    /** Atomically validates and consumes a token using the gate-owned security clock. */
    internal fun consume(
        token: String,
        plan: ActionPlan,
        providedLevel: AuthorizationLevel
    ): Boolean {
        val now = runCatching { clockEpochMillis() }
            .getOrNull()
            ?.takeIf { it >= 0L }
            ?: return false
        return consumeAt(token, plan, providedLevel, now)
    }

    /**
     * Legacy deterministic test/diagnostic entry point. Production authorization
     * paths must use the clock-owned overload above so callers cannot control TTL.
     */
    @Deprecated("Use the gate-owned-clock consume overload for security decisions")
    fun consume(
        token: String,
        plan: ActionPlan,
        providedLevel: AuthorizationLevel,
        nowEpochMillis: Long
    ): Boolean = consumeAt(token, plan, providedLevel, nowEpochMillis)

    private fun consumeAt(
        token: String,
        plan: ActionPlan,
        providedLevel: AuthorizationLevel,
        nowEpochMillis: Long
    ): Boolean {
        return emergencyStop.runIfInactive {
            if (token.isBlank() || token.length > MAX_TOKEN_LENGTH) return@runIfInactive false
            if (nowEpochMillis < 0) return@runIfInactive false
            if (!plan.isValid()) return@runIfInactive false
            if (providedLevel != requiredAuthorizationFor(plan)) return@runIfInactive false

            val expectedPlanHash = plan.stableHash()
            var consumed = false
            grants.computeIfPresent(token) { _, grant ->
                val valid =
                    nowEpochMillis <= grant.expiresAtEpochMillis &&
                        grant.emergencyStopGeneration == emergencyStop.generation() &&
                        grant.planHash == expectedPlanHash &&
                        authorizationRank(providedLevel) >= authorizationRank(grant.requiredLevel)
                if (valid) {
                    consumed = true
                    null
                } else {
                    grant
                }
            }
            consumed
        } ?: false
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
