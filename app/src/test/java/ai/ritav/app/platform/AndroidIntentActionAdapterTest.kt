package ai.ritav.app.platform

import ai.ritav.app.core.security.ActionPlan
import ai.ritav.app.core.security.AppCapabilityRegistry
import ai.ritav.app.core.security.AppCapabilitySpec
import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.RiskTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private class FakeTargetAppObserver(
        private val available: Boolean = true,
        private val observed: Boolean = false
    ) : AndroidTargetAppResultObserver {
        var canObserveCalls = 0
        var observeCalls = 0
        var lastPackage: String? = null
        var lastDispatchStartedAtMillis: Long? = null

        override fun canObserve(packageName: String): Boolean {
            canObserveCalls++
            lastPackage = packageName
            return available
        }

        override fun observeForegroundAfterDispatch(
            packageName: String,
            dispatchStartedAtMillis: Long
        ): Boolean {
            observeCalls++
            lastPackage = packageName
            lastDispatchStartedAtMillis = dispatchStartedAtMillis
            return observed
        }
    }

    private class FakeSemanticTaskObserver(
        private val available: Boolean = true,
        private val armed: Boolean = true,
        private val observed: Boolean = true
    ) : AndroidSemanticTaskObserver {
        var canObserveCalls = 0
        var armCalls = 0
        var observeCalls = 0
        var disarmCalls = 0
        var lastPackage: String? = null

        override fun canObserve(packageName: String): Boolean {
            canObserveCalls++
            lastPackage = packageName
            return available
        }

        override fun arm(packageName: String): Boolean {
            armCalls++
            lastPackage = packageName
            return armed
        }

        override fun observeCompletedAfterDispatch(
            packageName: String,
            dispatchCompletedAtElapsedMillis: Long
        ): Boolean {
            observeCalls++
            lastPackage = packageName
            return observed
        }

        override fun disarm() {
            disarmCalls++
        }
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

    private fun adapter(
        dispatcher: RecordingDispatcher,
        trusted: Boolean = true,
        observer: AndroidTargetAppResultObserver = FakeTargetAppObserver(),
        semanticObserver: AndroidSemanticTaskObserver = FakeSemanticTaskObserver(),
        clock: () -> Long = { 1_000L }
    ) = AndroidIntentActionAdapter(
        dispatcher = dispatcher,
        isTrustedPackage = { trusted },
        targetAppResultObserver = observer,
        semanticTaskObserver = semanticObserver,
        clock = clock
    )

    @Test fun unsupportedActionNeverReachesDispatcher() {
        val dispatcher = RecordingDispatcher(true)
        val result = adapter(dispatcher).execute(trustedPlan().copy(action = "close"))

        assertFalse(result.success)
        assertEquals(0, dispatcher.calls)
    }

    @Test fun untrustedPackageNeverReachesDispatcher() {
        val dispatcher = RecordingDispatcher(true)
        val result = adapter(dispatcher, trusted = false).execute(trustedPlan())

        assertFalse(result.success)
        assertEquals(0, dispatcher.calls)
    }

    @Test fun malformedPlanNeverReachesDispatcher() {
        val dispatcher = RecordingDispatcher(true)
        val result = adapter(dispatcher).execute(trustedPlan().copy(appId = ""))

        assertFalse(result.success)
        assertEquals(0, dispatcher.calls)
    }

    @Test fun unexpectedExpectedStateNeverReachesDispatcher() {
        val dispatcher = RecordingDispatcher(true)
        val result = adapter(dispatcher).execute(trustedPlan(expectedState = "OPENED"))

        assertFalse(result.success)
        assertEquals(0, dispatcher.calls)
    }

    @Test fun unavailableSemanticObservationPreventsDispatch() {
        val dispatcher = RecordingDispatcher(true)
        val semantic = FakeSemanticTaskObserver(available = false)
        val result = adapter(dispatcher, semanticObserver = semantic).execute(trustedPlan())

        assertFalse(result.success)
        assertFalse(result.verified)
        assertEquals(0, dispatcher.calls)
        assertEquals(1, semantic.canObserveCalls)
        assertEquals(0, semantic.armCalls)
        assertEquals(0, semantic.observeCalls)
    }

    @Test fun semanticObservationArmFailurePreventsDispatch() {
        val dispatcher = RecordingDispatcher(true)
        val semantic = FakeSemanticTaskObserver(armed = false)
        val result = adapter(dispatcher, semanticObserver = semantic).execute(trustedPlan())

        assertFalse(result.success)
        assertFalse(result.verified)
        assertEquals(0, dispatcher.calls)
        assertEquals(1, semantic.canObserveCalls)
        assertEquals(1, semantic.armCalls)
    }

    @Test fun unavailableObservationPreventsDispatch() {
        val dispatcher = RecordingDispatcher(true)
        val observer = FakeTargetAppObserver(available = false)
        val result = adapter(dispatcher, observer = observer).execute(trustedPlan())

        assertFalse(result.success)
        assertFalse(result.verified)
        assertEquals(0, dispatcher.calls)
        assertEquals(1, observer.canObserveCalls)
        assertEquals(0, observer.observeCalls)
    }

    @Test fun semanticObservationFailurePreventsVerifiedSuccess() {
        val dispatcher = RecordingDispatcher(true)
        val semantic = FakeSemanticTaskObserver(observed = false)
        val result = adapter(dispatcher, semanticObserver = semantic).execute(trustedPlan())

        assertTrue(result.success)
        assertFalse(result.verified)
        assertEquals(1, dispatcher.calls)
        assertEquals(1, semantic.armCalls)
        assertEquals(1, semantic.observeCalls)
        assertEquals(1, semantic.disarmCalls)
    }

    @Test fun successfulDispatchReturnsOnlyDispatchObservationUntilIndependentObservationCompletes() {
        val dispatcher = RecordingDispatcher(true)
        val observer = FakeTargetAppObserver(observed = false)
        val result = adapter(dispatcher, observer = observer).execute(trustedPlan())

        assertTrue(result.success)
        assertFalse(result.verified)
        assertEquals("LAUNCH_DISPATCHED", result.observedState)
        assertEquals("com.example.safe", dispatcher.lastPackage)
        assertEquals("com.example.safe", observer.lastPackage)
        assertEquals(1_000L, observer.lastDispatchStartedAtMillis)
        assertEquals(1, dispatcher.calls)
        assertEquals(1, observer.observeCalls)
    }

    @Test fun observationUsesTimestampSampledAfterDispatch() {
        val dispatcher = RecordingDispatcher(true)
        val observer = FakeTargetAppObserver(observed = true)
        var clockCalls = 0
        val result = adapter(
            dispatcher = dispatcher,
            observer = observer,
            clock = {
                clockCalls += 1
                if (clockCalls == 1) 1_000L else 2_000L
            }
        ).execute(trustedPlan())

        assertTrue(result.success)
        assertTrue(result.verified)
        assertEquals(2_000L, observer.lastDispatchStartedAtMillis)
        assertEquals(2, clockCalls)
    }

    @Test fun successfulDispatchAndIndependentObservationAreVerified() {
        val dispatcher = RecordingDispatcher(true)
        val observer = FakeTargetAppObserver(observed = true)
        val result = adapter(dispatcher, observer = observer).execute(trustedPlan())

        assertTrue(result.success)
        assertTrue(result.verified)
        assertEquals("LAUNCH_DISPATCHED", result.observedState)
        assertEquals(1, dispatcher.calls)
        assertEquals(1, observer.observeCalls)
    }

    @Test fun observerFailureCannotClaimVerifiedResult() {
        val dispatcher = RecordingDispatcher(true)
        val observer = object : AndroidTargetAppResultObserver {
            override fun canObserve(packageName: String): Boolean = true

            override fun observeForegroundAfterDispatch(
                packageName: String,
                dispatchStartedAtMillis: Long
            ): Boolean = error("observer failure")
        }

        val result = adapter(dispatcher, observer = observer).execute(trustedPlan())

        assertTrue(result.success)
        assertFalse(result.verified)
        assertEquals(1, dispatcher.calls)
    }

    @Test fun clockFailurePreventsDispatch() {
        val dispatcher = RecordingDispatcher(true)
        val result = adapter(dispatcher, clock = { error("clock failure") }).execute(trustedPlan())

        assertFalse(result.success)
        assertFalse(result.verified)
        assertEquals(0, dispatcher.calls)
    }

    @Test fun failedDispatchDoesNotReportSuccess() {
        val dispatcher = RecordingDispatcher(false)
        val result = adapter(dispatcher).execute(trustedPlan())

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
        assertEquals(null, registry.trustedCertificateSha256("com.example.unknown"))
    }

    @Test fun productionAdapterRejectsMissingCertificatePinBeforeDispatch() {
        val registry = trustedRegistry(pin = null)
        val dispatcher = RecordingDispatcher(true)
        val adapter = AndroidIntentActionAdapter(
            dispatcher = dispatcher,
            isTrustedPackage = AndroidPackageIdentityVerifier(
                registry = registry,
                certificateReader = FakeCertificateReader(listOf(certificateBytes))
            )::isTrusted,
            targetAppResultObserver = FakeTargetAppObserver(observed = true)
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
