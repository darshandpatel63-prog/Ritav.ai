package ai.ritav.app.core.orchestrator

import ai.ritav.app.core.security.ActionPlan
import ai.ritav.app.core.security.AppCapabilityRegistry
import ai.ritav.app.core.security.Capability
import java.util.regex.Pattern

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

        // AI-supplied risk is advisory only. Requiring agreement with
        // independent registry metadata prevents ambiguity without making the
        // model the source of authorization policy.
        if (proposal.riskTier != registeredRisk) return null
        if (!registry.allows(appId, proposal.capability, proposal.proposedAction, registeredRisk)) return null

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
        ).takeIf { it.isValid() }
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
            PACKAGE_NAME_PATTERN.matcher(appId).matches()

    private companion object {
        const val MAX_ID_LENGTH = 256
        const val MAX_ACTION_LENGTH = 4096
        const val MAX_TEXT_LENGTH = 4096
        val PACKAGE_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")
    }
}

/**
 * Security-owned verification-state mapping. It is deliberately smaller than
 * the capability registry and currently covers only the reviewed executable
 * Android action.
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
