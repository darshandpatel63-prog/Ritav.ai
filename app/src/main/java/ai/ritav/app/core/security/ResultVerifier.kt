package ai.ritav.app.core.security

/** Evidence returned by an approved adapter after an action attempt. */
data class ActionResultEvidence(
    val success: Boolean,
    val errorCode: String? = null
)

data class VerificationResult(
    val verified: Boolean,
    val reason: String
)

/**
 * Deterministic result boundary. Ritav reports success only when the adapter
 * provides positive evidence that the requested operation succeeded.
 */
class ResultVerifier {
    fun verify(expectedSuccess: Boolean, evidence: ActionResultEvidence): VerificationResult {
        if (!expectedSuccess) return VerificationResult(true, "No successful outcome was required")
        if (!evidence.success) {
            return VerificationResult(false, evidence.errorCode ?: "Action did not succeed")
        }
        return VerificationResult(true, "Action success verified by adapter evidence")
    }
}
