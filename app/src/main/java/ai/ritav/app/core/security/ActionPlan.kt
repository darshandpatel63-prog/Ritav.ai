package ai.ritav.app.core.security

import java.security.MessageDigest

/** Immutable description of the exact action a caller is asking Ritav to execute. */
data class ActionPlan(
    val appId: String,
    val capability: Capability,
    val action: String,
    val riskTier: RiskTier,
    /** Deterministic post-action state that must be observed before success is reported. */
    val expectedState: String,
    val sessionId: String? = null
) {
    fun stableHash(): String {
        val canonical = listOf(
            appId,
            capability.name,
            action,
            riskTier.name,
            expectedState,
            sessionId ?: ""
        ).joinToString("\u001f")
        return MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}

/** Authorization request bound to the exact action plan, not merely an app/session. */
data class AuthorizationRequest(
    val plan: ActionPlan,
    val requiredLevel: AuthorizationLevel,
    val userExplicitlyRequested: Boolean,
    val confirmedPlanHash: String? = null
)
