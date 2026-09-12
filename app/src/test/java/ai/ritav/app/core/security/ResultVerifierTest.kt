package ai.ritav.app.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultVerifierTest {
    private val verifier = ResultVerifier()

    @Test fun successfulEvidenceIsVerified() {
        val result = verifier.verify(true, ActionResultEvidence(success = true))
        assertTrue(result.verified)
    }

    @Test fun failedEvidenceCannotBeReportedAsVerified() {
        val result = verifier.verify(true, ActionResultEvidence(success = false, errorCode = "FAILED"))
        assertFalse(result.verified)
    }
}
