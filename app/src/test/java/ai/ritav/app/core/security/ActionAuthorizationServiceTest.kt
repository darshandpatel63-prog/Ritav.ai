package ai.ritav.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class ActionAuthorizationServiceTest {
    private val plan = ActionPlan(
        "demo.app", Capability.APP_LAUNCH, "open", RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE, expectedState = "OPENED"
    )
    private val userConfirmationPlan = plan.copy(riskTier = RiskTier.TIER_2_CONTENT_MUTATION)

    @Test fun userConfirmationRequiresExactPlanHash() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(gate, StubDeviceAuthorizationGateway())

        assertNull(service.issueUserConfirmationToken(userConfirmationPlan, "wrong-hash"))
        assertNotNull(service.issueUserConfirmationToken(userConfirmationPlan, userConfirmationPlan.stableHash()))
    }

    @Test fun emergencyStopBlocksUserConfirmationTokenIssuance() {
        val gate = ActionAuthorizationGate()
        val emergencyStop = EmergencyStopController().apply { activate() }
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(),
            emergencyStop = emergencyStop
        )

        assertNull(
            service.issueUserConfirmationToken(
                userConfirmationPlan,
                userConfirmationPlan.stableHash()
            )
        )
    }

    @Test fun emergencyStopBlocksDeviceAuthenticationTokenIssuanceBeforeAuthentication() {
        val gate = ActionAuthorizationGate()
        val emergencyStop = EmergencyStopController().apply { activate() }
        val gateway = StubDeviceAuthorizationGateway(available = true, result = true)
        val service = ActionAuthorizationService(
            gate,
            gateway,
            emergencyStop = emergencyStop
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }

        assertNull(token)
    }

    @Test fun emergencyStopRecheckAfterAuthenticationPreventsDeviceTokenMinting() {
        val gate = ActionAuthorizationGate()
        val emergencyStop = EmergencyStopController()
        var authenticationCallback: ((Boolean) -> Unit)? = null
        val gateway = object : DeviceAuthorizationGateway {
            override fun isDeviceAuthenticationAvailable(): Boolean = true

            override fun authenticate(reason: String, callback: (success: Boolean) -> Unit) {
                authenticationCallback = callback
            }
        }
        val service = ActionAuthorizationService(
            gate,
            gateway,
            clockEpochMillis = { 1000L },
            emergencyStop = emergencyStop
        )

        var token: String? = null
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }
        emergencyStop.activate()
        authenticationCallback!!.invoke(true)

        assertNull(token)
    }

    @Test fun userConfirmationCannotAuthorizeHigherRiskPlan() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(gate, StubDeviceAuthorizationGateway())
        val highRisk = plan
        assertNull(service.issueUserConfirmationToken(highRisk, highRisk.stableHash()))
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

    @Test fun userConfirmationTokenTtlStartsFromServiceClock() {
        val gate = ActionAuthorizationGate()
        var now = 1_000L
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(),
            clockEpochMillis = { now }
        )

        val token = requireNotNull(
            service.issueUserConfirmationToken(
                userConfirmationPlan,
                userConfirmationPlan.stableHash()
            )
        )

        assertEquals(
            true,
            gate.consume(
                token,
                userConfirmationPlan,
                AuthorizationLevel.USER_CONFIRMATION,
                61_000L
            )
        )

        now = 62_000L
        assertEquals(
            false,
            gate.consume(
                token,
                userConfirmationPlan,
                AuthorizationLevel.USER_CONFIRMATION,
                now
            )
        )
    }

    @Test fun userConfirmationClockFailureFailsClosed() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(),
            clockEpochMillis = { error("clock failure") }
        )

        assertNull(
            service.issueUserConfirmationToken(
                userConfirmationPlan,
                userConfirmationPlan.stableHash()
            )
        )
    }

    @Test fun deviceAuthenticationCallbackCanMintOnlyOneToken() {
        val gate = ActionAuthorizationGate()
        var authenticationCallback: ((Boolean) -> Unit)? = null
        val gateway = object : DeviceAuthorizationGateway {
            override fun isDeviceAuthenticationAvailable(): Boolean = true

            override fun authenticate(reason: String, callback: (success: Boolean) -> Unit) {
                authenticationCallback = callback
            }
        }
        val service = ActionAuthorizationService(gate, gateway, clockEpochMillis = { 1_000L })

        val tokens = mutableListOf<String?>()
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { tokens += it }
        authenticationCallback!!.invoke(true)
        authenticationCallback!!.invoke(true)

        assertEquals(1, tokens.size)
        assertNotNull(tokens.single())
    }

    @Test fun deviceAuthenticationFailureInPlatformGatewayFailsClosed() {
        val gate = ActionAuthorizationGate()
        val gateway = object : DeviceAuthorizationGateway {
            override fun isDeviceAuthenticationAvailable(): Boolean = true

            override fun authenticate(reason: String, callback: (success: Boolean) -> Unit) {
                error("platform authentication failure")
            }
        }
        val service = ActionAuthorizationService(gate, gateway)

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }

        assertNull(token)
    }

    @Test fun deviceAuthenticationClockFailureFailsClosedAndCallsBack() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(available = true, result = true),
            clockEpochMillis = { error("clock failure") }
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
            gate.consume(token!!, plan, AuthorizationLevel.DEVICE_AUTHENTICATION, 120_000L + 60_000L)
        )
    }

    @Test fun malformedDeviceAuthorizationRequestFailsClosedWithoutThrowing() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(available = true, result = true)
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan.copy(expectedState = ""), "Authorize action") { token = it }

        assertNull(token)
    }

    @Test fun deviceAuthorizationRejectsWrongRiskTier() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(available = true, result = true)
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(userConfirmationPlan, "Authorize action") { token = it }

        assertNull(token)
    }

    @Test fun deviceAuthorizationRejectsInvalidPostAuthenticationClock() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(available = true, result = true),
            clockEpochMillis = { -1L }
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }

        assertNull(token)
    }

    @Test fun deviceAuthorizationBoundsAuthenticationReason() {
        val gate = ActionAuthorizationGate()
        val service = ActionAuthorizationService(
            gate,
            StubDeviceAuthorizationGateway(available = true, result = true)
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "x".repeat(513)) { token = it }

        assertNull(token)
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
