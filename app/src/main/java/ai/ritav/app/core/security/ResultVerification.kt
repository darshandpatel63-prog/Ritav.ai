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
    fun verify(
        expectedSuccess: Boolean,
        evidence: ActionResultEvidence
    ): VerificationResult = when {
        !expectedSuccess -> VerificationResult(false, "Expected outcome was not success")
        evidence.success -> VerificationResult(true, "Adapter evidence confirms success")
        else -> VerificationResult(false, evidence.errorCode ?: "Success was not verified")
    }
}
