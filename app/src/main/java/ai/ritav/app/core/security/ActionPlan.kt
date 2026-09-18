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
    /** Deterministic structural validation for every security boundary that accepts a plan. */
    fun isValid(): Boolean =
        appId.isNotBlank() && appId.length <= MAX_APP_ID_LENGTH &&
            action.isNotBlank() && action.length <= MAX_ACTION_LENGTH &&
            expectedState.isNotBlank() && expectedState.length <= MAX_EXPECTED_STATE_LENGTH &&
            (sessionId == null || (sessionId.isNotBlank() && sessionId.length <= MAX_SESSION_ID_LENGTH))

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

private const val MAX_APP_ID_LENGTH = 256
private const val MAX_ACTION_LENGTH = 4096
private const val MAX_EXPECTED_STATE_LENGTH = 256
private const val MAX_SESSION_ID_LENGTH = 256

/** Authorization request bound to the exact action plan, not merely an app/session. */
data class AuthorizationRequest(
    val plan: ActionPlan,
    val requiredLevel: AuthorizationLevel,
    val userExplicitlyRequested: Boolean,
    val confirmedPlanHash: String? = null
)
