package ai.ritav.app.core.security

class PolicyEngine(
    private val permissionStore: PermissionStore = InMemoryPermissionStore(),
    private val emergencyStop: EmergencyStopController = EmergencyStopController()
) {
    fun evaluate(request: ActionRequest): PolicyDecision {
        if (emergencyStop.isActive()) return deny(AuthorizationLevel.NONE, "Emergency Stop is active")
        if (request.appId.isBlank() || request.action.isBlank()) {
            return deny(AuthorizationLevel.NONE, "App and action are required")
        }
        if (request.riskTier == RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED) {
            return deny(AuthorizationLevel.NONE, "Sensitive or prohibited action")
        }
        if (request.capability == Capability.FINANCIAL_ACTION) {
            return deny(AuthorizationLevel.DEVICE_AUTHENTICATION, "Financial actions are blocked by default")
        }
        if (request.containsSensitiveData) {
            return deny(AuthorizationLevel.NONE, "Sensitive data cannot enter action reasoning")
        }
        if (!permissionStore.isGranted(request.appId, request.capability, request.action, request.sessionId)) {
            return deny(AuthorizationLevel.NONE, "Required scoped capability is not granted")
        }
        if (request.riskTier >= RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE && !request.userExplicitlyRequested) {
            return deny(AuthorizationLevel.DEVICE_AUTHENTICATION, "Explicit user intent is required")
        }

        val requiredAuth = when (request.riskTier) {
            RiskTier.TIER_0_INFORMATIONAL, RiskTier.TIER_1_REVERSIBLE -> AuthorizationLevel.NONE
            RiskTier.TIER_2_CONTENT_MUTATION -> AuthorizationLevel.USER_CONFIRMATION
            RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE -> AuthorizationLevel.DEVICE_AUTHENTICATION
            RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED -> AuthorizationLevel.NONE
        }
        if (authorizationRank(request.authorizationLevel) < authorizationRank(requiredAuth)) {
            return deny(requiredAuth, "Risk-appropriate authorization is required")
        }
        return PolicyDecision(true, false, requiredAuth, "Allowed by deterministic policy")
    }

    fun stop() = emergencyStop.activate()

    /** Resume is intentionally explicit and must be confirmed by the user. */
    internal fun resumeAfterExplicitUserConfirmation(confirmed: Boolean) =
        emergencyStop.resetAfterExplicitUserConfirmation(confirmed)

    internal fun emergencyStopController() = emergencyStop

    private fun deny(required: AuthorizationLevel, reason: String) =
        PolicyDecision(false, false, required, reason)

    private fun authorizationRank(level: AuthorizationLevel): Int = when (level) {
        AuthorizationLevel.NONE -> 0
        AuthorizationLevel.USER_CONFIRMATION -> 1
        AuthorizationLevel.DEVICE_AUTHENTICATION -> 2
    }
}
