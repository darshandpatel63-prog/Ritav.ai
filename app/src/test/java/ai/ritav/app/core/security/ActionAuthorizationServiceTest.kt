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

        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway())

        assertNull(service.issueUserConfirmationToken(userConfirmationPlan, "wrong-hash"))
        assertNotNull(service.issueUserConfirmationToken(userConfirmationPlan, userConfirmationPlan.stableHash()))
    }

    @Test fun emergencyStopBlocksUserConfirmationTokenIssuance() {

        val emergencyStop = EmergencyStopController().apply { activate() }
        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway(),
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

        val emergencyStop = EmergencyStopController().apply { activate() }
        val gateway = StubDeviceAuthorizationGateway(available = true, result = true)
        val service = ActionAuthorizationService(gateway,
            emergencyStop = emergencyStop
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }

        assertNull(token)
    }

    @Test fun emergencyStopRecheckAfterAuthenticationPreventsDeviceTokenMinting() {

        val emergencyStop = EmergencyStopController()
        var authenticationCallback: ((Boolean) -> Unit)? = null
        val gateway = object : DeviceAuthorizationGateway {
            override fun isDeviceAuthenticationAvailable(): Boolean = true

            override fun authenticate(reason: String, callback: (success: Boolean) -> Unit) {
                authenticationCallback = callback
            }
        }
        val service = ActionAuthorizationService(gateway,
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

        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway())
        val highRisk = plan
        assertNull(service.issueUserConfirmationToken(highRisk, highRisk.stableHash()))
    }

    @Test fun deviceTokenCannotBeMintedWhenAuthenticationIsUnavailable() {

        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway(available = false, result = true)
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }
        assertNull(token)
    }

    @Test fun userConfirmationTokenTtlStartsFromServiceClock() {

        var now = 1_000L
        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway(),
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
            service.consume(
                token,
                userConfirmationPlan,
                AuthorizationLevel.USER_CONFIRMATION,
                61_000L
            )
        )

        now = 62_000L
        assertEquals(
            false,
            service.consume(
                token,
                userConfirmationPlan,
                AuthorizationLevel.USER_CONFIRMATION,
                now
            )
        )
    }

    @Test fun userConfirmationClockFailureFailsClosed() {

        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway(),
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

        var authenticationCallback: ((Boolean) -> Unit)? = null
        val gateway = object : DeviceAuthorizationGateway {
            override fun isDeviceAuthenticationAvailable(): Boolean = true

            override fun authenticate(reason: String, callback: (success: Boolean) -> Unit) {
                authenticationCallback = callback
            }
        }
        val service = ActionAuthorizationService(gateway, clockEpochMillis = { 1_000L })

        val tokens = mutableListOf<String?>()
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { tokens += it }
        authenticationCallback!!.invoke(true)
        authenticationCallback!!.invoke(true)

        assertEquals(1, tokens.size)
        assertNotNull(tokens.single())
    }

    @Test fun deviceAuthenticationFailureInPlatformGatewayFailsClosed() {

        val gateway = object : DeviceAuthorizationGateway {
            override fun isDeviceAuthenticationAvailable(): Boolean = true

            override fun authenticate(reason: String, callback: (success: Boolean) -> Unit) {
                error("platform authentication failure")
            }
        }
        val service = ActionAuthorizationService(gateway)

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }

        assertNull(token)
    }

    @Test fun deviceAuthenticationClockFailureFailsClosedAndCallsBack() {

        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway(available = true, result = true),
            clockEpochMillis = { error("clock failure") }
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }

        assertNull(token)
    }

    @Test fun deviceTokenIsMintedOnlyAfterSuccessfulAuthentication() {

        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway(available = true, result = true),
            clockEpochMillis = { 1000L }
        )

        var token: String? = null
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }
        assertNotNull(token)
        assertEquals(true, service.consume(token!!, plan, AuthorizationLevel.DEVICE_AUTHENTICATION, 1000L))
    }

    @Test fun deviceTokenTtlStartsFromPostAuthenticationClock() {

        var now = 1000L
        var authenticationCallback: ((Boolean) -> Unit)? = null
        val gateway = object : DeviceAuthorizationGateway {
            override fun isDeviceAuthenticationAvailable(): Boolean = true

            override fun authenticate(reason: String, callback: (success: Boolean) -> Unit) {
                authenticationCallback = callback
            }
        }
        val service = ActionAuthorizationService(gateway, clockEpochMillis = { now })

        var token: String? = null
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }
        assertNull(token)

        now = 120_000L
        authenticationCallback!!.invoke(true)
        assertNotNull(token)

        assertEquals(
            true,
            service.consume(token!!, plan, AuthorizationLevel.DEVICE_AUTHENTICATION, 120_000L + 60_000L)
        )
    }

    @Test fun malformedDeviceAuthorizationRequestFailsClosedWithoutThrowing() {

        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway(available = true, result = true)
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan.copy(expectedState = ""), "Authorize action") { token = it }

        assertNull(token)
    }

    @Test fun deviceAuthorizationRejectsWrongRiskTier() {

        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway(available = true, result = true)
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(userConfirmationPlan, "Authorize action") { token = it }

        assertNull(token)
    }

    @Test fun deviceAuthorizationRejectsInvalidPostAuthenticationClock() {

        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway(available = true, result = true),
            clockEpochMillis = { -1L }
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }

        assertNull(token)
    }

    @Test fun deviceAuthorizationBoundsAuthenticationReason() {

        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway(available = true, result = true)
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "x".repeat(513)) { token = it }

        assertNull(token)
    }

    @Test fun failedDeviceAuthenticationCannotMintToken() {

        val service = ActionAuthorizationService(StubDeviceAuthorizationGateway(available = true, result = false)
        )

        var token: String? = "unexpected"
        service.issueDeviceAuthenticationToken(plan, "Authorize action") { token = it }
        assertNull(token)
    }
}
