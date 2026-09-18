package ai.ritav.app.core.security

/**
 * Only trusted UI/device-authentication paths can mint execution tokens.
 * Callers cannot self-assert DEVICE_AUTHENTICATION by passing an enum.
 */
class ActionAuthorizationService(
    private val gate: ActionAuthorizationGate,
    private val deviceAuthorization: DeviceAuthorizationGateway,
    private val clockEpochMillis: () -> Long = System::currentTimeMillis
) {
    fun issueUserConfirmationToken(
        plan: ActionPlan,
        confirmedPlanHash: String,
        nowEpochMillis: Long
    ): String? {
        if (!plan.isValid()) return null
        if (plan.riskTier != RiskTier.TIER_2_CONTENT_MUTATION) return null
        if (confirmedPlanHash != plan.stableHash()) return null
        if (nowEpochMillis < 0) return null
        return gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, nowEpochMillis)
    }

    fun issueDeviceAuthenticationToken(
        plan: ActionPlan,
        reason: String,
        callback: (token: String?) -> Unit
    ) {
        if (!plan.isValid() ||
            plan.riskTier != RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE ||
            reason.isBlank() || reason.length > MAX_REASON_LENGTH ||
            !deviceAuthorization.isDeviceAuthenticationAvailable()
        ) {
            callback(null)
            return
        }
        deviceAuthorization.authenticate(reason) { success ->
            if (!success) {
                callback(null)
                return@authenticate
            }
            val now = clockEpochMillis()
            val token = runCatching {
                gate.issue(plan, AuthorizationLevel.DEVICE_AUTHENTICATION, now)
            }.getOrNull()
            callback(token)
        }
    }

    private companion object {
        const val MAX_REASON_LENGTH = 512
    }
}
