package ai.ritav.app.core.security

/**
 * Security-only orchestration boundary for consequential actions.
 * AI proposes; deterministic policy, authorization and verification decide.
 */
data class SecurityExecutionRequest(
    val action: ActionRequest,
    val plan: ActionPlan,
    val authorizationToken: String? = null,
    val identitySession: SecuritySession? = null,
    val nowEpochMillis: Long
)

data class SecurityExecutionDecision(
    val allowed: Boolean,
    val reason: String,
    val authorizationRequired: AuthorizationLevel
)

class SecurityExecutionPipeline(
    private val policyEngine: PolicyEngine,
    private val executionPolicyGate: ExecutionPolicyGate,
    private val authorizationGate: ActionAuthorizationGate,
    private val identitySessionManager: IdentitySessionManager = IdentitySessionManager()
) {
    fun authorize(request: SecurityExecutionRequest): SecurityExecutionDecision {
        if (request.plan.appId != request.action.appId ||
            request.plan.capability != request.action.capability ||
            request.plan.action != request.action.action ||
            request.plan.riskTier != request.action.riskTier ||
            request.plan.sessionId != request.action.sessionId
        ) {
            return deny("Action plan does not match execution request", AuthorizationLevel.NONE)
        }

        if (request.action.riskTier >= RiskTier.TIER_2_CONTENT_MUTATION &&
            !identitySessionManager.permitsProtectedCapability(request.identitySession, request.nowEpochMillis)
        ) {
            return deny("Protected action requires an active trusted identity session", AuthorizationLevel.USER_CONFIRMATION)
        }

        val decision = executionPolicyGate.authorize(request.action)
        if (!decision.allowed) {
            return deny(decision.reason, decision.requiredAuthorization)
        }

        val required = decision.requiredAuthorization
        if (required != AuthorizationLevel.NONE) {
            val token = request.authorizationToken
                ?: return deny("One-time authorization token is required", required)
            val providedLevel = request.action.authorizationLevel
            if (!authorizationGate.consume(token, request.plan, providedLevel, request.nowEpochMillis)) {
                return deny("Authorization token is invalid, expired, mismatched, or already consumed", required)
            }
        }

        return SecurityExecutionDecision(true, "Execution authorized by security pipeline", required)
    }

    private fun deny(reason: String, required: AuthorizationLevel) =
        SecurityExecutionDecision(false, reason, required)
}
