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
        if (confirmedPlanHash != plan.stableHash()) return null
        return gate.issue(plan, AuthorizationLevel.USER_CONFIRMATION, nowEpochMillis)
    }

    fun issueDeviceAuthenticationToken(
        plan: ActionPlan,
        reason: String,
        callback: (token: String?) -> Unit
    ) {
        if (!deviceAuthorization.isDeviceAuthenticationAvailable()) {
            callback(null)
            return
        }
        deviceAuthorization.authenticate(reason) { success ->
            callback(
                if (success) gate.issue(
                    plan,
                    AuthorizationLevel.DEVICE_AUTHENTICATION,
                    clockEpochMillis()
                ) else null
            )
        }
    }
}
