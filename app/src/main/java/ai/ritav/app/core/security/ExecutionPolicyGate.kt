package ai.ritav.app.core.security

/**
 * Final policy check immediately before an Android action is executed.
 *
 * The execution layer must never trust an agent/model's earlier decision.
 */
class ExecutionPolicyGate(
    private val policyEngine: PolicyEngine
) {
    fun authorize(request: ActionRequest): PolicyDecision {
        val decision = policyEngine.evaluate(request)
        if (!decision.allowed) return decision

        // Consequential actions must carry explicit intent and the authorization
        // level already required by the deterministic policy engine.
        if (request.riskTier >= RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE &&
            !request.userExplicitlyRequested
        ) {
            return PolicyDecision(
                allowed = false,
                requiresConfirmation = false,
                requiredAuthorization = AuthorizationLevel.DEVICE_AUTHENTICATION,
                reason = "Execution requires explicit user intent"
            )
        }
        return decision
    }
}
