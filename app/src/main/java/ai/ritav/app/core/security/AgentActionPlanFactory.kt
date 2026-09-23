package ai.ritav.app.core.security

import ai.ritav.app.core.orchestrator.AgentProposal

/**
 * Converts an untrusted agent proposal into an exact ActionPlan only when the
 * deterministic registry independently agrees with the requested capability,
 * action and risk metadata.
 *
 * The proposal cannot grant authorization, choose a safer risk tier, or define
 * its own verification semantics. User intent, authorization, identity/session
 * state and Emergency Stop remain downstream security concerns.
 */
class AgentActionPlanFactory(
    private val registry: AppCapabilityRegistry
) {
    fun create(
        proposal: AgentProposal,
        appId: String,
        sessionId: String? = null
    ): ActionPlan? {
        if (!isValidProposal(proposal)) return null
        if (!isValidAppId(appId)) return null
        if (proposal.capability == Capability.FINANCIAL_ACTION) return null

        val registeredRisk = registry.riskTierFor(
            packageName = appId,
            capability = proposal.capability,
            action = proposal.proposedAction
        ) ?: return null

        // The AI-provided risk is advisory only. Requiring it to agree with
        // independently registered metadata prevents ambiguity while never
        // trusting it as the source of authorization policy.
        if (proposal.riskTier != registeredRisk) return null
        if (!registry.allows(appId, proposal.capability, proposal.proposedAction, registeredRisk)) {
            return null
        }

        val expectedState = ExpectedActionStateRegistry.expectedStateFor(
            capability = proposal.capability,
            action = proposal.proposedAction
        ) ?: return null

        return ActionPlan(
            appId = appId,
            capability = proposal.capability,
            action = proposal.proposedAction,
            riskTier = registeredRisk,
            expectedState = expectedState,
            sessionId = sessionId
        ).takeIf(ActionPlan::isValid)
    }

    private fun isValidProposal(proposal: AgentProposal): Boolean =
        proposal.taskId.isNotBlank() &&
            proposal.taskId.length <= MAX_ID_LENGTH &&
            proposal.agentId.isNotBlank() &&
            proposal.agentId.length <= MAX_ID_LENGTH &&
            proposal.proposedAction.isNotBlank() &&
            proposal.proposedAction.length <= MAX_ACTION_LENGTH &&
            proposal.rationale.length <= MAX_TEXT_LENGTH

    private fun isValidAppId(appId: String): Boolean =
        appId.isNotBlank() &&
            appId.length <= MAX_ID_LENGTH &&
            appId.matches(PACKAGE_NAME_REGEX)

    private companion object {
        const val MAX_ID_LENGTH = 256
        const val MAX_ACTION_LENGTH = 4096
        const val MAX_TEXT_LENGTH = 4096
        val PACKAGE_NAME_REGEX = Regex("""^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$""")
    }
}

/**
 * Security-owned verification state mapping. It is deliberately smaller than
 * the capability registry and currently covers only the executable Android
 * action that has a reviewed adapter.
 */
internal object ExpectedActionStateRegistry {
    fun expectedStateFor(capability: Capability, action: String): String? =
        when {
            capability == Capability.APP_LAUNCH && action == OPEN_ACTION -> LAUNCH_DISPATCHED_STATE
            else -> null
        }

    const val OPEN_ACTION = "open"
    const val LAUNCH_DISPATCHED_STATE = "LAUNCH_DISPATCHED"
}
