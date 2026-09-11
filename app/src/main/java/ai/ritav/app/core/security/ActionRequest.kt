package ai.ritav.app.core.security

data class ActionRequest(
    val appId: String,
    val action: String,
    val riskTier: RiskTier,
    val containsSensitiveData: Boolean = false,
    val userExplicitlyRequested: Boolean = false,
    val permissionGranted: Boolean = false
)

data class PolicyDecision(
    val allowed: Boolean,
    val requiresConfirmation: Boolean,
    val reason: String
)
