package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class ActionAuthorizationServiceTest {
    private val plan = ActionPlan(
        "demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE
    )

    @Test fun userConfirmationRequiresExactPlanHash() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(gate, StubDeviceAuthorizationGateway())

        assertNull(service.issueUserConfirmationToken(plan, "wrong-hash", 1000L))
        assertNotNull(service.issueUserConfirmationToken(plan, plan.stableHash(), 1000L))
    }

    @Test fun deviceTokenCannotBeMintedWhenAuthenticationIsUnavailable() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(available = false, result = true)
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }
        assertNull(token)
    }

    @Test fun deviceTokenIsMintedOnlyAfterSuccessfulAuthentication() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(available = true, result = true),
            clockEpochMillis = { 1000L }
        )

        var token: String? = null
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }
        assertNotNull(token)
        assertEquals(true, gate.consume(token!!, plan, AuthorizationLevel.DEVICE_AUTHENTICATION, 1000L))
    }

    @Test fun deviceTokenTtlStartsFromPostAuthenticationClock() {
        val gate = ActionAuthorizationGate()
        var now = 1000L
        var authenticationCallback: ((Boolean) -> Unit)? = null
        val gateway = object : DeviceAuthorizationGateway {
            override fun isDeviceAuthenticationAvailable(): Boolean = true

            override fun authenticate(reason: String, callback: (success: Boolean) -> Unit) {
                authenticationCallback = callback
            }
        }
        val service = ActionAuthorizationService(gate, gateway, clockEpochMillis = { now })

        var token: String? = null
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }
        assertNull(token)

        now = 120_000L
        authenticationCallback!!.invoke(true)
        assertNotNull(token)

        assertEquals(
            true,
            gate.consume(token!!, plan, AuthorizationLevel.DEVICE_AUTHENTICATION, 179_999L)
        )
        assertEquals(
            false,
            gate.consume(token!!, plan, AuthorizationLevel.DEVICE_AUTHENTICATION, 180_000L)
        )
    }

    @Test fun failedDeviceAuthenticationCannotMintToken() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(available = true, result = false)
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }
        assertNull(token)
    }
}
