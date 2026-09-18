package ai.ritav.app.core.security

/**
 * Deterministic lifecycle boundary for changing app capability grants.
 *
 * Granting a capability is itself a consequential security operation. It must
 * be bound to the exact target package/capability/action and an authorization
 * token; callers cannot directly mutate the durable permission store.
 */
class CapabilityGrantService(
    private val registry: AppCapabilityRegistry,
    private val permissionStore: MutablePermissionStore,
    private val authorizationGate: ActionAuthorizationGate
) {
    fun createGrantPlan(
        packageName: String,
        capability: Capability,
        action: String,
        sessionId: String? = null
    ): ActionPlan? {
        val targetRisk = registry.riskTierFor(packageName, capability, action) ?: return null
        if (targetRisk == RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED) return null

        val authorizationRisk = when (targetRisk) {
            RiskTier.TIER_0_INFORMATIONAL,
            RiskTier.TIER_1_REVERSIBLE,
            RiskTier.TIER_2_CONTENT_MUTATION -> RiskTier.TIER_2_CONTENT_MUTATION
            RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE -> RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE
            RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED -> return null
        }

        val grantAction = "grant:$action"
        if (grantAction.length > MAX_PLAN_ACTION_LENGTH) return null

        return ActionPlan(
            appId = packageName,
            capability = capability,
            action = grantAction,
            riskTier = authorizationRisk,
            expectedState = GRANT_EXPECTED_STATE,
            sessionId = sessionId
        ).takeIf { it.isValid() }
    }

    fun grant(
        plan: ActionPlan,
        authorizationToken: String?,
        nowEpochMillis: Long
    ): Boolean {
        if (!plan.isValid() || plan.expectedState != GRANT_EXPECTED_STATE) return false
        if (plan.action.length <= GRANT_ACTION_PREFIX.length ||
            !plan.action.startsWith(GRANT_ACTION_PREFIX)
        ) return false

        val targetAction = plan.action.removePrefix(GRANT_ACTION_PREFIX)
        if (targetAction.isBlank() || targetAction.length > MAX_TARGET_ACTION_LENGTH) return false
        if (plan.riskTier == RiskTier.TIER_4_SENSITIVE_OR_PROHIBITED) return false

        val targetRisk = registry.riskTierFor(plan.appId, plan.capability, targetAction) ?: return false
        if (!registry.allows(plan.appId, plan.capability, targetAction, targetRisk)) return false
        if (!authorizationGate.consume(plan, authorizationToken, nowEpochMillis)) return false

        return runCatching {
            permissionStore.grant(
                CapabilityGrant(
                    appId = plan.appId,
                    capability = plan.capability,
                    action = targetAction,
                    sessionId = plan.sessionId,
                    enabled = true
                )
            )
            true
        }.getOrDefault(false)
    }

    fun revoke(grant: CapabilityGrant): Boolean =
        runCatching {
            if (grant.capability == Capability.FINANCIAL_ACTION) return false
            if (grant.appId.isBlank() || grant.action.isBlank()) return false
            permissionStore.revoke(grant)
            true
        }.getOrDefault(false)

    private companion object {
        const val GRANT_EXPECTED_STATE = "CAPABILITY_GRANT"
        const val GRANT_ACTION_PREFIX = "grant:"
        const val MAX_TARGET_ACTION_LENGTH = 128
        const val MAX_PLAN_ACTION_LENGTH = 4096
    }
}
