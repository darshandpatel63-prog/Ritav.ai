package ai.ritav.app.platform

import ai.ritav.app.core.security.ActionPlan
import ai.ritav.app.core.security.AppCapabilityRegistry
import ai.ritav.app.core.security.AppCapabilitySpec
import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.ExecutionResult
import ai.ritav.app.core.security.RiskTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class AndroidIntentActionAdapterTest {
    private class RecordingDispatcher(
        private val result: Boolean
    ) : AndroidAppLaunchDispatcher {
        var calls = 0
        var lastPackage: String? = null

        override fun dispatchLaunch(packageName: String): Boolean {
            calls++
            lastPackage = packageName
            return result
        }
    }

    private class FakeCertificateReader(
        private val certificates: List<ByteArray>?
    ) : AndroidPackageSigningCertificateReader {
        override fun read(packageName: String): List<ByteArray>? = certificates
    }

    private val certificateBytes = byteArrayOf(1, 2, 3, 4, 5)
    private val certificateSha256 = sha256(certificateBytes)

    private fun trustedPlan(
        expectedState: String = "LAUNCH_DISPATCHED"
    ) = ActionPlan(
        appId = "com.example.safe",
        capability = Capability.APP_LAUNCH,
        action = "open",
        riskTier = RiskTier.TIER_1_REVERSIBLE,
        expectedState = expectedState
    )

    private fun trustedRegistry(pin: String? = certificateSha256) =
        AppCapabilityRegistry(
            listOf(
                AppCapabilitySpec(
                    packageName = "com.example.safe",
                    capability = Capability.APP_LAUNCH,
                    actions = setOf("open"),
                    riskTier = RiskTier.TIER_1_REVERSIBLE,
                    trustedCertificateSha256 = pin
                )
            )
        )

    private fun verifier(
        registry: AppCapabilityRegistry,
        certificates: List<ByteArray>?
    ) = AndroidPackageIdentityVerifier(
        registry = registry,
        certificateReader = FakeCertificateReader(certificates)
    )

    @Test fun unsupportedActionNeverReachesDispatcher() {
        val dispatcher = RecordingDispatcher(true)
        val adapter = AndroidIntentActionAdapter(dispatcher) { true }
        val result = adapter.execute(trustedPlan().copy(action = "close"))

        assertFalse(result.success)
        assertEquals(0, dispatcher.calls)
    }

    @Test fun untrustedPackageNeverReachesDispatcher() {
        val dispatcher = RecordingDispatcher(true)
        val adapter = AndroidIntentActionAdapter(dispatcher) { false }
        val result = adapter.execute(trustedPlan())

        assertFalse(result.success)
        assertEquals(0, dispatcher.calls)
    }

    @Test fun malformedPlanNeverReachesDispatcher() {
        val dispatcher = RecordingDispatcher(true)
        val adapter = AndroidIntentActionAdapter(dispatcher) { true }
        val result = adapter.execute(trustedPlan().copy(appId = ""))

        assertFalse(result.success)
        assertEquals(0, dispatcher.calls)
    }

    @Test fun unexpectedExpectedStateNeverReachesDispatcher() {
        val dispatcher = RecordingDispatcher(true)
        val adapter = AndroidIntentActionAdapter(dispatcher) { true }
        val result = adapter.execute(trustedPlan(expectedState = "OPENED"))

        assertFalse(result.success)
        assertEquals(0, dispatcher.calls)
    }

    @Test fun successfulDispatchReturnsOnlyDispatchObservation() {
        val dispatcher = RecordingDispatcher(true)
        val adapter = AndroidIntentActionAdapter(dispatcher) { true }
        val result = adapter.execute(trustedPlan())

        assertTrue(result.success)
        assertFalse(result.verified)
        assertEquals("LAUNCH_DISPATCHED", result.observedState)
        assertEquals("com.example.safe", dispatcher.lastPackage)
        assertEquals(1, dispatcher.calls)
    }

    @Test fun failedDispatchDoesNotReportSuccess() {
        val dispatcher = RecordingDispatcher(false)
        val adapter = AndroidIntentActionAdapter(dispatcher) { true }
        val result = adapter.execute(trustedPlan())

        assertFalse(result.success)
        assertFalse(result.verified)
        assertEquals(1, dispatcher.calls)
    }

    @Test fun packageIdentityRequiresExactPinnedCertificate() {
        val registry = trustedRegistry()
        val result = verifier(registry, listOf(certificateBytes)).isTrusted("com.example.safe")

        assertTrue(result)
    }

    @Test fun packageIdentityRejectsWrongCertificate() {
        val registry = trustedRegistry()
        val result = verifier(
            registry,
            listOf(byteArrayOf(9, 8, 7, 6, 5))
        ).isTrusted("com.example.safe")

        assertFalse(result)
    }

    @Test fun packageIdentityRejectsMultipleSigners() {
        val registry = trustedRegistry()
        val result = verifier(
            registry,
            listOf(certificateBytes, byteArrayOf(6, 7, 8))
        ).isTrusted("com.example.safe")

        assertFalse(result)
    }

    @Test fun packageIdentityRejectsMissingCertificate() {
        val registry = trustedRegistry()
        val result = verifier(registry, null).isTrusted("com.example.safe")

        assertFalse(result)
    }

    @Test fun packageIdentityRejectsCertificateReaderFailure() {
        val registry = trustedRegistry()
        val failingReader = object : AndroidPackageSigningCertificateReader {
            override fun read(packageName: String): List<ByteArray>? {
                throw SecurityException("certificate access failed")
            }
        }

        val result = AndroidPackageIdentityVerifier(
            registry = registry,
            certificateReader = failingReader
        ).isTrusted("com.example.safe")

        assertFalse(result)
    }

    @Test fun packageIdentityRejectsUnregisteredPackageEvenWithMatchingCertificate() {
        val registry = trustedRegistry()
        val result = verifier(registry, listOf(certificateBytes)).isTrusted("com.example.unknown")

        assertFalse(result)
        assertNull(registry.trustedCertificateSha256("com.example.unknown"))
    }

    @Test fun productionAdapterRejectsMissingCertificatePinBeforeDispatch() {
        val registry = trustedRegistry(pin = null)
        val dispatcher = RecordingDispatcher(true)
        val adapter = AndroidIntentActionAdapter(
            dispatcher = dispatcher,
            isTrustedPackage = AndroidPackageIdentityVerifier(
                registry = registry,
                certificateReader = FakeCertificateReader(listOf(certificateBytes))
            )::isTrusted
        )

        val result = adapter.execute(trustedPlan())

        assertFalse(result.success)
        assertEquals(0, dispatcher.calls)
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte ->
                (byte.toInt() and 0xff).toString(16).padStart(2, '0')
            }
}
