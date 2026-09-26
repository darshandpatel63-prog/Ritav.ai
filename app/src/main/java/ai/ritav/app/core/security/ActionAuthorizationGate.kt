package ai.ritav.app.core.security

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Public authorization authority.
 *
 * Raw token minting is deliberately encapsulated inside this service so callers
 * cannot self-assert an authorization level or caller-controlled timestamp.
 */
internal class ActionAuthorizationService(
    private val deviceAuthorization: DeviceAuthorizationGateway,
    private val clockEpochMillis: () -> Long = System::currentTimeMillis,
    private val emergencyStop: EmergencyStopController = EmergencyStopController()
) {
    /**
     * Private token gate. Its existence and raw issuance surface are inaccessible
     * outside this authority, including to future UI code in the same app module.
     */
    private class TokenGate(
        private val emergencyStop: EmergencyStopController,
        private val clockEpochMillis: () -> Long
    ) {
        private data class Grant(
            val planHash: String,
            val requiredLevel: AuthorizationLevel,
            val expiresAtEpochMillis: Long,
            val emergencyStopGeneration: Long
        )

        private val grants = ConcurrentHashMap<String, Grant>()

        fun issue(
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

        fun consume(
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

        fun consumeAt(
            token: String,
            plan: ActionPlan,
            providedLevel: AuthorizationLevel,
            nowEpochMillis: Long
        ): Boolean {
            return emergencyStop.runIfInactive {
                if (token.isBlank() || token.length > MAX_TOKEN_LENGTH) return@runIfInactive false
                if (nowEpochMillis < 0L) return@runIfInactive false
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

        private fun purgeExpired(nowEpochMillis: Long) {
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

    private val tokenGate = TokenGate(
        emergencyStop = emergencyStop,
        clockEpochMillis = clockEpochMillis
    )

    /**
     * Mints a one-time user-confirmation token using the service-owned clock at
     * the authorization event itself. A caller-supplied timestamp is never trusted.
     */
    fun issueUserConfirmationToken(
        plan: ActionPlan,
        confirmedPlanHash: String
    ): String? {
        if (!plan.isValid()) return null
        if (emergencyStop.isActive()) return null
        if (plan.riskTier != RiskTier.TIER_2_CONTENT_MUTATION) return null
        if (confirmedPlanHash != plan.stableHash()) return null

        return runCatching {
            val now = clockEpochMillis()
            if (now < 0L || emergencyStop.isActive()) {
                return@runCatching null
            }
            tokenGate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, now)
        }.getOrNull()
    }

    fun issueDeviceAuthenticationToken(
        plan: ActionPlan,
        reason: String,
        callback: (token: String?) -> Unit
    ) {
        val responds = AtomicBoolean(false)
        fun respond(token: String?) {
            if (responds.compareAndSet(false, true)) {
                callback(token)
            }
        }

        val validRequest = runCatching {
            !emergencyStop.isActive() &&
                plan.isValid() &&
                plan.riskTier == RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE &&
                reason.isNotBlank() &&
                reason.length <= MAX_REASON_LENGTH &&
                deviceAuthorization.isDeviceAuthenticationAvailable()
        }.getOrDefault(false)

        if (!validRequest) {
            respond(null)
            return
        }

        runCatching {
            deviceAuthorization.authenticate(reason) { success ->
                if (!success) {
                    respond(null)
                    return@authenticate
                }
                val token = runCatching {
                    val now = clockEpochMillis()
                    if (emergencyStop.isActive() || now < 0) null
                    else tokenGate.issue(plan, AuthorizationLevel.DEVICE_AUTHENTICATION, now)
                }.getOrNull()
                respond(token)
            }
        }.onFailure {
            respond(null)
        }
    }

    /** Internal consumption surface used only by security-owned execution paths. */
    internal fun consume(
        token: String,
        plan: ActionPlan,
        providedLevel: AuthorizationLevel
    ): Boolean = tokenGate.consume(token, plan, providedLevel)

    internal fun emergencyStopController(): EmergencyStopController = emergencyStop

    private companion object {
        const val MAX_REASON_LENGTH = 512
    }
}
