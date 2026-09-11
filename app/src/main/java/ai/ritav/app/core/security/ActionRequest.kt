package ai.ritav.app.core.security

data class ActionRequest(
    val appId: String,
    val action: String,
    val riskTier: RiskTier,
    val capability: Capability = Capability.READ_ALLOWED_CONTENT,
    val sessionId: String? = null,
    val containsSensitiveData: Boolean = false,
    val userExplicitlyRequested: Boolean = false,
    val authorizationLevel: AuthorizationLevel = AuthorizationLevel.NONE
)

data class PolicyDecision(
    val allowed: Boolean,
    val requiresConfirmation: Boolean,
    val requiredAuthorization: AuthorizationLevel,
    val reason: String
)
