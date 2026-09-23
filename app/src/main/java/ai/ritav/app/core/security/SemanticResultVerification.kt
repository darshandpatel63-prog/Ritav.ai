package ai.ritav.app.core.security

/**
 * Deterministic evidence emitted by an approved platform adapter.
 *
 * Evidence is data, not authority. The semantic verifier independently checks
 * that the evidence type, subject, state and timing match the exact ActionPlan.
 */
data class VerificationEvidence(
    val evidenceType: String,
    val subject: String,
    val observedAtMillis: Long
) {
    fun isValid(): Boolean =
        evidenceType.isNotBlank() && evidenceType.length <= MAX_EVIDENCE_TYPE_LENGTH &&
            subject.isNotBlank() && subject.length <= MAX_SUBJECT_LENGTH &&
            observedAtMillis >= 0L

    private companion object {
        const val MAX_EVIDENCE_TYPE_LENGTH = 128
        const val MAX_SUBJECT_LENGTH = 256
    }
}

/**
 * Security-owned mapping from executable actions to the evidence class required
 * to establish successful completion.
 *
 * An action with no registered semantic evidence contract cannot be reported as
 * successfully completed.
 */
internal object ExpectedActionStateRegistry {
    const val OPEN_ACTION = "open"
    const val LAUNCH_DISPATCHED_STATE = "LAUNCH_DISPATCHED"

    fun expectedStateFor(capability: Capability, action: String): String? =
        when {
            capability == Capability.APP_LAUNCH && action == OPEN_ACTION -> LAUNCH_DISPATCHED_STATE
            else -> null
        }
}

internal object SemanticVerificationContract {
    const val TARGET_APP_FOREGROUND = "TARGET_APP_FOREGROUND"

    fun requiredEvidenceType(plan: ActionPlan): String? =
        when {
            ExpectedActionStateRegistry.expectedStateFor(plan.capability, plan.action) != null ->
                TARGET_APP_FOREGROUND
            else -> null
        }
}

/**
 * Final deterministic semantic-completion verifier.
 *
 * It combines the existing exact-state verifier with independently typed,
 * target-bound and time-bounded evidence. The adapter's own Boolean "verified"
 * flag is deliberately not treated as authorization or proof.
 */
class SemanticResultVerifier(
    private val resultVerifier: ResultVerifier = ResultVerifier()
) {
    fun verify(
        plan: ActionPlan,
        executionStartedAtMillis: Long,
        verificationCheckedAtMillis: Long,
        executionSucceeded: Boolean,
        observedState: String?,
        evidence: VerificationEvidence?
    ): VerificationResult {
        if (!plan.isValid()) {
            return VerificationResult(false, "Action plan is malformed")
        }
        if (executionStartedAtMillis < 0L || verificationCheckedAtMillis < 0L) {
            return VerificationResult(false, "Verification clock is invalid")
        }
        if (verificationCheckedAtMillis < executionStartedAtMillis) {
            return VerificationResult(false, "Verification clock moved backwards")
        }

        val expectedContractState = ExpectedActionStateRegistry.expectedStateFor(plan.capability, plan.action)
            ?: return VerificationResult(false, "No semantic verification contract exists for this action")
        if (plan.expectedState != expectedContractState) {
            return VerificationResult(false, "Action plan expected state does not match the security verification contract")
        }

        val requiredEvidenceType = SemanticVerificationContract.requiredEvidenceType(plan)
            ?: return VerificationResult(false, "No semantic verification contract exists for this action")

        val verifiedEvidence = evidence
            ?: return VerificationResult(false, "Required semantic evidence is missing")

        if (!verifiedEvidence.isValid()) {
            return VerificationResult(false, "Semantic evidence is malformed")
        }
        if (verifiedEvidence.evidenceType != requiredEvidenceType) {
            return VerificationResult(false, "Semantic evidence type does not match the action")
        }
        if (verifiedEvidence.subject != plan.appId) {
            return VerificationResult(false, "Semantic evidence target does not match the action target")
        }
        if (verifiedEvidence.observedAtMillis < executionStartedAtMillis) {
            return VerificationResult(false, "Semantic evidence predates action execution start")
        }
        if (verifiedEvidence.observedAtMillis > verificationCheckedAtMillis) {
            return VerificationResult(false, "Semantic evidence is from the future")
        }
        if (verifiedEvidence.observedAtMillis - executionStartedAtMillis > MAX_EVIDENCE_WINDOW_MILLIS) {
            return VerificationResult(false, "Semantic evidence falls outside the bounded verification window")
        }

        return resultVerifier.verify(
            expectedSuccess = executionSucceeded,
            evidence = ActionResultEvidence(
                success = executionSucceeded,
                observedState = observedState
            ),
            expectedState = plan.expectedState
        )
    }

    private companion object {
        const val MAX_EVIDENCE_WINDOW_MILLIS = 5_000L
    }
}
