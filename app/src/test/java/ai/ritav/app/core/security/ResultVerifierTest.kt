package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultVerifierTest {
    private val verifier = ResultVerifier()

    @Test fun successfulEvidenceIsVerified() {
        val result = verifier.verify(true, ActionResultEvidence(success = true, observedState = "OPENED"), "OPENED")
        assertTrue(result.verified)
    }

    @Test fun failedEvidenceCannotBeReportedAsVerified() {
        val result = verifier.verify(true, ActionResultEvidence(success = false, errorCode = "FAILED"), "OPENED")
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

    @Test fun blankExpectedStateCannotBeVerified() {
        val result = verifier.verify(true, ActionResultEvidence(success = true, observedState = "OPENED"), " ")
        assertFalse(result.verified)
    }
