package ai.ritav.app.platform

import ai.ritav.app.core.security.ActionPlan
import ai.ritav.app.core.security.AppCapabilityRegistry
import ai.ritav.app.core.security.Capability
import ai.ritav.app.core.security.ExecutionResult
import ai.ritav.app.core.security.RiskTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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

    private fun trustedPlan(
        expectedState: String = "LAUNCH_DISPATCHED"
    ) = ActionPlan(
        appId = "com.example.safe",
        capability = Capability.APP_LAUNCH,
        action = "open",
        riskTier = RiskTier.TIER_1_REVERSIBLE,
        expectedState = expectedState
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

    @Test fun missingCertificatePinIsNotTrustedByProductionVerifierContract() {
        val registry = AppCapabilityRegistry(
            listOf(
                ai.ritav.app.core.security.AppCapabilitySpec(
                    packageName = "com.example.safe",
                    capability = Capability.APP_LAUNCH,
                    actions = setOf("open"),
                    riskTier = RiskTier.TIER_1_REVERSIBLE
                )
            )
        )
        val dispatcher = RecordingDispatcher(true)
        val adapter = AndroidIntentActionAdapter(
            dispatcher = dispatcher,
            isTrustedPackage = { registry.trustedCertificateSha256("com.example.safe") != null }
        )
        val result = adapter.execute(trustedPlan())

        assertFalse(result.success)
        assertEquals(0, dispatcher.calls)
    }
}
