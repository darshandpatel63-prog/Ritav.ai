package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticResultVerificationTest {
    private val verifier = SemanticResultVerifier()
    private val plan = ActionPlan(
        appId = "com.example.safe",
        capability = Capability.APP_LAUNCH,
        action = ExpectedActionStateRegistry.OPEN_ACTION,
        riskTier = RiskTier.TIER_1_REVERSIBLE,
        expectedState = ExpectedActionStateRegistry.LAUNCH_DISPATCHED_STATE
    )

    private fun evidence(
        type: String = SemanticVerificationContract.TARGET_APP_FOREGROUND,
        subject: String = plan.appId,
        observedAtMillis: Long = 1_500L
    ) = VerificationEvidence(type, subject, observedAtMillis)

    private fun verify(
        plan: ActionPlan = this.plan,
        executionStartedAtMillis: Long = 1_000L,
        verificationCheckedAtMillis: Long = 2_000L,
        executionSucceeded: Boolean = true,
        observedState: String? = plan.expectedState,
        evidence: VerificationEvidence? = evidence()
    ) = verifier.verify(
        plan = plan,
        executionStartedAtMillis = executionStartedAtMillis,
        verificationCheckedAtMillis = verificationCheckedAtMillis,
        executionSucceeded = executionSucceeded,
        observedState = observedState,
        evidence = evidence
    )

    @Test fun exactRegisteredEvidenceAndStateAreAccepted() {
        val result = verify()
        assertTrue(result.verified)
    }

    @Test fun missingEvidenceFailsClosed() {
        val result = verify(evidence = null)
        assertFalse(result.verified)
        assertEquals("Required semantic evidence is missing", result.reason)
    }

    @Test fun wrongEvidenceTypeFailsClosed() {
        val result = verify(evidence = evidence(type = "SCREEN_TEXT"))
        assertFalse(result.verified)
    }

    @Test fun wrongEvidenceSubjectFailsClosed() {
        val result = verify(evidence = evidence(subject = "com.example.other"))
        assertFalse(result.verified)
    }

    @Test fun futureEvidenceFailsClosed() {
        val result = verify(
            verificationCheckedAtMillis = 1_400L,
            evidence = evidence(observedAtMillis = 1_500L)
        )
        assertFalse(result.verified)
    }

    @Test fun evidenceOutsideBoundedWindowFailsClosed() {
        val result = verify(evidence = evidence(observedAtMillis = 7_000L))
        assertFalse(result.verified)
    }

    @Test fun mismatchedExpectedStateFailsClosed() {
        val result = verify(
            plan = plan.copy(expectedState = "OPENED"),
            observedState = "OPENED"
        )
        assertFalse(result.verified)
    }

    @Test fun unsupportedActionFailsClosed() {
        val unsupported = plan.copy(action = "close", expectedState = "CLOSED")
        val result = verify(plan = unsupported, observedState = "CLOSED")
        assertFalse(result.verified)
    }

    @Test fun executionFailureCannotVerifyCompletion() {
        val result = verify(executionSucceeded = false)
        assertFalse(result.verified)
    }

    @Test fun malformedEvidenceFailsClosed() {
        val result = verify(
            evidence = VerificationEvidence("", plan.appId, 1_500L)
        )
        assertFalse(result.verified)
    }

    @Test fun verificationClockRegressionFailsClosed() {
        val result = verify(
            executionStartedAtMillis = 2_000L,
            verificationCheckedAtMillis = 1_000L,
            evidence = evidence(observedAtMillis = 2_000L)
        )
        assertFalse(result.verified)
    }
}
