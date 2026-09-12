package ai.ritav.app.core.security

/**
 * Final capability boundary between an action request and an app adapter.
 * Registry metadata is treated as security policy, not as advisory information.
 */
class CapabilityPolicyGate(
    private val registry: AppCapabilityRegistry
) {
    fun evaluate(request: ActionRequest): PolicyDecision {
        if (!registry.isRegistered(request.appId)) {
            return deny("Target app is not registered for Ritav automation")
        }
        if (registry.isFinancial(request.appId) || request.capability == Capability.FINANCIAL_ACTION) {
            return deny("Financial capabilities are blocked by default")
        }
        if (!registry.allows(request.appId, request.capability, request.action, request.riskTier)) {
            return deny("Requested capability or risk level is not registered")
        }
        return PolicyDecision(
            allowed = true,
            requiresConfirmation = request.riskTier >= RiskTier.TIER_2_CONTENT_MUTATION,
            requiredAuthorization = when (request.riskTier) {
                RiskTier.TIER_0_INFORMATIONAL, RiskTier.TIER_1_REVERSIBLE -> AuthorizationLevel.NONE
                RiskTier.TIER_2_CONTENT_MUTATION -> AuthorizationLevel.USER_CONFIRMATION
                RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE -> AuthorizationLevel.DEVICE_AUTHENTICATION
                RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED -> AuthorizationLevel.NONE
            },
            reason = "Capability is registered for this app and risk tier"
        )
    }

    private fun deny(reason: String) =
        PolicyDecision(false, false, AuthorizationLevel.NONE, reason)
}
