package ai.ritav.app.core.security

/** Evidence supplied by the Android/app adapter after an action attempt. */
data class ActionResultEvidence(
    val success: Boolean,
    val observedState: String? = null,
    val errorCode: String? = null
)

data class VerificationResult(
    val verified: Boolean,
    val reason: String
)

/**
 * Ritav must not claim an action succeeded merely because execution returned.
 * Verification is deliberately deterministic and does not trust AI-generated claims.
 */
class ResultVerifier {
    private companion object { const val MAX_STATE_LENGTH = 256 }
    fun verify(
        expectedSuccess: Boolean,
        evidence: ActionResultEvidence,
        expectedState: String
    ): VerificationResult = when {
        !expectedSuccess -> VerificationResult(false, "Expected outcome was not success")
        expectedState.isBlank() || expectedState.length > MAX_STATE_LENGTH -> VerificationResult(false, "Expected result state is invalid")
        !evidence.success -> VerificationResult(false, evidence.errorCode ?: "Action execution failed")
        evidence.observedState == null -> VerificationResult(false, "Observed result state is missing")
        evidence.observedState.length > MAX_STATE_LENGTH -> VerificationResult(false, "Observed result state is too large")
        evidence.observedState != expectedState -> VerificationResult(false, "Observed result state does not match expected state")
        else -> VerificationResult(true, "Observed result state matches expected state")
    }
}
