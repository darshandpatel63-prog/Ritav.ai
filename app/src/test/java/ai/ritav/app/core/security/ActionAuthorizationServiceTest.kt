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
        service.issueDeviceAuthenticationToken(plan, "Authorize action", 1000L) { token = it }
        assertNull(token)
    }

    @Test fun deviceTokenIsMintedOnlyAfterSuccessfulAuthentication() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(available = true, result = true)
        )

        var token: String? = null
        service.issueDeviceAuthenticationToken(plan, "Authorize action", 1000L) { token = it }
        assertNotNull(token)
        assertEquals(true, gate.consume(token!!, plan, AuthorizationLevel.DEVICE_AUTHENTICATION, 1000L))
    }

    @Test fun failedDeviceAuthenticationCannotMintToken() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(available = true, result = false)
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "Authorize action", 1000L) { token = it }
        assertNull(token)
    }
}
