package ai.ritav.app.core.security

class PolicyEngine(private val emergencyStop: EmergencyStopController = EmergencyStopController()) {
    fun evaluate(request: ActionRequest): PolicyDecision {
        if (emergencyStop.isActive()) return deny("Emergency Stop is active")
        if (request.appId.isBlank() || request.action.isBlank()) return deny("App and action are required")
        if (request.riskTier == RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED) {
            return deny("Sensitive or prohibited action")
        }
        if (request.containsSensitiveData) {
            return deny("Sensitive data cannot enter action reasoning")
        }
        if (!request.permissionGranted) {
            return deny("Required permission is not granted")
        }

        val requiredAuth = when (request.riskTier) {
            RiskTier.TIER_0_INFORMATIONAL -> AuthorizationLevel.NONE
            RiskTier.TIER_1_REVERSIBLE -> AuthorizationLevel.NONE
            RiskTier.TIER_2_CONTENT_MUTATION -> AuthorizationLevel.USER_CONFIRMATION
            RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE -> AuthorizationLevel.DEVICE_AUTHENTICATION
            RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED -> AuthorizationLevel.DEVICE_AUTHENTICATION
        }

        if (request.riskTier >= RiskTier.TIER_2_CONTENT_MUTATION && !request.userExplicitlyRequested) {
            return deny("Explicit user intent is required")
        }
        if (request.authorizationLevel.ordinal < requiredAuth.ordinal) {
            return PolicyDecision(
                allowed = false,
                requiresConfirmation = true,
                requiredAuthorization = requiredAuth,
                reason = "Risk-appropriate authorization is required"
            )
        }

        return PolicyDecision(
            allowed = true,
            requiresConfirmation = false,
            requiredAuthorization = requiredAuth,
            reason = "Allowed by deterministic policy"
        )
    }

    private fun deny(reason: String) = PolicyDecision(false, false, AuthorizationLevel.NONE, reason)

    fun stop() = emergencyStop.activate()
    fun resume() = emergencyStop.reset()
    fun emergencyStopController() = emergencyStop
}
