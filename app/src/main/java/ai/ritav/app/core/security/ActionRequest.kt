package ai.ritav.app.core.security

data class ActionRequest(
    val appId: String,
    val action: String,
    val riskTier: RiskTier,
    val containsSensitiveData: Boolean = false,
    val userExplicitlyRequested: Boolean = false,
    val permissionGranted: Boolean = false,
    val authorizationLevel: AuthorizationLevel = AuthorizationLevel.NONE
)

data class PolicyDecision(
    val allowed: Boolean,
    val requiresConfirmation: Boolean,
    val requiredAuthorization: AuthorizationLevel,
    val reason: String
)
