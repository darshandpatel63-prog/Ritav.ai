package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultVerificationTest {
    private val verifier = ResultVerifier()

    @Test fun successfulAdapterEvidenceIsVerified() {
        val result = verifier.verify(true, ActionResultEvidence(success = true, observedState = "OPENED"), "OPENED")
        assertTrue(result.verified)
    }

    @Test fun failedAdapterEvidenceCannotBeReportedAsSuccess() {
        val result = verifier.verify(true, ActionResultEvidence(success = false, errorCode = "NOT_CONFIRMED"), "OPENED")
        assertFalse(result.verified)
    }

    @Test fun unexpectedOutcomeIsNotVerified() {
        val result = verifier.verify(false, ActionResultEvidence(success = true))
        assertFalse(result.verified)
    }
}


    @Test fun missingObservedStateCannotBeVerified() {
        val result = verifier.verify(true, ActionResultEvidence(success = true), "OPENED")
        assertFalse(result.verified)
    }

    @Test fun mismatchedObservedStateCannotBeVerified() {
        val result = verifier.verify(true, ActionResultEvidence(success = true, observedState = "CLOSED"), "OPENED")
        assertFalse(result.verified)
    }
