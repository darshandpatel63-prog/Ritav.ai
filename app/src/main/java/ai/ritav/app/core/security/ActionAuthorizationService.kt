package ai.ritav.app.core.security

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Only trusted UI/device-authentication paths can mint execution tokens.
 * Callers cannot self-assert DEVICE_AUTHENTICATION by passing an enum.
 */
class ActionAuthorizationService(
    private val gate: ActionAuthorizationGate,
    private val deviceAuthorization: DeviceAuthorizationGateway,
    private val clockEpochMillis: () -> Long = System::currentTimeMillis,
    private val emergencyStop: EmergencyStopController = gate.emergencyStopController()
) {
    /**
     * Mints a one-time user-confirmation token using the service-owned clock at
     * the authorization event itself. A caller-supplied timestamp is never
     * trusted for token lifetime.
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
            gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, now)
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
                    else gate.issue(plan, AuthorizationLevel.DEVICE_AUTHENTICATION, now)
                }.getOrNull()
                respond(token)
            }
        }.onFailure {
            respond(null)
        }
    }

    private companion object {
        const val MAX_REASON_LENGTH = 512
    }
}
