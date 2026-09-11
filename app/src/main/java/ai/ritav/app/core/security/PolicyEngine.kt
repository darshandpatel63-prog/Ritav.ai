package ai.ritav.app.core.security

class PolicyEngine(private val emergencyStop: EmergencyStopController = EmergencyStopController()) {
    fun evaluate(request: ActionRequest): PolicyDecision {
        if (emergencyStop.isActive()) return PolicyDecision(false, false, "Emergency Stop is active")
        if (request.riskTier == RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED) {
            return PolicyDecision(false, false, "Sensitive or prohibited action")
        }
        if (request.containsSensitiveData) {
            return PolicyDecision(false, false, "Sensitive data cannot enter action reasoning")
        }
        if (!request.permissionGranted) {
            return PolicyDecision(false, false, "Required permission is not granted")
        }
        if (!request.userExplicitlyRequested && request.riskTier >= RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE) {
            return PolicyDecision(false, false, "Explicit user intent is required")
        }
        val confirmation = request.riskTier >= RiskTier.TIER_2_CONTENT_MUTATION
        return PolicyDecision(true, confirmation, if (confirmation) "Allowed after confirmation" else "Allowed")
    }

    fun stop() = emergencyStop.activate()
    fun resume() = emergencyStop.reset()
    fun emergencyStopController() = emergencyStop
}
